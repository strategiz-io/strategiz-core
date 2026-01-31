package io.strategiz.service.marketing.model.response;

import java.math.BigDecimal;

/**
 * Model for individual ticker items in the market ticker
 */
public class TickerItem {

	private String symbol;

	private String name;

	private String type; // crypto, stock, etf, etc.

	private BigDecimal price;

	private BigDecimal change;

	private BigDecimal changePercent;

	private boolean isPositive;

	public TickerItem() {
	}

	public TickerItem(String symbol, String name, String type, BigDecimal price, BigDecimal change,
			BigDecimal changePercent, boolean isPositive) {
		this.symbol = symbol;
		this.name = name;
		this.type = type;
		this.price = price;
		this.change = change;
		this.changePercent = changePercent;
		this.isPositive = isPositive;
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

	public boolean getIsPositive() {
		return isPositive;
	}

	public void setIsPositive(boolean isPositive) {
		this.isPositive = isPositive;
	}

}