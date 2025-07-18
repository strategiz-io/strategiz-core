package io.strategiz.service.marketing.model.response;

import java.util.List;

/**
 * Response model for market ticker data
 */
public class MarketTickerResponse {
    
    private List<TickerItem> items;
    private long timestamp;
    private int cacheDurationSeconds;
    
    public MarketTickerResponse() {}
    
    public MarketTickerResponse(List<TickerItem> items, long timestamp, int cacheDurationSeconds) {
        this.items = items;
        this.timestamp = timestamp;
        this.cacheDurationSeconds = cacheDurationSeconds;
    }
    
    // Getters and setters
    public List<TickerItem> getItems() {
        return items;
    }
    
    public void setItems(List<TickerItem> items) {
        this.items = items;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getCacheDurationSeconds() {
        return cacheDurationSeconds;
    }
    
    public void setCacheDurationSeconds(int cacheDurationSeconds) {
        this.cacheDurationSeconds = cacheDurationSeconds;
    }
}