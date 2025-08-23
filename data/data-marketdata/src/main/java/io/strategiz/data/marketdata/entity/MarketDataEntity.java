package io.strategiz.data.marketdata.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.spring.data.firestore.Document;
import io.strategiz.data.base.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Firestore entity for storing historical market data
 * 
 * Collection structure in Firestore:
 * - Collection: "marketdata"
 * - Document ID: "{symbol}_{date}_{timeframe}" (e.g., "AAPL_2024-01-15_1D")
 * 
 * This stores OHLCV (Open, High, Low, Close, Volume) data from Polygon.io
 */
@Document(collectionName = "marketdata")
public class MarketDataEntity extends BaseEntity {
    
    @DocumentId
    private String id;
    
    // Symbol information
    private String symbol;           // e.g., "AAPL", "BTC-USD", "EUR/USD"
    private String assetType;        // "STOCK", "CRYPTO", "FOREX", "OPTION"
    private String exchange;         // e.g., "NASDAQ", "NYSE", "CRYPTO"
    
    // Time information
    private LocalDate date;          // Trading date
    private LocalDateTime timestamp; // Exact timestamp of the bar
    private String timeframe;        // "1m", "5m", "15m", "1h", "1D", "1W", "1M"
    private Long timestampMillis;    // Unix timestamp in milliseconds
    
    // OHLCV data
    private BigDecimal open;         // Opening price
    private BigDecimal high;         // Highest price
    private BigDecimal low;          // Lowest price
    private BigDecimal close;        // Closing price
    private BigDecimal volume;       // Trading volume
    private BigDecimal vwap;         // Volume-weighted average price
    
    // Additional metrics
    private Long trades;             // Number of trades
    private BigDecimal changePercent; // Percentage change from previous close
    private BigDecimal changeAmount; // Dollar/point change from previous close
    
    // Data source information
    private String dataSource;       // "POLYGON", "YAHOO", "ALPHAVANTAGE"
    private String dataQuality;      // "REALTIME", "DELAYED", "HISTORICAL"
    
    // Metadata
    @ServerTimestamp
    private Instant collectedAt;     // When we fetched this data
    private Map<String, Object> metadata; // Additional data from provider
    
    // Constructor
    public MarketDataEntity() {
        this.metadata = new HashMap<>();
    }
    
    /**
     * Create a unique document ID for this market data point
     */
    public static String createId(String symbol, LocalDate date, String timeframe) {
        return String.format("%s_%s_%s", 
            symbol.toUpperCase().replace("/", "_"), 
            date.toString(), 
            timeframe);
    }
    
    /**
     * Calculate derived metrics
     */
    public void calculateDerivedMetrics(BigDecimal previousClose) {
        if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0 && close != null) {
            this.changeAmount = close.subtract(previousClose);
            this.changePercent = changeAmount
                .divide(previousClose, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
    
    /**
     * Check if this is a valid data point
     */
    public boolean isValid() {
        return symbol != null && 
               date != null && 
               open != null && 
               high != null && 
               low != null && 
               close != null && 
               volume != null &&
               volume.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Get typical price (HLC/3)
     */
    public BigDecimal getTypicalPrice() {
        if (high != null && low != null && close != null) {
            return high.add(low).add(close)
                .divide(BigDecimal.valueOf(3), 4, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }
    
    /**
     * Get price range (high - low)
     */
    public BigDecimal getPriceRange() {
        if (high != null && low != null) {
            return high.subtract(low);
        }
        return null;
    }
    
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
        if (this.id == null && this.date != null && this.timeframe != null) {
            this.id = createId(symbol, date, timeframe);
        }
    }
    
    public String getAssetType() {
        return assetType;
    }
    
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
        if (this.symbol != null && this.timeframe != null) {
            this.id = createId(symbol, date, timeframe);
        }
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTimeframe() {
        return timeframe;
    }
    
    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
        if (this.symbol != null && this.date != null) {
            this.id = createId(symbol, date, timeframe);
        }
    }
    
    public Long getTimestampMillis() {
        return timestampMillis;
    }
    
    public void setTimestampMillis(Long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }
    
    public BigDecimal getOpen() {
        return open;
    }
    
    public void setOpen(BigDecimal open) {
        this.open = open;
    }
    
    public BigDecimal getHigh() {
        return high;
    }
    
    public void setHigh(BigDecimal high) {
        this.high = high;
    }
    
    public BigDecimal getLow() {
        return low;
    }
    
    public void setLow(BigDecimal low) {
        this.low = low;
    }
    
    public BigDecimal getClose() {
        return close;
    }
    
    public void setClose(BigDecimal close) {
        this.close = close;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
    
    public BigDecimal getVwap() {
        return vwap;
    }
    
    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }
    
    public Long getTrades() {
        return trades;
    }
    
    public void setTrades(Long trades) {
        this.trades = trades;
    }
    
    public BigDecimal getChangePercent() {
        return changePercent;
    }
    
    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }
    
    public BigDecimal getChangeAmount() {
        return changeAmount;
    }
    
    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public String getDataQuality() {
        return dataQuality;
    }
    
    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }
    
    public Instant getCollectedAt() {
        return collectedAt;
    }
    
    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return String.format("MarketData[%s %s: O=%.2f H=%.2f L=%.2f C=%.2f V=%.0f]",
            symbol, date, open, high, low, close, volume);
    }
}