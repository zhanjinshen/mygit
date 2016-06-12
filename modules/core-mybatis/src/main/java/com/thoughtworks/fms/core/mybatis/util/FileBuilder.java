package com.thoughtworks.fms.core.mybatis.util;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import static java.nio.charset.Charset.defaultCharset;

public class FileBuilder {
    private static final int BLOCK_SIZE = 4 * 1024;
    private File file = null;

    public FileBuilder(String fileName) {
        file = new File(Files.createTempDir(), fileName);
        file.deleteOnExit();
    }

    public static FileBuilder builder() {
        return new FileBuilder(DateTimeHelper.appendDateTimeStr(UUID.randomUUID().toString(), "-"));
    }

    public static FileBuilder builder(String suffix) {
        return new FileBuilder(DateTimeHelper.appendDateTimeStr(UUID.randomUUID().toString(), "-") + suffix);
    }

    public FileBuilder withContent(String content) {
        return withContent(content.getBytes(Charset.forName(String.valueOf(defaultCharset()))));
    }

    public FileBuilder withContent(byte[] bytes) {
        return withContent(bytes, 0, bytes.length);
    }

    public FileBuilder withContent(InputStream inputStream) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buf = new byte[BLOCK_SIZE];

            int len;
            while ((len = ByteStreams.read(inputStream, buf, 0, BLOCK_SIZE)) != 0) {
                outputStream.write(buf, 0, len);
            }

            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public FileBuilder withContent(byte[] bytes, int offset, int len) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes, offset, len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return file.getName();
    }

    public File build() {
        return file;
    }
}
