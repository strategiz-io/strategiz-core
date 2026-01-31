package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregated deployment statistics for a strategy. Includes counts and performance
 * metrics across all alerts and bots.
 */
public class DeploymentStatsDTO {

	@JsonProperty("totalAlerts")
	private Integer totalAlerts;

	@JsonProperty("activeAlerts")
	private Integer activeAlerts;

	@JsonProperty("totalBots")
	private Integer totalBots;

	@JsonProperty("activeBots")
	private Integer activeBots;

	@JsonProperty("aggregatedAlertPerformance")
	private AggregatedAlertPerformanceDTO aggregatedAlertPerformance;

	@JsonProperty("aggregatedBotPerformance")
	private AggregatedBotPerformanceDTO aggregatedBotPerformance;

	// Getters and Setters
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

	public AggregatedAlertPerformanceDTO getAggregatedAlertPerformance() {
		return aggregatedAlertPerformance;
	}

	public void setAggregatedAlertPerformance(AggregatedAlertPerformanceDTO aggregatedAlertPerformance) {
		this.aggregatedAlertPerformance = aggregatedAlertPerformance;
	}

	public AggregatedBotPerformanceDTO getAggregatedBotPerformance() {
		return aggregatedBotPerformance;
	}

	public void setAggregatedBotPerformance(AggregatedBotPerformanceDTO aggregatedBotPerformance) {
		this.aggregatedBotPerformance = aggregatedBotPerformance;
	}

}
