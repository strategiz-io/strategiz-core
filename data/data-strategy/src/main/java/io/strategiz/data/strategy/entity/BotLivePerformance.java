package io.strategiz.data.strategy.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Live performance metrics for bot deployments. Tracks real-time trading performance and
 * comprehensive metrics for deployed bots. This mirrors StrategyPerformance but
 * represents actual live/paper trading results. This data is owner-only (not public).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotLivePerformance {

	// Core Performance Metrics (same as StrategyPerformance)
	@JsonProperty("totalReturn")
	private Double totalReturn; // Percentage return

	@JsonProperty("totalPnL")
	private Double totalPnL; // Dollar P&L

	@JsonProperty("winRate")
	private Double winRate; // Percentage (0-100)

	@JsonProperty("sharpeRatio")
	private Double sharpeRatio;

	@JsonProperty("maxDrawdown")
	private Double maxDrawdown; // Percentage

	@JsonProperty("profitFactor")
	private Double profitFactor; // Gross profit / gross loss

	// Trade Statistics
	@JsonProperty("totalTrades")
	private Integer totalTrades;

	@JsonProperty("profitableTrades")
	private Integer profitableTrades;

	@JsonProperty("losingTrades")
	private Integer losingTrades;

	@JsonProperty("avgWin")
	private Double avgWin;

	@JsonProperty("avgLoss")
	private Double avgLoss;

	@JsonProperty("largestWin")
	private Double largestWin;

	@JsonProperty("largestLoss")
	private Double largestLoss;

	// Current State
	@JsonProperty("currentDrawdown")
	private Double currentDrawdown; // Current drawdown from peak

	@JsonProperty("peakEquity")
	private Double peakEquity; // Highest portfolio value reached

	@JsonProperty("currentEquity")
	private Double currentEquity;

	// Real Trading Costs (not in backtest)
	@JsonProperty("totalSlippage")
	private Double totalSlippage; // Total slippage cost

	@JsonProperty("totalFees")
	private Double totalFees; // Brokerage fees/commissions

	@JsonProperty("avgSlippagePercent")
	private Double avgSlippagePercent;

	// Deployment Timeline
	@JsonProperty("deploymentStartDate")
	private Instant deploymentStartDate;

	@JsonProperty("lastTradeDate")
	private Instant lastTradeDate;

	@JsonProperty("daysSinceDeployment")
	private Integer daysSinceDeployment;

	// Trade Breakdown by Symbol (if multi-symbol bot)
	@JsonProperty("tradesBySymbol")
	private Map<String, Integer> tradesBySymbol;

	@JsonProperty("pnlBySymbol")
	private Map<String, Double> pnlBySymbol;

	// Constructors
	public BotLivePerformance() {
	}

	// Helper methods
	public boolean isProfitable() {
		return totalReturn != null && totalReturn > 0;
	}

	public boolean hasGoodWinRate() {
		return winRate != null && winRate >= 50.0;
	}

	// Getters and Setters
	public Double getTotalReturn() {
		return totalReturn;
	}

	public void setTotalReturn(Double totalReturn) {
		this.totalReturn = totalReturn;
	}

	public Double getTotalPnL() {
		return totalPnL;
	}

	public void setTotalPnL(Double totalPnL) {
		this.totalPnL = totalPnL;
	}

	public Double getWinRate() {
		return winRate;
	}

	public void setWinRate(Double winRate) {
		this.winRate = winRate;
	}

	public Double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(Double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public Double getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(Double maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public Double getProfitFactor() {
		return profitFactor;
	}

	public void setProfitFactor(Double profitFactor) {
		this.profitFactor = profitFactor;
	}

	public Integer getTotalTrades() {
		return totalTrades;
	}

	public void setTotalTrades(Integer totalTrades) {
		this.totalTrades = totalTrades;
	}

	public Integer getProfitableTrades() {
		return profitableTrades;
	}

	public void setProfitableTrades(Integer profitableTrades) {
		this.profitableTrades = profitableTrades;
	}

	public Integer getLosingTrades() {
		return losingTrades;
	}

	public void setLosingTrades(Integer losingTrades) {
		this.losingTrades = losingTrades;
	}

	public Double getAvgWin() {
		return avgWin;
	}

	public void setAvgWin(Double avgWin) {
		this.avgWin = avgWin;
	}

	public Double getAvgLoss() {
		return avgLoss;
	}

	public void setAvgLoss(Double avgLoss) {
		this.avgLoss = avgLoss;
	}

	public Double getLargestWin() {
		return largestWin;
	}

	public void setLargestWin(Double largestWin) {
		this.largestWin = largestWin;
	}

	public Double getLargestLoss() {
		return largestLoss;
	}

	public void setLargestLoss(Double largestLoss) {
		this.largestLoss = largestLoss;
	}

	public Double getCurrentDrawdown() {
		return currentDrawdown;
	}

	public void setCurrentDrawdown(Double currentDrawdown) {
		this.currentDrawdown = currentDrawdown;
	}

	public Double getPeakEquity() {
		return peakEquity;
	}

	public void setPeakEquity(Double peakEquity) {
		this.peakEquity = peakEquity;
	}

	public Double getCurrentEquity() {
		return currentEquity;
	}

	public void setCurrentEquity(Double currentEquity) {
		this.currentEquity = currentEquity;
	}

	public Double getTotalSlippage() {
		return totalSlippage;
	}

	public void setTotalSlippage(Double totalSlippage) {
		this.totalSlippage = totalSlippage;
	}

	public Double getTotalFees() {
		return totalFees;
	}

	public void setTotalFees(Double totalFees) {
		this.totalFees = totalFees;
	}

	public Double getAvgSlippagePercent() {
		return avgSlippagePercent;
	}

	public void setAvgSlippagePercent(Double avgSlippagePercent) {
		this.avgSlippagePercent = avgSlippagePercent;
	}

	public Instant getDeploymentStartDate() {
		return deploymentStartDate;
	}

	public void setDeploymentStartDate(Instant deploymentStartDate) {
		this.deploymentStartDate = deploymentStartDate;
	}

	public Instant getLastTradeDate() {
		return lastTradeDate;
	}

	public void setLastTradeDate(Instant lastTradeDate) {
		this.lastTradeDate = lastTradeDate;
	}

	public Integer getDaysSinceDeployment() {
		return daysSinceDeployment;
	}

	public void setDaysSinceDeployment(Integer daysSinceDeployment) {
		this.daysSinceDeployment = daysSinceDeployment;
	}

	public Map<String, Integer> getTradesBySymbol() {
		return tradesBySymbol;
	}

	public void setTradesBySymbol(Map<String, Integer> tradesBySymbol) {
		this.tradesBySymbol = tradesBySymbol;
	}

	public Map<String, Double> getPnlBySymbol() {
		return pnlBySymbol;
	}

	public void setPnlBySymbol(Map<String, Double> pnlBySymbol) {
		this.pnlBySymbol = pnlBySymbol;
	}

}
