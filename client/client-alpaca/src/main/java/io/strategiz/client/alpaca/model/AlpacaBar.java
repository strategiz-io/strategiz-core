package io.strategiz.client.alpaca.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO representing a single bar (candle) from Alpaca's Bars API
 *
 * Alpaca API Response Format:
 * {
 *   "t": "2024-11-23T14:30:00Z",  // timestamp
 *   "o": 185.50,                   // open
 *   "h": 186.25,                   // high
 *   "l": 185.40,                   // low
 *   "c": 186.00,                   // close
 *   "v": 1250000,                  // volume
 *   "n": 1250,                     // number of trades
 *   "vw": 185.85                   // VWAP
 * }
 */
public class AlpacaBar {

    /**
     * Timestamp in RFC-3339 format (e.g., "2024-11-23T14:30:00Z")
     */
    @JsonProperty("t")
    private String timestamp;

    /**
     * Opening price
     */
    @JsonProperty("o")
    private BigDecimal open;

    /**
     * Highest price in the bar
     */
    @JsonProperty("h")
    private BigDecimal high;

    /**
     * Lowest price in the bar
     */
    @JsonProperty("l")
    private BigDecimal low;

    /**
     * Closing price
     */
    @JsonProperty("c")
    private BigDecimal close;

    /**
     * Total volume
     */
    @JsonProperty("v")
    private Long volume;

    /**
     * Number of trades in this bar
     */
    @JsonProperty("n")
    private Long trades;

    /**
     * Volume-weighted average price
     */
    @JsonProperty("vw")
    private BigDecimal vwap;

    // Constructors
    public AlpacaBar() {
    }

    public AlpacaBar(String timestamp, BigDecimal open, BigDecimal high, BigDecimal low,
                     BigDecimal close, Long volume, Long trades, BigDecimal vwap) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.trades = trades;
        this.vwap = vwap;
    }

    // Getters and Setters
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

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getTrades() {
        return trades;
    }

    public void setTrades(Long trades) {
        this.trades = trades;
    }

    public BigDecimal getVwap() {
        return vwap;
    }

    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }

    @Override
    public String toString() {
        return String.format("AlpacaBar[t=%s, o=%.2f, h=%.2f, l=%.2f, c=%.2f, v=%d]",
            timestamp, open, high, low, close, volume);
    }
}
