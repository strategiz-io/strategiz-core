package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request model for updating alert notification preferences.
 */
public class UpdateAlertPreferencesRequest {

	@JsonProperty("phoneNumber")
	@Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +14155551234)")
	private String phoneNumber;

	@JsonProperty("emailForAlerts")
	@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
	private String emailForAlerts;

	@JsonProperty("enabledChannels")
	@Size(max = 4, message = "Maximum 4 channels allowed")
	private List<String> enabledChannels;

	@JsonProperty("quietHoursEnabled")
	private Boolean quietHoursEnabled;

	@JsonProperty("quietHoursStart")
	@Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:mm format")
	private String quietHoursStart;

	@JsonProperty("quietHoursEnd")
	@Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Time must be in HH:mm format")
	private String quietHoursEnd;

	@JsonProperty("quietHoursTimezone")
	private String quietHoursTimezone;

	@JsonProperty("maxAlertsPerHour")
	private Integer maxAlertsPerHour;

	// Default constructor
	public UpdateAlertPreferencesRequest() {
	}

	// Getters and setters
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmailForAlerts() {
		return emailForAlerts;
	}

	public void setEmailForAlerts(String emailForAlerts) {
		this.emailForAlerts = emailForAlerts;
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
