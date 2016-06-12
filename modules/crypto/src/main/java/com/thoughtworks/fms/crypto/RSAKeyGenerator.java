package com.thoughtworks.fms.crypto;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAKeyGenerator {
    public static final Logger LOGGER = LoggerFactory.getLogger(RSAKeyGenerator.class);
    private static final int DEFAULT_KEY_SIZE = 1024;

    private RSAKeyGenerator() {
    }

    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator kpg;
        kpg = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        kpg.initialize(keySize);

        return kpg.genKeyPair();
    }

    public static KeyPair generateKeyPair(int keySize, File privateKeyFile, File publicKeyFile) throws NoSuchAlgorithmException,
            InvalidKeySpecException, IOException, NoSuchProviderException {
        KeyPair keyPair = generateKeyPair(keySize);

        saveToFile(publicKeyFile, new X509EncodedKeySpec(keyPair.getPublic().getEncoded()));
        saveToFile(privateKeyFile, new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()));

        return keyPair;
    }

    private static void saveToFile(File file, EncodedKeySpec keySpec) throws IOException {
        Files.createParentDirs(file);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(keySpec.getEncoded());
            LOGGER.info("Succeed to create RSA Key:" + file.getCanonicalPath());
        }
    }

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        File privateKeyFile = new File(System.getenv("privateKeyPath"));
        File publicKeyFile = new File(System.getenv("publicKeyPath"));

        Files.createParentDirs(privateKeyFile);
        Files.createParentDirs(publicKeyFile);

        RSAKeyGenerator.generateKeyPair(DEFAULT_KEY_SIZE, privateKeyFile, publicKeyFile);

    }

}