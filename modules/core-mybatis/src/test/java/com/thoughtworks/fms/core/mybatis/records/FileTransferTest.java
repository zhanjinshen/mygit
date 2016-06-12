package com.thoughtworks.fms.core.mybatis.records;

import com.thoughtworks.fms.core.Transfer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FileTransferTest {

    private Transfer transfer;
    private static final String FILE_NAME = "test-oss";

    @Before
    public void setUp() {
        transfer = new FileTransfer();
    }

    @Test
    public void should_write_and_read_correct() throws Exception {
        File testFile = createTestFile();

        transfer.write(FILE_NAME, new FileInputStream(testFile));

        InputStream inputStream = transfer.read(FILE_NAME);

        String sourceString = IOUtils.toString(new FileInputStream(testFile), Charset.defaultCharset());
        String readString = IOUtils.toString(inputStream, Charset.defaultCharset());

        assertEquals(sourceString, readString);
    }

    private File createTestFile() throws IOException {
        File file = File.createTempFile("oss-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("0123456789011234567890\n");
        writer.close();

        return file;
    }

}