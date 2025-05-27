package io.strategiz.client.alphavantage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model class for stock market data from AlphaVantage API.
 */
@Data
public class StockData {
    private String symbol;          // Stock symbol (e.g., 'MSFT')
    private String name;            // Company name (e.g., 'Microsoft Corp')
    private String exchange;        // Exchange (e.g., 'NASDAQ')
    
    @JsonProperty("price")
    private BigDecimal price;       // Current stock price
    
    @JsonProperty("change")
    private BigDecimal change;      // Absolute price change
    
    @JsonProperty("change_percent")
    private BigDecimal changePercent; // Price change percentage
    
    @JsonProperty("market_cap")
    private BigDecimal marketCap;   // Market capitalization
    
    @JsonProperty("volume")
    private BigDecimal volume;      // Trading volume
    
    @JsonProperty("pe_ratio")
    private BigDecimal peRatio;     // Price to earnings ratio
    
    @JsonProperty("dividend_yield")
    private BigDecimal dividendYield; // Dividend yield percentage
    
    @JsonProperty("last_updated")
    private String lastUpdated;     // Last updated timestamp
}
