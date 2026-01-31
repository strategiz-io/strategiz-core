package io.strategiz.business.historicalinsights.service;

import io.strategiz.business.historicalinsights.model.DeploymentInsights;
import io.strategiz.business.historicalinsights.model.DeploymentInsights.DeploymentMode;
import io.strategiz.business.historicalinsights.model.DeploymentInsights.DrawdownRiskLevel;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Calculator service for generating deployment insights including: - Position sizing
 * recommendations using Kelly criterion - Drawdown risk analysis - Consecutive loss
 * analysis - Alert vs Bot deployment mode recommendations
 */
@Service
public class DeploymentInsightsCalculator {

	private static final Logger log = LoggerFactory.getLogger(DeploymentInsightsCalculator.class);

	// Allocation boundaries
	private static final double MIN_ALLOCATION = 2.0;

	private static final double MAX_ALLOCATION = 25.0;

	// Trade frequency thresholds (trades per year)
	private static final int HIGH_FREQUENCY_THRESHOLD = 500;

	private static final int MEDIUM_FREQUENCY_THRESHOLD = 100;

	private static final int LOW_FREQUENCY_THRESHOLD = 50;

	/**
	 * Calculate deployment insights for a strategy based on backtest results.
	 * @param result The strategy test result with performance metrics
	 * @param daysAnalyzed Number of days in the backtest period
	 * @return DeploymentInsights with recommendations
	 */
	public DeploymentInsights calculate(StrategyTestResult result, int daysAnalyzed) {
		DeploymentInsights insights = new DeploymentInsights();

		if (result == null || !result.isSuccess()) {
			return createDefaultInsights();
		}

		// Calculate position sizing
		calculatePositionSizing(insights, result);

		// Analyze drawdown risk
		analyzeDrawdownRisk(insights, result);

		// Analyze consecutive losses
		analyzeConsecutiveLosses(insights, result);

		// Interpret risk metrics
		interpretRiskMetrics(insights, result);

		// Determine deployment mode
		determineDeploymentMode(insights, result, daysAnalyzed);

		// Populate Alert vs Bot feature lists
		populateAlertBotFeatures(insights);

		log.debug("Calculated deployment insights: {}", insights);
		return insights;
	}

	/**
	 * Calculate Kelly criterion and recommended portfolio allocation. Kelly Formula: f* =
	 * (p * b - q) / b where p=winRate, b=avgWin/avgLoss, q=(1-p)
	 */
	private void calculatePositionSizing(DeploymentInsights insights, StrategyTestResult result) {
		double winRate = result.getWinRate(); // 0.0 - 1.0
		double avgWin = Math.abs(result.getProfitFactor() > 0 ? result.getProfitFactor() : 1.5); // approximation
		double avgLoss = 1.0; // normalize to 1

		// For more accurate calculation, use profit factor relationship
		// profitFactor = (winRate * avgWin) / ((1 - winRate) * avgLoss)
		// avgWin/avgLoss = profitFactor * (1 - winRate) / winRate
		double profitFactor = result.getProfitFactor();
		double winLossRatio;

		if (winRate > 0 && winRate < 1 && profitFactor > 0) {
			winLossRatio = profitFactor * (1 - winRate) / winRate;
		}
		else {
			// Fallback
			winLossRatio = profitFactor > 0 ? profitFactor : 1.5;
		}

		// Kelly Criterion: f* = (p * b - q) / b
		double p = winRate;
		double q = 1 - winRate;
		double b = winLossRatio;

		double kelly = 0;
		if (b > 0) {
			kelly = (p * b - q) / b;
		}

		// Kelly can be negative (don't bet) or very high
		kelly = Math.max(0, kelly * 100); // Convert to percentage

		insights.setKellyPercentage(Math.min(kelly, 100));

		// Conservative Kelly (half-Kelly is safer and still captures most growth)
		double conservativeKelly = kelly / 2;
		insights.setConservativeKelly(conservativeKelly);

		// Calculate recommended allocation with adjustments
		double allocation = conservativeKelly;

		// Reduce by drawdown factor: allocation *= (1 - maxDrawdown/100)
		double maxDrawdown = result.getMaxDrawdown();
		if (maxDrawdown > 0) {
			allocation *= (1 - maxDrawdown / 100);
		}

		// Reduce if Sharpe < 1.0
		double sharpe = result.getSharpeRatio();
		if (sharpe < 1.0) {
			allocation *= 0.8;
		}

		// Apply bounds
		allocation = Math.max(MIN_ALLOCATION, Math.min(MAX_ALLOCATION, allocation));

		insights.setRecommendedPortfolioAllocation(Math.round(allocation * 10) / 10.0);

		// Generate allocation rationale
		String rationale = generateAllocationRationale(allocation, kelly, maxDrawdown, sharpe);
		insights.setAllocationRationale(rationale);
	}

	/**
	 * Generate human-readable rationale for the allocation recommendation.
	 */
	private String generateAllocationRationale(double allocation, double kelly, double maxDrawdown, double sharpe) {
		StringBuilder sb = new StringBuilder();

		if (allocation <= 5) {
			sb.append("Conservative allocation (1-5%) recommended for testing or low confidence strategies. ");
		}
		else if (allocation <= 10) {
			sb.append("Moderate allocation (5-10%) suitable for validated strategies with consistent performance. ");
		}
		else if (allocation <= 15) {
			sb.append("Aggressive allocation (10-15%) for well-tested strategies. Ensure adequate capital buffer. ");
		}
		else if (allocation <= 25) {
			sb.append("Very aggressive allocation (15-25%). Only for extensively backtested strategies. ");
		}
		else {
			sb.append("Maximum allocation reached. ");
		}

		if (maxDrawdown > 20) {
			sb.append(String.format("Allocation reduced due to high drawdown (%.1f%%). ", maxDrawdown));
		}
		if (sharpe < 1.0) {
			sb.append("Allocation reduced due to lower risk-adjusted returns. ");
		}

		return sb.toString().trim();
	}

	/**
	 * Analyze drawdown risk and generate explanations.
	 */
	private void analyzeDrawdownRisk(DeploymentInsights insights, StrategyTestResult result) {
		double maxDrawdown = result.getMaxDrawdown();
		insights.setMaxDrawdown(maxDrawdown);

		// Classify risk level
		DrawdownRiskLevel riskLevel;
		if (maxDrawdown < 10) {
			riskLevel = DrawdownRiskLevel.LOW;
		}
		else if (maxDrawdown < 20) {
			riskLevel = DrawdownRiskLevel.MEDIUM;
		}
		else if (maxDrawdown < 35) {
			riskLevel = DrawdownRiskLevel.HIGH;
		}
		else {
			riskLevel = DrawdownRiskLevel.EXTREME;
		}
		insights.setDrawdownRiskLevel(riskLevel);

		// Calculate recovery required: (1 / (1 - drawdown/100) - 1) * 100
		double recoveryRequired = (1 / (1 - maxDrawdown / 100) - 1) * 100;
		insights.setRecoveryRequired(Math.round(recoveryRequired * 10) / 10.0);

		// Generate explanation
		String explanation = generateDrawdownExplanation(maxDrawdown, riskLevel, recoveryRequired);
		insights.setDrawdownExplanation(explanation);
	}

	/**
	 * Generate human-readable drawdown explanation.
	 */
	private String generateDrawdownExplanation(double maxDrawdown, DrawdownRiskLevel riskLevel, double recovery) {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("Maximum drawdown of %.1f%% means ", maxDrawdown));

		switch (riskLevel) {
			case LOW:
				sb.append("the strategy maintained good capital preservation. ");
				sb.append(String.format("A %.1f%% gain is needed to recover from the worst decline.", recovery));
				break;
			case MEDIUM:
				sb.append("you could temporarily lose 10-20% of allocated capital. ");
				sb.append(String.format("Recovery requires a %.1f%% gain, which may take significant time.", recovery));
				break;
			case HIGH:
				sb.append("significant capital can be at risk during adverse market conditions. ");
				sb.append(String.format("A %.1f%% gain is needed to recover - this requires strong conviction.",
						recovery));
				break;
			case EXTREME:
				sb.append("more than a third of capital could be lost during the worst periods. ");
				sb.append(String.format(
						"Recovery requires a %.1f%% gain, which could take years. Consider reducing " + "allocation.",
						recovery));
				break;
		}

		return sb.toString();
	}

	/**
	 * Analyze consecutive loss patterns.
	 */
	private void analyzeConsecutiveLosses(DeploymentInsights insights, StrategyTestResult result) {
		// Get consecutive losses from result if available, otherwise estimate
		int maxConsecutiveLosses = result.getMaxConsecutiveLosses();
		double worstStreakPercent = result.getWorstLosingStreakPercent();

		// If not calculated, estimate from win rate
		if (maxConsecutiveLosses <= 0) {
			double winRate = result.getWinRate();
			double lossRate = 1 - winRate;
			// Expected longest losing streak ~ log(totalTrades) / log(1/lossRate)
			int totalTrades = result.getTotalTrades();
			if (totalTrades > 0 && lossRate > 0 && lossRate < 1) {
				maxConsecutiveLosses = (int) Math.ceil(Math.log(totalTrades) / Math.log(1 / lossRate));
				maxConsecutiveLosses = Math.max(1, Math.min(maxConsecutiveLosses, totalTrades));
			}
			else {
				maxConsecutiveLosses = 3; // Default estimate
			}
		}

		insights.setMaxConsecutiveLosses(maxConsecutiveLosses);
		insights.setWorstLosingStreakPercent(worstStreakPercent);

		// Calculate probability of 5 consecutive losses
		double lossRate = 1 - result.getWinRate();
		double prob5Losses = Math.pow(lossRate, 5) * 100;
		insights.setProbabilityOf5ConsecutiveLosses(Math.round(prob5Losses * 100) / 100.0);

		// Generate explanation
		String explanation = generateConsecutiveLossExplanation(maxConsecutiveLosses, worstStreakPercent, prob5Losses,
				result.getWinRate());
		insights.setConsecutiveLossExplanation(explanation);
	}

	/**
	 * Generate explanation for consecutive loss analysis.
	 */
	private String generateConsecutiveLossExplanation(int maxLosses, double worstPercent, double prob5,
			double winRate) {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("The strategy experienced up to %d consecutive losing trades", maxLosses));
		if (worstPercent > 0) {
			sb.append(String.format(", losing %.1f%% during the worst streak", worstPercent));
		}
		sb.append(". ");

		if (prob5 > 10) {
			sb.append(String.format("With a %.1f%% win rate, there's a %.1f%% chance of 5 losses in a row - "
					+ "prepare mentally for " + "these drawdowns.", winRate * 100, prob5));
		}
		else if (prob5 > 1) {
			sb.append(String.format("There's a %.1f%% probability of experiencing 5 consecutive losses.", prob5));
		}
		else {
			sb.append("Extended losing streaks are relatively unlikely with this win rate.");
		}

		return sb.toString();
	}

	/**
	 * Interpret risk metrics (Sharpe, Profit Factor, Win Rate).
	 */
	private void interpretRiskMetrics(DeploymentInsights insights, StrategyTestResult result) {
		// Sharpe Ratio interpretation
		double sharpe = result.getSharpeRatio();
		String sharpeInterpretation;
		if (sharpe >= 3.0) {
			sharpeInterpretation = String.format("Excellent (%.2f) - Top-tier risk-adjusted returns", sharpe);
		}
		else if (sharpe >= 2.0) {
			sharpeInterpretation = String.format("Very Good (%.2f) - Strong risk-adjusted performance", sharpe);
		}
		else if (sharpe >= 1.0) {
			sharpeInterpretation = String.format("Good (%.2f) - Generates meaningful return per unit of risk", sharpe);
		}
		else if (sharpe >= 0.5) {
			sharpeInterpretation = String.format("Moderate (%.2f) - Consider improvements to reduce volatility",
					sharpe);
		}
		else {
			sharpeInterpretation = String.format("Poor (%.2f) - Returns don't adequately compensate for risk", sharpe);
		}
		insights.setSharpeRatioInterpretation(sharpeInterpretation);

		// Profit Factor interpretation
		double pf = result.getProfitFactor();
		String pfInterpretation;
		if (pf >= 3.0) {
			pfInterpretation = String.format("Excellent (%.2f) - Makes $%.2f for every $1 lost", pf, pf);
		}
		else if (pf >= 2.0) {
			pfInterpretation = String.format("Very Good (%.2f) - Strong profit-to-loss ratio", pf);
		}
		else if (pf >= 1.5) {
			pfInterpretation = String.format("Good (%.2f) - Profitable with reasonable margin", pf);
		}
		else if (pf >= 1.0) {
			pfInterpretation = String.format("Break-even (%.2f) - Minimal profit margin, needs improvement", pf);
		}
		else {
			pfInterpretation = String.format("Losing (%.2f) - Strategy loses money overall", pf);
		}
		insights.setProfitFactorInterpretation(pfInterpretation);

		// Win Rate interpretation
		double winRate = result.getWinRate() * 100;
		String winRateInterpretation;
		if (winRate >= 70) {
			winRateInterpretation = String.format("High (%.1f%%) - Most trades are winners", winRate);
		}
		else if (winRate >= 55) {
			winRateInterpretation = String.format("Good (%.1f%%) - Consistent edge over random", winRate);
		}
		else if (winRate >= 45) {
			winRateInterpretation = String.format("Moderate (%.1f%%) - Relies on larger wins than losses", winRate);
		}
		else if (winRate >= 35) {
			winRateInterpretation = String.format("Low (%.1f%%) - Trend-following style, few big winners", winRate);
		}
		else {
			winRateInterpretation = String.format("Very Low (%.1f%%) - High loss rate requires exceptional winners",
					winRate);
		}
		insights.setWinRateInterpretation(winRateInterpretation);
	}

	/**
	 * Determine recommended deployment mode (Alert vs Bot).
	 */
	private void determineDeploymentMode(DeploymentInsights insights, StrategyTestResult result, int daysAnalyzed) {
		int totalTrades = result.getTotalTrades();
		double yearsAnalyzed = daysAnalyzed / 365.25;
		int tradesPerYear = yearsAnalyzed > 0 ? (int) (totalTrades / yearsAnalyzed) : totalTrades;

		insights.setEstimatedTradesPerYear(tradesPerYear);

		// Classify trading frequency
		String frequencyClass;
		if (tradesPerYear >= HIGH_FREQUENCY_THRESHOLD) {
			frequencyClass = "High Frequency";
		}
		else if (tradesPerYear >= MEDIUM_FREQUENCY_THRESHOLD) {
			frequencyClass = "Active Trading";
		}
		else if (tradesPerYear >= LOW_FREQUENCY_THRESHOLD) {
			frequencyClass = "Swing Trading";
		}
		else {
			frequencyClass = "Position Trading";
		}
		insights.setTradingFrequencyClassification(frequencyClass);

		// Determine recommended mode
		DeploymentMode recommendedMode;
		String rationale;

		if (tradesPerYear >= HIGH_FREQUENCY_THRESHOLD) {
			recommendedMode = DeploymentMode.BOT;
			rationale = String
				.format("With ~%d trades/year, manual execution is impractical. Bot mode ensures consistent "
						+ "execution and eliminates emotional decisions.", tradesPerYear);
		}
		else if (tradesPerYear >= MEDIUM_FREQUENCY_THRESHOLD) {
			recommendedMode = DeploymentMode.BOT;
			rationale = String
				.format("At %d trades/year, bot mode is recommended for faster execution and 24/7 monitoring. "
						+ "Alert mode is viable if you prefer manual control.", tradesPerYear);
		}
		else if (tradesPerYear >= LOW_FREQUENCY_THRESHOLD) {
			recommendedMode = DeploymentMode.ALERT;
			rationale = String
				.format("With %d trades/year, alert mode allows you to review each signal before executing. "
						+ "This is ideal for learning and building confidence.", tradesPerYear);
		}
		else {
			recommendedMode = DeploymentMode.ALERT;
			rationale = String
				.format("Position trading with ~%d trades/year is well-suited for alert mode. You have time to "
						+ "analyze each opportunity thoroughly.", tradesPerYear);
		}

		insights.setRecommendedDeploymentMode(recommendedMode);
		insights.setDeploymentModeRationale(rationale);
	}

	/**
	 * Populate Alert and Bot feature comparison lists.
	 */
	private void populateAlertBotFeatures(DeploymentInsights insights) {
		// Alert advantages
		insights.setAlertAdvantages(Arrays.asList("Manual position sizing - you decide the size of each trade",
				"Review signals before execution - add human judgment",
				"Ideal for learning - understand why trades are triggered",
				"No execution risk - you control when to enter",
				"Flexibility to skip signals during uncertain markets"));

		// Alert limitations
		insights.setAlertLimitations(Arrays.asList("Execution depends on your availability",
				"Stop loss must be set manually each time", "Take profit must be set manually each time",
				"Not practical for 500+ trades per year", "May miss overnight or after-hours signals"));

		// Bot advantages
		insights.setBotAdvantages(Arrays.asList("Instant execution - millisecond response time",
				"Automatic position sizing based on your rules", "Auto-set stop loss per strategy parameters",
				"Auto-set take profit per strategy parameters", "Auto-pause if max daily loss is reached",
				"24/7 trading - never miss a signal", "Paper trading mode for safe testing",
				"Eliminates emotional decisions"));

		// Bot limitations
		insights.setBotLimitations(
				Arrays.asList("No manual review before execution", "Requires trust in strategy parameters",
						"Technical issues can affect execution", "Needs monitoring for unexpected market conditions"));
	}

	/**
	 * Create default insights when calculation isn't possible.
	 */
	private DeploymentInsights createDefaultInsights() {
		DeploymentInsights insights = new DeploymentInsights();
		insights.setRecommendedPortfolioAllocation(5.0);
		insights.setKellyPercentage(0);
		insights.setConservativeKelly(0);
		insights.setAllocationRationale("Default conservative allocation due to insufficient data.");
		insights.setDrawdownRiskLevel(DrawdownRiskLevel.MEDIUM);
		insights.setDrawdownExplanation("Unable to calculate drawdown risk from available data.");
		insights.setRecommendedDeploymentMode(DeploymentMode.ALERT);
		insights.setDeploymentModeRationale("Alert mode recommended until strategy is validated.");
		populateAlertBotFeatures(insights);
		return insights;
	}

}
