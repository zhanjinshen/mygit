package com.thoughtworks.fms.core.mybatis.util.cipher;

import com.google.common.io.ByteStreams;
import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

public class DefaultRSAKey {
    private static final String PUBLIC_KEY_NAME = PropertiesLoader.getProperty("rsa.public.key");
    private static final String PRIVATE_KEY_NAME = PropertiesLoader.getProperty("rsa.key.store.key.name");
    private static final String KEY_STORE_NAME = PropertiesLoader.getProperty("rsa.key.store.name");
    private static final String KEY_STORE_PASSWORD = PropertiesLoader.getProperty("ras.key.store.password");

    private volatile static DefaultRSAKey instance;
    private Key publicKey;
    private KeyStore keyStore;

    private DefaultRSAKey() throws InvalidKeyException {
        loadPublicKey();
        loadKeyStore();
    }

    public static DefaultRSAKey getInstance() throws InvalidKeyException {
        if (Objects.isNull(instance)) {
            synchronized (DefaultRSAKey.class) {
                if (Objects.isNull(instance)) {
                    instance = new DefaultRSAKey();
                }
            }
        }

        return instance;
    }

    public Key defaultPublicKey() {
        return publicKey;
    }

    public Key defaultPrivateKey() throws InvalidKeyException {
        try {
            return keyStore.getKey(PRIVATE_KEY_NAME, KEY_STORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }
    }

    private void loadKeyStore() throws InvalidKeyException {
        try {
            keyStore = KeyStore.getInstance("JCEKS", BaseCipher.DEFAULT_PROVIDER_NAME);
            keyStore.load(keyStoreInputStream(), KEY_STORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }
    }

    private InputStream keyStoreInputStream() {
        return getClass().getClassLoader().getResourceAsStream(KEY_STORE_NAME);
    }

    private void loadPublicKey() throws InvalidKeyException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PUBLIC_KEY_NAME)) {
            byte[] keyBytes = ByteStreams.toByteArray(inputStream);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }
    }

}
