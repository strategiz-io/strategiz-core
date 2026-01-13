package io.strategiz.business.strategyanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Market context from Historical Insights (7-year analysis).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketContext {

	@JsonProperty("volatilityRegime")
	private String volatilityRegime; // "Low", "Moderate", "High", "Extreme"

	@JsonProperty("trendDirection")
	private String trendDirection; // "Bullish", "Bearish", "Sideways"

	@JsonProperty("averageVolatility")
	private Double averageVolatility; // Percentage

	@JsonProperty("recommendedIndicators")
	private List<String> recommendedIndicators;

	@JsonProperty("marketCondition")
	private String marketCondition; // Human-readable summary

	public MarketContext() {
	}

	/**
	 * Create default market context when Historical Insights unavailable.
	 */
	public static MarketContext createDefault() {
		MarketContext context = new MarketContext();
		context.setVolatilityRegime("Moderate");
		context.setTrendDirection("Mixed");
		context.setMarketCondition("Normal market conditions");
		context.setAverageVolatility(20.0);
		return context;
	}

	// Getters and setters

	public String getVolatilityRegime() {
		return volatilityRegime;
	}

	public void setVolatilityRegime(String volatilityRegime) {
		this.volatilityRegime = volatilityRegime;
	}

	public String getTrendDirection() {
		return trendDirection;
	}

	public void setTrendDirection(String trendDirection) {
		this.trendDirection = trendDirection;
	}

	public Double getAverageVolatility() {
		return averageVolatility;
	}

	public void setAverageVolatility(Double averageVolatility) {
		this.averageVolatility = averageVolatility;
	}

	public List<String> getRecommendedIndicators() {
		return recommendedIndicators;
	}

	public void setRecommendedIndicators(List<String> recommendedIndicators) {
		this.recommendedIndicators = recommendedIndicators;
	}

	public String getMarketCondition() {
		return marketCondition;
	}

	public void setMarketCondition(String marketCondition) {
		this.marketCondition = marketCondition;
	}

}
