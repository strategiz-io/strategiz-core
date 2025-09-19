package io.strategiz.service.device.model.authenticated;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.service.device.model.BaseDeviceFingerprint;

/**
 * Request for creating an authenticated device registration
 * Used during sign-up step 2 or sign-in to register/update device for authenticated users
 * Captures comprehensive FingerprintJS data and Web Crypto API public key
 * Stores in: /users/{userId}/devices (user subcollection)
 */
public class CreateAuthenticatedDeviceRequest extends BaseDeviceFingerprint {
    
    // Additional fields specific to authenticated devices
    @JsonProperty("device_name")
    private String deviceName;  // User-friendly name like "Chrome on MacBook"
    
    @JsonProperty("trusted")
    private Boolean trusted;  // Can be set based on MFA completion or admin approval
    
    @JsonProperty("trust_level")
    private String trustLevel;  // e.g., "pending", "verified", "trusted"
    
    @JsonProperty("registration_type")
    private String registrationType;  // e.g., "signup", "signin", "mfa_verified"
    
    // Constructors
    public CreateAuthenticatedDeviceRequest() {
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
    
    public String getRegistrationType() {
        return registrationType;
    }
    
    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }
}