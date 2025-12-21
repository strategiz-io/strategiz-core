package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response model for subscription details.
 */
public class SubscriptionResponse {

	@JsonProperty("tier")
	private String tier;

	@JsonProperty("tierName")
	private String tierName;

	@JsonProperty("status")
	private String status;

	@JsonProperty("priceInCents")
	private int priceInCents;

	@JsonProperty("allowedModels")
	private List<String> allowedModels;

	@JsonProperty("dailyMessageLimit")
	private int dailyMessageLimit;

	@JsonProperty("dailyStrategyLimit")
	private int dailyStrategyLimit;

	@JsonProperty("dailyMessagesUsed")
	private int dailyMessagesUsed;

	@JsonProperty("dailyStrategiesUsed")
	private int dailyStrategiesUsed;

	@JsonProperty("cancelAtPeriodEnd")
	private boolean cancelAtPeriodEnd;

	// Default constructor
	public SubscriptionResponse() {
	}

	// Getters and setters
	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public String getTierName() {
		return tierName;
	}

	public void setTierName(String tierName) {
		this.tierName = tierName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getPriceInCents() {
		return priceInCents;
	}

	public void setPriceInCents(int priceInCents) {
		this.priceInCents = priceInCents;
	}

	public List<String> getAllowedModels() {
		return allowedModels;
	}

	public void setAllowedModels(List<String> allowedModels) {
		this.allowedModels = allowedModels;
	}

	public int getDailyMessageLimit() {
		return dailyMessageLimit;
	}

	public void setDailyMessageLimit(int dailyMessageLimit) {
		this.dailyMessageLimit = dailyMessageLimit;
	}

	public int getDailyStrategyLimit() {
		return dailyStrategyLimit;
	}

	public void setDailyStrategyLimit(int dailyStrategyLimit) {
		this.dailyStrategyLimit = dailyStrategyLimit;
	}

	public int getDailyMessagesUsed() {
		return dailyMessagesUsed;
	}

	public void setDailyMessagesUsed(int dailyMessagesUsed) {
		this.dailyMessagesUsed = dailyMessagesUsed;
	}

	public int getDailyStrategiesUsed() {
		return dailyStrategiesUsed;
	}

	public void setDailyStrategiesUsed(int dailyStrategiesUsed) {
		this.dailyStrategiesUsed = dailyStrategiesUsed;
	}

	public boolean isCancelAtPeriodEnd() {
		return cancelAtPeriodEnd;
	}

	public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
		this.cancelAtPeriodEnd = cancelAtPeriodEnd;
	}

}
