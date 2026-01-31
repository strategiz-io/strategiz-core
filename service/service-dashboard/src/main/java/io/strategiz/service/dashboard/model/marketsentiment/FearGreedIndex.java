package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Fear and Greed Index data.
 */
public class FearGreedIndex {

	private int value;

	private String classification;

	private BigDecimal previousClose;

	private BigDecimal weekAgo;

	private BigDecimal monthAgo;

	// Constructors
	public FearGreedIndex() {
	}

	public FearGreedIndex(int value, String classification, BigDecimal previousClose, BigDecimal weekAgo,
			BigDecimal monthAgo) {
		this.value = value;
		this.classification = classification;
		this.previousClose = previousClose;
		this.weekAgo = weekAgo;
		this.monthAgo = monthAgo;
	}

	// Getters and Setters
	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public BigDecimal getPreviousClose() {
		return previousClose;
	}

	public void setPreviousClose(BigDecimal previousClose) {
		this.previousClose = previousClose;
	}

	public BigDecimal getWeekAgo() {
		return weekAgo;
	}

	public void setWeekAgo(BigDecimal weekAgo) {
		this.weekAgo = weekAgo;
	}

	public BigDecimal getMonthAgo() {
		return monthAgo;
	}

	public void setMonthAgo(BigDecimal monthAgo) {
		this.monthAgo = monthAgo;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FearGreedIndex that = (FearGreedIndex) o;
		return value == that.value && Objects.equals(classification, that.classification)
				&& Objects.equals(previousClose, that.previousClose) && Objects.equals(weekAgo, that.weekAgo)
				&& Objects.equals(monthAgo, that.monthAgo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, classification, previousClose, weekAgo, monthAgo);
	}

	@Override
	public String toString() {
		return "FearGreedIndex{" + "value=" + value + ", classification='" + classification + '\'' + ", previousClose="
				+ previousClose + ", weekAgo=" + weekAgo + ", monthAgo=" + monthAgo + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private int value;

		private String classification;

		private BigDecimal previousClose;

		private BigDecimal weekAgo;

		private BigDecimal monthAgo;

		public Builder withValue(int value) {
			this.value = value;
			return this;
		}

		public Builder withClassification(String classification) {
			this.classification = classification;
			return this;
		}

		public Builder withPreviousClose(BigDecimal previousClose) {
			this.previousClose = previousClose;
			return this;
		}

		public Builder withWeekAgo(BigDecimal weekAgo) {
			this.weekAgo = weekAgo;
			return this;
		}

		public Builder withMonthAgo(BigDecimal monthAgo) {
			this.monthAgo = monthAgo;
			return this;
		}

		public FearGreedIndex build() {
			return new FearGreedIndex(value, classification, previousClose, weekAgo, monthAgo);
		}

	}

}
