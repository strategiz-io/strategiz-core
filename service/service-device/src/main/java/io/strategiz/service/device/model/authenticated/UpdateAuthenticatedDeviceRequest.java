package io.strategiz.service.device.model.authenticated;

/**
 * Request model for updating authenticated device
 */
public class UpdateAuthenticatedDeviceRequest {
    private String deviceName;
    private String fingerprint;
    private String platform;
    private String browser;
    private String userAgent;
    private String ipAddress;
    private Double trustScore;
    private Boolean isTrusted;
    
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
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
}