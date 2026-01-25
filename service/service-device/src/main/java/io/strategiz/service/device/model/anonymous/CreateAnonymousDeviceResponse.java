package io.strategiz.service.device.model.anonymous;

import java.time.Instant;

/**
 * Response model for creating anonymous device
 */
public class CreateAnonymousDeviceResponse {

    private boolean success;
    private String deviceId;
    private String fingerprint;
    private Double trustScore;
    private String trustLevel;
    private Long createdAt;
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

    public String getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(String trustLevel) {
        this.trustLevel = trustLevel;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(Instant instant) {
        this.createdAt = instant != null ? instant.toEpochMilli() : null;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Create a success response
     */
    public static CreateAnonymousDeviceResponse success(String deviceId, String fingerprint, Double trustScore, String trustLevel, Instant createdAt) {
        CreateAnonymousDeviceResponse response = new CreateAnonymousDeviceResponse();
        response.setSuccess(true);
        response.setDeviceId(deviceId);
        response.setFingerprint(fingerprint);
        response.setTrustScore(trustScore);
        response.setTrustLevel(trustLevel);
        response.setCreatedAt(createdAt);
        return response;
    }

    /**
     * Create an error response
     */
    public static CreateAnonymousDeviceResponse error(String errorMessage) {
        CreateAnonymousDeviceResponse response = new CreateAnonymousDeviceResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }
}
