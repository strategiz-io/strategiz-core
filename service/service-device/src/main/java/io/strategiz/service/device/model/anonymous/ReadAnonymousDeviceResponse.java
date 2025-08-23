package io.strategiz.service.device.model.anonymous;

/**
 * Response model for reading anonymous device
 */
public class ReadAnonymousDeviceResponse {
    private String deviceId;
    private String fingerprint;
    private String platform;
    private String browser;
    private String userAgent;
    private Double trustScore;
    private Boolean isAnonymous;
    private Long createdAt;
    private Long lastSeenAt;
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public Double getTrustScore() {
        return trustScore;
    }
    
    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }
    
    public Boolean getIsAnonymous() {
        return isAnonymous;
    }
    
    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getLastSeenAt() {
        return lastSeenAt;
    }
    
    public void setLastSeenAt(Long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}