package com.isaqcasey.aidocsassistant.Service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for hashing tokens before storing in database
 * Uses SHA-256 for one-way hashing of sensitive tokens
 */
@Service
public class TokenHashingService {

    /**
     * Hash a token using SHA-256
     * Returns a Base64-encoded hash that's safe to store in database
     *
     * @param token The plain text token to hash
     * @return Base64-encoded SHA-256 hash of the token
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify if a plain text token matches a hashed token
     *
     * @param plainToken The plain text token to verify
     * @param hashedToken The hashed token to compare against
     * @return true if tokens match, false otherwise
     */
    public boolean verifyToken(String plainToken, String hashedToken) {
        String hashOfPlainToken = hashToken(plainToken);
        return hashOfPlainToken.equals(hashedToken);
    }
}

