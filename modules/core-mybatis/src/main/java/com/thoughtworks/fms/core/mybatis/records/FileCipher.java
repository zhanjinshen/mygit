package com.thoughtworks.fms.core.mybatis.records;


import com.google.common.primitives.Shorts;
import com.thoughtworks.fms.core.Cipher;
import com.thoughtworks.fms.core.mybatis.util.cipher.AESCipher;
import com.thoughtworks.fms.core.mybatis.util.cipher.DefaultRSAKey;
import com.thoughtworks.fms.core.mybatis.util.cipher.RSACipher;
import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Random;

import static com.google.common.io.ByteStreams.readFully;
import static java.nio.charset.Charset.defaultCharset;

public class FileCipher implements Cipher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCipher.class);
    private static final int PASSWORD_LENGTH = 5;

    private static String decryptPassword(InputStream inputStream, Key rsaKey) throws IOException, DecryptionException {
        short passwordLength = getPasswordLength(inputStream);
        byte[] encryptedPassword = new byte[passwordLength];
        ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();

        readFully(inputStream, encryptedPassword);

        RSACipher.decrypt(rsaKey, new ByteArrayInputStream(encryptedPassword), decryptedStream);
        return new String(decryptedStream.toByteArray(), defaultCharset());
    }

    private static short getPasswordLength(InputStream inputStream) throws IOException {
        byte[] header = new byte[2];
        readFully(inputStream, header);
        return Shorts.fromByteArray(header);
    }

    @Override
    public void encrypt(InputStream toEncryptStream, OutputStream encryptedStream) throws EncryptionException {
        LOGGER.info("System Log: Start to encrypt file.");

        String password = generatePassword();
        appendPassword(password, encryptedStream);
        AESCipher.encrypt(password, toEncryptStream, encryptedStream);

        LOGGER.info("System Log: Finished to encrypt file.");
    }

    @Override
    public void decrypt(InputStream encryptedStream, OutputStream decryptedStream) throws DecryptionException {
        LOGGER.info("System Log: Start to decrypt file.");

        try {
            String password = decryptPassword(encryptedStream, DefaultRSAKey.getInstance().defaultPrivateKey());
            AESCipher.decrypt(password, encryptedStream, decryptedStream);
        } catch (IOException | InvalidKeyException e) {
            throw new DecryptionException(e.getMessage(), e);
        }

        LOGGER.info("System Log: Finished to decrypt file.");
    }

    private void appendPassword(String password, OutputStream outputStream) throws EncryptionException {
        ByteArrayOutputStream encryptedBytesStream = new ByteArrayOutputStream();

        try {
            RSACipher.encrypt(DefaultRSAKey.getInstance().defaultPublicKey(), new ByteArrayInputStream(password.getBytes(defaultCharset())),
                    encryptedBytesStream);

            short length = (short) encryptedBytesStream.size();
            outputStream.write(Shorts.toByteArray(length));
            outputStream.write(encryptedBytesStream.toByteArray());
        } catch (IOException | InvalidKeyException e) {
            throw new EncryptionException(e.getMessage(), e);
        }

    }

    private String generatePassword() {
        Random RANDOM = new SecureRandom();
        byte[] salt = new byte[PASSWORD_LENGTH];
        RANDOM.nextBytes(salt);

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < salt.length; i++) {
            String hex = Integer.toHexString(0xff & salt[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

}
