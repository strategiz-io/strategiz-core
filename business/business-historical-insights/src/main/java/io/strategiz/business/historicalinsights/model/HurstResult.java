package io.strategiz.business.historicalinsights.model;

/**
 * Result of Hurst Exponent calculation using Rescaled Range (R/S) analysis.
 * The Hurst exponent indicates the tendency of a time series to either regress
 * to the mean, cluster in a direction, or move randomly.
 *
 * H < 0.5: Mean-reverting (anti-persistent) - use oscillator strategies (RSI, Stochastic)
 * H = 0.5: Random walk - difficult to predict, market efficiency
 * H > 0.5: Trending (persistent) - use trend-following strategies (MACD, MA crossovers)
 */
public class HurstResult {

	private double hurstExponent;

	private String interpretation;

	private String recommendedStrategyType;

	private double confidence; // 0-1, based on R² of log-log regression

	public HurstResult() {
	}

	public HurstResult(double hurstExponent) {
		this.hurstExponent = hurstExponent;
		this.interpretation = interpretHurst(hurstExponent);
		this.recommendedStrategyType = recommendStrategy(hurstExponent);
		this.confidence = 0.8; // Default confidence
	}

	public HurstResult(double hurstExponent, double confidence) {
		this.hurstExponent = hurstExponent;
		this.interpretation = interpretHurst(hurstExponent);
		this.recommendedStrategyType = recommendStrategy(hurstExponent);
		this.confidence = confidence;
	}

	private String interpretHurst(double h) {
		if (h < 0.4) {
			return "STRONGLY_MEAN_REVERTING";
		}
		else if (h < 0.5) {
			return "MEAN_REVERTING";
		}
		else if (h < 0.55) {
			return "RANDOM_WALK";
		}
		else if (h < 0.65) {
			return "TRENDING";
		}
		else {
			return "STRONGLY_TRENDING";
		}
	}

	private String recommendStrategy(double h) {
		if (h < 0.45) {
			return "RSI_MEAN_REVERSION";
		}
		else if (h < 0.5) {
			return "BOLLINGER_BAND_BOUNCE";
		}
		else if (h < 0.55) {
			return "BREAKOUT_CONFIRMATION";
		}
		else if (h < 0.65) {
			return "MA_CROSSOVER";
		}
		else {
			return "TREND_FOLLOWING_MACD";
		}
	}

	// Getters and Setters

	public double getHurstExponent() {
		return hurstExponent;
	}

	public void setHurstExponent(double hurstExponent) {
		this.hurstExponent = hurstExponent;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public void setInterpretation(String interpretation) {
		this.interpretation = interpretation;
	}

	public String getRecommendedStrategyType() {
		return recommendedStrategyType;
	}

	public void setRecommendedStrategyType(String recommendedStrategyType) {
		this.recommendedStrategyType = recommendedStrategyType;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	/**
	 * Check if the market is mean-reverting (H < 0.5).
	 */
	public boolean isMeanReverting() {
		return hurstExponent < 0.5;
	}

	/**
	 * Check if the market is trending (H > 0.5).
	 */
	public boolean isTrending() {
		return hurstExponent > 0.5;
	}

	@Override
	public String toString() {
		return String.format("HurstResult[H=%.3f, %s, recommend=%s, confidence=%.2f]",
				hurstExponent, interpretation, recommendedStrategyType, confidence);
	}

}
