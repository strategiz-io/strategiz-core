package io.strategiz.data.strategy.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Strategy subscription entity - represents a user's subscription to a strategy.
 *
 * Collection: strategy_subscriptions (top-level)
 *
 * Tracks: - Who subscribed (userId/subscriberId) - Which strategy (strategyId) -
 * Subscription type (FREE, ONE_TIME, SUBSCRIPTION) - Subscription status (ACTIVE, TRIAL,
 * CANCELLED, EXPIRED, SUSPENDED) - Payment and renewal information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategy_subscriptions")
public class StrategySubscriptionEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("strategyId")
	@JsonProperty("strategyId")
	@NotBlank(message = "Strategy ID is required")
	private String strategyId;

	@PropertyName("userId")
	@JsonProperty("userId")
	@NotBlank(message = "User ID is required")
	private String userId;

	@PropertyName("creatorId")
	@JsonProperty("creatorId")
	@NotBlank(message = "Creator ID is required")
	private String creatorId; // Original author (for attribution)

	@PropertyName("ownerId")
	@JsonProperty("ownerId")
	@NotBlank(message = "Owner ID is required")
	private String ownerId; // Current rights holder (receives subscription payments)

	@PropertyName("subscriptionType")
	@JsonProperty("subscriptionType")
	@NotNull(message = "Subscription type is required")
	private PricingType subscriptionType;

	@PropertyName("status")
	@JsonProperty("status")
	@NotNull(message = "Status is required")
	private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

	@PropertyName("startedAt")
	@JsonProperty("startedAt")
	private Timestamp startedAt;

	@PropertyName("expiresAt")
	@JsonProperty("expiresAt")
	private Timestamp expiresAt;

	@PropertyName("cancelledAt")
	@JsonProperty("cancelledAt")
	private Timestamp cancelledAt;

	@PropertyName("renewalDate")
	@JsonProperty("renewalDate")
	private Timestamp renewalDate;

	@PropertyName("pricePaid")
	@JsonProperty("pricePaid")
	private BigDecimal pricePaid;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("transactionId")
	@JsonProperty("transactionId")
	private String transactionId;

	@PropertyName("autoRenew")
	@JsonProperty("autoRenew")
	private Boolean autoRenew = true;

	@PropertyName("activeDeployments")
	@JsonProperty("activeDeployments")
	private Integer activeDeployments = 0;

	// Denormalized fields for display
	@PropertyName("strategyName")
	@JsonProperty("strategyName")
	private String strategyName;

	@PropertyName("creatorName")
	@JsonProperty("creatorName")
	private String creatorName;

	@PropertyName("ownerName")
	@JsonProperty("ownerName")
	private String ownerName;

	// Constructors
	public StrategySubscriptionEntity() {
		super();
	}

	public StrategySubscriptionEntity(String strategyId, String userId, String creatorId, String ownerId,
			PricingType subscriptionType) {
		super();
		this.strategyId = strategyId;
		this.userId = userId;
		this.creatorId = creatorId;
		this.ownerId = ownerId;
		this.subscriptionType = subscriptionType;
		this.startedAt = Timestamp.now();

		// For ONE_TIME, no expiration
		if (subscriptionType == PricingType.ONE_TIME || subscriptionType == PricingType.FREE) {
			this.expiresAt = null;
			this.renewalDate = null;
		}
	}

	// Backward compatibility constructor (assumes creator = owner initially)
	public StrategySubscriptionEntity(String strategyId, String userId, String creatorId,
			PricingType subscriptionType) {
		this(strategyId, userId, creatorId, creatorId, subscriptionType);
	}

	// Getters and Setters
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public PricingType getSubscriptionType() {
		return subscriptionType;
	}

	public void setSubscriptionType(PricingType subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public SubscriptionStatus getStatus() {
		return status;
	}

	public void setStatus(SubscriptionStatus status) {
		this.status = status;
	}

	public Timestamp getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Timestamp startedAt) {
		this.startedAt = startedAt;
	}

	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Timestamp expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Timestamp getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Timestamp cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public Timestamp getRenewalDate() {
		return renewalDate;
	}

	public void setRenewalDate(Timestamp renewalDate) {
		this.renewalDate = renewalDate;
	}

	public BigDecimal getPricePaid() {
		return pricePaid;
	}

	public void setPricePaid(BigDecimal pricePaid) {
		this.pricePaid = pricePaid;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public Boolean getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(Boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	public Integer getActiveDeployments() {
		return activeDeployments;
	}

	public void setActiveDeployments(Integer activeDeployments) {
		this.activeDeployments = activeDeployments;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	// Helper methods

	/**
	 * Checks if the subscription is currently valid (can use the strategy).
	 */
	public boolean isValid() {
		if (status == SubscriptionStatus.EXPIRED || status == SubscriptionStatus.SUSPENDED) {
			return false;
		}

		// For ONE_TIME and FREE, always valid if active
		if (subscriptionType == PricingType.ONE_TIME || subscriptionType == PricingType.FREE) {
			return status == SubscriptionStatus.ACTIVE;
		}

		// For SUBSCRIPTION, check expiry
		if (expiresAt != null && expiresAt.compareTo(Timestamp.now()) < 0) {
			return false;
		}

		return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL
				|| status == SubscriptionStatus.CANCELLED;
	}

	/**
	 * Checks if the subscription needs renewal.
	 */
	public boolean needsRenewal() {
		if (subscriptionType != PricingType.SUBSCRIPTION) {
			return false;
		}
		if (renewalDate == null) {
			return false;
		}
		return renewalDate.compareTo(Timestamp.now()) <= 0;
	}

	/**
	 * Cancel the subscription (still valid until expiry).
	 */
	public void cancel() {
		this.status = SubscriptionStatus.CANCELLED;
		this.cancelledAt = Timestamp.now();
		this.autoRenew = false;
	}

	/**
	 * Expire the subscription.
	 */
	public void expire() {
		this.status = SubscriptionStatus.EXPIRED;
	}

	/**
	 * Increment deployment count.
	 */
	public void incrementDeployments() {
		this.activeDeployments = (this.activeDeployments != null ? this.activeDeployments : 0) + 1;
	}

	/**
	 * Decrement deployment count.
	 */
	public void decrementDeployments() {
		if (this.activeDeployments != null && this.activeDeployments > 0) {
			this.activeDeployments--;
		}
	}

	@Override
	public String toString() {
		return "StrategySubscriptionEntity{" + "id='" + id + '\'' + ", strategyId='" + strategyId + '\'' + ", userId='"
				+ userId + '\'' + ", subscriptionType=" + subscriptionType + ", status=" + status + ", startedAt="
				+ startedAt + ", expiresAt=" + expiresAt + ", isActive=" + getIsActive() + '}';
	}

}
