package io.strategiz.client.polygon;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response model for Polygon.io grouped daily endpoint
 */
public class PolygonGroupedDailyResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("count")
    private Integer count;
    
    @JsonProperty("results")
    private List<GroupedDailyData> results;
    
    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    
    public List<GroupedDailyData> getResults() { return results; }
    public void setResults(List<GroupedDailyData> results) { this.results = results; }
    
    /**
     * Grouped daily data for a single ticker
     */
    public static class GroupedDailyData {
        
        @JsonProperty("T")
        private String ticker;
        
        @JsonProperty("c")
        private BigDecimal close;
        
        @JsonProperty("h")
        private BigDecimal high;
        
        @JsonProperty("l")
        private BigDecimal low;
        
        @JsonProperty("o")
        private BigDecimal open;
        
        @JsonProperty("v")
        private Long volume;
        
        @JsonProperty("vw")
        private BigDecimal volumeWeightedAveragePrice;
        
        @JsonProperty("t")
        private Long timestamp;
        
        // Getters and setters
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }
        
        public BigDecimal getClose() { return close; }
        public void setClose(BigDecimal close) { this.close = close; }
        
        public BigDecimal getHigh() { return high; }
        public void setHigh(BigDecimal high) { this.high = high; }
        
        public BigDecimal getLow() { return low; }
        public void setLow(BigDecimal low) { this.low = low; }
        
        public BigDecimal getOpen() { return open; }
        public void setOpen(BigDecimal open) { this.open = open; }
        
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        
        public BigDecimal getVolumeWeightedAveragePrice() { return volumeWeightedAveragePrice; }
        public void setVolumeWeightedAveragePrice(BigDecimal vwap) { this.volumeWeightedAveragePrice = vwap; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return String.format("GroupedDailyData{ticker='%s', O=%.2f, H=%.2f, L=%.2f, C=%.2f, V=%d}",
                ticker, open, high, low, close, volume);
        }
    }
}