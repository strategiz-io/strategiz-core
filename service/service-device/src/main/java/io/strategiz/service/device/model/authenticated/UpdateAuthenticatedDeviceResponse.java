package io.strategiz.service.device.model.authenticated;

/**
 * Response model for updating authenticated device
 */
public class UpdateAuthenticatedDeviceResponse {
    private String deviceId;
    private String userId;
    private String deviceName;
    private String fingerprint;
    private Double trustScore;
    private Boolean isTrusted;
    private Long updatedAt;
    private Boolean success;
    
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
    
    public Double getTrustScore() {
        return trustScore;
    }
    
    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }
    
    public Boolean getIsTrusted() {
        return isTrusted;
    }
    
    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
}