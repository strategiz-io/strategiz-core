package io.strategiz.service.device.model.authenticated;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for read authenticated device operations
 */
public class ReadAuthenticatedDeviceResponse {
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("device_name")
    private String deviceName;
    
    @JsonProperty("fingerprint")
    private String fingerprint;
    
    @JsonProperty("visitor_id")
    private String visitorId;
    
    @JsonProperty("platform")
    private String platform;
    
    @JsonProperty("browser_name")
    private String browserName;
    
    @JsonProperty("os_name")
    private String osName;
    
    @JsonProperty("os_version")
    private String osVersion;
    
    @JsonProperty("trusted")
    private Boolean trusted;
    
    @JsonProperty("trust_score")
    private Integer trustScore;
    
    @JsonProperty("trust_level")
    private String trustLevel;
    
    @JsonProperty("last_seen")
    private Instant lastSeen;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // Getters and setters
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public String getVisitorId() {
        return visitorId;
    }
    
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getBrowserName() {
        return browserName;
    }
    
    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public void setOsName(String osName) {
        this.osName = osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public Boolean getTrusted() {
        return trusted;
    }
    
    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }
    
    public Integer getTrustScore() {
        return trustScore;
    }
    
    public void setTrustScore(Integer trustScore) {
        this.trustScore = trustScore;
    }
    
    public String getTrustLevel() {
        return trustLevel;
    }
    
    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }
    
    public Instant getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}