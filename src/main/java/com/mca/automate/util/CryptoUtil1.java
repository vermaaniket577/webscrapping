package com.mca.automate.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: CryptoUtil1.class */
public class CryptoUtil1 {
    private static final String PASS_TEXT = "d6163f0659cfe4196dc03c2c29aab06f10cb0a79cdfc74a45da2d72358712e80";
    private static final int KEY_SIZE = 128;
    private static final int ITERATIONS = 100;

    public static String encrypt(String plainText) {
        try {
            byte[] salt = md5("fc74a45dsalt");
            byte[] iv = md5("c29aab06iv");
            PBEKeySpec spec = new PBEKeySpec(PASS_TEXT.toCharArray(), salt, ITERATIONS, KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(encrypted);
            return URLEncoder.encode(base64, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private static byte[] md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String decrypt(String encryptedText) {
        try {
            String decodedUrl = URLDecoder.decode(encryptedText, StandardCharsets.UTF_8);
            byte[] cipherBytes = Base64.getDecoder().decode(decodedUrl);
            byte[] salt = md5("fc74a45dsalt");
            byte[] iv = md5("c29aab06iv");
            PBEKeySpec spec = new PBEKeySpec(PASS_TEXT.toCharArray(), salt, ITERATIONS, KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, secretKey, new IvParameterSpec(iv));
            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static void main(String[] args) {
        System.out.println(decrypt("jIa5vme%2FsRtPHoUBEDe3dABmog8brXumpR4zwj2%2BUs8%3D"));
    }
}
