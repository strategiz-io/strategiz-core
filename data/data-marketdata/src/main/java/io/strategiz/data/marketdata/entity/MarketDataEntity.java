package io.strategiz.data.marketdata.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Firestore entity for storing historical market data
 *
 * Collection structure in Firestore:
 * - Collection: "marketdata"
 * - Document ID: "{symbol}_{timestamp}_{timeframe}" (e.g., "AAPL_2024-01-15T00:00:00Z_1D")
 *
 * This stores OHLCV (Open, High, Low, Close, Volume) data from various providers
 */
@Collection("marketdata")
public class MarketDataEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    private String id;

    // Symbol information
    @PropertyName("symbol")
    @JsonProperty("symbol")
    private String symbol;           // e.g., "AAPL", "BTC-USD", "EUR/USD"

    @PropertyName("assetType")
    @JsonProperty("assetType")
    private String assetType;        // "STOCK", "CRYPTO", "FOREX", "OPTION"

    @PropertyName("exchange")
    @JsonProperty("exchange")
    private String exchange;         // e.g., "NASDAQ", "NYSE", "CRYPTO"

    // Time information - stored as epoch milliseconds (UTC)
    @PropertyName("timestamp")
    @JsonProperty("timestamp")
    private Long timestamp;          // Epoch milliseconds in UTC

    @PropertyName("timeframe")
    @JsonProperty("timeframe")
    private String timeframe;        // "1Min", "5Min", "15Min", "1H", "1D", "1W", "1M"

    // OHLCV data
    @PropertyName("open")
    @JsonProperty("open")
    private BigDecimal open;         // Opening price

    @PropertyName("high")
    @JsonProperty("high")
    private BigDecimal high;         // Highest price

    @PropertyName("low")
    @JsonProperty("low")
    private BigDecimal low;          // Lowest price

    @PropertyName("close")
    @JsonProperty("close")
    private BigDecimal close;        // Closing price

    @PropertyName("volume")
    @JsonProperty("volume")
    private BigDecimal volume;       // Trading volume

    @PropertyName("vwap")
    @JsonProperty("vwap")
    private BigDecimal vwap;         // Volume-weighted average price

    // Additional metrics
    @PropertyName("trades")
    @JsonProperty("trades")
    private Long trades;             // Number of trades

    @PropertyName("changePercent")
    @JsonProperty("changePercent")
    private BigDecimal changePercent; // Percentage change from previous close

    @PropertyName("changeAmount")
    @JsonProperty("changeAmount")
    private BigDecimal changeAmount; // Dollar/point change from previous close

    // Data source information
    @PropertyName("dataSource")
    @JsonProperty("dataSource")
    private String dataSource;       // "ALPACA", "POLYGON", "YAHOO", "ALPHAVANTAGE"

    @PropertyName("dataQuality")
    @JsonProperty("dataQuality")
    private String dataQuality;      // "REALTIME", "DELAYED", "HISTORICAL"

    // Metadata
    @PropertyName("collectedAt")
    @JsonProperty("collectedAt")
    private Long collectedAt;        // Epoch milliseconds when we fetched this data

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional data from provider

    // Constructor
    public MarketDataEntity() {
        this.metadata = new HashMap<>();
    }

    /**
     * Create a unique document ID for this market data point
     */
    public static String createId(String symbol, Long timestampMillis, String timeframe) {
        LocalDateTime dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestampMillis),
            ZoneId.of("UTC")
        );
        return String.format("%s_%s_%s",
            symbol.toUpperCase().replace("/", "_").replace("-", "_"),
            dt.toString(),
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
               timestamp != null &&
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
    @Exclude
    @JsonIgnore
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
    @Exclude
    @JsonIgnore
    public BigDecimal getPriceRange() {
        if (high != null && low != null) {
            return high.subtract(low);
        }
        return null;
    }

    // === Helper methods for LocalDateTime conversion ===

    /**
     * Get timestamp as LocalDateTime in UTC
     */
    @Exclude
    @JsonIgnore
    public LocalDateTime getTimestampAsLocalDateTime() {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.of("UTC")
        );
    }

    /**
     * Set timestamp from LocalDateTime (assumes UTC)
     */
    public void setTimestampFromLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            this.timestamp = null;
            return;
        }
        Instant instant = dateTime.atZone(ZoneId.of("UTC")).toInstant();
        this.timestamp = instant.toEpochMilli();
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
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

    public Long getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Long collectedAt) {
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
            symbol, getTimestampAsLocalDateTime(), open, high, low, close, volume);
    }
}
