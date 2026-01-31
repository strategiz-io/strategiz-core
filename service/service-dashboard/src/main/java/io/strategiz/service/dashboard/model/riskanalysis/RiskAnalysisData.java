package io.strategiz.service.dashboard.model.riskanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data model for portfolio risk analysis.
 */
public class RiskAnalysisData {

	/**
	 * Overall portfolio volatility score
	 */
	@JsonProperty("volatility")
	@NotNull
	private VolatilityMetric volatility;

	/**
	 * Portfolio diversification score
	 */
	@JsonProperty("diversification")
	@NotNull
	private DiversificationMetric diversification;

	/**
	 * Correlation metrics between assets
	 */
	@JsonProperty("correlation")
	@NotNull
	private CorrelationMetric correlation;

	// Constructors
	public RiskAnalysisData() {
	}

	public RiskAnalysisData(VolatilityMetric volatility, DiversificationMetric diversification,
			CorrelationMetric correlation) {
		this.volatility = volatility;
		this.diversification = diversification;
		this.correlation = correlation;
	}

	// Getters and Setters
	public VolatilityMetric getVolatility() {
		return volatility;
	}

	public void setVolatility(VolatilityMetric volatility) {
		this.volatility = volatility;
	}

	public DiversificationMetric getDiversification() {
		return diversification;
	}

	public void setDiversification(DiversificationMetric diversification) {
		this.diversification = diversification;
	}

	public CorrelationMetric getCorrelation() {
		return correlation;
	}

	public void setCorrelation(CorrelationMetric correlation) {
		this.correlation = correlation;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RiskAnalysisData that = (RiskAnalysisData) o;
		return Objects.equals(volatility, that.volatility) && Objects.equals(diversification, that.diversification)
				&& Objects.equals(correlation, that.correlation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(volatility, diversification, correlation);
	}

	@Override
	public String toString() {
		return "RiskAnalysisData{" + "volatility=" + volatility + ", diversification=" + diversification
				+ ", correlation=" + correlation + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private VolatilityMetric volatility;

		private DiversificationMetric diversification;

		private CorrelationMetric correlation;

		public Builder withVolatility(VolatilityMetric volatility) {
			this.volatility = volatility;
			return this;
		}

		public Builder withDiversification(DiversificationMetric diversification) {
			this.diversification = diversification;
			return this;
		}

		public Builder withCorrelation(CorrelationMetric correlation) {
			this.correlation = correlation;
			return this;
		}

		public RiskAnalysisData build() {
			return new RiskAnalysisData(volatility, diversification, correlation);
		}

	}

}
