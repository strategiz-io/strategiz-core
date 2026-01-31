package io.strategiz.business.historicalinsights.model;

import java.math.BigDecimal;

/**
 * Represents fundamental analysis insights for a symbol. This is populated when the user
 * enables "Include fundamental analysis" in Historical Market Insights (Autonomous AI
 * mode).
 */
public class FundamentalsInsights {

	private String symbol;

	// Valuation ratios
	private BigDecimal peRatio;

	private BigDecimal pbRatio;

	private BigDecimal psRatio;

	// Profitability
	private BigDecimal roe; // Return on Equity

	private BigDecimal profitMargin;

	// Growth
	private BigDecimal revenueGrowthYoY;

	private BigDecimal epsGrowthYoY;

	// Other
	private BigDecimal dividendYield;

	// Summary for AI prompt
	private String summary;

	public FundamentalsInsights() {
	}

	// Getters and setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getPeRatio() {
		return peRatio;
	}

	public void setPeRatio(BigDecimal peRatio) {
		this.peRatio = peRatio;
	}

	public BigDecimal getPbRatio() {
		return pbRatio;
	}

	public void setPbRatio(BigDecimal pbRatio) {
		this.pbRatio = pbRatio;
	}

	public BigDecimal getPsRatio() {
		return psRatio;
	}

	public void setPsRatio(BigDecimal psRatio) {
		this.psRatio = psRatio;
	}

	public BigDecimal getRoe() {
		return roe;
	}

	public void setRoe(BigDecimal roe) {
		this.roe = roe;
	}

	public BigDecimal getProfitMargin() {
		return profitMargin;
	}

	public void setProfitMargin(BigDecimal profitMargin) {
		this.profitMargin = profitMargin;
	}

	public BigDecimal getRevenueGrowthYoY() {
		return revenueGrowthYoY;
	}

	public void setRevenueGrowthYoY(BigDecimal revenueGrowthYoY) {
		this.revenueGrowthYoY = revenueGrowthYoY;
	}

	public BigDecimal getEpsGrowthYoY() {
		return epsGrowthYoY;
	}

	public void setEpsGrowthYoY(BigDecimal epsGrowthYoY) {
		this.epsGrowthYoY = epsGrowthYoY;
	}

	public BigDecimal getDividendYield() {
		return dividendYield;
	}

	public void setDividendYield(BigDecimal dividendYield) {
		this.dividendYield = dividendYield;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

}
