package io.strategiz.client.polygon;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response model for Polygon.io aggregates endpoint
 */
public class PolygonAggregatesResponse {
    
    @JsonProperty("ticker")
    private String ticker;
    
    @JsonProperty("queryCount")
    private Integer queryCount;
    
    @JsonProperty("resultsCount")
    private Integer resultsCount;
    
    @JsonProperty("adjusted")
    private Boolean adjusted;
    
    @JsonProperty("results")
    private List<AggregateData> results;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("count")
    private Integer count;
    
    // Getters and setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    
    public Integer getQueryCount() { return queryCount; }
    public void setQueryCount(Integer queryCount) { this.queryCount = queryCount; }
    
    public Integer getResultsCount() { return resultsCount; }
    public void setResultsCount(Integer resultsCount) { this.resultsCount = resultsCount; }
    
    public Boolean getAdjusted() { return adjusted; }
    public void setAdjusted(Boolean adjusted) { this.adjusted = adjusted; }
    
    public List<AggregateData> getResults() { return results; }
    public void setResults(List<AggregateData> results) { this.results = results; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    
    /**
     * Individual aggregate data point
     */
    public static class AggregateData {
        
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
        
        @JsonProperty("n")
        private Integer numberOfTransactions;
        
        // Getters and setters
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
        
        public Integer getNumberOfTransactions() { return numberOfTransactions; }
        public void setNumberOfTransactions(Integer numberOfTransactions) { this.numberOfTransactions = numberOfTransactions; }
        
        @Override
        public String toString() {
            return String.format("AggregateData{symbol='%s', date=%s, O=%.2f, H=%.2f, L=%.2f, C=%.2f, V=%d}",
                "N/A", timestamp, open, high, low, close, volume);
        }
    }
}