package io.strategiz.service.dashboard.model.riskanalysis;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Diversification metric
 */
public class DiversificationMetric {

	/**
	 * Diversification score from 0 (lowest) to 100 (highest)
	 */
	private BigDecimal score;

	/**
	 * Diversification category (Poor, Fair, Good, Excellent)
	 */
	private String category;

	/**
	 * Number of unique assets in portfolio
	 */
	private int assetCount;

	/**
	 * Percentage of portfolio in largest single asset
	 */
	private BigDecimal largestAllocation;

	/**
	 * Herfindahl-Hirschman Index (concentration measure)
	 */
	private BigDecimal concentrationIndex;

	// Constructors
	public DiversificationMetric() {
	}

	public DiversificationMetric(BigDecimal score, String category, int assetCount, BigDecimal largestAllocation,
			BigDecimal concentrationIndex) {
		this.score = score;
		this.category = category;
		this.assetCount = assetCount;
		this.largestAllocation = largestAllocation;
		this.concentrationIndex = concentrationIndex;
	}

	// Getters and Setters
	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getAssetCount() {
		return assetCount;
	}

	public void setAssetCount(int assetCount) {
		this.assetCount = assetCount;
	}

	public BigDecimal getLargestAllocation() {
		return largestAllocation;
	}

	public void setLargestAllocation(BigDecimal largestAllocation) {
		this.largestAllocation = largestAllocation;
	}

	public BigDecimal getConcentrationIndex() {
		return concentrationIndex;
	}

	public void setConcentrationIndex(BigDecimal concentrationIndex) {
		this.concentrationIndex = concentrationIndex;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DiversificationMetric that = (DiversificationMetric) o;
		return assetCount == that.assetCount && Objects.equals(score, that.score)
				&& Objects.equals(category, that.category) && Objects.equals(largestAllocation, that.largestAllocation)
				&& Objects.equals(concentrationIndex, that.concentrationIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(score, category, assetCount, largestAllocation, concentrationIndex);
	}

	@Override
	public String toString() {
		return "DiversificationMetric{" + "score=" + score + ", category='" + category + '\'' + ", assetCount="
				+ assetCount + ", largestAllocation=" + largestAllocation + ", concentrationIndex=" + concentrationIndex
				+ '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private BigDecimal score;

		private String category;

		private int assetCount;

		private BigDecimal largestAllocation;

		private BigDecimal concentrationIndex;

		public Builder withScore(BigDecimal score) {
			this.score = score;
			return this;
		}

		public Builder withCategory(String category) {
			this.category = category;
			return this;
		}

		public Builder withAssetCount(int assetCount) {
			this.assetCount = assetCount;
			return this;
		}

		public Builder withLargestAllocation(BigDecimal largestAllocation) {
			this.largestAllocation = largestAllocation;
			return this;
		}

		public Builder withConcentrationIndex(BigDecimal concentrationIndex) {
			this.concentrationIndex = concentrationIndex;
			return this;
		}

		public DiversificationMetric build() {
			return new DiversificationMetric(score, category, assetCount, largestAllocation, concentrationIndex);
		}

	}

}
