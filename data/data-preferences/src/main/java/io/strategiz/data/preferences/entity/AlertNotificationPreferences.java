package io.strategiz.data.preferences.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Alert notification preferences stored at users/{userId}/preferences/alertNotifications
 * Contains user settings for trading alert notifications.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("preferences")
public class AlertNotificationPreferences extends BaseEntity {

	/** Fixed document ID for alert preferences */
	public static final String PREFERENCE_ID = "alertNotifications";

	@DocumentId
	@PropertyName("preferenceId")
	@JsonProperty("preferenceId")
	private String preferenceId = PREFERENCE_ID;

	/**
	 * Phone number in E.164 format (e.g., +14155551234)
	 */
	@PropertyName("phoneNumber")
	@JsonProperty("phoneNumber")
	private String phoneNumber;

	/**
	 * Whether the phone number has been verified via OTP
	 */
	@PropertyName("phoneVerified")
	@JsonProperty("phoneVerified")
	private Boolean phoneVerified = false;

	/**
	 * Email address for alerts (can override account email)
	 */
	@PropertyName("emailForAlerts")
	@JsonProperty("emailForAlerts")
	private String emailForAlerts;

	/**
	 * Whether alert email has been verified
	 */
	@PropertyName("emailVerified")
	@JsonProperty("emailVerified")
	private Boolean emailVerified = false;

	/**
	 * Enabled notification channels: email, sms, push, in-app
	 */
	@PropertyName("enabledChannels")
	@JsonProperty("enabledChannels")
	private List<String> enabledChannels;

	/**
	 * Quiet hours start time (HH:mm format, e.g., "22:00")
	 */
	@PropertyName("quietHoursStart")
	@JsonProperty("quietHoursStart")
	private String quietHoursStart;

	/**
	 * Quiet hours end time (HH:mm format, e.g., "08:00")
	 */
	@PropertyName("quietHoursEnd")
	@JsonProperty("quietHoursEnd")
	private String quietHoursEnd;

	/**
	 * Timezone for quiet hours (e.g., "America/New_York")
	 */
	@PropertyName("quietHoursTimezone")
	@JsonProperty("quietHoursTimezone")
	private String quietHoursTimezone;

	/**
	 * Whether quiet hours are enabled
	 */
	@PropertyName("quietHoursEnabled")
	@JsonProperty("quietHoursEnabled")
	private Boolean quietHoursEnabled = false;

	/**
	 * Maximum alerts per hour (rate limiting)
	 */
	@PropertyName("maxAlertsPerHour")
	@JsonProperty("maxAlertsPerHour")
	private Integer maxAlertsPerHour = 10;

	/**
	 * Number of alerts sent in current hour
	 */
	@PropertyName("alertsThisHour")
	@JsonProperty("alertsThisHour")
	private Integer alertsThisHour = 0;

	/**
	 * When the hourly alert count was last reset
	 */
	@PropertyName("hourlyResetAt")
	@JsonProperty("hourlyResetAt")
	private Timestamp hourlyResetAt;

	/**
	 * When the last alert was sent
	 */
	@PropertyName("lastAlertSentAt")
	@JsonProperty("lastAlertSentAt")
	private Timestamp lastAlertSentAt;

	// Constructors
	public AlertNotificationPreferences() {
		super();
		this.enabledChannels = new ArrayList<>();
		this.enabledChannels.add("email");
		this.enabledChannels.add("in-app");
	}

	// BaseEntity implementation
	@Override
	public String getId() {
		return preferenceId;
	}

	@Override
	public void setId(String id) {
		this.preferenceId = id;
	}

	// Getters and Setters
	public String getPreferenceId() {
		return preferenceId;
	}

	public void setPreferenceId(String preferenceId) {
		this.preferenceId = preferenceId;
	}

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

	public Boolean getQuietHoursEnabled() {
		return quietHoursEnabled;
	}

	public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
		this.quietHoursEnabled = quietHoursEnabled;
	}

	public Integer getMaxAlertsPerHour() {
		return maxAlertsPerHour;
	}

	public void setMaxAlertsPerHour(Integer maxAlertsPerHour) {
		this.maxAlertsPerHour = maxAlertsPerHour;
	}

	public Integer getAlertsThisHour() {
		return alertsThisHour;
	}

	public void setAlertsThisHour(Integer alertsThisHour) {
		this.alertsThisHour = alertsThisHour;
	}

	public Timestamp getHourlyResetAt() {
		return hourlyResetAt;
	}

	public void setHourlyResetAt(Timestamp hourlyResetAt) {
		this.hourlyResetAt = hourlyResetAt;
	}

	public Timestamp getLastAlertSentAt() {
		return lastAlertSentAt;
	}

	public void setLastAlertSentAt(Timestamp lastAlertSentAt) {
		this.lastAlertSentAt = lastAlertSentAt;
	}

	// Convenience methods

	/**
	 * Check if a notification channel is enabled.
	 */
	public boolean isChannelEnabled(String channel) {
		return enabledChannels != null && enabledChannels.contains(channel.toLowerCase());
	}

	/**
	 * Check if SMS notifications are properly configured (phone verified and channel
	 * enabled).
	 */
	public boolean isSmsConfigured() {
		return phoneNumber != null && !phoneNumber.isEmpty() && Boolean.TRUE.equals(phoneVerified)
				&& isChannelEnabled("sms");
	}

	/**
	 * Check if email notifications are properly configured.
	 */
	public boolean isEmailConfigured() {
		return emailForAlerts != null && !emailForAlerts.isEmpty() && isChannelEnabled("email");
	}

	/**
	 * Check if the user is within rate limits.
	 */
	public boolean isWithinRateLimit() {
		return alertsThisHour == null || maxAlertsPerHour == null || alertsThisHour < maxAlertsPerHour;
	}

}
