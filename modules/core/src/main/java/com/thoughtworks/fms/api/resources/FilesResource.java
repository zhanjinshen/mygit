package com.thoughtworks.fms.api.resources;

import com.artofsolving.jodconverter.BasicDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import com.google.common.base.Splitter;
import com.sun.star.io.ConnectException;
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
import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

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
        //文件上传到oss
        long fileId = fileService.storeForCredit(sourceName, destName, inputStream,source);
//        String url= fileService.getUrl(destName);
        //credit固定路径
        String uri ="/creditAttachment/saveCreditAttachmentByFileId";
        clientService.informCredit(uri, fileId, sourceName, destName);


        String fi ="E:\\testin\\111.doc";
        String fo ="E:\\testout\\testaa.pdf";
        File inputFile = new File(fi);
        File outputFile = new File(fo);
        String OpenOffice_HOME = "C:\\Program Files (x86)\\OpenOffice 4";// 这里是OpenOffice的安装目录,
        // 在我的项目中,为了便于拓展接口,没有直接写成这个样子,但是这样是尽对没题目的
        // 假如从文件中读取的URL地址最后一个字符不是 '\'，则添加'\'
        if (OpenOffice_HOME.charAt(OpenOffice_HOME.length() - 1) != '/') {
            OpenOffice_HOME += "/";
        }
        Process pro = null;
        try {
            // 启动OpenOffice的服务
            String command = OpenOffice_HOME
                    + "program/soffice.exe -headless -accept=\"socket,host=127.0.0.1,port=8100;urp;\"";
            pro = Runtime.getRuntime().exec(command);
            // connect to an OpenOffice.org instance running on port 8100
            OpenOfficeConnection connection = new SocketOpenOfficeConnection("127.0.0.1", 8100);
            connection.connect();

            // convert
            DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
            converter.convert(inputFile,outputFile);

            // close the connection
            connection.disconnect();
            // 封闭OpenOffice服务的进程
            pro.destroy();

            return 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pro.destroy();
        }

//        return 1;








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

//    public void convert(File sourceFile, File targetFile) {
//
//        try {
//            // 1: 打开连接
//            OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
//            connection.connect();
//
//            DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
//            // 2:获取Format
//            DocumentFormatRegistry factory = new BasicDocumentFormatRegistry();
//            DocumentFormat inputDocumentFormat = factory
//                    .getFormatByFileExtension(getExtensionName(sourceFile.getAbsolutePath()));
//            DocumentFormat outputDocumentFormat = factory
//                    .getFormatByFileExtension(getExtensionName(targetFile.getAbsolutePath()));
//            // 3:执行转换
//            converter.convert(sourceFile, inputDocumentFormat, targetFile, outputDocumentFormat);
//        } catch (ConnectException e) {
//            LOGGER.info("文档转换PDF失败");
//        }
//    }


    public void convert(String input, String output){
        File inputFile = new File(input);
        File outputFile = new File(output);
        OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
        try {
            connection.connect();
            DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
            converter.convert(inputFile, outputFile);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try{ if(connection != null){connection.disconnect(); connection = null;}}catch(Exception e){}
        }
    }

//    public static int office2PDF(String sourceFile, String destFile) {
//
//        File inputFile = new File(sourceFile);
//        File outputFile = new File(destFile);
//        String OpenOffice_HOME = "C:\\Program Files (x86)\\OpenOffice 4\\program";// 这里是OpenOffice的安装目录,
//        // 在我的项目中,为了便于拓展接口,没有直接写成这个样子,但是这样是尽对没题目的
//        // 假如从文件中读取的URL地址最后一个字符不是 '\'，则添加'\'
//        if (OpenOffice_HOME.charAt(OpenOffice_HOME.length() - 1) != '/') {
//            OpenOffice_HOME += "/";
//        }
//        Process pro = null;
//        try {
//            // 启动OpenOffice的服务
//            String command = OpenOffice_HOME
//                    + "program/soffice.exe -headless -accept=\"socket,host=127.0.0.1,port=8100;urp;\"";
//            pro = Runtime.getRuntime().exec(command);
//            // connect to an OpenOffice.org instance running on port 8100
//            OpenOfficeConnection connection = new SocketOpenOfficeConnection("127.0.0.1", 8100);
//            connection.connect();
//
//            // convert
//            DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
//            converter.convert(inputFile, outputFile);
//
//            // close the connection
//            connection.disconnect();
//            // 封闭OpenOffice服务的进程
//            pro.destroy();
//
//            return 0;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return -1;
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            pro.destroy();
//        }
//
//        return 1;
//    }

}
