package com.thoughtworks.fms.core;

import com.thoughtworks.fms.exception.DecryptionException;
import com.thoughtworks.fms.exception.EncryptionException;

import java.io.InputStream;
import java.io.OutputStream;

public interface Cipher {

    void encrypt(InputStream toEncryptStream, OutputStream encryptedStream) throws EncryptionException;

    void decrypt(InputStream encryptedStream, OutputStream decryptedStream) throws DecryptionException;

}
