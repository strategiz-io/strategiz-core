package io.strategiz.data.strategy.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.PropertyName;

import java.math.BigDecimal;

/**
 * Embedded pricing configuration for a strategy.
 *
 * This is embedded within the Strategy entity, not a separate collection. Used when a
 * strategy is published to the marketplace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyPricing {

	@PropertyName("pricingType")
	@JsonProperty("pricingType")
	private PricingType pricingType = PricingType.FREE;

	@PropertyName("oneTimePrice")
	@JsonProperty("oneTimePrice")
	private BigDecimal oneTimePrice;

	@PropertyName("monthlyPrice")
	@JsonProperty("monthlyPrice")
	private BigDecimal monthlyPrice;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("trialDays")
	@JsonProperty("trialDays")
	private Integer trialDays;

	// Constructors
	public StrategyPricing() {
	}

	public static StrategyPricing free() {
		StrategyPricing pricing = new StrategyPricing();
		pricing.setPricingType(PricingType.FREE);
		return pricing;
	}

	public static StrategyPricing oneTime(BigDecimal price, String currency) {
		StrategyPricing pricing = new StrategyPricing();
		pricing.setPricingType(PricingType.ONE_TIME);
		pricing.setOneTimePrice(price);
		pricing.setCurrency(currency);
		return pricing;
	}

	public static StrategyPricing subscription(BigDecimal monthlyPrice, String currency, Integer trialDays) {
		StrategyPricing pricing = new StrategyPricing();
		pricing.setPricingType(PricingType.SUBSCRIPTION);
		pricing.setMonthlyPrice(monthlyPrice);
		pricing.setCurrency(currency);
		pricing.setTrialDays(trialDays);
		return pricing;
	}

	// Getters and Setters
	public PricingType getPricingType() {
		return pricingType;
	}

	public void setPricingType(PricingType pricingType) {
		this.pricingType = pricingType;
	}

	public BigDecimal getOneTimePrice() {
		return oneTimePrice;
	}

	public void setOneTimePrice(BigDecimal oneTimePrice) {
		this.oneTimePrice = oneTimePrice;
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

	public Integer getTrialDays() {
		return trialDays;
	}

	public void setTrialDays(Integer trialDays) {
		this.trialDays = trialDays;
	}

	// Helper methods
	public boolean isFree() {
		return pricingType == PricingType.FREE;
	}

	public boolean isPaid() {
		return pricingType != PricingType.FREE;
	}

	public boolean hasFreeTrial() {
		return trialDays != null && trialDays > 0;
	}

	/**
	 * Gets the effective price based on pricing type.
	 */
	public BigDecimal getEffectivePrice() {
		if (pricingType == PricingType.FREE) {
			return BigDecimal.ZERO;
		}
		else if (pricingType == PricingType.ONE_TIME) {
			return oneTimePrice != null ? oneTimePrice : BigDecimal.ZERO;
		}
		else {
			return monthlyPrice != null ? monthlyPrice : BigDecimal.ZERO;
		}
	}

	@Override
	public String toString() {
		return "StrategyPricing{" + "pricingType=" + pricingType + ", oneTimePrice=" + oneTimePrice + ", monthlyPrice="
				+ monthlyPrice + ", currency='" + currency + '\'' + ", trialDays=" + trialDays + '}';
	}

}
