package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.time.Instant;

/**
 * User subscription stored at users/{userId}/subscription/current
 */
@Collection("subscription")
public class UserSubscription extends BaseEntity {

	public static final String SUBSCRIPTION_ID = "current";

	@DocumentId
	@PropertyName("subscriptionId")
	@JsonProperty("subscriptionId")
	private String subscriptionId = SUBSCRIPTION_ID;

	@PropertyName("tier")
	@JsonProperty("tier")
	private String tier; // scout, trader, strategist

	@PropertyName("status")
	@JsonProperty("status")
	private String status; // active, canceled, past_due, trialing

	@PropertyName("stripeCustomerId")
	@JsonProperty("stripeCustomerId")
	private String stripeCustomerId;

	@PropertyName("stripeSubscriptionId")
	@JsonProperty("stripeSubscriptionId")
	private String stripeSubscriptionId;

	@PropertyName("currentPeriodStart")
	@JsonProperty("currentPeriodStart")
	private Instant currentPeriodStart;

	@PropertyName("currentPeriodEnd")
	@JsonProperty("currentPeriodEnd")
	private Instant currentPeriodEnd;

	@PropertyName("cancelAtPeriodEnd")
	@JsonProperty("cancelAtPeriodEnd")
	private Boolean cancelAtPeriodEnd = false;

	// Daily usage tracking
	@PropertyName("dailyMessagesUsed")
	@JsonProperty("dailyMessagesUsed")
	private Integer dailyMessagesUsed = 0; // Tracks both Learn chat + Labs strategy generation

	// DEPRECATED: No longer tracking strategies separately (combined with messages)
	// Kept for backward compatibility with existing Firestore documents
	// @PropertyName("dailyStrategiesUsed")
	// @JsonProperty("dailyStrategiesUsed")
	// private Integer dailyStrategiesUsed = 0;

	@PropertyName("usageResetDate")
	@JsonProperty("usageResetDate")
	private String usageResetDate; // YYYY-MM-DD format

	// Constructors
	public UserSubscription() {
		super();
		this.tier = SubscriptionTier.SCOUT.getId();
		this.status = "active";
	}

	public UserSubscription(SubscriptionTier tier) {
		super();
		this.tier = tier.getId();
		this.status = "active";
	}

	// Convenience methods
	public SubscriptionTier getTierEnum() {
		return SubscriptionTier.fromId(tier);
	}

	public boolean isActive() {
		return "active".equals(status) || "trialing".equals(status);
	}

	public boolean isPaid() {
		return !SubscriptionTier.SCOUT.getId().equals(tier);
	}

	// Getters and Setters
	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStripeCustomerId() {
		return stripeCustomerId;
	}

	public void setStripeCustomerId(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}

	public String getStripeSubscriptionId() {
		return stripeSubscriptionId;
	}

	public void setStripeSubscriptionId(String stripeSubscriptionId) {
		this.stripeSubscriptionId = stripeSubscriptionId;
	}

	public Instant getCurrentPeriodStart() {
		return currentPeriodStart;
	}

	public void setCurrentPeriodStart(Instant currentPeriodStart) {
		this.currentPeriodStart = currentPeriodStart;
	}

	public Instant getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
		this.currentPeriodEnd = currentPeriodEnd;
	}

	public Boolean getCancelAtPeriodEnd() {
		return cancelAtPeriodEnd;
	}

	public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) {
		this.cancelAtPeriodEnd = cancelAtPeriodEnd;
	}

	public Integer getDailyMessagesUsed() {
		return dailyMessagesUsed;
	}

	public void setDailyMessagesUsed(Integer dailyMessagesUsed) {
		this.dailyMessagesUsed = dailyMessagesUsed;
	}

	// DEPRECATED: Strategy usage now combined with message usage
	// public Integer getDailyStrategiesUsed() {
	// 	return dailyStrategiesUsed;
	// }

	// public void setDailyStrategiesUsed(Integer dailyStrategiesUsed) {
	// 	this.dailyStrategiesUsed = dailyStrategiesUsed;
	// }

	public String getUsageResetDate() {
		return usageResetDate;
	}

	public void setUsageResetDate(String usageResetDate) {
		this.usageResetDate = usageResetDate;
	}

	@Override
	public String getId() {
		return subscriptionId;
	}

	@Override
	public void setId(String id) {
		this.subscriptionId = id;
	}

}
