package io.strategiz.data.social.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.math.BigDecimal;

/**
 * Entity representing a user-to-user subscription.
 * Users subscribe to OWNERS (not individual strategies) to deploy all of the owner's strategies.
 *
 * Collection path: users/{ownerId}/subscribers/{subscriptionId}
 *
 * Business Model:
 * - FREE tier: Follow-only access (view performance, activity feed)
 * - BASIC tier: Deploy owner's strategies with your own money
 * - PREMIUM tier: Deploy + priority support + advanced features
 *
 * Subscriptions persist across strategy ownership transfers:
 * - If Bob subscribes to Alice, he can deploy Alice's owned strategies
 * - If Alice sells a strategy to Charlie, Bob still subscribes to Alice (not that strategy)
 * - Bob can now deploy all of Alice's remaining strategies + follow strategy ownership changes
 *
 * @author Strategiz Team
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("userSubscriptions")
public class UserSubscription extends BaseEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("subscriberId")
    private String subscriberId; // User who subscribes

    @JsonProperty("ownerId")
    private String ownerId; // User being subscribed to

    @JsonProperty("tier")
    private String tier; // FREE | BASIC | PREMIUM

    @JsonProperty("monthlyPrice")
    private BigDecimal monthlyPrice; // 0 for free subscriptions

    @JsonProperty("status")
    private String status; // ACTIVE | CANCELLED | EXPIRED

    @JsonProperty("subscribedAt")
    private Timestamp subscribedAt;

    @JsonProperty("expiresAt")
    private Timestamp expiresAt; // null for active subscriptions

    @JsonProperty("stripeSubscriptionId")
    private String stripeSubscriptionId; // null for free subscriptions

    @JsonProperty("cancelledAt")
    private Timestamp cancelledAt; // When subscription was cancelled

    @JsonProperty("cancellationReason")
    private String cancellationReason; // Why subscription was cancelled

    // Constructors
    public UserSubscription() {
        super();
        this.status = "ACTIVE";
        this.monthlyPrice = BigDecimal.ZERO;
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

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
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
     * Check if this is a free subscription
     */
    public boolean isFree() {
        return "FREE".equals(tier) || monthlyPrice == null || monthlyPrice.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if this is a paid subscription
     */
    public boolean isPaid() {
        return !isFree();
    }

    /**
     * Check if subscription is currently active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Check if subscription is cancelled
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    /**
     * Check if subscription is expired
     */
    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    /**
     * Check if subscription has Stripe integration
     */
    public boolean hasStripeSubscription() {
        return stripeSubscriptionId != null && !stripeSubscriptionId.isEmpty();
    }
}
