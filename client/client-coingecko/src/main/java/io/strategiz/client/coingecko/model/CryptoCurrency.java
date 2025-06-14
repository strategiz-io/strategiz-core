package io.strategiz.client.coingecko.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Model class for cryptocurrency data from CoinGecko API.
 */
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
    
    // Constructors
    public CryptoCurrency() {}
    
    public CryptoCurrency(String id, String symbol, String name) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public BigDecimal getPriceChangePercentage24h() { return priceChangePercentage24h; }
    public void setPriceChangePercentage24h(BigDecimal priceChangePercentage24h) { this.priceChangePercentage24h = priceChangePercentage24h; }
    
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    
    public BigDecimal getPriceChange24h() { return priceChange24h; }
    public void setPriceChange24h(BigDecimal priceChange24h) { this.priceChange24h = priceChange24h; }
    
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CryptoCurrency that = (CryptoCurrency) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(image, that.image) &&
               Objects.equals(currentPrice, that.currentPrice) &&
               Objects.equals(priceChangePercentage24h, that.priceChangePercentage24h) &&
               Objects.equals(marketCap, that.marketCap) &&
               Objects.equals(priceChange24h, that.priceChange24h) &&
               Objects.equals(totalVolume, that.totalVolume) &&
               Objects.equals(lastUpdated, that.lastUpdated);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, image, currentPrice, priceChangePercentage24h, 
                           marketCap, priceChange24h, totalVolume, lastUpdated);
    }
    
    @Override
    public String toString() {
        return "CryptoCurrency{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", image='" + image + '\'' +
               ", currentPrice=" + currentPrice +
               ", priceChangePercentage24h=" + priceChangePercentage24h +
               ", marketCap=" + marketCap +
               ", priceChange24h=" + priceChange24h +
               ", totalVolume=" + totalVolume +
               ", lastUpdated='" + lastUpdated + '\'' +
               '}';
    }
}
