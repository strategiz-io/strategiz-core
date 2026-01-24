package io.strategiz.business.historicalinsights.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the final result of a strategy optimization run.
 * Contains the best performing strategy along with top alternatives and metadata.
 */
public class OptimizationResult {

	private String symbol;

	private String timeframe;

	private int daysAnalyzed;

	// The winning strategy with highest total return
	@JsonProperty("bestStrategy")
	private StrategyTestResult bestStrategy;

	// Top 5 strategies for comparison (includes the best)
	@JsonProperty("topStrategies")
	private List<StrategyTestResult> topStrategies;

	// Total number of parameter combinations tested
	private int totalCombinationsTested;

	// How many completed successfully
	private int successfulTests;

	// Market regime detected during analysis
	private String marketRegime; // TRENDING, SIDEWAYS, VOLATILE, MIXED

	// Optimization execution stats
	private long totalExecutionTimeMs;

	private double avgExecutionTimePerStrategy;

	// Buy-and-hold baseline for comparison
	private double buyAndHoldReturn;

	// Performance improvement over buy-and-hold
	private double outperformance;

	public OptimizationResult() {
		this.topStrategies = new ArrayList<>();
	}

	public OptimizationResult(String symbol, String timeframe, int daysAnalyzed) {
		this.symbol = symbol;
		this.timeframe = timeframe;
		this.daysAnalyzed = daysAnalyzed;
		this.topStrategies = new ArrayList<>();
	}

	/**
	 * Generates a human-readable summary of the optimization results.
	 */
	public String toSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("=== OPTIMIZATION RESULTS FOR %s (%s) ===\n", symbol, timeframe));
		sb.append(String.format("Period: %d days (~%.1f years)\n", daysAnalyzed, daysAnalyzed / 365.25));
		sb.append(String.format("Market Regime: %s\n", marketRegime));
		sb.append(String.format("Strategies Tested: %d (successful: %d)\n", totalCombinationsTested, successfulTests));
		sb.append(String.format("Total Execution Time: %.1f seconds\n\n", totalExecutionTimeMs / 1000.0));

		sb.append("--- BUY & HOLD BASELINE ---\n");
		sb.append(String.format("Return: %.2f%%\n\n", buyAndHoldReturn));

		sb.append("--- BEST STRATEGY ---\n");
		if (bestStrategy != null) {
			sb.append(String.format("Type: %s\n", bestStrategy.getStrategyType().getDisplayName()));
			sb.append(String.format("Parameters: %s\n", bestStrategy.getParametersDisplay()));
			sb.append(String.format("Total Return: %.2f%%\n", bestStrategy.getTotalReturn()));
			sb.append(String.format("Outperformance: %.2f%%\n", outperformance));
			sb.append(String.format("Win Rate: %.1f%%\n", bestStrategy.getWinRate() * 100));
			sb.append(String.format("Sharpe Ratio: %.2f\n", bestStrategy.getSharpeRatio()));
			sb.append(String.format("Max Drawdown: %.1f%%\n", bestStrategy.getMaxDrawdown()));
			sb.append(String.format("Total Trades: %d\n", bestStrategy.getTotalTrades()));
		}
		else {
			sb.append("No successful strategies found.\n");
		}

		if (topStrategies.size() > 1) {
			sb.append("\n--- TOP 5 STRATEGIES ---\n");
			for (int i = 0; i < Math.min(5, topStrategies.size()); i++) {
				StrategyTestResult s = topStrategies.get(i);
				sb.append(String.format("%d. %s\n", i + 1, s.toSummary()));
			}
		}

		return sb.toString();
	}

	/**
	 * Generates an explanation suitable for including in the AI response.
	 */
	public String toExplanation() {
		if (bestStrategy == null) {
			return "Optimization failed to find a profitable strategy.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(String.format(
				"After testing %d strategy combinations across %d days of historical data, " + "the **%s** strategy with parameters (%s) achieved the best results:\n\n",
				totalCombinationsTested, daysAnalyzed, bestStrategy.getStrategyType().getDisplayName(), bestStrategy.getParametersDisplay()));

		sb.append(String.format("- **Total Return:** %.2f%% (vs %.2f%% buy-and-hold)\n", bestStrategy.getTotalReturn(), buyAndHoldReturn));
		sb.append(String.format("- **Outperformance:** %.2f%%\n", outperformance));
		sb.append(String.format("- **Win Rate:** %.1f%%\n", bestStrategy.getWinRate() * 100));
		sb.append(String.format("- **Sharpe Ratio:** %.2f\n", bestStrategy.getSharpeRatio()));
		sb.append(String.format("- **Max Drawdown:** %.1f%%\n", bestStrategy.getMaxDrawdown()));
		sb.append(String.format("- **Total Trades:** %d\n\n", bestStrategy.getTotalTrades()));

		sb.append(String.format("Market Regime: %s\n\n", marketRegime));
		sb.append(String.format("The strategy was optimized by testing all parameter combinations " + "and ranking by total return rather than win rate alone."));

		return sb.toString();
	}

	// Getters and Setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}

	public int getDaysAnalyzed() {
		return daysAnalyzed;
	}

	public void setDaysAnalyzed(int daysAnalyzed) {
		this.daysAnalyzed = daysAnalyzed;
	}

	public StrategyTestResult getBestStrategy() {
		return bestStrategy;
	}

	public void setBestStrategy(StrategyTestResult bestStrategy) {
		this.bestStrategy = bestStrategy;
	}

	public List<StrategyTestResult> getTopStrategies() {
		return topStrategies;
	}

	public void setTopStrategies(List<StrategyTestResult> topStrategies) {
		this.topStrategies = topStrategies;
	}

	public int getTotalCombinationsTested() {
		return totalCombinationsTested;
	}

	public void setTotalCombinationsTested(int totalCombinationsTested) {
		this.totalCombinationsTested = totalCombinationsTested;
	}

	public int getSuccessfulTests() {
		return successfulTests;
	}

	public void setSuccessfulTests(int successfulTests) {
		this.successfulTests = successfulTests;
	}

	public String getMarketRegime() {
		return marketRegime;
	}

	public void setMarketRegime(String marketRegime) {
		this.marketRegime = marketRegime;
	}

	public long getTotalExecutionTimeMs() {
		return totalExecutionTimeMs;
	}

	public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
		this.totalExecutionTimeMs = totalExecutionTimeMs;
	}

	public double getAvgExecutionTimePerStrategy() {
		return avgExecutionTimePerStrategy;
	}

	public void setAvgExecutionTimePerStrategy(double avgExecutionTimePerStrategy) {
		this.avgExecutionTimePerStrategy = avgExecutionTimePerStrategy;
	}

	public double getBuyAndHoldReturn() {
		return buyAndHoldReturn;
	}

	public void setBuyAndHoldReturn(double buyAndHoldReturn) {
		this.buyAndHoldReturn = buyAndHoldReturn;
	}

	public double getOutperformance() {
		return outperformance;
	}

	public void setOutperformance(double outperformance) {
		this.outperformance = outperformance;
	}

}
