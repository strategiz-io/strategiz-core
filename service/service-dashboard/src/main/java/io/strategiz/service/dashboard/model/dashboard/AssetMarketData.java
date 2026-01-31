package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing market data for an asset
 */
public class AssetMarketData {

	private String id;

	private String symbol;

	private String name;

	private BigDecimal currentPrice;

	private BigDecimal priceChange24h;

	private BigDecimal priceChangePercent24h;

	private BigDecimal volume24h;

	private BigDecimal marketCap;

	private LocalDateTime lastUpdated;

	private String type; // "crypto" or "stock"

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

	public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice) {
		this.currentPrice = currentPrice;
	}

	public BigDecimal getPriceChange24h() {
		return priceChange24h;
	}

	public void setPriceChange24h(BigDecimal priceChange24h) {
		this.priceChange24h = priceChange24h;
	}

	public BigDecimal getPriceChangePercent24h() {
		return priceChangePercent24h;
	}

	public void setPriceChangePercent24h(BigDecimal priceChangePercent24h) {
		this.priceChangePercent24h = priceChangePercent24h;
	}

	public BigDecimal getVolume24h() {
		return volume24h;
	}

	public void setVolume24h(BigDecimal volume24h) {
		this.volume24h = volume24h;
	}

	public BigDecimal getMarketCap() {
		return marketCap;
	}

	public void setMarketCap(BigDecimal marketCap) {
		this.marketCap = marketCap;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	// Additional methods used in DashboardService
	public BigDecimal getPrice() {
		return getCurrentPrice();
	}

	public BigDecimal getChange() {
		return getPriceChange24h();
	}

	public BigDecimal getChangePercent() {
		return getPriceChangePercent24h();
	}

	public boolean isPositiveChange() {
		return getPriceChange24h() != null && getPriceChange24h().compareTo(BigDecimal.ZERO) > 0;
	}

	// Constructor for crypto assets
	public AssetMarketData(String id, String symbol, String name, BigDecimal currentPrice, BigDecimal priceChange24h,
			BigDecimal priceChangePercent24h, boolean positiveChange) {
		this.id = id;
		this.symbol = symbol;
		this.name = name;
		this.currentPrice = currentPrice;
		this.priceChange24h = priceChange24h;
		this.priceChangePercent24h = priceChangePercent24h;
		this.type = "crypto";
		this.lastUpdated = LocalDateTime.now();
	}

	// Constructor for stock assets
	public AssetMarketData(String id, String symbol, String name, BigDecimal currentPrice, BigDecimal priceChange24h,
			BigDecimal priceChangePercent24h, boolean positiveChange, BigDecimal volume24h, BigDecimal marketCap) {
		this.id = id;
		this.symbol = symbol;
		this.name = name;
		this.currentPrice = currentPrice;
		this.priceChange24h = priceChange24h;
		this.priceChangePercent24h = priceChangePercent24h;
		this.type = "stock";
		this.volume24h = volume24h;
		this.marketCap = marketCap;
		this.lastUpdated = LocalDateTime.now();
	}

}
