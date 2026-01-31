package io.strategiz.client.alphavantage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Model class for stock market data from AlphaVantage API.
 */
public class StockData {

	private String symbol; // Stock symbol (e.g., 'MSFT')

	private String name; // Company name (e.g., 'Microsoft Corp')

	private String exchange; // Exchange (e.g., 'NASDAQ')

	@JsonProperty("price")
	private BigDecimal price; // Current stock price

	@JsonProperty("change")
	private BigDecimal change; // Absolute price change

	@JsonProperty("change_percent")
	private BigDecimal changePercent; // Price change percentage

	@JsonProperty("market_cap")
	private BigDecimal marketCap; // Market capitalization

	@JsonProperty("volume")
	private BigDecimal volume; // Trading volume

	@JsonProperty("pe_ratio")
	private BigDecimal peRatio; // Price to earnings ratio

	@JsonProperty("dividend_yield")
	private BigDecimal dividendYield; // Dividend yield percentage

	@JsonProperty("last_updated")
	private String lastUpdated; // Last updated timestamp

	// Constructors
	public StockData() {
	}

	public StockData(String symbol, String name, String exchange) {
		this.symbol = symbol;
		this.name = name;
		this.exchange = exchange;
	}

	// Getters and setters
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

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
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

	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(BigDecimal marketCap) {
		this.marketCap = marketCap;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public void setVolume(BigDecimal volume) {
		this.volume = volume;
	}

	public BigDecimal getPeRatio() {
		return peRatio;
	}

	public void setPeRatio(BigDecimal peRatio) {
		this.peRatio = peRatio;
	}

	public BigDecimal getDividendYield() {
		return dividendYield;
	}

	public void setDividendYield(BigDecimal dividendYield) {
		this.dividendYield = dividendYield;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		StockData stockData = (StockData) o;
		return Objects.equals(symbol, stockData.symbol) && Objects.equals(name, stockData.name)
				&& Objects.equals(exchange, stockData.exchange) && Objects.equals(price, stockData.price)
				&& Objects.equals(change, stockData.change) && Objects.equals(changePercent, stockData.changePercent)
				&& Objects.equals(marketCap, stockData.marketCap) && Objects.equals(volume, stockData.volume)
				&& Objects.equals(peRatio, stockData.peRatio) && Objects.equals(dividendYield, stockData.dividendYield)
				&& Objects.equals(lastUpdated, stockData.lastUpdated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbol, name, exchange, price, change, changePercent, marketCap, volume, peRatio,
				dividendYield, lastUpdated);
	}

	@Override
	public String toString() {
		return "StockData{" + "symbol='" + symbol + '\'' + ", name='" + name + '\'' + ", exchange='" + exchange + '\''
				+ ", price=" + price + ", change=" + change + ", changePercent=" + changePercent + ", marketCap="
				+ marketCap + ", volume=" + volume + ", peRatio=" + peRatio + ", dividendYield=" + dividendYield
				+ ", lastUpdated='" + lastUpdated + '\'' + '}';
	}

}
