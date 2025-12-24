package io.strategiz.data.marketdata.timescale.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for storing market data in TimescaleDB.
 * Maps to the 'market_data' hypertable.
 */
@Entity
@Table(name = "market_data",
    indexes = {
        @Index(name = "idx_market_data_symbol_time", columnList = "symbol, timestamp DESC"),
        @Index(name = "idx_market_data_symbol_timeframe", columnList = "symbol, timeframe, timestamp DESC")
    }
)
@IdClass(MarketDataTimescaleId.class)
public class MarketDataTimescaleEntity {

    @Id
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Id
    @Column(name = "timeframe", nullable = false, length = 10)
    private String timeframe;

    @Column(name = "asset_type", length = 20)
    private String assetType;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "open_price", precision = 18, scale = 8)
    private BigDecimal open;

    @Column(name = "high_price", precision = 18, scale = 8)
    private BigDecimal high;

    @Column(name = "low_price", precision = 18, scale = 8)
    private BigDecimal low;

    @Column(name = "close_price", precision = 18, scale = 8)
    private BigDecimal close;

    @Column(name = "volume", precision = 24, scale = 8)
    private BigDecimal volume;

    @Column(name = "vwap", precision = 18, scale = 8)
    private BigDecimal vwap;

    @Column(name = "trades")
    private Long trades;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "change_amount", precision = 18, scale = 8)
    private BigDecimal changeAmount;

    @Column(name = "data_source", length = 20)
    private String dataSource;

    @Column(name = "data_quality", length = 20)
    private String dataQuality;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor
    public MarketDataTimescaleEntity() {
        this.createdAt = Instant.now();
    }

    // Builder-style constructor
    public MarketDataTimescaleEntity(String symbol, Instant timestamp, String timeframe) {
        this();
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.timeframe = timeframe;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("MarketData[%s %s %s: O=%.4f H=%.4f L=%.4f C=%.4f V=%.0f]",
            symbol, timeframe, timestamp, open, high, low, close, volume);
    }
}
