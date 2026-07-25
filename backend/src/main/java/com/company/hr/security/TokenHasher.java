package com.company.hr.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Refresh tokens are opaque, high-entropy random strings — never a JWT — specifically so they
 * can be looked up by an exact hash match and revoked server-side (docs/adr/0003). SHA-256 (not
 * BCrypt) is appropriate here: BCrypt defends against brute-forcing a *low-entropy* human secret;
 * a 256-bit random token has no feasible precomputed-table or brute-force attack, and a
 * deterministic hash is required so the DB can look it up by equality at all.
 */
@Component
public class TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
