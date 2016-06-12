package com.thoughtworks.fms.core.mybatis.records;


import com.thoughtworks.fms.core.mybatis.util.FileBuilder;
import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class FileCipherTest {

    private FileCipher cipher;

    @Before
    public void setUp() {
        cipher = new FileCipher();
    }

    @Test
    public void should_encrypt_file_correctly() throws IOException, EncryptionException, DecryptionException {
        File toEncryptFile = new FileBuilder("aes-encryption-test.txt").withContent("original file content").build();
        File encryptedFile = new FileBuilder("encrypted-file").getFile();
        File toDecryptFile = new FileBuilder("decrypt-file").getFile();

        try (FileInputStream inputStream = new FileInputStream(toEncryptFile);
             FileOutputStream outputStream = new FileOutputStream(encryptedFile)) {
            cipher.encrypt(inputStream, outputStream);
        }

        try (FileInputStream inputStream = new FileInputStream(encryptedFile);
             FileOutputStream outputStream = new FileOutputStream(toDecryptFile)) {
            cipher.decrypt(inputStream, outputStream);
        }

        assertThat(FileUtils.readLines(toEncryptFile, defaultCharset()), not(FileUtils.readLines(encryptedFile, defaultCharset())));
        assertThat(FileUtils.readLines(toEncryptFile, defaultCharset()), is(FileUtils.readLines(toDecryptFile, defaultCharset())));

        toEncryptFile.delete();
        encryptedFile.delete();
        toDecryptFile.delete();
    }


}