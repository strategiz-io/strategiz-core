package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Market trends data (from MarketSentimentData)
 */
public class MarketTrendsData {

	/**
	 * Fear and Greed Index value
	 */
	private BigDecimal fearGreedIndex;

	/**
	 * Fear and Greed category
	 */
	private String fearGreedCategory;

	/**
	 * Percentage of assets in uptrend across the market
	 */
	private BigDecimal uptrendPercentage;

	/**
	 * Percentage of assets in downtrend across the market
	 */
	private BigDecimal downtrendPercentage;

	/**
	 * Percentage of assets in sideways/neutral trend
	 */
	private BigDecimal neutralTrendPercentage;

	// Constructors
	public MarketTrendsData() {
	}

	public MarketTrendsData(BigDecimal fearGreedIndex, String fearGreedCategory, BigDecimal uptrendPercentage,
			BigDecimal downtrendPercentage, BigDecimal neutralTrendPercentage) {
		this.fearGreedIndex = fearGreedIndex;
		this.fearGreedCategory = fearGreedCategory;
		this.uptrendPercentage = uptrendPercentage;
		this.downtrendPercentage = downtrendPercentage;
		this.neutralTrendPercentage = neutralTrendPercentage;
	}

	// Getters and Setters
	public BigDecimal getFearGreedIndex() {
		return fearGreedIndex;
	}

	public void setFearGreedIndex(BigDecimal fearGreedIndex) {
		this.fearGreedIndex = fearGreedIndex;
	}

	public String getFearGreedCategory() {
		return fearGreedCategory;
	}

	public void setFearGreedCategory(String fearGreedCategory) {
		this.fearGreedCategory = fearGreedCategory;
	}

	public BigDecimal getUptrendPercentage() {
		return uptrendPercentage;
	}

	public void setUptrendPercentage(BigDecimal uptrendPercentage) {
		this.uptrendPercentage = uptrendPercentage;
	}

	public BigDecimal getDowntrendPercentage() {
		return downtrendPercentage;
	}

	public void setDowntrendPercentage(BigDecimal downtrendPercentage) {
		this.downtrendPercentage = downtrendPercentage;
	}

	public BigDecimal getNeutralTrendPercentage() {
		return neutralTrendPercentage;
	}

	public void setNeutralTrendPercentage(BigDecimal neutralTrendPercentage) {
		this.neutralTrendPercentage = neutralTrendPercentage;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MarketTrendsData that = (MarketTrendsData) o;
		return Objects.equals(fearGreedIndex, that.fearGreedIndex)
				&& Objects.equals(fearGreedCategory, that.fearGreedCategory)
				&& Objects.equals(uptrendPercentage, that.uptrendPercentage)
				&& Objects.equals(downtrendPercentage, that.downtrendPercentage)
				&& Objects.equals(neutralTrendPercentage, that.neutralTrendPercentage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fearGreedIndex, fearGreedCategory, uptrendPercentage, downtrendPercentage,
				neutralTrendPercentage);
	}

	@Override
	public String toString() {
		return "MarketTrendsData{" + "fearGreedIndex=" + fearGreedIndex + ", fearGreedCategory='" + fearGreedCategory
				+ '\'' + ", uptrendPercentage=" + uptrendPercentage + ", downtrendPercentage=" + downtrendPercentage
				+ ", neutralTrendPercentage=" + neutralTrendPercentage + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private BigDecimal fearGreedIndex;

		private String fearGreedCategory;

		private BigDecimal uptrendPercentage;

		private BigDecimal downtrendPercentage;

		private BigDecimal neutralTrendPercentage;

		public Builder withFearGreedIndex(BigDecimal fearGreedIndex) {
			this.fearGreedIndex = fearGreedIndex;
			return this;
		}

		public Builder withFearGreedCategory(String fearGreedCategory) {
			this.fearGreedCategory = fearGreedCategory;
			return this;
		}

		public Builder withUptrendPercentage(BigDecimal uptrendPercentage) {
			this.uptrendPercentage = uptrendPercentage;
			return this;
		}

		public Builder withDowntrendPercentage(BigDecimal downtrendPercentage) {
			this.downtrendPercentage = downtrendPercentage;
			return this;
		}

		public Builder withNeutralTrendPercentage(BigDecimal neutralTrendPercentage) {
			this.neutralTrendPercentage = neutralTrendPercentage;
			return this;
		}

		public MarketTrendsData build() {
			return new MarketTrendsData(fearGreedIndex, fearGreedCategory, uptrendPercentage, downtrendPercentage,
					neutralTrendPercentage);
		}

	}

}
