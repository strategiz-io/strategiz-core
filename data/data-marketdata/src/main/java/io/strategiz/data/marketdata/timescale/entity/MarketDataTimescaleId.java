package io.strategiz.data.marketdata.timescale.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Composite primary key for MarketDataTimescaleEntity.
 * Composite key: (symbol, timestamp, timeframe)
 */
public class MarketDataTimescaleId implements Serializable {

    private String symbol;
    private Instant timestamp;
    private String timeframe;

    public MarketDataTimescaleId() {
    }

    public MarketDataTimescaleId(String symbol, Instant timestamp, String timeframe) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.timeframe = timeframe;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketDataTimescaleId that = (MarketDataTimescaleId) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(timeframe, that.timeframe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timestamp, timeframe);
    }

    @Override
    public String toString() {
        return String.format("MarketDataId[%s_%s_%s]", symbol, timestamp, timeframe);
    }
}
