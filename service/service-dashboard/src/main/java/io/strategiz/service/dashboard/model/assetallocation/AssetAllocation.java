package io.strategiz.service.dashboard.model.assetallocation;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Model for individual asset allocation data.
 */
public class AssetAllocation {

	private String id;

	private String name;

	private String symbol;

	private String exchange;

	private BigDecimal value;

	private BigDecimal percentage;

	private String color;

	// Constructors
	public AssetAllocation() {
	}

	public AssetAllocation(String id, String name, String symbol, String exchange, BigDecimal value,
			BigDecimal percentage, String color) {
		this.id = id;
		this.name = name;
		this.symbol = symbol;
		this.exchange = exchange;
		this.value = value;
		this.percentage = percentage;
		this.color = color;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AssetAllocation that = (AssetAllocation) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(symbol, that.symbol)
				&& Objects.equals(exchange, that.exchange) && Objects.equals(value, that.value)
				&& Objects.equals(percentage, that.percentage) && Objects.equals(color, that.color);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, symbol, exchange, value, percentage, color);
	}

	@Override
	public String toString() {
		return "AssetAllocation{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", symbol='" + symbol + '\''
				+ ", exchange='" + exchange + '\'' + ", value=" + value + ", percentage=" + percentage + ", color='"
				+ color + '\'' + '}';
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String id;

		private String name;

		private String symbol;

		private String exchange;

		private BigDecimal value;

		private BigDecimal percentage;

		private String color;

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withSymbol(String symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder withExchange(String exchange) {
			this.exchange = exchange;
			return this;
		}

		public Builder withValue(BigDecimal value) {
			this.value = value;
			return this;
		}

		public Builder withPercentage(BigDecimal percentage) {
			this.percentage = percentage;
			return this;
		}

		public Builder withColor(String color) {
			this.color = color;
			return this;
		}

		public AssetAllocation build() {
			return new AssetAllocation(id, name, symbol, exchange, value, percentage, color);
		}

	}

}
