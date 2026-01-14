package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.time.Instant;

/**
 * User subscription stored at users/{userId}/subscription/current.
 * Tracks subscription tier, payment info, and credit-based usage.
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
	private String tier; // trial, explorer, strategist, quant

	@PropertyName("status")
	@JsonProperty("status")
	private String status; // active, canceled, past_due, trialing, trial_expired, blocked

	// Stripe integration
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

	// Credit-based usage tracking
	@PropertyName("monthlyCreditsAllowed")
	@JsonProperty("monthlyCreditsAllowed")
	private Integer monthlyCreditsAllowed = 0;

	@PropertyName("monthlyCreditsUsed")
	@JsonProperty("monthlyCreditsUsed")
	private Integer monthlyCreditsUsed = 0;

	@PropertyName("creditResetDate")
	@JsonProperty("creditResetDate")
	private Instant creditResetDate;

	@PropertyName("usageWarningLevel")
	@JsonProperty("usageWarningLevel")
	private String usageWarningLevel = "none"; // none, warning (80%), critical (95%), blocked (100%)

	// Trial tracking
	@PropertyName("trialStartDate")
	@JsonProperty("trialStartDate")
	private Instant trialStartDate;

	@PropertyName("trialEndDate")
	@JsonProperty("trialEndDate")
	private Instant trialEndDate;

	// Legacy fields (deprecated but kept for backward compatibility)
	@PropertyName("dailyMessagesUsed")
	@JsonProperty("dailyMessagesUsed")
	@Deprecated(forRemoval = true)
	private Integer dailyMessagesUsed = 0;

	@PropertyName("usageResetDate")
	@JsonProperty("usageResetDate")
	@Deprecated(forRemoval = true)
	private String usageResetDate;

	// Constructors
	public UserSubscription() {
		super();
		this.tier = SubscriptionTier.TRIAL.getId();
		this.status = "trialing";
		this.monthlyCreditsAllowed = SubscriptionTier.TRIAL.getMonthlyCredits();
	}

	public UserSubscription(SubscriptionTier tier) {
		super();
		this.tier = tier.getId();
		this.status = tier.isTrial() ? "trialing" : "active";
		this.monthlyCreditsAllowed = tier.getMonthlyCredits();
	}

	// Convenience methods
	public SubscriptionTier getTierEnum() {
		return SubscriptionTier.fromId(tier);
	}

	public boolean isActive() {
		return "active".equals(status) || "trialing".equals(status);
	}

	public boolean isPaid() {
		SubscriptionTier tierEnum = getTierEnum();
		return !tierEnum.isFree() && !tierEnum.isTrial();
	}

	public boolean isTrial() {
		return getTierEnum().isTrial() || "trialing".equals(status);
	}

	public boolean isTrialExpired() {
		return "trial_expired".equals(status);
	}

	public boolean isBlocked() {
		return "blocked".equals(status) || "blocked".equals(usageWarningLevel);
	}

	/**
	 * Get remaining credits for the current billing period.
	 */
	public int getRemainingCredits() {
		return Math.max(0, monthlyCreditsAllowed - monthlyCreditsUsed);
	}

	/**
	 * Get usage percentage (0-100).
	 */
	public int getUsagePercentage() {
		if (monthlyCreditsAllowed == null || monthlyCreditsAllowed == 0) {
			return 0;
		}
		return (int) ((monthlyCreditsUsed * 100.0) / monthlyCreditsAllowed);
	}

	/**
	 * Check if user can consume the specified number of credits.
	 */
	public boolean canConsumeCredits(int credits) {
		return isActive() && getRemainingCredits() >= credits;
	}

	/**
	 * Consume credits and update warning level.
	 * @param credits Number of credits to consume
	 * @return true if credits were consumed, false if insufficient credits
	 */
	public boolean consumeCredits(int credits) {
		if (!canConsumeCredits(credits)) {
			return false;
		}
		this.monthlyCreditsUsed += credits;
		updateWarningLevel();
		return true;
	}

	/**
	 * Update the warning level based on current usage.
	 */
	public void updateWarningLevel() {
		int percentage = getUsagePercentage();
		if (percentage >= 100) {
			this.usageWarningLevel = "blocked";
		}
		else if (percentage >= 95) {
			this.usageWarningLevel = "critical";
		}
		else if (percentage >= 80) {
			this.usageWarningLevel = "warning";
		}
		else {
			this.usageWarningLevel = "none";
		}
	}

	/**
	 * Reset credits for a new billing period.
	 */
	public void resetCredits() {
		this.monthlyCreditsUsed = 0;
		this.usageWarningLevel = "none";
		this.creditResetDate = Instant.now();
	}

	/**
	 * Initialize credits when upgrading tier.
	 */
	public void initializeForTier(SubscriptionTier newTier) {
		this.tier = newTier.getId();
		this.monthlyCreditsAllowed = newTier.getMonthlyCredits();
		this.monthlyCreditsUsed = 0;
		this.usageWarningLevel = "none";
		this.status = newTier.isTrial() ? "trialing" : "active";
		this.creditResetDate = Instant.now();
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

	public Integer getMonthlyCreditsAllowed() {
		return monthlyCreditsAllowed;
	}

	public void setMonthlyCreditsAllowed(Integer monthlyCreditsAllowed) {
		this.monthlyCreditsAllowed = monthlyCreditsAllowed;
	}

	public Integer getMonthlyCreditsUsed() {
		return monthlyCreditsUsed;
	}

	public void setMonthlyCreditsUsed(Integer monthlyCreditsUsed) {
		this.monthlyCreditsUsed = monthlyCreditsUsed;
	}

	public Instant getCreditResetDate() {
		return creditResetDate;
	}

	public void setCreditResetDate(Instant creditResetDate) {
		this.creditResetDate = creditResetDate;
	}

	public String getUsageWarningLevel() {
		return usageWarningLevel;
	}

	public void setUsageWarningLevel(String usageWarningLevel) {
		this.usageWarningLevel = usageWarningLevel;
	}

	public Instant getTrialStartDate() {
		return trialStartDate;
	}

	public void setTrialStartDate(Instant trialStartDate) {
		this.trialStartDate = trialStartDate;
	}

	public Instant getTrialEndDate() {
		return trialEndDate;
	}

	public void setTrialEndDate(Instant trialEndDate) {
		this.trialEndDate = trialEndDate;
	}

	// Legacy getters/setters (deprecated)
	@Deprecated(forRemoval = true)
	public Integer getDailyMessagesUsed() {
		return dailyMessagesUsed;
	}

	@Deprecated(forRemoval = true)
	public void setDailyMessagesUsed(Integer dailyMessagesUsed) {
		this.dailyMessagesUsed = dailyMessagesUsed;
	}

	@Deprecated(forRemoval = true)
	public String getUsageResetDate() {
		return usageResetDate;
	}

	@Deprecated(forRemoval = true)
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
