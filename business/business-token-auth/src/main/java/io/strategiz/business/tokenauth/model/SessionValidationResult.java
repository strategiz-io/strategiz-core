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
    private final String acr; // Authentication Context Reference (0-3)
    private final List<String> amr; // Authentication Methods References
    private final String tradingMode; // Trading mode ("demo" or "live")
    private final Instant lastAccessedAt;
    private final Instant expiresAt;
    private final boolean valid;

    public SessionValidationResult(String userId, String userEmail, String sessionId,
                                 String acr, List<String> amr, String tradingMode,
                                 Instant lastAccessedAt, Instant expiresAt, boolean valid) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.sessionId = sessionId;
        this.acr = acr;
        this.amr = amr;
        this.tradingMode = tradingMode != null ? tradingMode : "demo";
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

    public List<String> getAmr() {
        return amr;
    }

    public String getTradingMode() {
        return tradingMode;
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
        // ACR 1+ means at least basic authentication
        return "1".equals(acr) || "2".equals(acr) || "3".equals(acr);
    }

    public boolean isPartiallyAuthenticated() {
        return "0".equals(acr);
    }

    public boolean isHighAssurance() {
        // ACR 3 indicates hardware-based MFA
        return "3".equals(acr);
    }

    public boolean isMultiFactor() {
        // ACR 2+ indicates MFA
        return "2".equals(acr) || "3".equals(acr);
    }

    public boolean hasPasskey() {
        return amr != null && amr.contains("passkeys");
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
                ", amr=" + amr +
                ", tradingMode='" + tradingMode + '\'' +
                ", valid=" + valid +
                '}';
    }
}