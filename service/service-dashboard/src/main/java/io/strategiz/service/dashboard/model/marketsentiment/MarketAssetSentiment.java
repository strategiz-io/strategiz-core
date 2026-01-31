package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Asset-specific sentiment data (from MarketSentimentData)
 */
public class MarketAssetSentiment {

	/**
	 * Asset symbol
	 */
	private String symbol;

	/**
	 * Asset name
	 */
	private String name;

	/**
	 * Sentiment score from 0 (most bearish) to 100 (most bullish)
	 */
	private BigDecimal sentimentScore;

	/**
	 * Sentiment category (Bearish, Slightly Bearish, Neutral, Slightly Bullish, Bullish)
	 */
	private String sentimentCategory;

	/**
	 * Color for visualization (usually green for bullish, red for bearish)
	 */
	private String color;

	// Constructors
	public MarketAssetSentiment() {
	}

	public MarketAssetSentiment(String symbol, String name, BigDecimal sentimentScore, String sentimentCategory,
			String color) {
		this.symbol = symbol;
		this.name = name;
		this.sentimentScore = sentimentScore;
		this.sentimentCategory = sentimentCategory;
		this.color = color;
	}

	// Getters and Setters
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getSentimentScore() {
		return sentimentScore;
	}

	public void setSentimentScore(BigDecimal sentimentScore) {
		this.sentimentScore = sentimentScore;
	}

	public String getSentimentCategory() {
		return sentimentCategory;
	}

	public void setSentimentCategory(String sentimentCategory) {
		this.sentimentCategory = sentimentCategory;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MarketAssetSentiment that = (MarketAssetSentiment) o;
		return Objects.equals(symbol, that.symbol) && Objects.equals(name, that.name)
				&& Objects.equals(sentimentScore, that.sentimentScore)
				&& Objects.equals(sentimentCategory, that.sentimentCategory) && Objects.equals(color, that.color);
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbol, name, sentimentScore, sentimentCategory, color);
	}

	@Override
	public String toString() {
		return "MarketAssetSentiment{" + "symbol='" + symbol + '\'' + ", name='" + name + '\'' + ", sentimentScore="
				+ sentimentScore + ", sentimentCategory='" + sentimentCategory + '\'' + ", color='" + color + '\''
				+ '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String symbol;

		private String name;

		private BigDecimal sentimentScore;

		private String sentimentCategory;

		private String color;

		public Builder withSymbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withSentimentScore(BigDecimal sentimentScore) {
			this.sentimentScore = sentimentScore;
			return this;
		}

		public Builder withSentimentCategory(String sentimentCategory) {
			this.sentimentCategory = sentimentCategory;
			return this;
		}

		public Builder withColor(String color) {
			this.color = color;
			return this;
		}

		public MarketAssetSentiment build() {
			return new MarketAssetSentiment(symbol, name, sentimentScore, sentimentCategory, color);
		}

	}

}
