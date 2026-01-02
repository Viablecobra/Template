package com.origin.launcher;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

public final class CryptoUtils {
    private CryptoUtils() {}

    public static String generateCodeVerifier() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return urlSafeBase64(b);
    }

    public static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static String urlSafeBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public static String randomString(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return urlSafeBase64(b);
    }
}