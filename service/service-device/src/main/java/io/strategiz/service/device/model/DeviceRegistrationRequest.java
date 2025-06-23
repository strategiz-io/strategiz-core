package io.strategiz.service.device.model;

import java.util.Map;

/**
 * Model for device registration requests
 */
public class DeviceRegistrationRequest {
    
    private String deviceId;
    private Map<String, Object> deviceInfo;
    private String publicKey;
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }
    
    public void setDeviceInfo(Map<String, Object> deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
