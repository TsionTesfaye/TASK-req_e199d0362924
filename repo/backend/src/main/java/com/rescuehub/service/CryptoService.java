package com.rescuehub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final byte[] keyBytes;

    public CryptoService(@Value("${rescuehub.crypto.key:}") String key) {
        // No fallback default. The application MUST refuse to start without an
        // explicit ENCRYPTION_KEY (mapped via application.yml to rescuehub.crypto.key).
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY is required. Set the ENCRYPTION_KEY environment variable "
                            + "(minimum 32 bytes of entropy).");
        }
        byte[] raw = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY must be at least 32 bytes; got " + raw.length + ".");
        }
        this.keyBytes = new byte[32];
        System.arraycopy(raw, 0, this.keyBytes, 0, 32);
    }

    public record EncryptResult(byte[] ciphertext, byte[] iv) {}

    public EncryptResult encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new EncryptResult(ciphertext, iv);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(byte[] ciphertext, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static String sha256Hex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}
