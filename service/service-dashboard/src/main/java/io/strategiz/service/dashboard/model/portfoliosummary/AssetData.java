package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Asset data within an exchange.
 */
public class AssetData {

	private String symbol;

	private String name;

	private BigDecimal quantity;

	private BigDecimal price;

	private BigDecimal value;

	private BigDecimal allocationPercent;

	private BigDecimal dailyChange;

	private BigDecimal dailyChangePercent;

	/**
	 * Default constructor
	 */
	public AssetData() {
	}

	/**
	 * All-args constructor
	 */
	public AssetData(String symbol, String name, BigDecimal quantity, BigDecimal price, BigDecimal value,
			BigDecimal allocationPercent, BigDecimal dailyChange, BigDecimal dailyChangePercent) {
		this.symbol = symbol;
		this.name = name;
		this.quantity = quantity;
		this.price = price;
		this.value = value;
		this.allocationPercent = allocationPercent;
		this.dailyChange = dailyChange;
		this.dailyChangePercent = dailyChangePercent;
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

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getAllocationPercent() {
		return allocationPercent;
	}

	public void setAllocationPercent(BigDecimal allocationPercent) {
		this.allocationPercent = allocationPercent;
	}

	public BigDecimal getDailyChange() {
		return dailyChange;
	}

	public void setDailyChange(BigDecimal dailyChange) {
		this.dailyChange = dailyChange;
	}

	public BigDecimal getDailyChangePercent() {
		return dailyChangePercent;
	}

	public void setDailyChangePercent(BigDecimal dailyChangePercent) {
		this.dailyChangePercent = dailyChangePercent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AssetData assetData = (AssetData) o;
		return Objects.equals(symbol, assetData.symbol) && Objects.equals(name, assetData.name)
				&& Objects.equals(quantity, assetData.quantity) && Objects.equals(price, assetData.price)
				&& Objects.equals(value, assetData.value)
				&& Objects.equals(allocationPercent, assetData.allocationPercent)
				&& Objects.equals(dailyChange, assetData.dailyChange)
				&& Objects.equals(dailyChangePercent, assetData.dailyChangePercent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbol, name, quantity, price, value, allocationPercent, dailyChange, dailyChangePercent);
	}

	@Override
	public String toString() {
		return "AssetData{" + "symbol='" + symbol + '\'' + ", name='" + name + '\'' + ", quantity=" + quantity
				+ ", price=" + price + ", value=" + value + ", allocationPercent=" + allocationPercent
				+ ", dailyChange=" + dailyChange + ", dailyChangePercent=" + dailyChangePercent + '}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String symbol;

		private String name;

		private BigDecimal quantity;

		private BigDecimal price;

		private BigDecimal value;

		private BigDecimal allocationPercent;

		private BigDecimal dailyChange;

		private BigDecimal dailyChangePercent;

		public Builder withSymbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withQuantity(BigDecimal quantity) {
			this.quantity = quantity;
			return this;
		}

		public Builder withPrice(BigDecimal price) {
			this.price = price;
			return this;
		}

		public Builder withValue(BigDecimal value) {
			this.value = value;
			return this;
		}

		public Builder withAllocationPercent(BigDecimal allocationPercent) {
			this.allocationPercent = allocationPercent;
			return this;
		}

		public Builder withDailyChange(BigDecimal dailyChange) {
			this.dailyChange = dailyChange;
			return this;
		}

		public Builder withDailyChangePercent(BigDecimal dailyChangePercent) {
			this.dailyChangePercent = dailyChangePercent;
			return this;
		}

		public AssetData build() {
			return new AssetData(symbol, name, quantity, price, value, allocationPercent, dailyChange,
					dailyChangePercent);
		}

	}

}
