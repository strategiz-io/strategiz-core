package io.strategiz.business.portfolio.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing portfolio data and holdings
 */
public class PortfolioData {

	/**
	 * User ID associated with this portfolio data
	 */
	private String userId;

	/**
	 * Total portfolio value
	 */
	private BigDecimal totalValue;

	/**
	 * Daily change in portfolio value
	 */
	private BigDecimal dailyChange;

	/**
	 * Daily change percentage
	 */
	private BigDecimal dailyChangePercent;

	/**
	 * Map of exchange ID to exchange data
	 */
	private Map<String, ExchangeData> exchanges;

	// Default constructor
	public PortfolioData() {
	}

	// All-args constructor
	public PortfolioData(String userId, BigDecimal totalValue, BigDecimal dailyChange, BigDecimal dailyChangePercent,
			Map<String, ExchangeData> exchanges) {
		this.userId = userId;
		this.totalValue = totalValue;
		this.dailyChange = dailyChange;
		this.dailyChangePercent = dailyChangePercent;
		this.exchanges = exchanges;
	}

	// Getters
	public String getUserId() {
		return userId;
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public BigDecimal getDailyChange() {
		return dailyChange;
	}

	public BigDecimal getDailyChangePercent() {
		return dailyChangePercent;
	}

	public Map<String, ExchangeData> getExchanges() {
		return exchanges;
	}

	// Setters
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public void setDailyChange(BigDecimal dailyChange) {
		this.dailyChange = dailyChange;
	}

	public void setDailyChangePercent(BigDecimal dailyChangePercent) {
		this.dailyChangePercent = dailyChangePercent;
	}

	public void setExchanges(Map<String, ExchangeData> exchanges) {
		this.exchanges = exchanges;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PortfolioData that = (PortfolioData) o;
		return Objects.equals(userId, that.userId) && Objects.equals(totalValue, that.totalValue)
				&& Objects.equals(dailyChange, that.dailyChange)
				&& Objects.equals(dailyChangePercent, that.dailyChangePercent)
				&& Objects.equals(exchanges, that.exchanges);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, totalValue, dailyChange, dailyChangePercent, exchanges);
	}

	@Override
	public String toString() {
		return "PortfolioData{" + "userId='" + userId + '\'' + ", totalValue=" + totalValue + ", dailyChange="
				+ dailyChange + ", dailyChangePercent=" + dailyChangePercent + ", exchanges=" + exchanges + '}';
	}

	/**
	 * Exchange data
	 */
	public static class ExchangeData {

		private String id;

		private String name;

		private BigDecimal value;

		private Map<String, AssetData> assets;

		// Default constructor
		public ExchangeData() {
		}

		// All-args constructor
		public ExchangeData(String id, String name, BigDecimal value, Map<String, AssetData> assets) {
			this.id = id;
			this.name = name;
			this.value = value;
			this.assets = assets;
		}

		// Getters
		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public BigDecimal getValue() {
			return value;
		}

		public Map<String, AssetData> getAssets() {
			return assets;
		}

		// Setters
		public void setId(String id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		public void setAssets(Map<String, AssetData> assets) {
			this.assets = assets;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ExchangeData that = (ExchangeData) o;
			return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(value, that.value)
					&& Objects.equals(assets, that.assets);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name, value, assets);
		}

		@Override
		public String toString() {
			return "ExchangeData{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", value=" + value + ", assets="
					+ assets + '}';
		}

	}

	/**
	 * Asset data
	 */
	public static class AssetData {

		private String id;

		private String symbol;

		private String name;

		private BigDecimal quantity;

		private BigDecimal amount; // Added for compatibility with existing code

		private BigDecimal price;

		private BigDecimal value;

		private BigDecimal allocationPercent;

		// Additional fields for AI Portfolio Insights
		private BigDecimal costBasis;

		private BigDecimal profitLoss;

		private BigDecimal profitLossPercent;

		private String sector;

		private String assetType; // crypto, stock, etf, bond, etc.

		// Default constructor
		public AssetData() {
		}

		// All-args constructor
		public AssetData(String id, String symbol, String name, BigDecimal quantity, BigDecimal amount,
				BigDecimal price, BigDecimal value, BigDecimal allocationPercent) {
			this.id = id;
			this.symbol = symbol;
			this.name = name;
			this.quantity = quantity;
			this.amount = amount;
			this.price = price;
			this.value = value;
			this.allocationPercent = allocationPercent;
		}

		// Getters
		public String getId() {
			return id;
		}

		public String getSymbol() {
			return symbol;
		}

		public String getName() {
			return name;
		}

		public BigDecimal getQuantity() {
			return quantity;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public BigDecimal getValue() {
			return value;
		}

		public BigDecimal getAllocationPercent() {
			return allocationPercent;
		}

		// Setters
		public void setId(String id) {
			this.id = id;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setQuantity(BigDecimal quantity) {
			this.quantity = quantity;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		public void setAllocationPercent(BigDecimal allocationPercent) {
			this.allocationPercent = allocationPercent;
		}

		public BigDecimal getCostBasis() {
			return costBasis;
		}

		public void setCostBasis(BigDecimal costBasis) {
			this.costBasis = costBasis;
		}

		public BigDecimal getProfitLoss() {
			return profitLoss;
		}

		public void setProfitLoss(BigDecimal profitLoss) {
			this.profitLoss = profitLoss;
		}

		public BigDecimal getProfitLossPercent() {
			return profitLossPercent;
		}

		public void setProfitLossPercent(BigDecimal profitLossPercent) {
			this.profitLossPercent = profitLossPercent;
		}

		public String getSector() {
			return sector;
		}

		public void setSector(String sector) {
			this.sector = sector;
		}

		public String getAssetType() {
			return assetType;
		}

		public void setAssetType(String assetType) {
			this.assetType = assetType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			AssetData assetData = (AssetData) o;
			return Objects.equals(id, assetData.id) && Objects.equals(symbol, assetData.symbol)
					&& Objects.equals(name, assetData.name) && Objects.equals(quantity, assetData.quantity)
					&& Objects.equals(amount, assetData.amount) && Objects.equals(price, assetData.price)
					&& Objects.equals(value, assetData.value)
					&& Objects.equals(allocationPercent, assetData.allocationPercent);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, symbol, name, quantity, amount, price, value, allocationPercent);
		}

		@Override
		public String toString() {
			return "AssetData{" + "id='" + id + '\'' + ", symbol='" + symbol + '\'' + ", name='" + name + '\''
					+ ", quantity=" + quantity + ", amount=" + amount + ", price=" + price + ", value=" + value
					+ ", allocationPercent=" + allocationPercent + '}';
		}

	}

}
