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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Path("files")
public class FilesResource {

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

    @GET
    @Path("{type}")
    public Response downloadFile(@PathParam("type") String type,
                                 @QueryParam("download_token") String token,
                                 @Context ServerProperties properties,
                                 @Context HttpServletRequest servletRequest,
                                 @Context FileService fileService,
                                 @Context ValidationService validationService,
                                 @Context SessionService sessionService) {
        validationService.ensureDownloadTokenValid(token);

        Map attribute = Json.parseJson(sessionService.getAttribute(servletRequest,
                properties.getDownloadTokenKey(token)).toString(), Map.class);
        sessionService.removeAttribute(servletRequest, properties.getDownloadTokenKey(token));

        String fileIds = attribute.get("fileIds").toString();
        return getResponse(fileService, fileIds);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @SystemAuthentication(system = "midas")
    public Response downloadFiles(@QueryParam("fileIds") String fileIds,
                                  @Context ContainerRequestContext context,
                                  @Context HttpServletRequest servletRequest,
                                  @Context FileService fileService) {
        return getResponse(fileService, fileIds);
    }

    private Response getResponse(FileService fileService, String fileIds) {
        List<Long> fileIdsList = Splitter.on(",").splitToList(fileIds)
                .stream().map(Long::valueOf).collect(toList());

        final File zipFile = fileService.fetch(fileIdsList);
        StreamingOutput streamingOutput = output -> {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile))) {
                IOUtils.copy(inputStream, output);
            } finally {
                zipFile.delete();
            }
        };

        return Response
                .ok(streamingOutput, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=" + zipFile.getName())
                .build();
    }

}
