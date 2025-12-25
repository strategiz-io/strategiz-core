package io.strategiz.client.execution.model;

/**
 * Market data bar (OHLCV)
 */
public class MarketDataBar {
    private String timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    public MarketDataBar() {}

    public MarketDataBar(String timestamp, double open, double high, double low, double close, long volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public static MarketDataBarBuilder builder() {
        return new MarketDataBarBuilder();
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }
    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }
    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }
    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }
    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public static class MarketDataBarBuilder {
        private String timestamp;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;

        public MarketDataBarBuilder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        public MarketDataBarBuilder open(double open) {
            this.open = open;
            return this;
        }
        public MarketDataBarBuilder high(double high) {
            this.high = high;
            return this;
        }
        public MarketDataBarBuilder low(double low) {
            this.low = low;
            return this;
        }
        public MarketDataBarBuilder close(double close) {
            this.close = close;
            return this;
        }
        public MarketDataBarBuilder volume(long volume) {
            this.volume = volume;
            return this;
        }
        public MarketDataBar build() {
            return new MarketDataBar(timestamp, open, high, low, close, volume);
        }
    }
}
