package io.strategiz.client.yahoofinance.model;

import java.math.BigDecimal;

/**
 * Data model for Yahoo Finance stock information
 */
public class YahooStockData {
    
    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal volume;
    private String marketState;
    
    // Constructors
    public YahooStockData() {}
    
    public YahooStockData(String symbol, String name, BigDecimal currentPrice, 
                         BigDecimal change, BigDecimal changePercent) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.change = change;
        this.changePercent = changePercent;
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
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
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
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    public String getMarketState() {
        return marketState;
    }
    
    public void setMarketState(String marketState) {
        this.marketState = marketState;
    }
    
    @Override
    public String toString() {
        return "YahooStockData{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", change=" + change +
                ", changePercent=" + changePercent +
                ", volume=" + volume +
                ", marketState='" + marketState + '\'' +
                '}';
    }
}