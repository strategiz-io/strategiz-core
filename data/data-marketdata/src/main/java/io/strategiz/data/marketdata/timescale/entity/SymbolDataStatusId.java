package io.strategiz.data.marketdata.timescale.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Composite primary key for SymbolDataStatusEntity.
 * Composite key: (symbol, timeframe, lastUpdate)
 *
 * This allows tracking symbol freshness history over time while ensuring
 * the materialized view can efficiently query the latest status per symbol/timeframe.
 */
public class SymbolDataStatusId implements Serializable {

    private String symbol;
    private String timeframe;
    private Instant lastUpdate;

    public SymbolDataStatusId() {
    }

    public SymbolDataStatusId(String symbol, String timeframe, Instant lastUpdate) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.lastUpdate = lastUpdate;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolDataStatusId that = (SymbolDataStatusId) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(timeframe, that.timeframe) &&
               Objects.equals(lastUpdate, that.lastUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timeframe, lastUpdate);
    }

    @Override
    public String toString() {
        return String.format("SymbolDataStatusId[%s_%s_%s]", symbol, timeframe, lastUpdate);
    }
}
