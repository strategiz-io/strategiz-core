package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sentiment data for a specific asset.
 */
public class AssetSentiment {

	private String assetId;

	private String symbol;

	private String name;

	private String sentiment;

	private BigDecimal sentimentScore;

	private String color;

	private List<String> signals;

	private Map<String, BigDecimal> technicalIndicators;

	private String sentimentCategory;

	// Constructors
	public AssetSentiment() {
	}

	public AssetSentiment(String assetId, String symbol, String name, String sentiment, BigDecimal sentimentScore,
			String color, List<String> signals, Map<String, BigDecimal> technicalIndicators, String sentimentCategory) {
		this.assetId = assetId;
		this.symbol = symbol;
		this.name = name;
		this.sentiment = sentiment;
		this.sentimentScore = sentimentScore;
		this.color = color;
		this.signals = signals;
		this.technicalIndicators = technicalIndicators;
		this.sentimentCategory = sentimentCategory;
	}

	// Getters and Setters
	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

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

	public String getSentiment() {
		return sentiment;
	}

	public void setSentiment(String sentiment) {
		this.sentiment = sentiment;
	}

	public BigDecimal getSentimentScore() {
		return sentimentScore;
	}

	public void setSentimentScore(BigDecimal sentimentScore) {
		this.sentimentScore = sentimentScore;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public List<String> getSignals() {
		return signals;
	}

	public void setSignals(List<String> signals) {
		this.signals = signals;
	}

	public Map<String, BigDecimal> getTechnicalIndicators() {
		return technicalIndicators;
	}

	public void setTechnicalIndicators(Map<String, BigDecimal> technicalIndicators) {
		this.technicalIndicators = technicalIndicators;
	}

	public String getSentimentCategory() {
		return sentimentCategory;
	}

	public void setSentimentCategory(String sentimentCategory) {
		this.sentimentCategory = sentimentCategory;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AssetSentiment that = (AssetSentiment) o;
		return Objects.equals(assetId, that.assetId) && Objects.equals(symbol, that.symbol)
				&& Objects.equals(name, that.name) && Objects.equals(sentiment, that.sentiment)
				&& Objects.equals(sentimentScore, that.sentimentScore) && Objects.equals(color, that.color)
				&& Objects.equals(signals, that.signals)
				&& Objects.equals(technicalIndicators, that.technicalIndicators)
				&& Objects.equals(sentimentCategory, that.sentimentCategory);
	}

	@Override
	public int hashCode() {
		return Objects.hash(assetId, symbol, name, sentiment, sentimentScore, color, signals, technicalIndicators,
				sentimentCategory);
	}

	@Override
	public String toString() {
		return "AssetSentiment{" + "assetId='" + assetId + '\'' + ", symbol='" + symbol + '\'' + ", name='" + name
				+ '\'' + ", sentiment='" + sentiment + '\'' + ", sentimentScore=" + sentimentScore + ", color='" + color
				+ '\'' + ", signals=" + signals + ", technicalIndicators=" + technicalIndicators
				+ ", sentimentCategory='" + sentimentCategory + '\'' + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String assetId;

		private String symbol;

		private String name;

		private String sentiment;

		private BigDecimal sentimentScore;

		private String color;

		private List<String> signals;

		private Map<String, BigDecimal> technicalIndicators;

		private String sentimentCategory;

		public Builder withAssetId(String assetId) {
			this.assetId = assetId;
			return this;
		}

		public Builder withSymbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withSentiment(String sentiment) {
			this.sentiment = sentiment;
			return this;
		}

		public Builder withSentimentScore(BigDecimal sentimentScore) {
			this.sentimentScore = sentimentScore;
			return this;
		}

		public Builder withColor(String color) {
			this.color = color;
			return this;
		}

		public Builder withSignals(List<String> signals) {
			this.signals = signals;
			return this;
		}

		public Builder withTechnicalIndicators(Map<String, BigDecimal> technicalIndicators) {
			this.technicalIndicators = technicalIndicators;
			return this;
		}

		public Builder withSentimentCategory(String sentimentCategory) {
			this.sentimentCategory = sentimentCategory;
			return this;
		}

		public AssetSentiment build() {
			return new AssetSentiment(assetId, symbol, name, sentiment, sentimentScore, color, signals,
					technicalIndicators, sentimentCategory);
		}

	}

}
