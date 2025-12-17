package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response model for alert notification preferences.
 */
public class AlertPreferencesResponse {

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("phoneVerified")
    private Boolean phoneVerified;

    @JsonProperty("emailForAlerts")
    private String emailForAlerts;

    @JsonProperty("emailVerified")
    private Boolean emailVerified;

    @JsonProperty("enabledChannels")
    private List<String> enabledChannels;

    @JsonProperty("quietHoursEnabled")
    private Boolean quietHoursEnabled;

    @JsonProperty("quietHoursStart")
    private String quietHoursStart;

    @JsonProperty("quietHoursEnd")
    private String quietHoursEnd;

    @JsonProperty("quietHoursTimezone")
    private String quietHoursTimezone;

    @JsonProperty("maxAlertsPerHour")
    private Integer maxAlertsPerHour;

    // Default constructor
    public AlertPreferencesResponse() {
    }

    // Getters and setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getEmailForAlerts() {
        return emailForAlerts;
    }

    public void setEmailForAlerts(String emailForAlerts) {
        this.emailForAlerts = emailForAlerts;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public List<String> getEnabledChannels() {
        return enabledChannels;
    }

    public void setEnabledChannels(List<String> enabledChannels) {
        this.enabledChannels = enabledChannels;
    }

    public Boolean getQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String getQuietHoursTimezone() {
        return quietHoursTimezone;
    }

    public void setQuietHoursTimezone(String quietHoursTimezone) {
        this.quietHoursTimezone = quietHoursTimezone;
    }

    public Integer getMaxAlertsPerHour() {
        return maxAlertsPerHour;
    }

    public void setMaxAlertsPerHour(Integer maxAlertsPerHour) {
        this.maxAlertsPerHour = maxAlertsPerHour;
    }
}
