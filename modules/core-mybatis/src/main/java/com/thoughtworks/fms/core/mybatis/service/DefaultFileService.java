package com.thoughtworks.fms.core.mybatis.service;

import com.google.common.base.Splitter;
import com.thoughtworks.fms.api.service.FileService;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.FileMetadata;
import com.thoughtworks.fms.core.FileRepository;
import com.thoughtworks.fms.core.Transfer;
import com.thoughtworks.fms.core.mybatis.exception.FMSErrorCode;
import com.thoughtworks.fms.core.mybatis.exception.InternalServerException;
import com.thoughtworks.fms.core.mybatis.exception.InvalidRequestException;
import com.thoughtworks.fms.core.mybatis.util.DateTimeHelper;
import com.thoughtworks.fms.core.mybatis.util.FileBuilder;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;
import com.thoughtworks.fms.exception.TransferException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;

public class DefaultFileService implements FileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFileService.class);
    private static final int BUFFER = 2048;
    private static final int EOF = -1;
    private static List<String> ACCEPT_EXTENSIONS = Splitter.on(",")
            .splitToList(PropertiesLoader.getProperty("file.accept.extensions"));
    @Inject
    private FileRepository repository;

    @Inject
    private Cipher cipher;

    @Inject
    private Transfer transfer;

    @Override
    public long store(String sourceName, String destName, InputStream inputStream) {
        destName = DateTimeHelper.appendDateTimeStr(destName, "-" + UUID.randomUUID().toString().substring(0, 9));
        String suffix = getAcceptedSuffix(sourceName);
        long count;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ReadCounterStream readCounterStream = new ReadCounterStream(inputStream)) {
                cipher.encrypt(readCounterStream, outputStream);
                count = readCounterStream.getCount();
            }

            try (ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                transfer.write(destName, encryptedInputStream);
            }
        } catch (TransferException | IOException | EncryptionException e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.UPLOAD_FILE_FAIL);
        }

        return repository.storeMetadata(sourceName, destName, "." + suffix, count);
    }

    private String getAcceptedSuffix(String name) {
        int pos = name.lastIndexOf(".");
        String suffix = name.substring(pos + 1);
        ensureAcceptedSuffix(suffix);

        return suffix;
    }

    private void ensureAcceptedSuffix(String suffix) {
        if (!ACCEPT_EXTENSIONS.contains(suffix)) {
            throw new InvalidRequestException(FMSErrorCode.FILE_EXTENSION_NOT_ACCEPT);
        }
    }

    @Override
    public InputStream fetch(String destName) {
        ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream(BUFFER);

        try {
            InputStream encryptedStream = transfer.read(destName);
            cipher.decrypt(encryptedStream, decryptedStream);
        } catch (TransferException | DecryptionException e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.DOWNLOAD_FILE_FAIL);
        }

        return new ByteArrayInputStream(decryptedStream.toByteArray());
    }

    @Override
    public File fetch(List<Long> fileIds) {
        List<FileMetadata> metadatas = repository.findMetadataByIds(fileIds);
        List<Entry> entries = metadatas.stream().parallel()
                .map(metadata -> {
                    String fileName = metadata.getDestName() + metadata.getSuffix();
                    fileName = fileName.replaceAll(".*/(.*)", "$1");
                    return new Entry(fileName, fetch(metadata.getDestName()));
                }).collect(toList());

        return compressZip(entries);
    }

    private File compressZip(List<Entry> entries) {
        File zipFile = FileBuilder.builder(".zip").build();
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFFER))) {
            byte data[] = new byte[BUFFER];

            for (Entry entry : entries) {
                try (BufferedInputStream origin = new BufferedInputStream(entry.getInputStream())) {
                    ZipEntry zipEntry = new ZipEntry(entry.getName());
                    out.putNextEntry(zipEntry);

                    int count;
                    while ((count = origin.read(data)) != EOF) {
                        out.write(data, 0, count);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            throw new InternalServerException(FMSErrorCode.SERVER_INTERNAL_ERROR);
        }

        return zipFile;
    }

    private static class Entry {
        private InputStream inputStream;
        private String name;

        public Entry(String name, InputStream inputStream) {
            this.inputStream = inputStream;
            this.name = name;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getName() {
            return name;
        }
    }

    private static class ReadCounterStream extends InputStream {
        private InputStream origin;
        private long count;

        ReadCounterStream(InputStream origin) {
            this.origin = origin;
            this.count = 0;
        }

        @Override
        public int read(byte b[], int off, int sz) throws IOException {
            int res = origin.read(b, off, sz);
            if (res != EOF) {
                count += res;
            }

            return res;
        }

        @Override
        public int read() throws IOException {
            int res = origin.read();
            if (res != EOF) {
                this.count += res;
            }

            return res;
        }

        @Override
        public void close() throws IOException {
            if (Objects.nonNull(origin)) {
                origin.close();
            }
        }

        long getCount() {
            return this.count;
        }
    }

}
