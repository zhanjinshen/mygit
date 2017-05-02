package com.thoughtworks.fms.api.resources;

import com.google.common.base.Splitter;
import com.thoughtworks.fms.api.Json;
import com.thoughtworks.fms.api.filter.SystemAuthentication;
import com.thoughtworks.fms.api.service.ClientService;
import com.thoughtworks.fms.api.service.FileService;
import com.thoughtworks.fms.api.service.SessionService;
import com.thoughtworks.fms.api.service.ValidationService;
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



    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/uploadFileForCredit")
    public long uploadFileForCredit(FormDataMultiPart multiPart,
                                    @FormDataParam("file") FormDataContentDisposition metadata,
                                    @Context ServerProperties properties,
                                    @Context HttpServletRequest servletRequest,
                                    @Context FileService fileService,
                                    @Context ValidationService validationService,
                                    @Context ClientService clientService,
                                    @Context SessionService sessionService) throws UnsupportedEncodingException {

        String destName = new String(metadata.getFileName().getBytes("ISO-8859-1"));
        String source= servletRequest.getParameter("source");
        String sourceName = new String(metadata.getFileName().getBytes("ISO-8859-1"));
        InputStream inputStream = multiPart.getField("file").getValueAs(InputStream.class);
       String newFilePath= fileService.saveUploadFileForView(inputStream,destName);
        long fileId;
            try {
                if(""!=newFilePath){
              Map<String,Object> fileMap=  fileService.doc2swf(newFilePath);
                    if(fileMap.containsKey("docFile")){
                        File docFile=(File)fileMap.get("docFile");
                        docFile.delete();
                    }
                    if(fileMap.containsKey("pdfFile")){
                        File pdfFile=(File)fileMap.get("pdfFile");
                        fileService.convertForView(pdfFile);
                        pdfFile.delete();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //文件上传到oss
              fileId = fileService.storeForCredit(sourceName, destName, inputStream,source);
//        String url= fileService.getUrl(destName);
                //credit固定路径
                String uri ="/creditAttachment/saveCreditAttachmentByFileId";
                clientService.informCredit(uri, fileId, sourceName, destName);
            }

        //将上传的文件进行备份用作预览处理

        return fileId;
    }

    @GET
    @Path("/downloadFileForCredit")
    public Response downloadFileForCredit(@Context ServerProperties properties,
                                 @Context HttpServletRequest servletRequest,
                                 @Context FileService fileService,
                                 @Context ValidationService validationService,
                                 @Context SessionService sessionService) throws UnsupportedEncodingException {
    	Object fileIds =servletRequest.getParameter("fileIds");
    	Object fileName =servletRequest.getParameter("fileName");
//        String fileIds = sessionService.getAttribute(servletRequest,"fileIds".toString()).toString();
//        String fileName = sessionService.getAttribute(servletRequest,"fileName".toString()).toString();
        String userAgent = servletRequest.getHeader("User-Agent");
        return getResponseForCredit(fileService, fileIds.toString(), fileName.toString(), userAgent);
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

    private String getContentDispositionFileName(String userAgent, String fileName) throws UnsupportedEncodingException {
        LOGGER.debug("userAgent=" + userAgent);
        if (userAgent.indexOf("MSIE") != -1) {
            return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
        } else {
            return "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8");
        }

    }


}
