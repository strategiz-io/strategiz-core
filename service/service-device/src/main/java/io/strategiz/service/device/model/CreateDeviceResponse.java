package io.strategiz.service.device.model;

import java.time.Instant;

/**
 * Model for device creation responses
 * Enhanced to include trust score and additional metadata
 */
public class CreateDeviceResponse {
    
    private boolean success;
    private String deviceId;
    private Instant registrationTime;
    private String error;
    private Integer trustScore;
    private String trustLevel;
    private Boolean requiresVerification;
    
    // Default constructor
    public CreateDeviceResponse() {
    }
    
    // Static factory methods for common responses
    public static CreateDeviceResponse success(String deviceId, Instant registrationTime) {
        CreateDeviceResponse response = new CreateDeviceResponse();
        response.setSuccess(true);
        response.setDeviceId(deviceId);
        response.setRegistrationTime(registrationTime);
        return response;
    }
    
    public static CreateDeviceResponse success(String deviceId, Instant registrationTime, Integer trustScore) {
        CreateDeviceResponse response = success(deviceId, registrationTime);
        response.setTrustScore(trustScore);
        response.setTrustLevel(determineTrustLevel(trustScore));
        response.setRequiresVerification(trustScore < 50);
        return response;
    }
    
    public static CreateDeviceResponse error(String errorMessage) {
        CreateDeviceResponse response = new CreateDeviceResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }
    
    private static String determineTrustLevel(Integer trustScore) {
        if (trustScore == null) return "unknown";
        if (trustScore >= 80) return "high";
        if (trustScore >= 50) return "medium";
        if (trustScore >= 30) return "low";
        return "blocked";
    }
    
    // Getters and setters
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
    
    public Boolean getRequiresVerification() {
        return requiresVerification;
    }
    
    public void setRequiresVerification(Boolean requiresVerification) {
        this.requiresVerification = requiresVerification;
    }
}
