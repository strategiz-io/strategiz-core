package io.strategiz.data.portfolio.model;

import java.math.BigDecimal;

public class PortfolioMetrics {

	private BigDecimal sharpeRatio;

	private BigDecimal beta;

	private BigDecimal alpha;

	private BigDecimal volatility;

	private BigDecimal maxDrawdown;

	private BigDecimal annualizedReturn;

	// Getters and Setters
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

}
