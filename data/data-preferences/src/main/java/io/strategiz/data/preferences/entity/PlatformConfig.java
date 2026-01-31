package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Platform configuration stored at config/platform/current. Contains configurable
 * settings for the Strategiz platform.
 *
 * <p>
 * This entity stores platform-wide settings that can be adjusted without code changes,
 * including platform fees, STRAT token rates, and other configurable values.
 * </p>
 *
 * <p>
 * Firestore path: config/platform/current
 * </p>
 *
 * @see io.strategiz.data.social.entity.OwnerSubscription
 * @see io.strategiz.data.social.entity.OwnerSubscriptionSettings
 * @see io.strategiz.data.cryptotoken.entity.CryptoWallet
 */
@Collection("platformConfig")
public class PlatformConfig extends BaseEntity {

	public static final String CONFIG_ID = "current";

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id = CONFIG_ID;

	/**
	 * Platform fee percentage as a decimal (e.g., 0.15 for 15%). Applied to owner
	 * subscription revenue and tips.
	 */
	@PropertyName("platformFeePercent")
	@JsonProperty("platformFeePercent")
	private BigDecimal platformFeePercent;

	/**
	 * Number of STRAT tokens per 1 USD. Default: 100 (so $1 = 100 STRAT)
	 */
	@PropertyName("stratTokensPerUsd")
	@JsonProperty("stratTokensPerUsd")
	private Integer stratTokensPerUsd;

	/**
	 * Minimum purchase amount in USD cents. Default: 500 ($5.00)
	 */
	@PropertyName("minimumPurchaseCents")
	@JsonProperty("minimumPurchaseCents")
	private Integer minimumPurchaseCents;

	/**
	 * Minimum tip amount in STRAT tokens. Default: 10
	 */
	@PropertyName("minimumTipStrat")
	@JsonProperty("minimumTipStrat")
	private Long minimumTipStrat;

	/**
	 * Minimum withdrawal amount in STRAT tokens. Default: 1000
	 */
	@PropertyName("minimumWithdrawStrat")
	@JsonProperty("minimumWithdrawStrat")
	private Long minimumWithdrawStrat;

	/**
	 * Default owner subscription price in USD. Used as suggestion when owner enables
	 * subscriptions. Default: 50.00
	 */
	@PropertyName("defaultOwnerPrice")
	@JsonProperty("defaultOwnerPrice")
	private BigDecimal defaultOwnerPrice;

	@PropertyName("updatedAt")
	@JsonProperty("updatedAt")
	private Instant updatedAt;

	@PropertyName("updatedBy")
	@JsonProperty("updatedBy")
	private String updatedBy;

	// Constructors
	public PlatformConfig() {
		super();
		// Set sensible defaults
		this.platformFeePercent = new BigDecimal("0.15"); // 15%
		this.stratTokensPerUsd = 100;
		this.minimumPurchaseCents = 500; // $5
		this.minimumTipStrat = 10L;
		this.minimumWithdrawStrat = 1000L;
		this.defaultOwnerPrice = new BigDecimal("50.00");
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public BigDecimal getPlatformFeePercent() {
		return platformFeePercent;
	}

	public void setPlatformFeePercent(BigDecimal platformFeePercent) {
		this.platformFeePercent = platformFeePercent;
	}

	public Integer getStratTokensPerUsd() {
		return stratTokensPerUsd;
	}

	public void setStratTokensPerUsd(Integer stratTokensPerUsd) {
		this.stratTokensPerUsd = stratTokensPerUsd;
	}

	public Integer getMinimumPurchaseCents() {
		return minimumPurchaseCents;
	}

	public void setMinimumPurchaseCents(Integer minimumPurchaseCents) {
		this.minimumPurchaseCents = minimumPurchaseCents;
	}

	public Long getMinimumTipStrat() {
		return minimumTipStrat;
	}

	public void setMinimumTipStrat(Long minimumTipStrat) {
		this.minimumTipStrat = minimumTipStrat;
	}

	public Long getMinimumWithdrawStrat() {
		return minimumWithdrawStrat;
	}

	public void setMinimumWithdrawStrat(Long minimumWithdrawStrat) {
		this.minimumWithdrawStrat = minimumWithdrawStrat;
	}

	public BigDecimal getDefaultOwnerPrice() {
		return defaultOwnerPrice;
	}

	public void setDefaultOwnerPrice(BigDecimal defaultOwnerPrice) {
		this.defaultOwnerPrice = defaultOwnerPrice;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	// Helper methods

	/**
	 * Calculate the owner's share percentage (1 - platformFeePercent). E.g., if
	 * platformFeePercent is 0.15, owner share is 0.85 (85%).
	 */
	public BigDecimal getOwnerSharePercent() {
		return BigDecimal.ONE.subtract(platformFeePercent);
	}

	/**
	 * Convert USD amount to STRAT tokens.
	 * @param usdAmount Amount in USD
	 * @return Equivalent amount in STRAT tokens
	 */
	public long usdToStrat(BigDecimal usdAmount) {
		return usdAmount.multiply(BigDecimal.valueOf(stratTokensPerUsd)).longValue();
	}

	/**
	 * Convert STRAT tokens to USD amount.
	 * @param stratAmount Amount in STRAT tokens
	 * @return Equivalent amount in USD
	 */
	public BigDecimal stratToUsd(long stratAmount) {
		return BigDecimal.valueOf(stratAmount)
			.divide(BigDecimal.valueOf(stratTokensPerUsd), 2, java.math.RoundingMode.HALF_UP);
	}

	/**
	 * Calculate platform fee for a given amount.
	 * @param amount The gross amount
	 * @return The fee amount
	 */
	public BigDecimal calculateFee(BigDecimal amount) {
		return amount.multiply(platformFeePercent);
	}

	/**
	 * Calculate owner's net revenue after platform fee.
	 * @param grossAmount The gross revenue
	 * @return Net revenue after fee
	 */
	public BigDecimal calculateNetRevenue(BigDecimal grossAmount) {
		return grossAmount.multiply(getOwnerSharePercent());
	}

	@Override
	public String toString() {
		return "PlatformConfig{" + "id='" + id + '\'' + ", platformFeePercent=" + platformFeePercent
				+ ", stratTokensPerUsd=" + stratTokensPerUsd + ", minimumPurchaseCents=" + minimumPurchaseCents
				+ ", minimumTipStrat=" + minimumTipStrat + ", defaultOwnerPrice=" + defaultOwnerPrice + '}';
	}

}
