package com.thoughtworks.fms.crypto;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class RSAKey {
    private static final Logger LOGGER = LoggerFactory.getLogger("");

    private static RSAKey instance;
    private KeyPair keyPair;

    private RSAKey() throws InvalidKeyException {
        initial();
    }

    public static RSAKey getInstance() throws InvalidKeyException {
        if (Objects.isNull(instance)) {
            synchronized (RSAKey.class) {
                if (Objects.isNull(instance)) {
                    instance = new RSAKey();
                }
            }
        }

        return instance;
    }

    private void initial() throws InvalidKeyException {
        PublicKey publicKey = readPublicKeyFromFile(System.getenv("publicKeyPath"));
        PrivateKey privateKey = readPrivateKeyFromFile(System.getenv("privateKeyPath"));

        keyPair = new KeyPair(publicKey, privateKey);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    private PublicKey readPublicKeyFromFile(String filePath) throws InvalidKeyException {
        return (PublicKey) readKeyFromFile(filePath, false);
    }

    private PrivateKey readPrivateKeyFromFile(String filePath) throws InvalidKeyException {
        return (PrivateKey) readKeyFromFile(filePath, true);
    }

    private Key readKeyFromFile(String filePath, boolean isPrivateKey) throws InvalidKeyException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new InvalidKeyException("File '" + filePath + "'  is not exists.");
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] keyBytes = ByteStreams.toByteArray(inputStream);
            return generateKey(keyBytes, isPrivateKey);
        } catch (IOException e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }
    }

    private Key generateKey(byte[] keyBytes, boolean isPrivateKey) throws InvalidKeyException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            if (isPrivateKey) {
                return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            } else {
                return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            }
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }
    }

}
