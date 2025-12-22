package io.strategiz.data.user.entity;

import com.google.cloud.firestore.annotation.DocumentId;

import java.time.Instant;

/**
 * Entity for one-time authentication tokens used for cross-app SSO.
 * Tokens are single-use and expire quickly (60 seconds).
 *
 * Stored in Firestore collection: auth_tokens
 *
 * Note: This is a simple entity without BaseEntity lifecycle management
 * since tokens are ephemeral and don't need audit trails or soft deletes.
 */
public class AuthTokenEntity {

    @DocumentId
    private String token;

    private String userId;
    private String targetApp;
    private String redirectUrl;
    private long expiresAtEpochSecond;
    private boolean used;
    private String createdFromIp;
    private long createdAtEpochSecond;

    public AuthTokenEntity() {
        this.used = false;
        this.createdAtEpochSecond = Instant.now().getEpochSecond();
    }

    public AuthTokenEntity(String token, String userId, String targetApp, String redirectUrl, int ttlSeconds) {
        this();
        this.token = token;
        this.userId = userId;
        this.targetApp = targetApp;
        this.redirectUrl = redirectUrl;
        this.expiresAtEpochSecond = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTargetApp() {
        return targetApp;
    }

    public void setTargetApp(String targetApp) {
        this.targetApp = targetApp;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public long getExpiresAtEpochSecond() {
        return expiresAtEpochSecond;
    }

    public void setExpiresAtEpochSecond(long expiresAtEpochSecond) {
        this.expiresAtEpochSecond = expiresAtEpochSecond;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getCreatedFromIp() {
        return createdFromIp;
    }

    public void setCreatedFromIp(String createdFromIp) {
        this.createdFromIp = createdFromIp;
    }

    public long getCreatedAtEpochSecond() {
        return createdAtEpochSecond;
    }

    public void setCreatedAtEpochSecond(long createdAtEpochSecond) {
        this.createdAtEpochSecond = createdAtEpochSecond;
    }

    /**
     * Check if token is valid (not expired and not used)
     */
    public boolean isValid() {
        return !used && Instant.now().getEpochSecond() < expiresAtEpochSecond;
    }

    @Override
    public String toString() {
        return "AuthTokenEntity{" +
                "token='" + (token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null") + '\'' +
                ", userId='" + userId + '\'' +
                ", targetApp='" + targetApp + '\'' +
                ", expiresAt=" + Instant.ofEpochSecond(expiresAtEpochSecond) +
                ", used=" + used +
                '}';
    }
}
