package io.strategiz.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * DTO for market data bars returned to the frontend
 * Represents a single OHLCV data point for charting
 */
public class MarketDataBarDTO {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("timestamp")
    private String timestamp;  // ISO 8601 format: "2024-09-01T00:00:00Z"

    @JsonProperty("open")
    private BigDecimal open;

    @JsonProperty("high")
    private BigDecimal high;

    @JsonProperty("low")
    private BigDecimal low;

    @JsonProperty("close")
    private BigDecimal close;

    @JsonProperty("volume")
    private BigDecimal volume;

    @JsonProperty("timeframe")
    private String timeframe;

    // Default constructor
    public MarketDataBarDTO() {
    }

    // Full constructor
    public MarketDataBarDTO(String symbol, String timestamp, BigDecimal open, BigDecimal high,
                            BigDecimal low, BigDecimal close, BigDecimal volume, String timeframe) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timeframe = timeframe;
    }

    /**
     * Convert MarketDataEntity to DTO
     */
    public static MarketDataBarDTO fromEntity(MarketDataEntity entity) {
        if (entity == null) {
            return null;
        }

        MarketDataBarDTO dto = new MarketDataBarDTO();
        dto.setSymbol(entity.getSymbol());
        dto.setOpen(entity.getOpen());
        dto.setHigh(entity.getHigh());
        dto.setLow(entity.getLow());
        dto.setClose(entity.getClose());
        dto.setVolume(entity.getVolume());
        dto.setTimeframe(entity.getTimeframe());

        // Convert timestamp (epoch milliseconds) to ISO 8601 string
        if (entity.getTimestamp() != null) {
            Instant instant = Instant.ofEpochMilli(entity.getTimestamp());
            String isoTimestamp = instant.atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);
            dto.setTimestamp(isoTimestamp);
        }

        return dto;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    @Override
    public String toString() {
        return String.format("MarketDataBarDTO[symbol=%s, timestamp=%s, close=%.2f, volume=%.0f]",
            symbol, timestamp, close, volume);
    }
}
