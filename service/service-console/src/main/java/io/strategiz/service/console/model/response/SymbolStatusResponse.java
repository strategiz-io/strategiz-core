package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response model for paginated symbol status list.
 * Used for the symbol data grid in the admin console.
 */
public class SymbolStatusResponse {

    @JsonProperty("symbols")
    private List<SymbolStatus> symbols;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("totalCount")
    private Long totalCount;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("summary")
    private StatusSummary summary;

    // Constructors
    public SymbolStatusResponse() {
    }

    public SymbolStatusResponse(
            List<SymbolStatus> symbols,
            Integer page,
            Integer pageSize,
            Long totalCount,
            Integer totalPages,
            StatusSummary summary) {
        this.symbols = symbols;
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.summary = summary;
    }

    // Nested Classes

    /**
     * Status for a single symbol across timeframes.
     */
    public static class SymbolStatus {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("name")
        private String name;

        @JsonProperty("sector")
        private String sector;

        @JsonProperty("timeframeStatus")
        private Map<String, TimeframeStatus> timeframeStatus;  // Key: timeframe (e.g., "1D")

        @JsonProperty("overallStatus")
        private String overallStatus;  // ACTIVE, STALE, FAILED

        public SymbolStatus() {
        }

        // Getters and Setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public Map<String, TimeframeStatus> getTimeframeStatus() { return timeframeStatus; }
        public void setTimeframeStatus(Map<String, TimeframeStatus> timeframeStatus) { this.timeframeStatus = timeframeStatus; }
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    }

    /**
     * Status for a single timeframe of a symbol.
     */
    public static class TimeframeStatus {
        @JsonProperty("status")
        private String status;  // ACTIVE, STALE, FAILED

        @JsonProperty("lastUpdate")
        private String lastUpdate;  // ISO 8601 string

        @JsonProperty("lastBarTimestamp")
        private String lastBarTimestamp;  // ISO 8601 string

        @JsonProperty("recordCount")
        private Long recordCount;

        @JsonProperty("consecutiveFailures")
        private Integer consecutiveFailures;

        @JsonProperty("lastError")
        private String lastError;

        public TimeframeStatus() {
        }

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
        public String getLastBarTimestamp() { return lastBarTimestamp; }
        public void setLastBarTimestamp(String lastBarTimestamp) { this.lastBarTimestamp = lastBarTimestamp; }
        public Long getRecordCount() { return recordCount; }
        public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }
        public Integer getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
    }

    /**
     * Summary statistics for symbol status.
     */
    public static class StatusSummary {
        @JsonProperty("activeCount")
        private Integer activeCount;

        @JsonProperty("staleCount")
        private Integer staleCount;

        @JsonProperty("failedCount")
        private Integer failedCount;

        @JsonProperty("totalCount")
        private Integer totalCount;

        public StatusSummary() {
        }

        public StatusSummary(Integer activeCount, Integer staleCount, Integer failedCount) {
            this.activeCount = activeCount;
            this.staleCount = staleCount;
            this.failedCount = failedCount;
            this.totalCount = activeCount + staleCount + failedCount;
        }

        // Getters and Setters
        public Integer getActiveCount() { return activeCount; }
        public void setActiveCount(Integer activeCount) { this.activeCount = activeCount; }
        public Integer getStaleCount() { return staleCount; }
        public void setStaleCount(Integer staleCount) { this.staleCount = staleCount; }
        public Integer getFailedCount() { return failedCount; }
        public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    }

    // Getters and Setters
    public List<SymbolStatus> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolStatus> symbols) {
        this.symbols = symbols;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public StatusSummary getSummary() {
        return summary;
    }

    public void setSummary(StatusSummary summary) {
        this.summary = summary;
    }
}
