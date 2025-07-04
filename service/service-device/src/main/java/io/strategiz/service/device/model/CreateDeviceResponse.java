package io.strategiz.service.device.model;

import java.time.Instant;

/**
 * Model for device creation responses
 */
public class CreateDeviceResponse {
    
    private boolean success;
    private String deviceId;
    private Instant registrationTime;
    private String error;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public Instant getRegistrationTime() {
        return registrationTime;
    }
    
    public void setRegistrationTime(Instant registrationTime) {
        this.registrationTime = registrationTime;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
