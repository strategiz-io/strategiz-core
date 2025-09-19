package io.strategiz.service.device.model.authenticated;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.service.device.model.BaseDeviceFingerprint;

/**
 * Request for updating an authenticated device registration
 * Updates device fingerprint and trust status for authenticated users
 * Stores in: /users/{userId}/devices (user subcollection)
 */
public class UpdateAuthenticatedDeviceRequest extends BaseDeviceFingerprint {
    
    // Additional fields specific to authenticated device updates
    @JsonProperty("device_name")
    private String deviceName;  // User can update device name
    
    @JsonProperty("trusted")
    private Boolean trusted;  // Can be updated based on MFA or admin action
    
    @JsonProperty("trust_level")
    private String trustLevel;  // Can be upgraded/downgraded
    
    // Constructors
    public UpdateAuthenticatedDeviceRequest() {
        super();
    }
    
    // Getters and Setters for additional fields
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public Boolean getTrusted() {
        return trusted;
    }
    
    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }
    
    public String getTrustLevel() {
        return trustLevel;
    }
    
    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }
}