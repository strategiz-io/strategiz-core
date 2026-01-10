package io.strategiz.data.strategy.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Live performance metrics for alert deployments.
 * Tracks real-time signal activity and statistics for deployed alerts.
 * This data is owner-only (not public).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertLivePerformance {

	// Signal Statistics
	@JsonProperty("totalSignals")
	private Integer totalSignals;

	@JsonProperty("signalsThisMonth")
	private Integer signalsThisMonth;

	@JsonProperty("signalsThisWeek")
	private Integer signalsThisWeek;

	@JsonProperty("signalsToday")
	private Integer signalsToday;

	// Signal Breakdown (BUY, SELL, HOLD counts)
	@JsonProperty("signalBreakdown")
	private Map<String, Integer> signalBreakdown; // {"BUY": 47, "SELL": 32, "HOLD": 12}

	// Symbol-specific stats
	@JsonProperty("signalsBySymbol")
	private Map<String, Integer> signalsBySymbol; // {"AAPL": 23, "MSFT": 15}

	// Timing metrics
	@JsonProperty("deploymentStartDate")
	private Instant deploymentStartDate;

	@JsonProperty("lastSignalDate")
	private Instant lastSignalDate;

	@JsonProperty("daysSinceDeployment")
	private Integer daysSinceDeployment;

	// Optional: Outcome tracking (if user reports trade results)
	@JsonProperty("trackedOutcomes")
	private Integer trackedOutcomes; // How many signals user marked as win/loss

	@JsonProperty("reportedWins")
	private Integer reportedWins;

	@JsonProperty("reportedWinRate")
	private Double reportedWinRate; // Based on user feedback

	// Constructors
	public AlertLivePerformance() {
	}

	// Getters and Setters
	public Integer getTotalSignals() {
		return totalSignals;
	}

	public void setTotalSignals(Integer totalSignals) {
		this.totalSignals = totalSignals;
	}

	public Integer getSignalsThisMonth() {
		return signalsThisMonth;
	}

	public void setSignalsThisMonth(Integer signalsThisMonth) {
		this.signalsThisMonth = signalsThisMonth;
	}

	public Integer getSignalsThisWeek() {
		return signalsThisWeek;
	}

	public void setSignalsThisWeek(Integer signalsThisWeek) {
		this.signalsThisWeek = signalsThisWeek;
	}

	public Integer getSignalsToday() {
		return signalsToday;
	}

	public void setSignalsToday(Integer signalsToday) {
		this.signalsToday = signalsToday;
	}

	public Map<String, Integer> getSignalBreakdown() {
		return signalBreakdown;
	}

	public void setSignalBreakdown(Map<String, Integer> signalBreakdown) {
		this.signalBreakdown = signalBreakdown;
	}

	public Map<String, Integer> getSignalsBySymbol() {
		return signalsBySymbol;
	}

	public void setSignalsBySymbol(Map<String, Integer> signalsBySymbol) {
		this.signalsBySymbol = signalsBySymbol;
	}

	public Instant getDeploymentStartDate() {
		return deploymentStartDate;
	}

	public void setDeploymentStartDate(Instant deploymentStartDate) {
		this.deploymentStartDate = deploymentStartDate;
	}

	public Instant getLastSignalDate() {
		return lastSignalDate;
	}

	public void setLastSignalDate(Instant lastSignalDate) {
		this.lastSignalDate = lastSignalDate;
	}

	public Integer getDaysSinceDeployment() {
		return daysSinceDeployment;
	}

	public void setDaysSinceDeployment(Integer daysSinceDeployment) {
		this.daysSinceDeployment = daysSinceDeployment;
	}

	public Integer getTrackedOutcomes() {
		return trackedOutcomes;
	}

	public void setTrackedOutcomes(Integer trackedOutcomes) {
		this.trackedOutcomes = trackedOutcomes;
	}

	public Integer getReportedWins() {
		return reportedWins;
	}

	public void setReportedWins(Integer reportedWins) {
		this.reportedWins = reportedWins;
	}

	public Double getReportedWinRate() {
		return reportedWinRate;
	}

	public void setReportedWinRate(Double reportedWinRate) {
		this.reportedWinRate = reportedWinRate;
	}

}
