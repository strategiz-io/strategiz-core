package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dashboard statistics response for My Strategies page.
 * Provides a comprehensive overview of all user strategies, alerts, and bots.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {

	// Strategy Overview
	@JsonProperty("totalStrategies")
	private Integer totalStrategies; // created + purchased

	@JsonProperty("createdStrategies")
	private Integer createdStrategies;

	@JsonProperty("purchasedStrategies")
	private Integer purchasedStrategies;

	@JsonProperty("publishedStrategies")
	private Integer publishedStrategies;

	// Deployment Overview
	@JsonProperty("totalAlerts")
	private Integer totalAlerts;

	@JsonProperty("activeAlerts")
	private Integer activeAlerts;

	@JsonProperty("totalBots")
	private Integer totalBots;

	@JsonProperty("activeBots")
	private Integer activeBots;

	// Signal Activity (This Month)
	@JsonProperty("totalSignalsThisMonth")
	private Integer totalSignalsThisMonth;

	@JsonProperty("alertSignalsThisMonth")
	private Integer alertSignalsThisMonth;

	@JsonProperty("botTradesThisMonth")
	private Integer botTradesThisMonth;

	// Average Performance (Backtest - across all strategies)
	@JsonProperty("avgBacktestReturn")
	private Double avgBacktestReturn;

	@JsonProperty("avgBacktestWinRate")
	private Double avgBacktestWinRate;

	// Live Performance Summary (across all deployments)
	@JsonProperty("avgLiveBotReturn")
	private Double avgLiveBotReturn; // Average across all bots

	@JsonProperty("avgLiveBotWinRate")
	private Double avgLiveBotWinRate;

	@JsonProperty("totalLiveTrades")
	private Integer totalLiveTrades; // Sum of all bot trades

	// Revenue (for published strategies)
	@JsonProperty("totalSubscribers")
	private Integer totalSubscribers;

	@JsonProperty("monthlyRevenue")
	private Double monthlyRevenue; // Sum of all subscription revenue

	// Activity Trends
	@JsonProperty("signalsToday")
	private Integer signalsToday;

	@JsonProperty("signalsThisWeek")
	private Integer signalsThisWeek;

	// Subscription Tier Info
	@JsonProperty("subscriptionTier")
	private String subscriptionTier; // FREE, PRO, etc.

	@JsonProperty("remainingAlerts")
	private Integer remainingAlerts;

	@JsonProperty("remainingBots")
	private Integer remainingBots;

	// Constructors
	public DashboardStatsResponse() {
	}

	// Getters and Setters
	public Integer getTotalStrategies() {
		return totalStrategies;
	}

	public void setTotalStrategies(Integer totalStrategies) {
		this.totalStrategies = totalStrategies;
	}

	public Integer getCreatedStrategies() {
		return createdStrategies;
	}

	public void setCreatedStrategies(Integer createdStrategies) {
		this.createdStrategies = createdStrategies;
	}

	public Integer getPurchasedStrategies() {
		return purchasedStrategies;
	}

	public void setPurchasedStrategies(Integer purchasedStrategies) {
		this.purchasedStrategies = purchasedStrategies;
	}

	public Integer getPublishedStrategies() {
		return publishedStrategies;
	}

	public void setPublishedStrategies(Integer publishedStrategies) {
		this.publishedStrategies = publishedStrategies;
	}

	public Integer getTotalAlerts() {
		return totalAlerts;
	}

	public void setTotalAlerts(Integer totalAlerts) {
		this.totalAlerts = totalAlerts;
	}

	public Integer getActiveAlerts() {
		return activeAlerts;
	}

	public void setActiveAlerts(Integer activeAlerts) {
		this.activeAlerts = activeAlerts;
	}

	public Integer getTotalBots() {
		return totalBots;
	}

	public void setTotalBots(Integer totalBots) {
		this.totalBots = totalBots;
	}

	public Integer getActiveBots() {
		return activeBots;
	}

	public void setActiveBots(Integer activeBots) {
		this.activeBots = activeBots;
	}

	public Integer getTotalSignalsThisMonth() {
		return totalSignalsThisMonth;
	}

	public void setTotalSignalsThisMonth(Integer totalSignalsThisMonth) {
		this.totalSignalsThisMonth = totalSignalsThisMonth;
	}

	public Integer getAlertSignalsThisMonth() {
		return alertSignalsThisMonth;
	}

	public void setAlertSignalsThisMonth(Integer alertSignalsThisMonth) {
		this.alertSignalsThisMonth = alertSignalsThisMonth;
	}

	public Integer getBotTradesThisMonth() {
		return botTradesThisMonth;
	}

	public void setBotTradesThisMonth(Integer botTradesThisMonth) {
		this.botTradesThisMonth = botTradesThisMonth;
	}

	public Double getAvgBacktestReturn() {
		return avgBacktestReturn;
	}

	public void setAvgBacktestReturn(Double avgBacktestReturn) {
		this.avgBacktestReturn = avgBacktestReturn;
	}

	public Double getAvgBacktestWinRate() {
		return avgBacktestWinRate;
	}

	public void setAvgBacktestWinRate(Double avgBacktestWinRate) {
		this.avgBacktestWinRate = avgBacktestWinRate;
	}

	public Double getAvgLiveBotReturn() {
		return avgLiveBotReturn;
	}

	public void setAvgLiveBotReturn(Double avgLiveBotReturn) {
		this.avgLiveBotReturn = avgLiveBotReturn;
	}

	public Double getAvgLiveBotWinRate() {
		return avgLiveBotWinRate;
	}

	public void setAvgLiveBotWinRate(Double avgLiveBotWinRate) {
		this.avgLiveBotWinRate = avgLiveBotWinRate;
	}

	public Integer getTotalLiveTrades() {
		return totalLiveTrades;
	}

	public void setTotalLiveTrades(Integer totalLiveTrades) {
		this.totalLiveTrades = totalLiveTrades;
	}

	public Integer getTotalSubscribers() {
		return totalSubscribers;
	}

	public void setTotalSubscribers(Integer totalSubscribers) {
		this.totalSubscribers = totalSubscribers;
	}

	public Double getMonthlyRevenue() {
		return monthlyRevenue;
	}

	public void setMonthlyRevenue(Double monthlyRevenue) {
		this.monthlyRevenue = monthlyRevenue;
	}

	public Integer getSignalsToday() {
		return signalsToday;
	}

	public void setSignalsToday(Integer signalsToday) {
		this.signalsToday = signalsToday;
	}

	public Integer getSignalsThisWeek() {
		return signalsThisWeek;
	}

	public void setSignalsThisWeek(Integer signalsThisWeek) {
		this.signalsThisWeek = signalsThisWeek;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Integer getRemainingAlerts() {
		return remainingAlerts;
	}

	public void setRemainingAlerts(Integer remainingAlerts) {
		this.remainingAlerts = remainingAlerts;
	}

	public Integer getRemainingBots() {
		return remainingBots;
	}

	public void setRemainingBots(Integer remainingBots) {
		this.remainingBots = remainingBots;
	}

}
