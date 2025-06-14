package io.strategiz.service.exchange.trading.agent.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents historical price data for a cryptocurrency asset
 */
public class HistoricalPriceData {
    
    private String assetSymbol;
    private LocalDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal quoteVolume;

    // Default constructor
    public HistoricalPriceData() {}

    // All-args constructor
    public HistoricalPriceData(String assetSymbol, LocalDateTime timestamp, BigDecimal open, 
                              BigDecimal high, BigDecimal low, BigDecimal close, 
                              BigDecimal volume, BigDecimal quoteVolume) {
        this.assetSymbol = assetSymbol;
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.quoteVolume = quoteVolume;
    }

    // Getters
    public String getAssetSymbol() {
        return assetSymbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    // Setters
    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public void setQuoteVolume(BigDecimal quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    /**
     * Calculate the typical price for this data point
     * @return typical price (high + low + close) / 3
     */
    public BigDecimal getTypicalPrice() {
        if (high == null || low == null || close == null) {
            return null;
        }
        return high.add(low).add(close).divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP);
    }

    /**
     * Calculate the price range for this data point
     * @return price range (high - low)
     */
    public BigDecimal getPriceRange() {
        if (high == null || low == null) {
            return null;
        }
        return high.subtract(low);
    }

    /**
     * Calculate the price change percentage from open to close
     * @return price change percentage
     */
    public BigDecimal getPriceChangePercent() {
        if (open == null || close == null || open.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return close.subtract(open).divide(open, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoricalPriceData that = (HistoricalPriceData) o;
        return Objects.equals(assetSymbol, that.assetSymbol) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(open, that.open) &&
               Objects.equals(high, that.high) &&
               Objects.equals(low, that.low) &&
               Objects.equals(close, that.close) &&
               Objects.equals(volume, that.volume) &&
               Objects.equals(quoteVolume, that.quoteVolume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetSymbol, timestamp, open, high, low, close, volume, quoteVolume);
    }

    @Override
    public String toString() {
        return "HistoricalPriceData{" +
               "assetSymbol='" + assetSymbol + '\'' +
               ", timestamp=" + timestamp +
               ", open=" + open +
               ", high=" + high +
               ", low=" + low +
               ", close=" + close +
               ", volume=" + volume +
               ", quoteVolume=" + quoteVolume +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String assetSymbol;
        private LocalDateTime timestamp;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal quoteVolume;

        public Builder assetSymbol(String assetSymbol) {
            this.assetSymbol = assetSymbol;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder open(BigDecimal open) {
            this.open = open;
            return this;
        }

        public Builder high(BigDecimal high) {
            this.high = high;
            return this;
        }

        public Builder low(BigDecimal low) {
            this.low = low;
            return this;
        }

        public Builder close(BigDecimal close) {
            this.close = close;
            return this;
        }

        public Builder volume(BigDecimal volume) {
            this.volume = volume;
            return this;
        }

        public Builder quoteVolume(BigDecimal quoteVolume) {
            this.quoteVolume = quoteVolume;
            return this;
        }

        public HistoricalPriceData build() {
            return new HistoricalPriceData(assetSymbol, timestamp, open, high, low, close, volume, quoteVolume);
        }
    }
}
