package io.strategiz.service.auth.model.token;

import java.time.Instant;

/**
 * Represents the payload for a signup identity token
 * This token is issued after email verification and used throughout the signup process
 */
public record SignupIdentityToken(
    String email,
    String verificationId,
    boolean emailVerified,
    String displayName,
    Instant issuedAt,
    Instant expiresAt
) {
    /**
     * Create a new signup identity token
     * 
     * @param email User's verified email
     * @param verificationId ID of the verification record
     * @param displayName User's display name (if provided)
     * @return A new signup identity token
     */
    public static SignupIdentityToken create(String email, String verificationId, String displayName) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600); // Token valid for 1 hour
        
        return new SignupIdentityToken(
            email,
            verificationId,
            true,  // Email is verified at this point
            displayName != null ? displayName : email.split("@")[0], // Use email prefix as fallback
            now,
            expiry
        );
    }
    
    /**
     * Check if this token has expired
     * 
     * @return true if token has expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
