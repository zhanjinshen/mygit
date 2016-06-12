package com.thoughtworks.fms.core.mybatis.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    public static String getHmacMD5(String privateKey, String input) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = privateKey.getBytes();
        Key key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacMD5");
        Mac mac = Mac.getInstance("HmacMD5");
        mac.init(key);

        return byteArrayToHex(mac.doFinal(input.getBytes()));
    }

    protected static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();

        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }

//    http://www.tomred.net/tutorials/tomred-java-generate-hmac-md5-sha1.html
}