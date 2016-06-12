package com.thoughtworks.fms.core.mybatis.util.cipher;

import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class RSACipher extends BaseCipher {

    public static void encrypt(Key key, InputStream inputStream, OutputStream outputStream) throws EncryptionException {
        try {
            Cipher cipher = init(Cipher.ENCRYPT_MODE, key, DEFAULT_PROVIDER_NAME);
            transform(inputStream, outputStream, cipher);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeyException | NoSuchPaddingException
                | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
    }

    public static void decrypt(Key key, InputStream inputStream, OutputStream outputStream) throws DecryptionException {
        try {
            Cipher cipher = init(Cipher.DECRYPT_MODE, key, DEFAULT_PROVIDER_NAME);
            transform(inputStream, outputStream, cipher);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeyException | NoSuchPaddingException
                | BadPaddingException | NoSuchProviderException | IllegalBlockSizeException e) {
            throw new DecryptionException(e.getMessage(), e);
        }
    }

    private static Cipher init(int mode, Key key, String providerName) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", providerName);
        cipher.init(mode, key);

        return cipher;
    }

}
