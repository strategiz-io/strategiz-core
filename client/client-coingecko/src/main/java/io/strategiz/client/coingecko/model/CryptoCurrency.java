package io.strategiz.client.coingecko.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model class for cryptocurrency data from CoinGecko API.
 */
@Data
public class CryptoCurrency {
    private String id;             // CoinGecko ID (e.g., 'bitcoin')
    private String symbol;         // Symbol (e.g., 'btc')
    private String name;           // Full name (e.g., 'Bitcoin')
    private String image;          // URL to the image
    
    @JsonProperty("current_price")
    private BigDecimal currentPrice;  // Current price in USD
    
    @JsonProperty("price_change_percentage_24h")
    private BigDecimal priceChangePercentage24h;  // 24h price change percentage
    
    @JsonProperty("market_cap")
    private BigDecimal marketCap;     // Market capitalization
    
    @JsonProperty("price_change_24h")
    private BigDecimal priceChange24h; // 24h price change amount
    
    @JsonProperty("total_volume")
    private BigDecimal totalVolume;   // 24h trading volume
    
    @JsonProperty("last_updated")
    private String lastUpdated;   // Last updated timestamp
}
