package io.strategiz.business.historicalinsights.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the effectiveness ranking of a technical indicator based on historical
 * backtesting.
 */
public class IndicatorRanking {

	private String indicatorName; // "RSI", "MACD", "BB", "MA_CROSS"

	private double effectivenessScore; // 0.0 - 1.0 (higher is better)

	private Map<String, Object> optimalSettings; // e.g., {"period": 14, "oversold": 28}

	private String reason; // Human-readable explanation of effectiveness

	public IndicatorRanking() {
		this.optimalSettings = new HashMap<>();
	}

	public IndicatorRanking(String indicatorName, double effectivenessScore, Map<String, Object> optimalSettings,
			String reason) {
		this.indicatorName = indicatorName;
		this.effectivenessScore = effectivenessScore;
		this.optimalSettings = optimalSettings != null ? optimalSettings : new HashMap<>();
		this.reason = reason;
	}

	public String getIndicatorName() {
		return indicatorName;
	}

	public void setIndicatorName(String indicatorName) {
		this.indicatorName = indicatorName;
	}

	public double getEffectivenessScore() {
		return effectivenessScore;
	}

	public void setEffectivenessScore(double effectivenessScore) {
		this.effectivenessScore = effectivenessScore;
	}

	public Map<String, Object> getOptimalSettings() {
		return optimalSettings;
	}

	public void setOptimalSettings(Map<String, Object> optimalSettings) {
		this.optimalSettings = optimalSettings;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
