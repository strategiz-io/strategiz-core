package io.strategiz.data.social.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.math.BigDecimal;

/**
 * Entity representing an owner's subscription settings.
 * Owners can enable subscriptions to allow others to subscribe and deploy their PUBLIC strategies.
 *
 * <p>Collection path: users/{userId}/ownerSubscriptionSettings/current</p>
 *
 * <p>Business Model:</p>
 * <ul>
 *   <li>Platform fee: Configurable (see PlatformConfig entity)</li>
 *   <li>Owner sets ONE price in USD (no tiers)</li>
 *   <li>Payments via Stripe Connect or STRAT tokens</li>
 *   <li>Payouts: Via Stripe Connect (minus platform fee)</li>
 *   <li>Price changes: Existing subscribers are grandfathered at their original rate</li>
 * </ul>
 *
 * @see io.strategiz.data.social.entity.OwnerSubscription
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("ownerSubscriptionSettings")
public class OwnerSubscriptionSettings extends BaseEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("userId")
    private String userId; // Owner user ID

    @JsonProperty("enabled")
    private boolean enabled; // Whether subscriptions are enabled

    @JsonProperty("monthlyPrice")
    private BigDecimal monthlyPrice; // Current price for new subscribers

    @JsonProperty("currency")
    private String currency; // USD

    @JsonProperty("profilePitch")
    private String profilePitch; // Marketing pitch shown on public profile (max 500 chars)

    @JsonProperty("stripeConnectAccountId")
    private String stripeConnectAccountId; // Stripe Connect account ID

    @JsonProperty("stripeConnectStatus")
    private String stripeConnectStatus; // not_started | pending | active | restricted

    @JsonProperty("subscriberCount")
    private int subscriberCount; // Denormalized for quick access

    @JsonProperty("totalRevenue")
    private BigDecimal totalRevenue; // Total lifetime revenue (in cents)

    @JsonProperty("monthlyRevenue")
    private BigDecimal monthlyRevenue; // Current month revenue (in cents)

    @JsonProperty("publicStrategyCount")
    private int publicStrategyCount; // Number of public strategies (denormalized)

    @JsonProperty("enabledAt")
    private Timestamp enabledAt; // When subscriptions were first enabled

    @JsonProperty("payoutsEnabled")
    private boolean payoutsEnabled; // Whether Stripe payouts are enabled

    // Constructors
    public OwnerSubscriptionSettings() {
        super();
        this.enabled = false;
        this.currency = "USD";
        this.monthlyPrice = BigDecimal.ZERO;
        this.stripeConnectStatus = "not_started";
        this.subscriberCount = 0;
        this.totalRevenue = BigDecimal.ZERO;
        this.monthlyRevenue = BigDecimal.ZERO;
        this.publicStrategyCount = 0;
        this.payoutsEnabled = false;
    }

    public OwnerSubscriptionSettings(String userId) {
        this();
        this.userId = userId;
        this.id = userId; // Use userId as document ID for easy lookup
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

    public int getPublicStrategyCount() {
        return publicStrategyCount;
    }

    public void setPublicStrategyCount(int publicStrategyCount) {
        this.publicStrategyCount = publicStrategyCount;
    }

    public Timestamp getEnabledAt() {
        return enabledAt;
    }

    public void setEnabledAt(Timestamp enabledAt) {
        this.enabledAt = enabledAt;
    }

    public boolean isPayoutsEnabled() {
        return payoutsEnabled;
    }

    public void setPayoutsEnabled(boolean payoutsEnabled) {
        this.payoutsEnabled = payoutsEnabled;
    }

    // Helper methods

    /**
     * Check if Stripe Connect is fully configured and active
     */
    public boolean hasActiveStripeConnect() {
        return "active".equals(stripeConnectStatus) && stripeConnectAccountId != null;
    }

    /**
     * Check if subscriptions can be enabled (Stripe must be connected)
     */
    public boolean canEnableSubscriptions() {
        return hasActiveStripeConnect();
    }

    /**
     * Check if owner can receive payments
     */
    public boolean canReceivePayments() {
        return hasActiveStripeConnect() && payoutsEnabled;
    }

    /**
     * Calculate owner's net revenue after platform fee.
     *
     * @param platformFeePercent The platform fee as a decimal (e.g., 0.15 for 15%)
     * @return Net revenue after fee deduction
     */
    public BigDecimal getNetRevenue(BigDecimal platformFeePercent) {
        if (totalRevenue == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal ownerPercent = BigDecimal.ONE.subtract(platformFeePercent);
        return totalRevenue.multiply(ownerPercent);
    }

    /**
     * Calculate owner's net monthly revenue after platform fee.
     *
     * @param platformFeePercent The platform fee as a decimal (e.g., 0.15 for 15%)
     * @return Net monthly revenue after fee deduction
     */
    public BigDecimal getNetMonthlyRevenue(BigDecimal platformFeePercent) {
        if (monthlyRevenue == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal ownerPercent = BigDecimal.ONE.subtract(platformFeePercent);
        return monthlyRevenue.multiply(ownerPercent);
    }

    /**
     * @deprecated Use {@link #getNetRevenue(BigDecimal)} instead. Fee is now configurable.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getNetRevenue() {
        // Default to 15% for backward compatibility
        return getNetRevenue(new BigDecimal("0.15"));
    }

    /**
     * @deprecated Use {@link #getNetMonthlyRevenue(BigDecimal)} instead. Fee is now configurable.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getNetMonthlyRevenue() {
        // Default to 15% for backward compatibility
        return getNetMonthlyRevenue(new BigDecimal("0.15"));
    }

    @Override
    public String toString() {
        return "OwnerSubscriptionSettings{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", enabled=" + enabled +
                ", monthlyPrice=" + monthlyPrice +
                ", subscriberCount=" + subscriberCount +
                ", stripeConnectStatus='" + stripeConnectStatus + '\'' +
                '}';
    }
}
