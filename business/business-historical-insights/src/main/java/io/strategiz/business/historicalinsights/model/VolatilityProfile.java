package io.strategiz.business.historicalinsights.model;

/**
 * Represents the volatility characteristics of a symbol based on historical data
 * analysis.
 */
public class VolatilityProfile {

	private double avgATR; // Average True Range

	private String regime; // LOW, MEDIUM, HIGH, EXTREME

	private double avgDailyRangePercent; // Average (high - low) / close percentage

	public VolatilityProfile() {
	}

	public VolatilityProfile(double avgATR, String regime, double avgDailyRangePercent) {
		this.avgATR = avgATR;
		this.regime = regime;
		this.avgDailyRangePercent = avgDailyRangePercent;
	}

	public double getAvgATR() {
		return avgATR;
	}

	public void setAvgATR(double avgATR) {
		this.avgATR = avgATR;
	}

	public String getRegime() {
		return regime;
	}

	public void setRegime(String regime) {
		this.regime = regime;
	}

	public double getAvgDailyRangePercent() {
		return avgDailyRangePercent;
	}

	public void setAvgDailyRangePercent(double avgDailyRangePercent) {
		this.avgDailyRangePercent = avgDailyRangePercent;
	}

}
