package io.strategiz.business.historicalinsights.model;

/**
 * Represents the market behavior characteristics of a symbol. Includes trend direction,
 * strength, and whether the symbol exhibits mean-reverting behavior.
 */
public class MarketCharacteristics {

	private String trendDirection; // BULLISH, BEARISH, SIDEWAYS

	private double trendStrength; // 0.0 - 1.0 (R-squared from linear regression)

	private boolean isMeanReverting; // Based on Hurst exponent (< 0.5 = mean-reverting)

	private double hurstExponent; // Hurst exponent value

	public MarketCharacteristics() {
	}

	public MarketCharacteristics(String trendDirection, double trendStrength, boolean isMeanReverting,
			double hurstExponent) {
		this.trendDirection = trendDirection;
		this.trendStrength = trendStrength;
		this.isMeanReverting = isMeanReverting;
		this.hurstExponent = hurstExponent;
	}

	public String getTrendDirection() {
		return trendDirection;
	}

	public void setTrendDirection(String trendDirection) {
		this.trendDirection = trendDirection;
	}

	public double getTrendStrength() {
		return trendStrength;
	}

	public void setTrendStrength(double trendStrength) {
		this.trendStrength = trendStrength;
	}

	public boolean isMeanReverting() {
		return isMeanReverting;
	}

	public void setMeanReverting(boolean meanReverting) {
		isMeanReverting = meanReverting;
	}

	public double getHurstExponent() {
		return hurstExponent;
	}

	public void setHurstExponent(double hurstExponent) {
		this.hurstExponent = hurstExponent;
	}

}
