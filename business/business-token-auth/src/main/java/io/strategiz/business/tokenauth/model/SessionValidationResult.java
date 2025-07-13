package io.strategiz.business.tokenauth.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of session validation containing user information and session metadata
 */
public class SessionValidationResult {

    private final String userId;
    private final String userEmail;
    private final String sessionId;
    private final String acr; // Authentication Context Reference
    private final String aal; // Authenticator Assurance Level  
    private final List<String> amr; // Authentication Methods References
    private final Instant lastAccessedAt;
    private final Instant expiresAt;
    private final boolean valid;

    public SessionValidationResult(String userId, String userEmail, String sessionId,
                                 String acr, String aal, List<String> amr,
                                 Instant lastAccessedAt, Instant expiresAt, boolean valid) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.sessionId = sessionId;
        this.acr = acr;
        this.aal = aal;
        this.amr = amr;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
        this.valid = valid;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAcr() {
        return acr;
    }

    public String getAal() {
        return aal;
    }

    public List<String> getAmr() {
        return amr;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isValid() {
        return valid;
    }

    // Convenience methods for authentication level checks
    public boolean isFullyAuthenticated() {
        return "2".equals(acr);
    }

    public boolean isPartiallyAuthenticated() {
        return "1".equals(acr);
    }

    public boolean isHighAssurance() {
        return "3".equals(aal); // Hardware crypto (passkeys)
    }

    public boolean isMultiFactor() {
        return "2".equals(aal) || "3".equals(aal);
    }

    public boolean hasPasskey() {
        return amr != null && amr.contains("passkey");
    }

    public boolean hasTotp() {
        return amr != null && amr.contains("totp");
    }

    @Override
    public String toString() {
        return "SessionValidationResult{" +
                "userId='" + userId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", acr='" + acr + '\'' +
                ", aal='" + aal + '\'' +
                ", valid=" + valid +
                '}';
    }
}