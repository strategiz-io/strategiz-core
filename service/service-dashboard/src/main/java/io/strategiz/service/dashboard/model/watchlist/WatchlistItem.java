package io.strategiz.service.dashboard.model.watchlist;

import java.math.BigDecimal;
import java.util.Objects;

public class WatchlistItem {

	private String id;

	private String symbol;

	private String name;

	private String type;

	private BigDecimal price;

	private BigDecimal change;

	private BigDecimal changePercent;

	private boolean positiveChange;

	private String chartDataUrl;

	private Long volume;

	// Constructors
	public WatchlistItem() {
	}

	public WatchlistItem(String id, String symbol, String name, String type, BigDecimal price, BigDecimal change,
			BigDecimal changePercent, boolean positiveChange, String chartDataUrl, Long volume) {
		this.id = id;
		this.symbol = symbol;
		this.name = name;
		this.type = type;
		this.price = price;
		this.change = change;
		this.changePercent = changePercent;
		this.positiveChange = positiveChange;
		this.chartDataUrl = chartDataUrl;
		this.volume = volume;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public void setCurrentPrice(BigDecimal price) {
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

	public void setPriceChangePercentage24h(BigDecimal priceChangePercentage24h) {
		this.changePercent = priceChangePercentage24h;
	}

	public boolean isPositiveChange() {
		return positiveChange;
	}

	public void setPositiveChange(boolean positiveChange) {
		this.positiveChange = positiveChange;
	}

	public String getChartDataUrl() {
		return chartDataUrl;
	}

	public void setChartDataUrl(String chartDataUrl) {
		this.chartDataUrl = chartDataUrl;
	}

	public Long getVolume() {
		return volume;
	}

	public void setVolume(Long volume) {
		this.volume = volume;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WatchlistItem that = (WatchlistItem) o;
		return positiveChange == that.positiveChange && Objects.equals(id, that.id)
				&& Objects.equals(symbol, that.symbol) && Objects.equals(name, that.name)
				&& Objects.equals(type, that.type) && Objects.equals(price, that.price)
				&& Objects.equals(change, that.change) && Objects.equals(changePercent, that.changePercent)
				&& Objects.equals(chartDataUrl, that.chartDataUrl) && Objects.equals(volume, that.volume);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, symbol, name, type, price, change, changePercent, positiveChange, chartDataUrl, volume);
	}

	@Override
	public String toString() {
		return "WatchlistItem{" + "id='" + id + '\'' + ", symbol='" + symbol + '\'' + ", name='" + name + '\''
				+ ", type='" + type + '\'' + ", price=" + price + ", change=" + change + ", changePercent="
				+ changePercent + ", positiveChange=" + positiveChange + ", chartDataUrl='" + chartDataUrl + '\''
				+ ", volume=" + volume + '}';
	}

}
