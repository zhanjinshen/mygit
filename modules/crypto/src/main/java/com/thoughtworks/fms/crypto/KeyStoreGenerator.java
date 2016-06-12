package com.thoughtworks.fms.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static com.google.common.io.Files.touch;

public class KeyStoreGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreGenerator.class);

    private static final String DEFAULT_PROVIDER = "SunJCE";
    private static String PASSWORD;
    private static File KEY_STORE_FILE;
    private KeyStore keyStore;

    static {
        try {
            KEY_STORE_FILE = getKeyStorePath();
            PASSWORD = getPassWord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private KeyStoreGenerator() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        loadKeyStore();
    }

    private static String getPassWord() {
        final String passWord = System.getenv("password");

        if (passWord == null || passWord.isEmpty()) {
            throw new InvalidParameterException("Password is empty.");
        }

        LOGGER.info("The password of key store is: " + passWord);
        return passWord;
    }

    private static File getKeyStorePath() throws IOException {
        String fmsKeyStoreFileName = "/opt/fms_keystore";
        final String keyStoreFileNameEnv = System.getenv("filePath");

        if (keyStoreFileNameEnv != null && !keyStoreFileNameEnv.isEmpty()) {
            fmsKeyStoreFileName = keyStoreFileNameEnv;
        }

        LOGGER.info("The path of key store is: " + fmsKeyStoreFileName);

        File file = new File(fmsKeyStoreFileName);
        if (!file.exists()) {
            touch(file);
        }

        return file;
    }

    public void addFileKey(String keyName, KeyPair keyPair) throws IOException, KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException {
        java.security.cert.Certificate certificate = BCCertificateX509.generateCertificate(keyPair);
        addRSAPrivateKey(keyName, keyPair.getPrivate(), certificate);
    }

    private void addRSAPrivateKey(String name, PrivateKey privateKey, java.security.cert.Certificate certificate) throws KeyStoreException, IOException {
        keyStore.setKeyEntry(name, privateKey, PASSWORD.toCharArray(), new java.security.cert.Certificate[]{certificate});
        saveKeyStore();
    }

    private void loadKeyStore() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, NoSuchProviderException {
        keyStore = KeyStore.getInstance("JCEKS", DEFAULT_PROVIDER);
        keyStore.load(null, PASSWORD.toCharArray());
    }

    private void saveKeyStore() throws KeyStoreException, IOException {
        try (FileOutputStream fos = new FileOutputStream(KEY_STORE_FILE)) {
            keyStore.store(fos, PASSWORD.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        }
    }

    public static void main(String[] args) throws InvalidKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, InvalidKeySpecException {
        String keyName = System.getenv("keyName");

        KeyStoreGenerator generator = new KeyStoreGenerator();
        KeyPair keyPair = RSAKey.getInstance().getKeyPair();

        generator.addFileKey(keyName, keyPair);
    }
}
