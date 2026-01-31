package io.strategiz.data.social.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.math.BigDecimal;

/**
 * Entity representing a user-to-user (owner) subscription. Users subscribe to OWNERS (not
 * individual strategies) to deploy all of the owner's PUBLIC strategies.
 *
 * <p>
 * Collection path: users/{ownerId}/subscribers/{subscriptionId}
 * </p>
 *
 * <p>
 * Business Model (NO TIERS - Owner sets ONE price):
 * </p>
 * <ul>
 * <li>Owner sets a monthly price in USD (e.g., $50/month)</li>
 * <li>Subscriber pays in USD or STRAT tokens</li>
 * <li>Subscriber gets access to deploy ALL owner's PUBLIC strategies</li>
 * <li>Platform fee is configurable (see PlatformConfig)</li>
 * <li>Owner cashes out via Stripe Connect</li>
 * </ul>
 *
 * <p>
 * Subscriptions persist across strategy ownership transfers:
 * </p>
 * <ul>
 * <li>If Bob subscribes to Alice, he can deploy Alice's owned strategies</li>
 * <li>If Alice sells a strategy to Charlie, Bob still subscribes to Alice (not that
 * strategy)</li>
 * <li>Bob can now deploy all of Alice's remaining strategies + follow strategy ownership
 * changes</li>
 * </ul>
 *
 * <p>
 * Note: This is different from PlatformSubscription (data-preferences) which tracks user
 * subscriptions to the Strategiz PLATFORM (Explorer, Strategist, Quant tiers).
 * </p>
 *
 * @see io.strategiz.data.social.entity.OwnerSubscriptionSettings
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("ownerSubscriptions")
public class OwnerSubscription extends BaseEntity {

	@JsonProperty("id")
	private String id;

	@JsonProperty("subscriberId")
	private String subscriberId; // User who subscribes

	@JsonProperty("ownerId")
	private String ownerId; // User being subscribed to

	@JsonProperty("monthlyPrice")
	private BigDecimal monthlyPrice; // Owner's set price in USD

	@JsonProperty("stratTokenPrice")
	private Long stratTokenPrice; // Equivalent price in STRAT tokens (auto-calculated)

	@JsonProperty("paymentMethod")
	private String paymentMethod; // USD | STRAT

	@JsonProperty("status")
	private String status; // ACTIVE | CANCELLED | EXPIRED

	@JsonProperty("subscribedAt")
	private Timestamp subscribedAt;

	@JsonProperty("expiresAt")
	private Timestamp expiresAt; // null for active subscriptions

	@JsonProperty("stripeSubscriptionId")
	private String stripeSubscriptionId; // null for STRAT payments

	@JsonProperty("cancelledAt")
	private Timestamp cancelledAt; // When subscription was cancelled

	@JsonProperty("cancellationReason")
	private String cancellationReason; // Why subscription was cancelled

	// Payment method constants
	public static final String PAYMENT_USD = "USD";

	public static final String PAYMENT_STRAT = "STRAT";

	// Status constants
	public static final String STATUS_ACTIVE = "ACTIVE";

	public static final String STATUS_CANCELLED = "CANCELLED";

	public static final String STATUS_EXPIRED = "EXPIRED";

	// Constructors
	public OwnerSubscription() {
		super();
		this.status = STATUS_ACTIVE;
		this.monthlyPrice = BigDecimal.ZERO;
		this.paymentMethod = PAYMENT_USD;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	// Getters and Setters
	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public BigDecimal getMonthlyPrice() {
		return monthlyPrice;
	}

	public void setMonthlyPrice(BigDecimal monthlyPrice) {
		this.monthlyPrice = monthlyPrice;
	}

	public Long getStratTokenPrice() {
		return stratTokenPrice;
	}

	public void setStratTokenPrice(Long stratTokenPrice) {
		this.stratTokenPrice = stratTokenPrice;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getSubscribedAt() {
		return subscribedAt;
	}

	public void setSubscribedAt(Timestamp subscribedAt) {
		this.subscribedAt = subscribedAt;
	}

	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Timestamp expiresAt) {
		this.expiresAt = expiresAt;
	}

	public String getStripeSubscriptionId() {
		return stripeSubscriptionId;
	}

	public void setStripeSubscriptionId(String stripeSubscriptionId) {
		this.stripeSubscriptionId = stripeSubscriptionId;
	}

	public Timestamp getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Timestamp cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public void setCancellationReason(String cancellationReason) {
		this.cancellationReason = cancellationReason;
	}

	// Helper methods

	/**
	 * Check if this is a free subscription (zero price).
	 */
	public boolean isFree() {
		return monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) == 0;
	}

	/**
	 * Check if this is a paid subscription.
	 */
	public boolean isPaid() {
		return !isFree();
	}

	/**
	 * Check if subscription is currently active.
	 */
	public boolean isActive() {
		return STATUS_ACTIVE.equals(status);
	}

	/**
	 * Check if subscription is cancelled.
	 */
	public boolean isCancelled() {
		return STATUS_CANCELLED.equals(status);
	}

	/**
	 * Check if subscription is expired.
	 */
	public boolean isExpired() {
		return STATUS_EXPIRED.equals(status);
	}

	/**
	 * Check if subscription was paid with USD (has Stripe subscription).
	 */
	public boolean isPaidWithUsd() {
		return PAYMENT_USD.equals(paymentMethod);
	}

	/**
	 * Check if subscription was paid with STRAT tokens.
	 */
	public boolean isPaidWithStrat() {
		return PAYMENT_STRAT.equals(paymentMethod);
	}

	/**
	 * Check if subscription has Stripe integration.
	 */
	public boolean hasStripeSubscription() {
		return stripeSubscriptionId != null && !stripeSubscriptionId.isEmpty();
	}

	/**
	 * @deprecated Use {@link #getMonthlyPrice()} instead. Tier field has been removed.
	 */
	@Deprecated(forRemoval = true)
	public String getTier() {
		// Return tier based on whether subscription is free or paid (for backward
		// compatibility)
		return isFree() ? "FREE" : "BASIC";
	}

	/**
	 * @deprecated Tier field has been removed. Owner sets ONE price, no tiers.
	 */
	@Deprecated(forRemoval = true)
	public void setTier(String tier) {
		// No-op for backward compatibility - tier field removed
	}

}
