package io.strategiz.data.strategy.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Strategy performance metrics from backtest execution
 *
 * Stores comprehensive performance data for trading strategies including returns, win
 * rates, trades, and risk metrics like Sharpe ratio and max drawdown.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyPerformance implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonProperty("totalReturn")
	private Double totalReturn; // Return percentage (e.g., 82.7 for 82.7%)

	@JsonProperty("totalPnL")
	private Double totalPnL; // Total profit/loss in currency

	@JsonProperty("winRate")
	private Double winRate; // Win rate percentage (0-100)

	@JsonProperty("totalTrades")
	private Integer totalTrades; // Total number of trades executed

	@JsonProperty("profitableTrades")
	private Integer profitableTrades; // Number of winning trades

	@JsonProperty("buyCount")
	private Integer buyCount; // Number of buy signals

	@JsonProperty("sellCount")
	private Integer sellCount; // Number of sell signals

	@JsonProperty("avgWin")
	private Double avgWin; // Average winning trade amount

	@JsonProperty("avgLoss")
	private Double avgLoss; // Average losing trade amount

	@JsonProperty("profitFactor")
	private Double profitFactor; // Gross profit / gross loss

	@JsonProperty("maxDrawdown")
	private Double maxDrawdown; // Maximum drawdown percentage

	@JsonProperty("sharpeRatio")
	private Double sharpeRatio; // Risk-adjusted return metric

	@JsonProperty("lastTestedAt")
	private String lastTestedAt; // ISO timestamp of last backtest

	@JsonProperty("testPeriod")
	private String testPeriod; // Period tested (e.g., "7Y", "1Y", "6M")

	@JsonProperty("backtestStartDate")
	private String backtestStartDate; // ISO timestamp of first data point

	@JsonProperty("backtestEndDate")
	private String backtestEndDate; // ISO timestamp of last data point

	@JsonProperty("backtestPeriodDays")
	private Integer backtestPeriodDays; // Actual days tested

	@JsonProperty("timeframe")
	private String timeframe; // Candlestick interval (e.g., "1D", "1h", "30m")

	// Constructors
	public StrategyPerformance() {
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

	public Integer getBuyCount() {
		return buyCount;
	}

	public void setBuyCount(Integer buyCount) {
		this.buyCount = buyCount;
	}

	public Integer getSellCount() {
		return sellCount;
	}

	public void setSellCount(Integer sellCount) {
		this.sellCount = sellCount;
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

	public Double getProfitFactor() {
		return profitFactor;
	}

	public void setProfitFactor(Double profitFactor) {
		this.profitFactor = profitFactor;
	}

	public Double getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(Double maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public Double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(Double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public String getLastTestedAt() {
		return lastTestedAt;
	}

	public void setLastTestedAt(String lastTestedAt) {
		this.lastTestedAt = lastTestedAt;
	}

	public String getTestPeriod() {
		return testPeriod;
	}

	public void setTestPeriod(String testPeriod) {
		this.testPeriod = testPeriod;
	}

	public String getBacktestStartDate() {
		return backtestStartDate;
	}

	public void setBacktestStartDate(String backtestStartDate) {
		this.backtestStartDate = backtestStartDate;
	}

	public String getBacktestEndDate() {
		return backtestEndDate;
	}

	public void setBacktestEndDate(String backtestEndDate) {
		this.backtestEndDate = backtestEndDate;
	}

	public Integer getBacktestPeriodDays() {
		return backtestPeriodDays;
	}

	public void setBacktestPeriodDays(Integer backtestPeriodDays) {
		this.backtestPeriodDays = backtestPeriodDays;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}

	// Helper methods

	/**
	 * Check if performance data exists
	 */
	public boolean hasData() {
		return totalReturn != null || totalTrades != null;
	}

	/**
	 * Check if this strategy is profitable
	 */
	public boolean isProfitable() {
		return totalReturn != null && totalReturn > 0;
	}

	/**
	 * Check if win rate is good (>= 50%)
	 */
	public boolean hasGoodWinRate() {
		return winRate != null && winRate >= 50;
	}

	/**
	 * Check if performance data is empty/null
	 */
	public boolean isEmpty() {
		return !hasData();
	}

}
