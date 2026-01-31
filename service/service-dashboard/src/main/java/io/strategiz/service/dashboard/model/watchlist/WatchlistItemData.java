package io.strategiz.service.dashboard.model.watchlist;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an asset in a user's watchlist.
 */
public class WatchlistItemData {

	private String id;

	private String symbol;

	private String name;

	private String category;

	private BigDecimal price;

	private BigDecimal change;

	private BigDecimal changePercent;

	private boolean positiveChange;

	private String chartDataUrl;

	// Constructors
	public WatchlistItemData() {
	}

	public WatchlistItemData(String id, String symbol, String name, String category, BigDecimal price,
			BigDecimal change, BigDecimal changePercent, boolean positiveChange, String chartDataUrl) {
		this.id = id;
		this.symbol = symbol;
		this.name = name;
		this.category = category;
		this.price = price;
		this.change = change;
		this.changePercent = changePercent;
		this.positiveChange = positiveChange;
		this.chartDataUrl = chartDataUrl;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WatchlistItemData that = (WatchlistItemData) o;
		return positiveChange == that.positiveChange && Objects.equals(id, that.id)
				&& Objects.equals(symbol, that.symbol) && Objects.equals(name, that.name)
				&& Objects.equals(category, that.category) && Objects.equals(price, that.price)
				&& Objects.equals(change, that.change) && Objects.equals(changePercent, that.changePercent)
				&& Objects.equals(chartDataUrl, that.chartDataUrl);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, symbol, name, category, price, change, changePercent, positiveChange, chartDataUrl);
	}

	@Override
	public String toString() {
		return "WatchlistItemData{" + "id='" + id + '\'' + ", symbol='" + symbol + '\'' + ", name='" + name + '\''
				+ ", category='" + category + '\'' + ", price=" + price + ", change=" + change + ", changePercent="
				+ changePercent + ", positiveChange=" + positiveChange + ", chartDataUrl='" + chartDataUrl + '\'' + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String id;

		private String symbol;

		private String name;

		private String category;

		private BigDecimal price;

		private BigDecimal change;

		private BigDecimal changePercent;

		private boolean positiveChange;

		private String chartDataUrl;

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withSymbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withCategory(String category) {
			this.category = category;
			return this;
		}

		public Builder withPrice(BigDecimal price) {
			this.price = price;
			return this;
		}

		public Builder withChange(BigDecimal change) {
			this.change = change;
			return this;
		}

		public Builder withChangePercent(BigDecimal changePercent) {
			this.changePercent = changePercent;
			return this;
		}

		public Builder withPositiveChange(boolean positiveChange) {
			this.positiveChange = positiveChange;
			return this;
		}

		public Builder withChartDataUrl(String chartDataUrl) {
			this.chartDataUrl = chartDataUrl;
			return this;
		}

		public WatchlistItemData build() {
			return new WatchlistItemData(id, symbol, name, category, price, change, changePercent, positiveChange,
					chartDataUrl);
		}

	}

}
