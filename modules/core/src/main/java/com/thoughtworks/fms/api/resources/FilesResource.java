package com.thoughtworks.fms.api.resources;

import com.google.common.base.Splitter;
import com.thoughtworks.fms.api.Json;
import com.thoughtworks.fms.api.filter.SystemAuthentication;
import com.thoughtworks.fms.api.service.ClientService;
import com.thoughtworks.fms.api.service.FileService;
import com.thoughtworks.fms.api.service.SessionService;
import com.thoughtworks.fms.api.service.ValidationService;
import com.thoughtworks.fms.core.CipherUtils;
import com.thoughtworks.fms.core.FileMetadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Path("files")
public class FilesResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesResource.class);
    private final String CONVERTFILETYPE = "pdf,jpg,jpeg,font,gif,png,wav";
    private final String imageType="jpg,jpeg,png,gif";
    private final String BASE_ENCODE = "89601CD4D2A12A979D1E284DE53E3562";//32位随机数
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(FormDataMultiPart multiPart,
                               @FormDataParam("file") FormDataContentDisposition metadata,
                               @Context ServerProperties properties,
                               @Context HttpServletRequest servletRequest,
                               @Context FileService fileService,
                               @Context ValidationService validationService,
                               @Context ClientService clientService,
                               @Context SessionService sessionService) throws UnsupportedEncodingException {
        FormDataBodyPart field = multiPart.getField("upload_token");
        validationService.ensureUploadTokenValid(field);

        Map attribute = Json.parseJson(sessionService.getAttribute(servletRequest,
                properties.getUploadTokenKey(field.getValueAs(String.class))).toString(), Map.class);
        sessionService.removeAttribute(servletRequest, properties.getUploadTokenKey(field.getValueAs(String.class)));

        String destName = attribute.get("fileName").toString();
        String uri = attribute.get("uri").toString();

        String sourceName = new String(metadata.getFileName().getBytes("ISO-8859-1"));
        InputStream inputStream = multiPart.getField("file").getValueAs(InputStream.class);
        long fileId = fileService.store(sourceName, destName, inputStream);
        clientService.informUms(uri, fileId, sourceName);

        return Response.created(Routing.file(fileId)).build();
    }

    /**
     * 上传文件 涉及多线程 内存开销比较大 后期并发上来后 需要优化
     * @param multiPart
     * @param metadata
     * @param properties
     * @param servletRequest
     * @param fileService
     * @param validationService
     * @param clientService
     * @param sessionService
     * @return
     * @throws UnsupportedEncodingException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/uploadFileForCredit")
    public String uploadFileForCredit(FormDataMultiPart multiPart,
                                    @FormDataParam("file") FormDataContentDisposition metadata,
                                    @Context ServerProperties properties,
                                    @Context HttpServletRequest servletRequest,
                                    @Context FileService fileService,
                                    @Context ValidationService validationService,
                                    @Context ClientService clientService,
                                    @Context SessionService sessionService) throws UnsupportedEncodingException {

        String destName = new String(metadata.getFileName().getBytes("ISO-8859-1"));
        String source = servletRequest.getParameter("source");
        String sourceName = new String(metadata.getFileName().getBytes("ISO-8859-1"));
        //这里将文件实例化为两个流，一个用作oss上传一个用作文件转换，感觉写法有点尴尬，目前没有什么比较好的处理办法
        InputStream inputStream = multiPart.getField("file").getValueAs(InputStream.class);
        InputStream inputStreamForUpload = multiPart.getField("file").getValueAs(InputStream.class);
        //上传的文件用另一个流进行处理在服务器上生成一个文件
        String newFilePath = fileService.saveUploadFileForView(inputStream, destName);
        LOGGER.info("服务文件生成路径=" + newFilePath);
        String fileExtensionName= FilenameUtils.getExtension(newFilePath);
        //图片压缩处理（处理完后再对压缩后的文件进行处理时会有问题）
        String compressFile="";
        if(imageType.indexOf(fileExtensionName)>-1) {
            compressFile= fileService.compressImage(newFilePath, FilenameUtils.getBaseName(newFilePath));
        }
        //对图片进行压缩处理
        long fileId;
        String url ="";
        try {
            if ("" != newFilePath) {
                if (CONVERTFILETYPE.indexOf(fileExtensionName)>-1) {
                    LOGGER.info("除pdf格式外的文件开始执行转换");
                    File newFile;
                    if(null!=compressFile) {
                        newFile = new File(compressFile);
                        url = fileService.convertForView(newFile);
                        LOGGER.info("转换压缩过后的文件成功！");
                        newFile.delete();
                    }
                   if("".equals(url)){
                       LOGGER.info("转换压缩过后的文件失败，正在尝试通过源文件转换！");
                       newFile = new File(newFilePath);
                       url = fileService.convertForView(newFile);
                       newFile.delete();
                   }

                } else {
                    LOGGER.info("开始执行转换");
                    Map<String, Object> fileMap = fileService.doc2swf(newFilePath);
                    if (fileMap.containsKey("docFile")) {
                        File docFile = (File) fileMap.get("docFile");
                        docFile.delete();
                        LOGGER.info("doc文件成功生成=" + docFile);
                    }
                    if (fileMap.containsKey("pdfFile")) {
                        File pdfFile = (File) fileMap.get("pdfFile");
                        url=   fileService.convertForView(pdfFile);
                        pdfFile.delete();
                        LOGGER.info("pdf文件成功生成，并且转换成swf文件成功=" + pdfFile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //文件上传到oss
           fileId = fileService.storeForCredit(sourceName, destName, inputStreamForUpload, source,url);
  //              fileService.storeForCredit(sourceName, destName, inputStream, source);
//        String url= fileService.getUrl(destName);
            //credit固定路径
            String uri = "/creditAttachment/saveCreditAttachmentByFileId";
            clientService.informCredit(uri, null!=url&&""!=url?Long.valueOf(url):0, sourceName, destName);
        }
        return url;
    }

    /**
     * credit下载原始代码
     * @param properties
     * @param servletRequest
     * @param fileService
     * @param validationService
     * @param sessionService
     * @return
     * @throws UnsupportedEncodingException
     */
//    @GET
//    @Path("/downloadFileForCredit")
//    public Response downloadFileForCredit(@Context ServerProperties properties,
//                                          @Context HttpServletRequest servletRequest,
//                                          @Context FileService fileService,
//                                          @Context ValidationService validationService,
//                                          @Context SessionService sessionService) throws UnsupportedEncodingException {
//        Object fileIds = servletRequest.getParameter("fileIds");
//        Object fileName = servletRequest.getParameter("fileName");
////        String fileIds = sessionService.getAttribute(servletRequest,"fileIds".toString()).toString();
////        String fileName = sessionService.getAttribute(servletRequest,"fileName".toString()).toString();
//        String userAgent = servletRequest.getHeader("User-Agent");
//        return getResponseForCredit(fileService, fileIds.toString(), fileName.toString(), userAgent);
//    }

    @GET
    @Path("/downloadFileForCredit")
    public String downloadFileForCredit(@Context ServerProperties properties,
                                          @Context HttpServletRequest servletRequest,
                                          @Context FileService fileService,
                                          @Context ValidationService validationService,
                                          @Context SessionService sessionService) throws UnsupportedEncodingException {
        Object fileIds = servletRequest.getParameter("fileIds");
        Object fileName = servletRequest.getParameter("fileName");
//        String fileIds = sessionService.getAttribute(servletRequest,"fileIds".toString()).toString();
//        String fileName = sessionService.getAttribute(servletRequest,"fileName".toString()).toString();
        String userAgent = servletRequest.getHeader("User-Agent");
        if(null==fileIds){
            return "";
        }
        FileMetadata fileMetadata= fileService.findMetadataById(Long.valueOf(fileIds.toString()));
        if (null!=fileMetadata&&null!=fileMetadata.getSwfFileName()&&!"".equals(fileMetadata.getSwfFileName())){
                return fileMetadata.getSwfFileName();
        }
        //将文件下载到服务器进行处理
        String swfUrl= getSwfUrlForCredit(fileService, fileIds.toString(), fileName.toString(), userAgent);
        String url ="";
        String fileExtensionName= FilenameUtils.getExtension(swfUrl);
        try {
            LOGGER.info("（下载）文件转换开始");
            if ("" != swfUrl) {
                if (CONVERTFILETYPE.indexOf(fileExtensionName)>-1) {
                    LOGGER.info("除pdf格式外的文件开始执行转换");
                    File newFile = new File(swfUrl);
                    url=  fileService.convertForView(newFile);
                    newFile.delete();
                } else {
                    LOGGER.info("（下载）开始执行转换");
                    Map<String, Object> fileMap = fileService.doc2swf(swfUrl);
                    if (fileMap.containsKey("docFile")) {
                        File docFile = (File) fileMap.get("docFile");
                        docFile.delete();
                        LOGGER.info("doc文件成功生成=" + docFile);
                    }
                    if (fileMap.containsKey("pdfFile")) {
                        File pdfFile = (File) fileMap.get("pdfFile");
                        url=   fileService.convertForView(pdfFile);
                        pdfFile.delete();
                        LOGGER.info("（下载）pdf文件成功生成，并且转换成swf文件成功=" + pdfFile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }



    @GET
    @Path("{type}")
    public Response downloadFile(@PathParam("type") String type,
                                 @QueryParam("download_token") String token,
                                 @Context ServerProperties properties,
                                 @Context HttpServletRequest servletRequest,
                                 @Context FileService fileService,
                                 @Context ValidationService validationService,
                                 @Context SessionService sessionService) throws UnsupportedEncodingException {
        validationService.ensureDownloadTokenValid(token);

        Map attribute = Json.parseJson(sessionService.getAttribute(servletRequest,
                properties.getDownloadTokenKey(token)).toString(), Map.class);
        sessionService.removeAttribute(servletRequest, properties.getDownloadTokenKey(token));

        String fileIds = attribute.get("fileIds").toString();
        String fileName = attribute.get("fileName").toString();
        String userAgent = servletRequest.getHeader("User-Agent");
        return getResponse(fileService, fileIds, fileName, userAgent);
    }


    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @SystemAuthentication(system = "midas")
    public Response downloadFiles(@QueryParam("fileIds") String fileIds,
                                  @Context ContainerRequestContext context,
                                  @Context HttpServletRequest servletRequest,
                                  @Context FileService fileService) throws UnsupportedEncodingException {
        String fileName = UUID.randomUUID().toString();
        String userAgent = servletRequest.getHeader("User-Agent");
        return getResponse(fileService, fileIds, fileName, userAgent);
    }

    private Response getResponse(FileService fileService, String fileIds, String fileName, String userAgent) throws UnsupportedEncodingException {
        List<Long> fileIdsList = Splitter.on(",").splitToList(fileIds)
                .stream().map(Long::valueOf).collect(toList());

        final File zipFile = fileService.fetch(fileIdsList, fileName);
        StreamingOutput streamingOutput = output -> {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile))) {
                IOUtils.copy(inputStream, output);
            } finally {
                zipFile.delete();
            }
        };

        return Response
                .ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", getContentDispositionFileName(userAgent, zipFile.getName()))
                .build();
    }

    private Response getResponseForCredit(FileService fileService, String fileIds, String fileName, String userAgent) throws UnsupportedEncodingException {
        List<Long> fileIdsList = Splitter.on(",").splitToList(fileIds)
                .stream().map(Long::valueOf).collect(toList());
        final File zipFile = fileService.fetchForCredit(fileIdsList, fileName);
        StreamingOutput streamingOutput = output -> {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile))) {
                IOUtils.copy(inputStream, output);
            } finally {
                zipFile.delete();
            }
        };
        return Response
                .ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", getContentDispositionFileName(userAgent, zipFile.getName()))
                .build();
    }

    private String getSwfUrlForCredit(FileService fileService, String fileIds, String fileName, String userAgent) throws UnsupportedEncodingException {
        List<Long> fileIdsList = Splitter.on(",").splitToList(fileIds)
                .stream().map(Long::valueOf).collect(toList());
        final File swfFile = fileService.fetchForCredit(fileIdsList, fileName);
       return swfFile.getPath();
    }

    @POST
    @Path("/uploadFileForZjfCredit")
    public String uploadFileForZjf(@Context HttpServletRequest servletRequest,
                                   @Context ContainerRequestContext context,
                                   @Context FileService fileService,
                                   @Context ClientService clientService) throws IOException {

        String destName = context.getHeaderString("FileName");
        String midasSystem = context.getHeaderString("MIDAS_SYSTEM");
        String time = context.getHeaderString("MIDAS_DATE");

        InputStream initStream = servletRequest.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = initStream.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        //check parameters
        InputStream checkStream = new ByteArrayInputStream(baos.toByteArray());
        checkParametersBeforeUpload(checkStream, destName, midasSystem, time);

        //compress file
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        InputStream uploadStream = new ByteArrayInputStream(baos.toByteArray());

        String source = "zjf_app";
        String sourceName = new String(destName.getBytes("ISO-8859-1"));

        String newFilePath = fileService.saveUploadFileForView(inputStream, destName);
        LOGGER.info("服务文件生成路径=" + newFilePath);
        String fileExtensionName= FilenameUtils.getExtension(newFilePath);
        String compressFile="", url="";
        if(imageType.indexOf(fileExtensionName)>-1) {
            compressFile= fileService.compressImage(newFilePath, FilenameUtils.getBaseName(newFilePath));
        }
        File newFile;
        if(null!=compressFile) {
            newFile = new File(compressFile);
            url = fileService.convertForView(newFile);
            uploadStream = new FileInputStream(newFile);
            newFile.delete();
        }

        long fileId = fileService.storeForCredit(sourceName, destName, uploadStream, source, url);
        //回调
        String uri = "/creditAttachment/saveCreditAttachmentByFileId";
        clientService.informCredit(uri, null!=url&&""!=url?Long.valueOf(url):0, destName, sourceName);
        if("".equals(url)){
            url=fileId+"";
        }
        return url;
    }

    @POST
    @Path("/findFileForZjfCredit")
    public Response findFileForZjfCredit(@Context FileService fileService,
                                         @Context ContainerRequestContext context,
                                         @Context HttpServletRequest servletRequest) throws UnsupportedEncodingException {
        //
        String fileIds = servletRequest.getParameter("fileIds");
        String fileName = servletRequest.getParameter("fileName");
        String userAgent = servletRequest.getHeader("User-Agent");
        List<String> fileIdsList = Splitter.on(",").splitToList(fileIds)
                .stream().collect(toList());
        final File zipFile = fileService.fetchForCreditBySwf(fileIdsList, fileName);

        StreamingOutput streamingOutput = output -> {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile))) {
                IOUtils.copy(inputStream, output);
            } finally {
                zipFile.delete();
            }
        };

        return  Response
                .ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", getContentDispositionFileName(userAgent, zipFile.getName()))
                .build();
    }

    private String getContentDispositionFileName(String userAgent, String fileName) throws UnsupportedEncodingException {
        LOGGER.debug("userAgent=" + userAgent);
        if (userAgent.indexOf("MSIE") != -1) {
            return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
        } else {
            return "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8");
        }
    }

    private void checkParametersBeforeUpload(InputStream checkStream, String destName, String midasSystem, String time) throws IOException {
        if (stringCheck(destName) || stringCheck(midasSystem) || stringCheck(time)){
            throw new RuntimeException("request parameter is empty");
        }

        byte[] bytes = new byte[checkStream.available()];
        checkStream.read(bytes);

        Integer num = Integer.valueOf(time.substring(0, 3)) + Integer.valueOf(time.substring(time.length()-3));
        String encryptStr = CipherUtils.SHAEncode(BASE_ENCODE + destName + time + new String(bytes).substring(0, num));

        if(encryptStr==null || !encryptStr.equals(midasSystem)){
            throw new RuntimeException("文件比对失败!");
        }
    }

    private boolean stringCheck(String str){
        if(str==null || "".equals(str)){
            return true;
        }
        return false;
    }
}
