package com.mca.automate.util;

import jakarta.annotation.PostConstruct;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: CryptoUtil.class */
@Component
public class CryptoUtil {

    @Value("${mca.crypto.passText}")
    private String passText;

    @Value("${mca.crypto.salt}")
    private String salt;

    @Value("${mca.crypto.iv}")
    private String ivString;

    @Value("${mca.crypto.keySize:128}")
    private int keySize;

    @Value("${mca.crypto.iterations:100}")
    private int iterations;
    private SecretKeySpec keySpec;
    private IvParameterSpec iv;

    @PostConstruct
    public void init() throws Exception {
        this.keySpec = deriveKey(this.passText, md5Bytes(this.salt), this.iterations, this.keySize);
        this.iv = new IvParameterSpec(md5Bytes(this.ivString));
    }

    public String encrypt(String text) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, this.keySpec, this.iv);
            byte[] enc = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(enc);
            return encodeURIComponent(base64);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            String decoded = encrypted.contains("%") ? URLDecoder.decode(encrypted, StandardCharsets.UTF_8) : encrypted;
            byte[] bytes = Base64.getDecoder().decode(decoded);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, this.keySpec, this.iv);
            return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static SecretKeySpec deriveKey(String pass, byte[] salt, int iter, int size) throws Exception {
        KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, iter, size);
        byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] md5Bytes(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeURIComponent(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20").replace("%21", "!").replace("%27", "'").replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }

    public static void main(String[] args) {
        CryptoUtil cyrCryptoUtil = new CryptoUtil();
        String de = cyrCryptoUtil.decrypt("Ut8pBOc0RSM6iYqffqN1ovhz2q2LDeGGq5yUjLY%2BYtNEJb1CN%2BIr3Rv3Il36wTJnpjZjT2s9WMovDH9kUqwFx4Mj5%2BepDz4ZVNhn%2B9phyz309Mi4Zs7bBuj5BRitFAVCoDa9ZsV8734NNZJZmvJClN9zeOq31NB46in7RcF7re%2BEp41t7l%2BTletG9IhUch6Ju3Y4dXm7oR0q02gtodtPjf7Q2sIYl0AuQV9IEnQRvVjgJ%2BpZ%2B10fjRX4O4ym106dC5J7qdsNOjRJElIGL9x5AslQ%2B8vA2KXp1zDX7Lbyuc2iAKtMah4sv8UfIGai1UNETyB4Y9%2FdDNTeyHrJCpv0%2BcAQ0XtuuNcA2Qt33eosfj5E9Mcr%2FHpOCgKrG5%2FYCVev");
        System.out.println(de);
    }
}
