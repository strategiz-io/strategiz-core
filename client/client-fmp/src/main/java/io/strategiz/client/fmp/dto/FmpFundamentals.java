package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * FMP fundamentals data combining financial statements and key metrics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpFundamentals {

	private String symbol;

	private String date;

	private String period; // "Q1", "Q2", "Q3", "Q4", "FY"

	// Income Statement
	private BigDecimal revenue;

	@JsonProperty("costOfRevenue")
	private BigDecimal costOfRevenue;

	@JsonProperty("grossProfit")
	private BigDecimal grossProfit;

	@JsonProperty("operatingIncome")
	private BigDecimal operatingIncome;

	@JsonProperty("netIncome")
	private BigDecimal netIncome;

	private BigDecimal eps;

	@JsonProperty("ebitda")
	private BigDecimal ebitda;

	// Balance Sheet
	@JsonProperty("totalAssets")
	private BigDecimal totalAssets;

	@JsonProperty("totalLiabilities")
	private BigDecimal totalLiabilities;

	@JsonProperty("totalStockholdersEquity")
	private BigDecimal totalStockholdersEquity;

	@JsonProperty("cashAndCashEquivalents")
	private BigDecimal cash;

	@JsonProperty("totalDebt")
	private BigDecimal totalDebt;

	// Cash Flow
	@JsonProperty("operatingCashFlow")
	private BigDecimal operatingCashFlow;

	@JsonProperty("capitalExpenditure")
	private BigDecimal capitalExpenditure;

	@JsonProperty("freeCashFlow")
	private BigDecimal freeCashFlow;

	// Key Metrics
	@JsonProperty("peRatio")
	private BigDecimal peRatio;

	@JsonProperty("priceToBookRatio")
	private BigDecimal priceToBook;

	@JsonProperty("debtToEquity")
	private BigDecimal debtToEquity;

	@JsonProperty("returnOnEquity")
	private BigDecimal roe;

	@JsonProperty("returnOnAssets")
	private BigDecimal roa;

	@JsonProperty("currentRatio")
	private BigDecimal currentRatio;

	@JsonProperty("quickRatio")
	private BigDecimal quickRatio;

	@JsonProperty("grossProfitMargin")
	private BigDecimal grossMargin;

	@JsonProperty("operatingProfitMargin")
	private BigDecimal operatingMargin;

	@JsonProperty("netProfitMargin")
	private BigDecimal netMargin;

	@JsonProperty("dividendYield")
	private BigDecimal dividendYield;

	@JsonProperty("payoutRatio")
	private BigDecimal payoutRatio;

	// Getters and Setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public BigDecimal getRevenue() {
		return revenue;
	}

	public void setRevenue(BigDecimal revenue) {
		this.revenue = revenue;
	}

	public BigDecimal getCostOfRevenue() {
		return costOfRevenue;
	}

	public void setCostOfRevenue(BigDecimal costOfRevenue) {
		this.costOfRevenue = costOfRevenue;
	}

	public BigDecimal getGrossProfit() {
		return grossProfit;
	}

	public void setGrossProfit(BigDecimal grossProfit) {
		this.grossProfit = grossProfit;
	}

	public BigDecimal getOperatingIncome() {
		return operatingIncome;
	}

	public void setOperatingIncome(BigDecimal operatingIncome) {
		this.operatingIncome = operatingIncome;
	}

	public BigDecimal getNetIncome() {
		return netIncome;
	}

	public void setNetIncome(BigDecimal netIncome) {
		this.netIncome = netIncome;
	}

	public BigDecimal getEps() {
		return eps;
	}

	public void setEps(BigDecimal eps) {
		this.eps = eps;
	}

	public BigDecimal getEbitda() {
		return ebitda;
	}

	public void setEbitda(BigDecimal ebitda) {
		this.ebitda = ebitda;
	}

	public BigDecimal getTotalAssets() {
		return totalAssets;
	}

	public void setTotalAssets(BigDecimal totalAssets) {
		this.totalAssets = totalAssets;
	}

	public BigDecimal getTotalLiabilities() {
		return totalLiabilities;
	}

	public void setTotalLiabilities(BigDecimal totalLiabilities) {
		this.totalLiabilities = totalLiabilities;
	}

	public BigDecimal getTotalStockholdersEquity() {
		return totalStockholdersEquity;
	}

	public void setTotalStockholdersEquity(BigDecimal totalStockholdersEquity) {
		this.totalStockholdersEquity = totalStockholdersEquity;
	}

	public BigDecimal getCash() {
		return cash;
	}

	public void setCash(BigDecimal cash) {
		this.cash = cash;
	}

	public BigDecimal getTotalDebt() {
		return totalDebt;
	}

	public void setTotalDebt(BigDecimal totalDebt) {
		this.totalDebt = totalDebt;
	}

	public BigDecimal getOperatingCashFlow() {
		return operatingCashFlow;
	}

	public void setOperatingCashFlow(BigDecimal operatingCashFlow) {
		this.operatingCashFlow = operatingCashFlow;
	}

	public BigDecimal getCapitalExpenditure() {
		return capitalExpenditure;
	}

	public void setCapitalExpenditure(BigDecimal capitalExpenditure) {
		this.capitalExpenditure = capitalExpenditure;
	}

	public BigDecimal getFreeCashFlow() {
		return freeCashFlow;
	}

	public void setFreeCashFlow(BigDecimal freeCashFlow) {
		this.freeCashFlow = freeCashFlow;
	}

	public BigDecimal getPeRatio() {
		return peRatio;
	}

	public void setPeRatio(BigDecimal peRatio) {
		this.peRatio = peRatio;
	}

	public BigDecimal getPriceToBook() {
		return priceToBook;
	}

	public void setPriceToBook(BigDecimal priceToBook) {
		this.priceToBook = priceToBook;
	}

	public BigDecimal getDebtToEquity() {
		return debtToEquity;
	}

	public void setDebtToEquity(BigDecimal debtToEquity) {
		this.debtToEquity = debtToEquity;
	}

	public BigDecimal getRoe() {
		return roe;
	}

	public void setRoe(BigDecimal roe) {
		this.roe = roe;
	}

	public BigDecimal getRoa() {
		return roa;
	}

	public void setRoa(BigDecimal roa) {
		this.roa = roa;
	}

	public BigDecimal getCurrentRatio() {
		return currentRatio;
	}

	public void setCurrentRatio(BigDecimal currentRatio) {
		this.currentRatio = currentRatio;
	}

	public BigDecimal getQuickRatio() {
		return quickRatio;
	}

	public void setQuickRatio(BigDecimal quickRatio) {
		this.quickRatio = quickRatio;
	}

	public BigDecimal getGrossMargin() {
		return grossMargin;
	}

	public void setGrossMargin(BigDecimal grossMargin) {
		this.grossMargin = grossMargin;
	}

	public BigDecimal getOperatingMargin() {
		return operatingMargin;
	}

	public void setOperatingMargin(BigDecimal operatingMargin) {
		this.operatingMargin = operatingMargin;
	}

	public BigDecimal getNetMargin() {
		return netMargin;
	}

	public void setNetMargin(BigDecimal netMargin) {
		this.netMargin = netMargin;
	}

	public BigDecimal getDividendYield() {
		return dividendYield;
	}

	public void setDividendYield(BigDecimal dividendYield) {
		this.dividendYield = dividendYield;
	}

	public BigDecimal getPayoutRatio() {
		return payoutRatio;
	}

	public void setPayoutRatio(BigDecimal payoutRatio) {
		this.payoutRatio = payoutRatio;
	}

}
