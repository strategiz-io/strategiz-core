package io.strategiz.service.portfolio.model.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a single position/asset in a portfolio. Used across all providers for
 * consistent UI display.
 */
public class PortfolioPositionResponse {

	private String symbol; // BTC, AAPL, EUR

	private String name; // Bitcoin, Apple Inc., Euro

	private String assetType; // crypto, stock, forex

	private BigDecimal quantity; // Amount owned (Balance in Kraken)

	private BigDecimal averageBuyPrice; // Average purchase price

	private BigDecimal currentPrice; // Current market price

	private BigDecimal currentValue; // quantity * currentPrice (Value in Kraken)

	private BigDecimal costBasis; // Total amount paid

	private BigDecimal profitLoss; // currentValue - costBasis (Unrealized Return in
									// Kraken)

	private BigDecimal profitLossPercent; // (profitLoss / costBasis) * 100

	private BigDecimal priceChange24h; // 24 hour price change percentage

	private String provider; // Which provider this came from

	private Map<String, Object> metadata; // Additional metadata (e.g., staking info, APR)

	// Constructors
	public PortfolioPositionResponse() {
	}

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

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice) {
		this.currentPrice = currentPrice;
	}

	public BigDecimal getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(BigDecimal currentValue) {
		this.currentValue = currentValue;
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

	public BigDecimal getAverageBuyPrice() {
		return averageBuyPrice;
	}

	public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
		this.averageBuyPrice = averageBuyPrice;
	}

	public BigDecimal getPriceChange24h() {
		return priceChange24h;
	}

	public void setPriceChange24h(BigDecimal priceChange24h) {
		this.priceChange24h = priceChange24h;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}