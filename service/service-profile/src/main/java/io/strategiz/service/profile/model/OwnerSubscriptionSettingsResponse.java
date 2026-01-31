package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.social.entity.OwnerSubscriptionSettings;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for owner subscription settings.
 */
public class OwnerSubscriptionSettingsResponse {

	@JsonProperty("userId")
	private String userId;

	@JsonProperty("enabled")
	private boolean enabled;

	@JsonProperty("monthlyPrice")
	private BigDecimal monthlyPrice;

	@JsonProperty("currency")
	private String currency;

	@JsonProperty("profilePitch")
	private String profilePitch;

	@JsonProperty("stripeConnectAccountId")
	private String stripeConnectAccountId;

	@JsonProperty("stripeConnectStatus")
	private String stripeConnectStatus;

	@JsonProperty("subscriberCount")
	private int subscriberCount;

	@JsonProperty("totalRevenue")
	private BigDecimal totalRevenue;

	@JsonProperty("monthlyRevenue")
	private BigDecimal monthlyRevenue;

	@JsonProperty("netMonthlyRevenue")
	private BigDecimal netMonthlyRevenue;

	@JsonProperty("publicStrategyCount")
	private int publicStrategyCount;

	@JsonProperty("enabledAt")
	private Instant enabledAt;

	@JsonProperty("payoutsEnabled")
	private boolean payoutsEnabled;

	// Default constructor
	public OwnerSubscriptionSettingsResponse() {
	}

	/**
	 * Create a response from an entity.
	 */
	public static OwnerSubscriptionSettingsResponse fromEntity(OwnerSubscriptionSettings entity) {
		OwnerSubscriptionSettingsResponse response = new OwnerSubscriptionSettingsResponse();
		response.setUserId(entity.getUserId());
		response.setEnabled(entity.isEnabled());
		response.setMonthlyPrice(entity.getMonthlyPrice());
		response.setCurrency(entity.getCurrency());
		response.setProfilePitch(entity.getProfilePitch());
		response.setStripeConnectAccountId(entity.getStripeConnectAccountId());
		response.setStripeConnectStatus(entity.getStripeConnectStatus());
		response.setSubscriberCount(entity.getSubscriberCount());
		response.setTotalRevenue(entity.getTotalRevenue());
		response.setMonthlyRevenue(entity.getMonthlyRevenue());
		response.setNetMonthlyRevenue(entity.getNetMonthlyRevenue());
		response.setPublicStrategyCount(entity.getPublicStrategyCount());
		response.setPayoutsEnabled(entity.isPayoutsEnabled());

		if (entity.getEnabledAt() != null) {
			response.setEnabledAt(
					Instant.ofEpochSecond(entity.getEnabledAt().getSeconds(), entity.getEnabledAt().getNanos()));
		}

		return response;
	}

	// Getters and setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public BigDecimal getMonthlyPrice() {
		return monthlyPrice;
	}

	public void setMonthlyPrice(BigDecimal monthlyPrice) {
		this.monthlyPrice = monthlyPrice;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getProfilePitch() {
		return profilePitch;
	}

	public void setProfilePitch(String profilePitch) {
		this.profilePitch = profilePitch;
	}

	public String getStripeConnectAccountId() {
		return stripeConnectAccountId;
	}

	public void setStripeConnectAccountId(String stripeConnectAccountId) {
		this.stripeConnectAccountId = stripeConnectAccountId;
	}

	public String getStripeConnectStatus() {
		return stripeConnectStatus;
	}

	public void setStripeConnectStatus(String stripeConnectStatus) {
		this.stripeConnectStatus = stripeConnectStatus;
	}

	public int getSubscriberCount() {
		return subscriberCount;
	}

	public void setSubscriberCount(int subscriberCount) {
		this.subscriberCount = subscriberCount;
	}

	public BigDecimal getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(BigDecimal totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public BigDecimal getMonthlyRevenue() {
		return monthlyRevenue;
	}

	public void setMonthlyRevenue(BigDecimal monthlyRevenue) {
		this.monthlyRevenue = monthlyRevenue;
	}

	public BigDecimal getNetMonthlyRevenue() {
		return netMonthlyRevenue;
	}

	public void setNetMonthlyRevenue(BigDecimal netMonthlyRevenue) {
		this.netMonthlyRevenue = netMonthlyRevenue;
	}

	public int getPublicStrategyCount() {
		return publicStrategyCount;
	}

	public void setPublicStrategyCount(int publicStrategyCount) {
		this.publicStrategyCount = publicStrategyCount;
	}

	public Instant getEnabledAt() {
		return enabledAt;
	}

	public void setEnabledAt(Instant enabledAt) {
		this.enabledAt = enabledAt;
	}

	public boolean isPayoutsEnabled() {
		return payoutsEnabled;
	}

	public void setPayoutsEnabled(boolean payoutsEnabled) {
		this.payoutsEnabled = payoutsEnabled;
	}

}
