package com.deadlineiq.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for verifying legacy salted SHA-256 hashes during progressive BCrypt migration.
 * Plaintext passwords are NOT supported.
 */
public class PasswordUtils {

    private static final String LEGACY_SALT = "DeadlineIQ_SecureSalt_2026";

    /**
     * Checks if a raw password matches an existing legacy salted SHA-256 password hash.
     */
    public static boolean isLegacySha256Match(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        return hashLegacySha256(rawPassword).equalsIgnoreCase(storedHash.trim());
    }

    public static String hashLegacySha256(String rawPassword) {
        if (rawPassword == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = LEGACY_SALT + rawPassword;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing legacy hash", e);
        }
    }
}
