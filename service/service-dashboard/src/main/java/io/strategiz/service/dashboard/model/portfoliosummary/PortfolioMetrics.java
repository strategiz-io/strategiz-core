package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Portfolio performance metrics.
 */
public class PortfolioMetrics {

	private BigDecimal sharpeRatio;

	private BigDecimal beta;

	private BigDecimal alpha;

	private BigDecimal volatility;

	private BigDecimal maxDrawdown;

	private BigDecimal annualizedReturn;

	/**
	 * Default constructor
	 */
	public PortfolioMetrics() {
	}

	/**
	 * All-args constructor
	 */
	public PortfolioMetrics(BigDecimal sharpeRatio, BigDecimal beta, BigDecimal alpha, BigDecimal volatility,
			BigDecimal maxDrawdown, BigDecimal annualizedReturn) {
		this.sharpeRatio = sharpeRatio;
		this.beta = beta;
		this.alpha = alpha;
		this.volatility = volatility;
		this.maxDrawdown = maxDrawdown;
		this.annualizedReturn = annualizedReturn;
	}

	public BigDecimal getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(BigDecimal sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public BigDecimal getBeta() {
		return beta;
	}

	public void setBeta(BigDecimal beta) {
		this.beta = beta;
	}

	public BigDecimal getAlpha() {
		return alpha;
	}

	public void setAlpha(BigDecimal alpha) {
		this.alpha = alpha;
	}

	public BigDecimal getVolatility() {
		return volatility;
	}

	public void setVolatility(BigDecimal volatility) {
		this.volatility = volatility;
	}

	public BigDecimal getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(BigDecimal maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public BigDecimal getAnnualizedReturn() {
		return annualizedReturn;
	}

	public void setAnnualizedReturn(BigDecimal annualizedReturn) {
		this.annualizedReturn = annualizedReturn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PortfolioMetrics that = (PortfolioMetrics) o;
		return Objects.equals(sharpeRatio, that.sharpeRatio) && Objects.equals(beta, that.beta)
				&& Objects.equals(alpha, that.alpha) && Objects.equals(volatility, that.volatility)
				&& Objects.equals(maxDrawdown, that.maxDrawdown)
				&& Objects.equals(annualizedReturn, that.annualizedReturn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sharpeRatio, beta, alpha, volatility, maxDrawdown, annualizedReturn);
	}

	@Override
	public String toString() {
		return "PortfolioMetrics{" + "sharpeRatio=" + sharpeRatio + ", beta=" + beta + ", alpha=" + alpha
				+ ", volatility=" + volatility + ", maxDrawdown=" + maxDrawdown + ", annualizedReturn="
				+ annualizedReturn + '}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private BigDecimal sharpeRatio;

		private BigDecimal beta;

		private BigDecimal alpha;

		private BigDecimal volatility;

		private BigDecimal maxDrawdown;

		private BigDecimal annualizedReturn;

		public Builder withSharpeRatio(BigDecimal sharpeRatio) {
			this.sharpeRatio = sharpeRatio;
			return this;
		}

		public Builder withBeta(BigDecimal beta) {
			this.beta = beta;
			return this;
		}

		public Builder withAlpha(BigDecimal alpha) {
			this.alpha = alpha;
			return this;
		}

		public Builder withVolatility(BigDecimal volatility) {
			this.volatility = volatility;
			return this;
		}

		public Builder withMaxDrawdown(BigDecimal maxDrawdown) {
			this.maxDrawdown = maxDrawdown;
			return this;
		}

		public Builder withAnnualizedReturn(BigDecimal annualizedReturn) {
			this.annualizedReturn = annualizedReturn;
			return this;
		}

		public PortfolioMetrics build() {
			return new PortfolioMetrics(sharpeRatio, beta, alpha, volatility, maxDrawdown, annualizedReturn);
		}

	}

}
