package io.strategiz.service.device.model.anonymous;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Response model for updating anonymous device
 */
public class UpdateAnonymousDeviceResponse {
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("visitor_id")
    private String visitorId;
    
    @JsonProperty("trust_score")
    private Double trustScore;
    
    @JsonProperty("trust_level")
    private String trustLevel;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    @JsonProperty("blocked")
    private Boolean blocked;
    
    @JsonProperty("success")
    private Boolean success;
    
    // Constructors
    public UpdateAnonymousDeviceResponse() {
        this.success = true;
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getVisitorId() {
        return visitorId;
    }
    
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
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
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getBlocked() {
        return blocked;
    }
    
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
}