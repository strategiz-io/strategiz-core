package io.strategiz.business.historicalinsights.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of backtesting a single strategy configuration.
 * Used by the StrategyOptimizationEngine to track and rank strategy performance.
 */
public class StrategyTestResult implements Comparable<StrategyTestResult> {

	private StrategyType strategyType;

	@JsonProperty("parameters")
	private Map<String, Object> parameters;

	// PRIMARY RANKING METRIC - percentage return (e.g., 85.0 = 85% return)
	private double totalReturn;

	// Secondary metrics for analysis
	private double winRate; // 0.0 - 1.0

	private double maxDrawdown; // percentage (e.g., 15.0 = 15% drawdown)

	private double sharpeRatio;

	private double profitFactor;

	private int totalTrades;

	// Consecutive loss analysis
	private int maxConsecutiveLosses;

	private double worstLosingStreakPercent;

	// The generated Python code for this strategy
	private String pythonCode;

	// Execution metadata
	private long executionTimeMs;

	private boolean success;

	private String errorMessage;

	public StrategyTestResult() {
		this.parameters = new HashMap<>();
		this.success = true;
	}

	public StrategyTestResult(StrategyType strategyType, Map<String, Object> parameters) {
		this.strategyType = strategyType;
		this.parameters = parameters != null ? parameters : new HashMap<>();
		this.success = true;
	}

	/**
	 * Compare by total return (descending - higher return is better).
	 */
	@Override
	public int compareTo(StrategyTestResult other) {
		return Double.compare(other.totalReturn, this.totalReturn);
	}

	/**
	 * Creates a failed result with error message.
	 */
	public static StrategyTestResult failed(StrategyType type, Map<String, Object> params, String error) {
		StrategyTestResult result = new StrategyTestResult(type, params);
		result.setSuccess(false);
		result.setErrorMessage(error);
		result.setTotalReturn(Double.NEGATIVE_INFINITY);
		return result;
	}

	/**
	 * Generates a human-readable summary of this strategy's performance.
	 */
	public String toSummary() {
		if (!success) {
			return String.format("%s - FAILED: %s", strategyType.getDisplayName(), errorMessage);
		}
		return String.format("%s | Return: %.2f%% | Win Rate: %.1f%% | Sharpe: %.2f | Drawdown: %.1f%% | Trades: %d",
				strategyType.getDisplayName(), totalReturn, winRate * 100, sharpeRatio, maxDrawdown, totalTrades);
	}

	/**
	 * Returns the parameters as a formatted string for display.
	 */
	public String getParametersDisplay() {
		StringBuilder sb = new StringBuilder();
		parameters.forEach((key, value) -> {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(key).append("=").append(value);
		});
		return sb.toString();
	}

	// Getters and Setters

	public StrategyType getStrategyType() {
		return strategyType;
	}

	public void setStrategyType(StrategyType strategyType) {
		this.strategyType = strategyType;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public double getTotalReturn() {
		return totalReturn;
	}

	public void setTotalReturn(double totalReturn) {
		this.totalReturn = totalReturn;
	}

	public double getWinRate() {
		return winRate;
	}

	public void setWinRate(double winRate) {
		this.winRate = winRate;
	}

	public double getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(double maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public double getProfitFactor() {
		return profitFactor;
	}

	public void setProfitFactor(double profitFactor) {
		this.profitFactor = profitFactor;
	}

	public int getTotalTrades() {
		return totalTrades;
	}

	public void setTotalTrades(int totalTrades) {
		this.totalTrades = totalTrades;
	}

	public int getMaxConsecutiveLosses() {
		return maxConsecutiveLosses;
	}

	public void setMaxConsecutiveLosses(int maxConsecutiveLosses) {
		this.maxConsecutiveLosses = maxConsecutiveLosses;
	}

	public double getWorstLosingStreakPercent() {
		return worstLosingStreakPercent;
	}

	public void setWorstLosingStreakPercent(double worstLosingStreakPercent) {
		this.worstLosingStreakPercent = worstLosingStreakPercent;
	}

	public String getPythonCode() {
		return pythonCode;
	}

	public void setPythonCode(String pythonCode) {
		this.pythonCode = pythonCode;
	}

	public long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
