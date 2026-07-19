package com.mca.automate.service;

import ch.qos.logback.classic.pattern.Util;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: Test.class */
public final class Test {
    static String passText = "d6163f0659cfe4196dc03c2c29aab06f10cb0a79cdfc74a45da2d72358712e80";
    static String salt = "fc74a45dsalt";
    static String ivv = "c29aab06iv";
    static int keySize = 128;
    static int iterations = 100;

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(decrypt("xkQOkzRAtzK3RZ67YHrhQiA6tawT3Klud1hNzm%2FPa9n1r0pcCa1zOdSofICvVcPK5bZgXn8FcumaSew6nPCHelPrA%2BG1zjwtIrVCpi%2F2ACksn%2F5as6uNaCQf2SdqTXh625r7rdk%2FMFNY4gdUofy%2FnDbuUk9cVRQyZhS6EQsdUn2QNC1fVD%2B2o2PicefDG6zU"));
        new Util();
    }

    private Test() {
    }

    public static String encrypt(String plaintext) throws Exception {
        SecretKeySpec key = deriveKey(passText, md5Bytes(salt), iterations, keySize);
        IvParameterSpec iv = new IvParameterSpec(md5Bytes(ivv));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(1, key, iv);
        byte[] enc = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        String base64 = Base64.getEncoder().encodeToString(enc);
        return encodeURIComponent(base64);
    }

    public static String decrypt(String transitMessage) throws Exception {
        String maybeBase64 = transitMessage.contains("%") ? URLDecoder.decode(transitMessage, StandardCharsets.UTF_8) : transitMessage;
        byte[] enc = Base64.getDecoder().decode(maybeBase64);
        SecretKeySpec key = deriveKey(passText, md5Bytes(salt), iterations, keySize);
        IvParameterSpec iv = new IvParameterSpec(md5Bytes(ivv));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(2, key, iv);
        byte[] plain = cipher.doFinal(enc);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec deriveKey(String passText2, byte[] salt2, int iterations2, int keySizeBits) throws Exception {
        KeySpec spec = new PBEKeySpec(passText2.toCharArray(), salt2, iterations2, keySizeBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] md5Bytes(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeURIComponent(String s) {
        String encoded = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20").replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }

    private static byte[] md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] getSalt() throws Exception {
        return md5("fc74a45dsalt");
    }

    private static IvParameterSpec getIv() throws Exception {
        return new IvParameterSpec(md5("c29aab06iv"));
    }

    private static SecretKeySpec getKey() throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passText.toCharArray(), getSalt(), iterations, keySize);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encryptA(String msg) throws Exception {
        SecretKeySpec key = getKey();
        IvParameterSpec iv = getIv();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(1, key, iv);
        byte[] encrypted = cipher.doFinal(msg.getBytes(StandardCharsets.UTF_8));
        String base64 = Base64.getEncoder().encodeToString(encrypted);
        return URLEncoder.encode(base64, StandardCharsets.UTF_8);
    }

    public static String decryptA(String encryptedMsg) throws Exception {
        SecretKeySpec key = getKey();
        IvParameterSpec iv = getIv();
        String clean = URLDecoder.decode(URLDecoder.decode(encryptedMsg, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        byte[] decodedBytes = Base64.getDecoder().decode(clean);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(2, key, iv);
        byte[] decrypted = cipher.doFinal(decodedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
