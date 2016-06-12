package com.thoughtworks.fms.core.mybatis.util.cipher;

import com.thoughtworks.fms.core.mybatis.util.PropertiesLoader;
import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static com.google.common.io.BaseEncoding.base64;
import static com.google.common.io.ByteStreams.readFully;
import static java.nio.charset.Charset.defaultCharset;

public class AESCipher extends BaseCipher {
    private static final int DEFAULT_KEY_SIZE = Integer.parseInt(PropertiesLoader.getProperty("aes.key.size"));
    private static final int IV_LENGTH = 16;
    private static final int ITERATION_ROUND = 5;

    public static void encrypt(String password, InputStream toEncryptStream, OutputStream encryptedStream) throws EncryptionException {
        try {
            SecretKey key = generateAESKey(password, DEFAULT_KEY_SIZE);
            byte[] iv = Arrays.copyOfRange(base64().encode(key.getEncoded()).getBytes(defaultCharset()), 0, IV_LENGTH);
            encryptedStream.write(iv);

            Cipher cipher = init(Cipher.ENCRYPT_MODE, iv, key);
            transform(toEncryptStream, encryptedStream, cipher);
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException | NoSuchProviderException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
    }

    public static void decrypt(String password, InputStream inputStream, OutputStream outputStream) throws DecryptionException {
        try {
            SecretKey key = generateAESKey(password, DEFAULT_KEY_SIZE);
            Cipher cipher = init(Cipher.DECRYPT_MODE, fetchIVFromStream(inputStream), key);

            transform(inputStream, outputStream, cipher);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IOException
                | InvalidAlgorithmParameterException | NoSuchProviderException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new DecryptionException(e.getMessage(), e);
        }
    }

    private static Cipher init(int mode, byte[] iv, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING", DEFAULT_PROVIDER_NAME);
        cipher.init(mode, key, new IvParameterSpec(iv));
        return cipher;
    }

    private static SecretKey generateAESKey(String password, int keySize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = base64().encode(password.getBytes(defaultCharset())).getBytes(defaultCharset());
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_ROUND, keySize);

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static byte[] fetchIVFromStream(InputStream inputStream) throws IOException {
        byte[] ivInStream = new byte[IV_LENGTH];
        readFully(inputStream, ivInStream);
        return ivInStream;
    }

}
