package com.thoughtworks.fms.api;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.thoughtworks.fms.ResourceRunner;
import com.thoughtworks.fms.ResourceTest;
import com.thoughtworks.fms.api.service.SessionService;
import com.thoughtworks.fms.api.service.ValidationService;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.FileMetadata;
import com.thoughtworks.fms.core.FileRepository;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import com.thoughtworks.fms.core.mybatis.util.FileBuilder;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.core.mybatis.util.RestClient;
import com.thoughtworks.fms.exception.EncryptionException;
import com.thoughtworks.fms.exception.TransferException;
import org.apache.commons.io.FileUtils;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(ResourceRunner.class)
public class FilesResourceTest extends ResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesResourceTest.class);
    private static final String TEST_FILE = "test1.png";
    private static final String TEST_FILE2 = "test.png";

    @Inject
    private ValidationService validationService;

    @Inject
    private SessionService sessionService;

    @Inject
    private FileRepository repository;

    @Inject
    private Transfer transfer;

    @Inject
    private Cipher cipher;

    @Test
    public void should_upload_file_successful() {
        doNothing().when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        File testFile = loadTestFile(TEST_FILE);
        WebTarget webTarget = getPathWebTarget();
        MultiPart entity = getMultiPart(testFile, true);
        Response response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.CREATED_201.getStatusCode()));

        long fileId = Long.valueOf(response.getLocation().getPath().replaceAll(".*/(\\d+)", "$1"));
        FileMetadata metadata = repository.findMetadataById(fileId);

        assertThat(metadata.getId(), greaterThan(0L));
        assertThat(metadata.getSuffix(), is(".png"));
        assertThat(metadata.getDestName().length(), greaterThan(0));
    }

    @Test
    public void should_upload_file_fail_when_token_invalid() {
        doThrow(new InvalidRequestException(FMSErrorCode.FILE_TOKEN_INVALID))
                .when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        WebTarget webTarget = getPathWebTarget();
        MultiPart entity = getMultiPart(loadTestFile(TEST_FILE), true);
        Response response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.BAD_REQUEST_400.getStatusCode()));

        Map result = response.readEntity(Map.class);
        assertThat(result.get("code"), is("FILE_TOKEN_INVALID"));
    }

    @Test
    public void should_upload_file_fail_when_suffix_not_accept() {
        doNothing().when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        WebTarget webTarget = getPathWebTarget();
        MultiPart entity = getMultiPart(loadTestFile("test.html"), true);
        Response response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.BAD_REQUEST_400.getStatusCode()));

        Map result = response.readEntity(Map.class);
        assertThat(result.get("code"), is("FILE_EXTENSION_NOT_ACCEPT"));
        assertThat(result.get("message"), is("不允许该类型的文件上传"));
    }

    @Test
    public void should_upload_file_fail_when_token_missing() {
        doThrow(new InvalidRequestException(FMSErrorCode.FILE_TOKEN_MISSING))
                .when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        WebTarget webTarget = getPathWebTarget();
        MultiPart entity = getMultiPart(loadTestFile(TEST_FILE), false);
        Response response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.BAD_REQUEST_400.getStatusCode()));

        Map result = response.readEntity(Map.class);
        assertThat(result.get("code"), is("FILE_TOKEN_MISSING"));
    }

    @Test
    public void should_download_file_successful() throws TransferException, IOException, EncryptionException {
        doNothing().when(validationService).ensureUploadTokenValid(any());
        doNothing().when(validationService).ensureDownloadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        WebTarget uploadWebTarget = getPathWebTarget();
        File testFile = loadTestFile(TEST_FILE);
        MultiPart entity = getMultiPart(testFile, true);
        Response uploadResponse = uploadWebTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(uploadResponse.getStatus(), is(HttpStatus.CREATED_201.getStatusCode()));

        long fileId = Long.valueOf(uploadResponse.getLocation().getPath().replaceAll(".*/(\\d+)", "$1"));

        Mockito.reset(sessionService);
        when(sessionService.getAttribute(any(), any())).thenReturn(validDownloadSessionAttribute(fileId));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cipher.encrypt(new FileInputStream(testFile), outputStream);
        when(transfer.read(any())).thenReturn(new ByteArrayInputStream(outputStream.toByteArray()));

        WebTarget downloadWebTarget = uploadWebTarget.path("type").queryParam("download_token", "hello");
        Response downloadResponse = downloadWebTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .get();

        String fileName = downloadResponse.getHeaderString("content-disposition").replaceAll(".*=(.*)", "$1");
        InputStream inputStream = downloadResponse.readEntity(InputStream.class);

        File file = new FileBuilder(fileName).withContent(inputStream).getFile();
        assertThat(downloadResponse.getStatus(), is(HttpStatus.OK_200.getStatusCode()));
        assertThat(file.length(), greaterThan(0L));
    }

    @Test
    public void should_download_file_fail_when_token_not_exist() throws TransferException, FileNotFoundException, EncryptionException {
        doNothing().when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        WebTarget uploadWebTarget = getPathWebTarget();
        File testFile = loadTestFile(TEST_FILE);
        MultiPart entity = getMultiPart(testFile, true);
        Response uploadResponse = uploadWebTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entity, entity.getMediaType()));

        assertThat(uploadResponse.getStatus(), is(HttpStatus.CREATED_201.getStatusCode()));

        long fileId = Long.valueOf(uploadResponse.getLocation().getPath().replaceAll(".*/(\\d+)", "$1"));

        Mockito.reset(sessionService);
        when(sessionService.getAttribute(any(), any())).thenReturn(validDownloadSessionAttribute(fileId));
        doThrow(new InvalidRequestException(FMSErrorCode.FILE_TOKEN_MISSING))
                .when(validationService).ensureDownloadTokenValid(any());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cipher.encrypt(new FileInputStream(testFile), outputStream);
        when(transfer.read(any())).thenReturn(new ByteArrayInputStream(outputStream.toByteArray()));

        WebTarget downloadWebTarget = uploadWebTarget.path(fileId + "").queryParam("download_token", "");
        Response downloadResponse = downloadWebTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .get();

        assertThat(downloadResponse.getStatus(), is(HttpStatus.BAD_REQUEST_400.getStatusCode()));
        Map result = downloadResponse.readEntity(Map.class);
        assertThat(result.get("code"), is("FILE_TOKEN_MISSING"));
    }

    @Test
    public void should_download_files_successful() throws FileNotFoundException, EncryptionException, TransferException {
        doNothing().when(validationService).ensureUploadTokenValid(any());
        when(sessionService.getAttribute(any(), any())).thenReturn(validUploadSessionAttribute());

        File testFileOne = loadTestFile(TEST_FILE);
        WebTarget webTarget = getPathWebTarget();
        MultiPart entityOne = getMultiPart(testFileOne, true);
        Response response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entityOne, entityOne.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.CREATED_201.getStatusCode()));
        long fileIdOne = Long.valueOf(response.getLocation().getPath().replaceAll(".*/(\\d+)", "$1"));

        File testFileTwo = loadTestFile(TEST_FILE2);
        MultiPart entityTwo = getMultiPart(testFileTwo, true);
        response = webTarget.request()
                .cookie("_csrf", "csrf_token")
                .header("CSRF-TOKEN", "csrf_token")
                .post(Entity.entity(entityTwo, entityTwo.getMediaType()));

        assertThat(response.getStatus(), is(HttpStatus.CREATED_201.getStatusCode()));
        long fileIdTwo = Long.valueOf(response.getLocation().getPath().replaceAll(".*/(\\d+)", "$1"));

        String join = Joiner.on(",").join(fileIdOne, fileIdTwo);
        String url = getBaseUri().toASCIIString() + "/files?fileIds=" + join;

        RestClient restClient = new RestClient();

        ByteArrayOutputStream outputStreamOne = new ByteArrayOutputStream();
        cipher.encrypt(new FileInputStream(testFileOne), outputStreamOne);

        ByteArrayOutputStream outputStreamTwo = new ByteArrayOutputStream();
        cipher.encrypt(new FileInputStream(testFileTwo), outputStreamTwo);

        when(transfer.read(any())).thenReturn(new ByteArrayInputStream(outputStreamOne.toByteArray()))
                .thenReturn(new ByteArrayInputStream(outputStreamTwo.toByteArray()));

        restClient.get(url, "midas", PropertiesLoader.getProperty("midas.secret"), response1 -> {
            assertThat(response1.getStatus(), is(HttpStatus.OK_200.getStatusCode()));
            String fileName = response1.getHeaderString("content-disposition").replaceAll(".*=(.*)", "$1");
            InputStream inputStream = response1.readEntity(InputStream.class);

            File file = new FileBuilder(fileName).withContent(inputStream).getFile();

            assertThat(file.length(), greaterThan(0L));

            file.delete();
            return null;
        });
    }

    private WebTarget getPathWebTarget() {
        return ClientBuilder.newBuilder()
                .register(MultiPartFeature.class)
                .build()
                .target(getBaseUri().toASCIIString())
                .path("files");
    }

    private MultiPart getMultiPart(File file, boolean containToken) {
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        if (containToken) {
            formDataMultiPart.field("upload_token", "xfdsfsdfds");
        }

        FileDataBodyPart filePart = new FileDataBodyPart("file", file,
                MediaType.MULTIPART_FORM_DATA_TYPE);

        return formDataMultiPart.bodyPart(filePart);
    }

    private File loadTestFile(String testFile) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(testFile)) {
            byte[] bytes = ByteStreams.toByteArray(inputStream);

            FileBuilder builder = new FileBuilder(testFile);
            File file = builder.withContent(bytes).build();
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    private String validUploadSessionAttribute() {
        Map<String, Object> attribute = new HashMap<>();
        attribute.put("fileName", "hello");
        attribute.put("uri", "/users/1/xx");
        return Json.toJson(attribute);
    }

    private String validDownloadSessionAttribute(Long... fileIds) {
        Map<String, Object> attribute = new HashMap<>();
        String join = Joiner.on(",").join(Arrays.asList(fileIds));

        attribute.put("fileIds", join);
        return Json.toJson(attribute);
    }

}
