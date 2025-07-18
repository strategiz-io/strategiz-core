package io.strategiz.data.session.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User session model for business layer
 * This is a business domain object that wraps UserSessionEntity
 */
public class UserSession {
    
    private String sessionId;
    private String userId;
    private String userEmail;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private String acr;
    private String aal;
    private List<String> amr;
    private List<String> authorities;
    private Map<String, Object> attributes;
    private boolean active;
    private String terminationReason;

    // === CONSTRUCTORS ===

    public UserSession() {
        this.active = true;
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.attributes = new HashMap<>();
    }

    public UserSession(String userId) {
        this();
        this.userId = userId;
    }

    // === SESSION VALIDATION METHODS ===

    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if session is valid (active and not expired)
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Terminate the session
     */
    public void terminate(String reason) {
        this.active = false;
        this.terminationReason = reason;
    }

    // === GETTERS AND SETTERS ===

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public String getAal() {
        return aal;
    }

    public void setAal(String aal) {
        this.aal = aal;
    }

    public List<String> getAmr() {
        return amr;
    }

    public void setAmr(List<String> amr) {
        this.amr = amr;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }
}