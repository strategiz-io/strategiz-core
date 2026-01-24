package io.strategiz.client.fmp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for FMP quote data from the /quote endpoint.
 *
 * <p>
 * Contains real-time price information including price, change, volume, and key metrics.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FmpQuote {

	private String symbol;

	private String name;

	private BigDecimal price;

	private BigDecimal change;

	@JsonProperty("changesPercentage")
	private BigDecimal changePercent;

	private BigDecimal previousClose;

	private BigDecimal open;

	private BigDecimal dayHigh;

	private BigDecimal dayLow;

	@JsonProperty("yearHigh")
	private BigDecimal yearHigh;

	@JsonProperty("yearLow")
	private BigDecimal yearLow;

	private Long volume;

	@JsonProperty("avgVolume")
	private Long avgVolume;

	private BigDecimal marketCap;

	@JsonProperty("pe")
	private BigDecimal peRatio;

	private BigDecimal eps;

	@JsonProperty("earningsAnnouncement")
	private String earningsAnnouncement;

	@JsonProperty("sharesOutstanding")
	private Long sharesOutstanding;

	private Long timestamp;

	// Getters and Setters

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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getChange() {
		return change;
	}

	public void setChange(BigDecimal change) {
		this.change = change;
	}

	public BigDecimal getChangePercent() {
		return changePercent;
	}

	public void setChangePercent(BigDecimal changePercent) {
		this.changePercent = changePercent;
	}

	public BigDecimal getPreviousClose() {
		return previousClose;
	}

	public void setPreviousClose(BigDecimal previousClose) {
		this.previousClose = previousClose;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public void setOpen(BigDecimal open) {
		this.open = open;
	}

	public BigDecimal getDayHigh() {
		return dayHigh;
	}

	public void setDayHigh(BigDecimal dayHigh) {
		this.dayHigh = dayHigh;
	}

	public BigDecimal getDayLow() {
		return dayLow;
	}

	public void setDayLow(BigDecimal dayLow) {
		this.dayLow = dayLow;
	}

	public BigDecimal getYearHigh() {
		return yearHigh;
	}

	public void setYearHigh(BigDecimal yearHigh) {
		this.yearHigh = yearHigh;
	}

	public BigDecimal getYearLow() {
		return yearLow;
	}

	public void setYearLow(BigDecimal yearLow) {
		this.yearLow = yearLow;
	}

	public Long getVolume() {
		return volume;
	}

	public void setVolume(Long volume) {
		this.volume = volume;
	}

	public Long getAvgVolume() {
		return avgVolume;
	}

	public void setAvgVolume(Long avgVolume) {
		this.avgVolume = avgVolume;
	}

	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(BigDecimal marketCap) {
		this.marketCap = marketCap;
	}

	public BigDecimal getPeRatio() {
		return peRatio;
	}

	public void setPeRatio(BigDecimal peRatio) {
		this.peRatio = peRatio;
	}

	public BigDecimal getEps() {
		return eps;
	}

	public void setEps(BigDecimal eps) {
		this.eps = eps;
	}

	public String getEarningsAnnouncement() {
		return earningsAnnouncement;
	}

	public void setEarningsAnnouncement(String earningsAnnouncement) {
		this.earningsAnnouncement = earningsAnnouncement;
	}

	public Long getSharesOutstanding() {
		return sharesOutstanding;
	}

	public void setSharesOutstanding(Long sharesOutstanding) {
		this.sharesOutstanding = sharesOutstanding;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Check if the quote shows a positive change.
	 * @return true if change is positive or zero
	 */
	public boolean isPositive() {
		return change != null && change.compareTo(BigDecimal.ZERO) >= 0;
	}

	/**
	 * Format the quote for AI context display.
	 * @return formatted string like "SPY: $582.45 (+0.8%)"
	 */
	public String toContextString() {
		if (price == null) {
			return symbol + ": N/A";
		}

		String priceStr = String.format("$%.2f", price);
		String changeStr = "";
		if (changePercent != null) {
			String sign = changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
			changeStr = String.format(" (%s%.2f%%)", sign, changePercent);
		}
		return symbol + ": " + priceStr + changeStr;
	}

	/**
	 * Format the quote with volume for detailed context.
	 * @return formatted string with volume info
	 */
	public String toDetailedContextString() {
		StringBuilder sb = new StringBuilder();
		sb.append(toContextString());
		if (volume != null && avgVolume != null && avgVolume > 0) {
			double volumeRatio = (double) volume / avgVolume;
			sb.append(String.format(" [Vol: %.1fx avg]", volumeRatio));
		}
		return sb.toString();
	}

}
