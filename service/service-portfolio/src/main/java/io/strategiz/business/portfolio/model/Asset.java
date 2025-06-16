package io.strategiz.business.portfolio.model;

import java.math.BigDecimal;

/**
 * Business model for portfolio asset.
 */
public class Asset {
    private String id;
    private String name;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal currentValue;
    private BigDecimal totalValue;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    private String type;
    private String exchange;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getDailyChange() { return dailyChange; }
    public void setDailyChange(BigDecimal dailyChange) { this.dailyChange = dailyChange; }
    
    public BigDecimal getDailyChangePercent() { return dailyChangePercent; }
    public void setDailyChangePercent(BigDecimal dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
