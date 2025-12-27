package io.strategiz.client.yahoofinance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Yahoo Finance Key Statistics DTO.
 *
 * Contains valuation metrics, share information, and key ratios.
 * Corresponds to the keyStatistics and defaultKeyStatistics sections of Yahoo's quote summary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooKeyStatistics {

	@JsonProperty("marketCap")
	private BigDecimal marketCap;

	@JsonProperty("enterpriseValue")
	private BigDecimal enterpriseValue;

	@JsonProperty("trailingPE")
	private BigDecimal trailingPE;

	@JsonProperty("forwardPE")
	private BigDecimal forwardPE;

	@JsonProperty("priceToBook")
	private BigDecimal priceToBook;

	@JsonProperty("priceToSales")
	private BigDecimal priceToSales;

	@JsonProperty("pegRatio")
	private BigDecimal pegRatio;

	@JsonProperty("enterpriseToRevenue")
	private BigDecimal enterpriseToRevenue;

	@JsonProperty("enterpriseToEbitda")
	private BigDecimal enterpriseToEbitda;

	@JsonProperty("sharesOutstanding")
	private Long sharesOutstanding;

	@JsonProperty("floatShares")
	private Long floatShares;

	@JsonProperty("sharesShort")
	private Long sharesShort;

	@JsonProperty("shortRatio")
	private BigDecimal shortRatio;

	@JsonProperty("shortPercentOfFloat")
	private BigDecimal shortPercentOfFloat;

	@JsonProperty("beta")
	private BigDecimal beta;

	@JsonProperty("bookValue")
	private BigDecimal bookValue;

	@JsonProperty("priceToBook")
	private BigDecimal priceToBookValue;

	@JsonProperty("trailingEps")
	private BigDecimal trailingEps;

	@JsonProperty("forwardEps")
	private BigDecimal forwardEps;

	@JsonProperty("fiftyTwoWeekLow")
	private BigDecimal fiftyTwoWeekLow;

	@JsonProperty("fiftyTwoWeekHigh")
	private BigDecimal fiftyTwoWeekHigh;

	@JsonProperty("fiftyDayAverage")
	private BigDecimal fiftyDayAverage;

	@JsonProperty("twoHundredDayAverage")
	private BigDecimal twoHundredDayAverage;

	@JsonProperty("dividendRate")
	private BigDecimal dividendRate;

	@JsonProperty("dividendYield")
	private BigDecimal dividendYield;

	@JsonProperty("exDividendDate")
	private Long exDividendDate;

	@JsonProperty("payoutRatio")
	private BigDecimal payoutRatio;

	@JsonProperty("fiveYearAvgDividendYield")
	private BigDecimal fiveYearAvgDividendYield;

	// Constructors
	public YahooKeyStatistics() {
	}

	// Getters and Setters
	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(BigDecimal marketCap) {
		this.marketCap = marketCap;
	}

	public BigDecimal getEnterpriseValue() {
		return enterpriseValue;
	}

	public void setEnterpriseValue(BigDecimal enterpriseValue) {
		this.enterpriseValue = enterpriseValue;
	}

	public BigDecimal getTrailingPE() {
		return trailingPE;
	}

	public void setTrailingPE(BigDecimal trailingPE) {
		this.trailingPE = trailingPE;
	}

	public BigDecimal getForwardPE() {
		return forwardPE;
	}

	public void setForwardPE(BigDecimal forwardPE) {
		this.forwardPE = forwardPE;
	}

	public BigDecimal getPriceToBook() {
		return priceToBook;
	}

	public void setPriceToBook(BigDecimal priceToBook) {
		this.priceToBook = priceToBook;
	}

	public BigDecimal getPriceToSales() {
		return priceToSales;
	}

	public void setPriceToSales(BigDecimal priceToSales) {
		this.priceToSales = priceToSales;
	}

	public BigDecimal getPegRatio() {
		return pegRatio;
	}

	public void setPegRatio(BigDecimal pegRatio) {
		this.pegRatio = pegRatio;
	}

	public BigDecimal getEnterpriseToRevenue() {
		return enterpriseToRevenue;
	}

	public void setEnterpriseToRevenue(BigDecimal enterpriseToRevenue) {
		this.enterpriseToRevenue = enterpriseToRevenue;
	}

	public BigDecimal getEnterpriseToEbitda() {
		return enterpriseToEbitda;
	}

	public void setEnterpriseToEbitda(BigDecimal enterpriseToEbitda) {
		this.enterpriseToEbitda = enterpriseToEbitda;
	}

	public Long getSharesOutstanding() {
		return sharesOutstanding;
	}

	public void setSharesOutstanding(Long sharesOutstanding) {
		this.sharesOutstanding = sharesOutstanding;
	}

	public Long getFloatShares() {
		return floatShares;
	}

	public void setFloatShares(Long floatShares) {
		this.floatShares = floatShares;
	}

	public Long getSharesShort() {
		return sharesShort;
	}

	public void setSharesShort(Long sharesShort) {
		this.sharesShort = sharesShort;
	}

	public BigDecimal getShortRatio() {
		return shortRatio;
	}

	public void setShortRatio(BigDecimal shortRatio) {
		this.shortRatio = shortRatio;
	}

	public BigDecimal getShortPercentOfFloat() {
		return shortPercentOfFloat;
	}

	public void setShortPercentOfFloat(BigDecimal shortPercentOfFloat) {
		this.shortPercentOfFloat = shortPercentOfFloat;
	}

	public BigDecimal getBeta() {
		return beta;
	}

	public void setBeta(BigDecimal beta) {
		this.beta = beta;
	}

	public BigDecimal getBookValue() {
		return bookValue;
	}

	public void setBookValue(BigDecimal bookValue) {
		this.bookValue = bookValue;
	}

	public BigDecimal getPriceToBookValue() {
		return priceToBookValue;
	}

	public void setPriceToBookValue(BigDecimal priceToBookValue) {
		this.priceToBookValue = priceToBookValue;
	}

	public BigDecimal getTrailingEps() {
		return trailingEps;
	}

	public void setTrailingEps(BigDecimal trailingEps) {
		this.trailingEps = trailingEps;
	}

	public BigDecimal getForwardEps() {
		return forwardEps;
	}

	public void setForwardEps(BigDecimal forwardEps) {
		this.forwardEps = forwardEps;
	}

	public BigDecimal getFiftyTwoWeekLow() {
		return fiftyTwoWeekLow;
	}

	public void setFiftyTwoWeekLow(BigDecimal fiftyTwoWeekLow) {
		this.fiftyTwoWeekLow = fiftyTwoWeekLow;
	}

	public BigDecimal getFiftyTwoWeekHigh() {
		return fiftyTwoWeekHigh;
	}

	public void setFiftyTwoWeekHigh(BigDecimal fiftyTwoWeekHigh) {
		this.fiftyTwoWeekHigh = fiftyTwoWeekHigh;
	}

	public BigDecimal getFiftyDayAverage() {
		return fiftyDayAverage;
	}

	public void setFiftyDayAverage(BigDecimal fiftyDayAverage) {
		this.fiftyDayAverage = fiftyDayAverage;
	}

	public BigDecimal getTwoHundredDayAverage() {
		return twoHundredDayAverage;
	}

	public void setTwoHundredDayAverage(BigDecimal twoHundredDayAverage) {
		this.twoHundredDayAverage = twoHundredDayAverage;
	}

	public BigDecimal getDividendRate() {
		return dividendRate;
	}

	public void setDividendRate(BigDecimal dividendRate) {
		this.dividendRate = dividendRate;
	}

	public BigDecimal getDividendYield() {
		return dividendYield;
	}

	public void setDividendYield(BigDecimal dividendYield) {
		this.dividendYield = dividendYield;
	}

	public Long getExDividendDate() {
		return exDividendDate;
	}

	public void setExDividendDate(Long exDividendDate) {
		this.exDividendDate = exDividendDate;
	}

	public BigDecimal getPayoutRatio() {
		return payoutRatio;
	}

	public void setPayoutRatio(BigDecimal payoutRatio) {
		this.payoutRatio = payoutRatio;
	}

	public BigDecimal getFiveYearAvgDividendYield() {
		return fiveYearAvgDividendYield;
	}

	public void setFiveYearAvgDividendYield(BigDecimal fiveYearAvgDividendYield) {
		this.fiveYearAvgDividendYield = fiveYearAvgDividendYield;
	}

}
