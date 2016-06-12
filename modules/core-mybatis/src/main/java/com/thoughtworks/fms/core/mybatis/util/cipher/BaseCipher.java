package com.thoughtworks.fms.core.mybatis.util.cipher;

import com.google.common.io.ByteStreams;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

abstract class BaseCipher {
    static final String DEFAULT_PROVIDER_NAME = "SunJCE";
    private static final int BLOCK_SIZE = 4 * 1024;

    static void transform(InputStream inputStream, OutputStream outputStream, Cipher cipher)
            throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] buf = new byte[BLOCK_SIZE];

        int len;
        while ((len = ByteStreams.read(inputStream, buf, 0, BLOCK_SIZE)) == BLOCK_SIZE) {
            outputStream.write(cipher.update(buf));
        }

        outputStream.write(cipher.doFinal(buf, 0, len));
    }

}
