package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.time.Instant;

/**
 * Platform subscription stored at users/{userId}/subscription/current.
 * Tracks the user's subscription to Strategiz platform (tier, payment info, STRAT-based usage).
 *
 * <p>This entity represents the user's subscription to the PLATFORM (Strategiz),
 * NOT to another user/owner. For owner subscriptions, see data-social/OwnerSubscription.</p>
 *
 * <p>Tiers:</p>
 * <ul>
 *   <li>EXPLORER: Free freemium tier (5,000 STRAT/month)</li>
 *   <li>STRATEGIST: $89/month (25,000 STRAT/month)</li>
 *   <li>QUANT: $129/month (40,000 STRAT/month)</li>
 * </ul>
 *
 * @see SubscriptionTier
 */
@Collection("subscription")
public class PlatformSubscription extends BaseEntity {

	public static final String SUBSCRIPTION_ID = "current";

	@DocumentId
	@PropertyName("subscriptionId")
	@JsonProperty("subscriptionId")
	private String subscriptionId = SUBSCRIPTION_ID;

	@PropertyName("tier")
	@JsonProperty("tier")
	private String tier; // explorer, strategist, quant

	@PropertyName("status")
	@JsonProperty("status")
	private String status; // active, canceled, past_due, blocked

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

	// STRAT-based usage tracking
	// Firestore field names kept as legacy "monthlyCreditsAllowed/Used" for backward compatibility
	// with existing documents. Use migration endpoint to rename, then switch to new names.
	@PropertyName("monthlyCreditsAllowed")
	@JsonProperty("monthlyStratAllowed")
	private Integer monthlyStratAllowed = 0;

	@PropertyName("monthlyCreditsUsed")
	@JsonProperty("monthlyStratUsed")
	private Integer monthlyStratUsed = 0;

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
	public PlatformSubscription() {
		super();
		this.tier = SubscriptionTier.EXPLORER.getId();
		this.status = "active";
		this.monthlyStratAllowed = SubscriptionTier.EXPLORER.getMonthlyStrat();
	}

	public PlatformSubscription(SubscriptionTier tier) {
		super();
		this.tier = tier.getId();
		this.status = "active";
		this.monthlyStratAllowed = tier.getMonthlyStrat();
	}

	// Convenience methods
	public SubscriptionTier getTierEnum() {
		return SubscriptionTier.fromId(tier);
	}

	public boolean isActive() {
		return "active".equals(status);
	}

	public boolean isPaid() {
		SubscriptionTier tierEnum = getTierEnum();
		return !tierEnum.isFree();
	}

	/**
	 * @deprecated Trial tier has been removed. Use {@link #isFree()} instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean isTrial() {
		return false; // Trial tier removed - all users start on Explorer (free)
	}

	/**
	 * Check if this is a free tier subscription (Explorer).
	 */
	public boolean isFree() {
		return getTierEnum().isFree();
	}

	/**
	 * @deprecated Trial tier has been removed.
	 */
	@Deprecated(forRemoval = true)
	public boolean isTrialExpired() {
		return false; // Trial tier removed
	}

	public boolean isBlocked() {
		return "blocked".equals(status) || "blocked".equals(usageWarningLevel);
	}

	/**
	 * Get remaining STRAT for the current billing period.
	 */
	public int getRemainingStrat() {
		return Math.max(0, monthlyStratAllowed - monthlyStratUsed);
	}

	/**
	 * @deprecated Use {@link #getRemainingStrat()} instead.
	 */
	@Deprecated(forRemoval = true)
	public int getRemainingCredits() {
		return getRemainingStrat();
	}

	/**
	 * Get usage percentage (0-100).
	 */
	public int getUsagePercentage() {
		if (monthlyStratAllowed == null || monthlyStratAllowed == 0) {
			return 0;
		}
		return (int) ((monthlyStratUsed * 100.0) / monthlyStratAllowed);
	}

	/**
	 * Check if user can consume the specified amount of STRAT.
	 */
	public boolean canConsumeStrat(int strat) {
		return isActive() && getRemainingStrat() >= strat;
	}

	/**
	 * @deprecated Use {@link #canConsumeStrat(int)} instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean canConsumeCredits(int credits) {
		return canConsumeStrat(credits);
	}

	/**
	 * Consume STRAT and update warning level.
	 * @param strat Amount of STRAT to consume
	 * @return true if STRAT was consumed, false if insufficient
	 */
	public boolean consumeStrat(int strat) {
		if (!canConsumeStrat(strat)) {
			return false;
		}
		this.monthlyStratUsed += strat;
		updateWarningLevel();
		return true;
	}

	/**
	 * @deprecated Use {@link #consumeStrat(int)} instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean consumeCredits(int credits) {
		return consumeStrat(credits);
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
	 * Reset STRAT usage for a new billing period.
	 */
	public void resetStratUsage() {
		this.monthlyStratUsed = 0;
		this.usageWarningLevel = "none";
		this.creditResetDate = Instant.now();
	}

	/**
	 * @deprecated Use {@link #resetStratUsage()} instead.
	 */
	@Deprecated(forRemoval = true)
	public void resetCredits() {
		resetStratUsage();
	}

	/**
	 * Initialize STRAT allocation when upgrading tier.
	 */
	public void initializeForTier(SubscriptionTier newTier) {
		this.tier = newTier.getId();
		this.monthlyStratAllowed = newTier.getMonthlyStrat();
		this.monthlyStratUsed = 0;
		this.usageWarningLevel = "none";
		this.status = "active";
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

	public Integer getMonthlyStratAllowed() {
		return monthlyStratAllowed;
	}

	public void setMonthlyStratAllowed(Integer monthlyStratAllowed) {
		this.monthlyStratAllowed = monthlyStratAllowed;
	}

	public Integer getMonthlyStratUsed() {
		return monthlyStratUsed;
	}

	public void setMonthlyStratUsed(Integer monthlyStratUsed) {
		this.monthlyStratUsed = monthlyStratUsed;
	}

	/**
	 * @deprecated Use {@link #getMonthlyStratAllowed()} instead.
	 */
	@Deprecated(forRemoval = true)
	public Integer getMonthlyCreditsAllowed() {
		return monthlyStratAllowed;
	}

	/**
	 * @deprecated Use {@link #setMonthlyStratAllowed(Integer)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setMonthlyCreditsAllowed(Integer monthlyCreditsAllowed) {
		this.monthlyStratAllowed = monthlyCreditsAllowed;
	}

	/**
	 * @deprecated Use {@link #getMonthlyStratUsed()} instead.
	 */
	@Deprecated(forRemoval = true)
	public Integer getMonthlyCreditsUsed() {
		return monthlyStratUsed;
	}

	/**
	 * @deprecated Use {@link #setMonthlyStratUsed(Integer)} instead.
	 */
	@Deprecated(forRemoval = true)
	public void setMonthlyCreditsUsed(Integer monthlyCreditsUsed) {
		this.monthlyStratUsed = monthlyCreditsUsed;
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
