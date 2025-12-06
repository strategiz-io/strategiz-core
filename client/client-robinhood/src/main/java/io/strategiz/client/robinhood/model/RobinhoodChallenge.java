package io.strategiz.client.robinhood.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MFA Challenge from Robinhood.
 * When login requires MFA, Robinhood returns a challenge that must be completed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobinhoodChallenge {

    @JsonProperty("id")
    private String id;

    @JsonProperty("user")
    private String user;

    @JsonProperty("type")
    private String type;

    @JsonProperty("alternate_type")
    private String alternateType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("remaining_retries")
    private Integer remainingRetries;

    @JsonProperty("remaining_attempts")
    private Integer remainingAttempts;

    @JsonProperty("expires_at")
    private String expiresAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlternateType() {
        return alternateType;
    }

    public void setAlternateType(String alternateType) {
        this.alternateType = alternateType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRemainingRetries() {
        return remainingRetries;
    }

    public void setRemainingRetries(Integer remainingRetries) {
        this.remainingRetries = remainingRetries;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isCompleted() {
        return "validated".equalsIgnoreCase(status);
    }
}
