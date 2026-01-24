package io.strategiz.business.historicalinsights.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive deployment guidance combining risk analysis, position sizing
 * recommendations, and Alert vs Bot comparison for strategy deployment.
 */
public class DeploymentInsights {

	// Position Sizing
	private double recommendedPortfolioAllocation; // Percentage-based allocation (e.g., 5.0 = "Risk no more than 5%")

	private double kellyPercentage; // Full Kelly criterion result

	private double conservativeKelly; // Half-Kelly criterion for safer sizing

	private String allocationRationale; // Explanation of the allocation recommendation

	// Drawdown Risk Analysis
	private double maxDrawdown; // Maximum drawdown percentage

	private DrawdownRiskLevel drawdownRiskLevel; // LOW, MEDIUM, HIGH, EXTREME

	private String drawdownExplanation; // Detailed explanation of drawdown implications

	private double recoveryRequired; // Percentage gain required to recover from drawdown

	// Consecutive Loss Analysis
	private int maxConsecutiveLosses; // Maximum losing streak count

	private double worstLosingStreakPercent; // Cumulative loss during worst streak

	private double probabilityOf5ConsecutiveLosses; // Statistical probability

	private String consecutiveLossExplanation;

	// Risk Metric Interpretations
	private String sharpeRatioInterpretation;

	private String profitFactorInterpretation;

	private String winRateInterpretation;

	// Deployment Mode Recommendation
	private DeploymentMode recommendedDeploymentMode; // ALERT or BOT

	private String deploymentModeRationale;

	// Alert Features
	private List<String> alertAdvantages;

	private List<String> alertLimitations;

	// Bot Features
	private List<String> botAdvantages;

	private List<String> botLimitations;

	// Trade Frequency Classification
	private String tradingFrequencyClassification; // "High Frequency", "Day Trading", "Swing Trading", "Position
													// Trading"

	private int estimatedTradesPerYear;

	public DeploymentInsights() {
		this.alertAdvantages = new ArrayList<>();
		this.alertLimitations = new ArrayList<>();
		this.botAdvantages = new ArrayList<>();
		this.botLimitations = new ArrayList<>();
	}

	/**
	 * Drawdown risk level classification.
	 */
	public enum DrawdownRiskLevel {

		LOW("Low Risk", "Easy recovery, suitable for most portfolios"), MEDIUM("Moderate Risk",
				"Requires patience to recover"), HIGH("High Risk",
						"Significant capital at risk, requires strong conviction"), EXTREME("Extreme Risk",
								"Capital devastating, not recommended for most traders");

		private final String label;

		private final String description;

		DrawdownRiskLevel(String label, String description) {
			this.label = label;
			this.description = description;
		}

		public String getLabel() {
			return label;
		}

		public String getDescription() {
			return description;
		}

	}

	/**
	 * Deployment mode recommendation.
	 */
	public enum DeploymentMode {

		ALERT("Alert Mode", "Receive signals and execute trades manually"), BOT("Bot Mode",
				"Fully automated trade execution");

		private final String label;

		private final String description;

		DeploymentMode(String label, String description) {
			this.label = label;
			this.description = description;
		}

		public String getLabel() {
			return label;
		}

		public String getDescription() {
			return description;
		}

	}

	// Getters and Setters

	public double getRecommendedPortfolioAllocation() {
		return recommendedPortfolioAllocation;
	}

	public void setRecommendedPortfolioAllocation(double recommendedPortfolioAllocation) {
		this.recommendedPortfolioAllocation = recommendedPortfolioAllocation;
	}

	public double getKellyPercentage() {
		return kellyPercentage;
	}

	public void setKellyPercentage(double kellyPercentage) {
		this.kellyPercentage = kellyPercentage;
	}

	public double getConservativeKelly() {
		return conservativeKelly;
	}

	public void setConservativeKelly(double conservativeKelly) {
		this.conservativeKelly = conservativeKelly;
	}

	public String getAllocationRationale() {
		return allocationRationale;
	}

	public void setAllocationRationale(String allocationRationale) {
		this.allocationRationale = allocationRationale;
	}

	public double getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(double maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public DrawdownRiskLevel getDrawdownRiskLevel() {
		return drawdownRiskLevel;
	}

	public void setDrawdownRiskLevel(DrawdownRiskLevel drawdownRiskLevel) {
		this.drawdownRiskLevel = drawdownRiskLevel;
	}

	public String getDrawdownExplanation() {
		return drawdownExplanation;
	}

	public void setDrawdownExplanation(String drawdownExplanation) {
		this.drawdownExplanation = drawdownExplanation;
	}

	public double getRecoveryRequired() {
		return recoveryRequired;
	}

	public void setRecoveryRequired(double recoveryRequired) {
		this.recoveryRequired = recoveryRequired;
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

	public double getProbabilityOf5ConsecutiveLosses() {
		return probabilityOf5ConsecutiveLosses;
	}

	public void setProbabilityOf5ConsecutiveLosses(double probabilityOf5ConsecutiveLosses) {
		this.probabilityOf5ConsecutiveLosses = probabilityOf5ConsecutiveLosses;
	}

	public String getConsecutiveLossExplanation() {
		return consecutiveLossExplanation;
	}

	public void setConsecutiveLossExplanation(String consecutiveLossExplanation) {
		this.consecutiveLossExplanation = consecutiveLossExplanation;
	}

	public String getSharpeRatioInterpretation() {
		return sharpeRatioInterpretation;
	}

	public void setSharpeRatioInterpretation(String sharpeRatioInterpretation) {
		this.sharpeRatioInterpretation = sharpeRatioInterpretation;
	}

	public String getProfitFactorInterpretation() {
		return profitFactorInterpretation;
	}

	public void setProfitFactorInterpretation(String profitFactorInterpretation) {
		this.profitFactorInterpretation = profitFactorInterpretation;
	}

	public String getWinRateInterpretation() {
		return winRateInterpretation;
	}

	public void setWinRateInterpretation(String winRateInterpretation) {
		this.winRateInterpretation = winRateInterpretation;
	}

	public DeploymentMode getRecommendedDeploymentMode() {
		return recommendedDeploymentMode;
	}

	public void setRecommendedDeploymentMode(DeploymentMode recommendedDeploymentMode) {
		this.recommendedDeploymentMode = recommendedDeploymentMode;
	}

	public String getDeploymentModeRationale() {
		return deploymentModeRationale;
	}

	public void setDeploymentModeRationale(String deploymentModeRationale) {
		this.deploymentModeRationale = deploymentModeRationale;
	}

	public List<String> getAlertAdvantages() {
		return alertAdvantages;
	}

	public void setAlertAdvantages(List<String> alertAdvantages) {
		this.alertAdvantages = alertAdvantages;
	}

	public List<String> getAlertLimitations() {
		return alertLimitations;
	}

	public void setAlertLimitations(List<String> alertLimitations) {
		this.alertLimitations = alertLimitations;
	}

	public List<String> getBotAdvantages() {
		return botAdvantages;
	}

	public void setBotAdvantages(List<String> botAdvantages) {
		this.botAdvantages = botAdvantages;
	}

	public List<String> getBotLimitations() {
		return botLimitations;
	}

	public void setBotLimitations(List<String> botLimitations) {
		this.botLimitations = botLimitations;
	}

	public String getTradingFrequencyClassification() {
		return tradingFrequencyClassification;
	}

	public void setTradingFrequencyClassification(String tradingFrequencyClassification) {
		this.tradingFrequencyClassification = tradingFrequencyClassification;
	}

	public int getEstimatedTradesPerYear() {
		return estimatedTradesPerYear;
	}

	public void setEstimatedTradesPerYear(int estimatedTradesPerYear) {
		this.estimatedTradesPerYear = estimatedTradesPerYear;
	}

	@Override
	public String toString() {
		return String.format(
				"DeploymentInsights[allocation=%.1f%%, kelly=%.1f%%, drawdown=%s, mode=%s, tradesPerYear=%d]",
				recommendedPortfolioAllocation, kellyPercentage,
				drawdownRiskLevel != null ? drawdownRiskLevel.getLabel() : "N/A",
				recommendedDeploymentMode != null ? recommendedDeploymentMode.getLabel() : "N/A", estimatedTradesPerYear);
	}

}
