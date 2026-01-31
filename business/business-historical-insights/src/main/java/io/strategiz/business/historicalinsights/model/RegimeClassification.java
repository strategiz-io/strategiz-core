package io.strategiz.business.historicalinsights.model;

/**
 * Market regime classification based on trend strength, volatility, and mean-reversion
 * analysis. Different regimes require different trading strategies for optimal
 * performance.
 */
public class RegimeClassification {

	/**
	 * Market regime types based on trend direction and strength.
	 */
	public enum RegimeType {

		STRONG_UPTREND("Strong Uptrend", "Trend-following with tight trailing stops"),
		UPTREND("Uptrend", "Buy dips, trend-following MACD crossovers"),
		RANGING_VOLATILE("Ranging Volatile", "Mean-reversion RSI strategies with wider stops"),
		RANGING_CALM("Ranging Calm", "Bollinger Band bounce, tight stops"),
		DOWNTREND("Downtrend", "Short selling, sell rallies"),
		STRONG_DOWNTREND("Strong Downtrend", "Stay out or short with trailing stops"),
		TRANSITIONAL("Transitional", "Reduce position size, wait for confirmation");

		private final String displayName;

		private final String recommendedStrategy;

		RegimeType(String displayName, String recommendedStrategy) {
			this.displayName = displayName;
			this.recommendedStrategy = recommendedStrategy;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getRecommendedStrategy() {
			return recommendedStrategy;
		}

	}

	private RegimeType regime;

	private double confidence; // 0-1, how confident we are in this classification

	private String recommendedStrategy;

	private String recommendedIndicators; // Comma-separated list

	private double trendStrength; // 0-1

	private double volatilityLevel; // LOW, MEDIUM, HIGH as 0.0, 0.5, 1.0

	private boolean isMeanReverting;

	// Supporting metrics
	private double adxValue; // Average Directional Index

	private double atrPercent; // ATR as % of price

	private double hurstExponent;

	public RegimeClassification() {
		this.regime = RegimeType.RANGING_CALM;
		this.confidence = 0.5;
	}

	public RegimeClassification(RegimeType regime, double confidence) {
		this.regime = regime;
		this.confidence = confidence;
		this.recommendedStrategy = regime.getRecommendedStrategy();
		setRecommendedIndicatorsForRegime();
	}

	private String getRecommendedStrategy() {
		return regime != null ? regime.getRecommendedStrategy() : "Mixed approach";
	}

	private void setRecommendedIndicatorsForRegime() {
		switch (regime) {
			case STRONG_UPTREND:
			case UPTREND:
				this.recommendedIndicators = "MACD, EMA Crossover, ADX";
				break;
			case RANGING_VOLATILE:
				this.recommendedIndicators = "RSI, Stochastic, Bollinger Bands";
				break;
			case RANGING_CALM:
				this.recommendedIndicators = "Bollinger Bands, RSI, Support/Resistance";
				break;
			case DOWNTREND:
			case STRONG_DOWNTREND:
				this.recommendedIndicators = "MACD (sell signals), RSI (overbought shorts)";
				break;
			case TRANSITIONAL:
				this.recommendedIndicators = "ADX, Volume, Price Action";
				break;
			default:
				this.recommendedIndicators = "RSI, MACD, Bollinger Bands";
		}
	}

	// Getters and Setters

	public RegimeType getRegime() {
		return regime;
	}

	public void setRegime(RegimeType regime) {
		this.regime = regime;
		this.recommendedStrategy = regime.getRecommendedStrategy();
		setRecommendedIndicatorsForRegime();
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getRecommendedStrategyDescription() {
		return recommendedStrategy;
	}

	public void setRecommendedStrategy(String recommendedStrategy) {
		this.recommendedStrategy = recommendedStrategy;
	}

	public String getRecommendedIndicators() {
		return recommendedIndicators;
	}

	public void setRecommendedIndicators(String recommendedIndicators) {
		this.recommendedIndicators = recommendedIndicators;
	}

	public double getTrendStrength() {
		return trendStrength;
	}

	public void setTrendStrength(double trendStrength) {
		this.trendStrength = trendStrength;
	}

	public double getVolatilityLevel() {
		return volatilityLevel;
	}

	public void setVolatilityLevel(double volatilityLevel) {
		this.volatilityLevel = volatilityLevel;
	}

	public boolean isMeanReverting() {
		return isMeanReverting;
	}

	public void setMeanReverting(boolean meanReverting) {
		isMeanReverting = meanReverting;
	}

	public double getAdxValue() {
		return adxValue;
	}

	public void setAdxValue(double adxValue) {
		this.adxValue = adxValue;
	}

	public double getAtrPercent() {
		return atrPercent;
	}

	public void setAtrPercent(double atrPercent) {
		this.atrPercent = atrPercent;
	}

	public double getHurstExponent() {
		return hurstExponent;
	}

	public void setHurstExponent(double hurstExponent) {
		this.hurstExponent = hurstExponent;
	}

	/**
	 * Returns true if this regime favors trend-following strategies.
	 */
	public boolean favorsTrendFollowing() {
		return regime == RegimeType.STRONG_UPTREND || regime == RegimeType.UPTREND || regime == RegimeType.DOWNTREND
				|| regime == RegimeType.STRONG_DOWNTREND;
	}

	/**
	 * Returns true if this regime favors mean-reversion strategies.
	 */
	public boolean favorsMeanReversion() {
		return regime == RegimeType.RANGING_VOLATILE || regime == RegimeType.RANGING_CALM;
	}

	/**
	 * Generate a prompt-friendly summary of the regime.
	 */
	public String toPromptSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append(
				String.format("## MARKET REGIME: %s (%.0f%% confidence)\n", regime.getDisplayName(), confidence * 100));
		sb.append(String.format("→ IMPLICATION: %s\n", recommendedStrategy));
		sb.append(String.format("→ RECOMMENDED INDICATORS: %s\n", recommendedIndicators));

		if (isMeanReverting) {
			sb.append(
					"→ IMPLICATION: Market shows mean-reverting behavior - oscillator strategies likely profitable\n");
		}
		else {
			sb.append("→ IMPLICATION: Market shows trending behavior - momentum strategies recommended\n");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return String.format("RegimeClassification[%s, confidence=%.2f, trend=%.2f, volatility=%.2f]",
				regime.getDisplayName(), confidence, trendStrength, volatilityLevel);
	}

}
