package io.strategiz.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for PASETO token data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasetoToken {
    /**
     * Unique identifier for the token
     */
    private String id;
    
    /**
     * User ID associated with this token
     */
    private String userId;
    
    /**
     * Token type (e.g., "access", "refresh")
     */
    private String tokenType;
    
    /**
     * The actual PASETO token value
     */
    private String tokenValue;
    
    /**
     * Token issued at timestamp (seconds since epoch)
     */
    private long issuedAt;
    
    /**
     * Token expiration timestamp (seconds since epoch)
     */
    private long expiresAt;
    
    /**
     * Optional device ID that generated this token
     */
    private String deviceId;
    
    /**
     * IP address where token was issued
     */
    private String issuedFrom;
    
    /**
     * Whether the token has been revoked
     */
    private boolean revoked;
    
    /**
     * Optional reason for revocation
     */
    private String revocationReason;
    
    /**
     * Timestamp when token was revoked (seconds since epoch)
     */
    private Long revokedAt;
    
    /**
     * Additional claims stored in the token
     */
    @Builder.Default
    private Map<String, Object> claims = new HashMap<>();

    /**
     * Check if the token is expired
     * 
     * @return true if the token is expired
     */
    public boolean isExpired() {
        return Instant.now().getEpochSecond() > expiresAt;
    }
    
    /**
     * Check if the token is valid (not expired and not revoked)
     * 
     * @return true if the token is valid
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }
}
