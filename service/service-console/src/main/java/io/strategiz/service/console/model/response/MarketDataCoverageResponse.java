package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response model for market data coverage snapshot.
 * Contains aggregate statistics about data completeness across symbols and timeframes.
 */
public class MarketDataCoverageResponse {

    @JsonProperty("snapshotId")
    private String snapshotId;

    @JsonProperty("calculatedAt")
    private String calculatedAt;  // ISO 8601 string

    @JsonProperty("totalSymbols")
    private Integer totalSymbols;

    @JsonProperty("totalTimeframes")
    private Integer totalTimeframes;

    @JsonProperty("byTimeframe")
    private Map<String, TimeframeCoverage> byTimeframe;

    @JsonProperty("storage")
    private StorageStats storage;

    @JsonProperty("dataQuality")
    private QualityStats dataQuality;

    @JsonProperty("gaps")
    private List<DataGap> gaps;

    // Constructor
    public MarketDataCoverageResponse() {
        this.byTimeframe = new HashMap<>();
        this.gaps = new ArrayList<>();
    }

    // Nested Classes

    /**
     * Coverage statistics for a single timeframe.
     */
    public static class TimeframeCoverage {
        @JsonProperty("symbolsWithData")
        private Integer symbolsWithData;

        @JsonProperty("coveragePercent")
        private Double coveragePercent;

        @JsonProperty("totalBars")
        private Long totalBars;

        @JsonProperty("avgBarsPerSymbol")
        private Long avgBarsPerSymbol;

        @JsonProperty("dateRangeStart")
        private String dateRangeStart;

        @JsonProperty("dateRangeEnd")
        private String dateRangeEnd;

        @JsonProperty("missingSymbols")
        private List<String> missingSymbols;

        public TimeframeCoverage() {
            this.missingSymbols = new ArrayList<>();
        }

        // Getters and Setters
        public Integer getSymbolsWithData() { return symbolsWithData; }
        public void setSymbolsWithData(Integer symbolsWithData) { this.symbolsWithData = symbolsWithData; }
        public Double getCoveragePercent() { return coveragePercent; }
        public void setCoveragePercent(Double coveragePercent) { this.coveragePercent = coveragePercent; }
        public Long getTotalBars() { return totalBars; }
        public void setTotalBars(Long totalBars) { this.totalBars = totalBars; }
        public Long getAvgBarsPerSymbol() { return avgBarsPerSymbol; }
        public void setAvgBarsPerSymbol(Long avgBarsPerSymbol) { this.avgBarsPerSymbol = avgBarsPerSymbol; }
        public String getDateRangeStart() { return dateRangeStart; }
        public void setDateRangeStart(String dateRangeStart) { this.dateRangeStart = dateRangeStart; }
        public String getDateRangeEnd() { return dateRangeEnd; }
        public void setDateRangeEnd(String dateRangeEnd) { this.dateRangeEnd = dateRangeEnd; }
        public List<String> getMissingSymbols() { return missingSymbols; }
        public void setMissingSymbols(List<String> missingSymbols) { this.missingSymbols = missingSymbols; }
    }

    /**
     * Storage and cost statistics.
     */
    public static class StorageStats {
        @JsonProperty("timescaleDbRowCount")
        private Long timescaleDbRowCount;

        @JsonProperty("timescaleDbSizeBytes")
        private Long timescaleDbSizeBytes;

        @JsonProperty("firestoreDocCount")
        private Long firestoreDocCount;

        @JsonProperty("estimatedCostPerMonth")
        private Double estimatedCostPerMonth;

        // Getters and Setters
        public Long getTimescaleDbRowCount() { return timescaleDbRowCount; }
        public void setTimescaleDbRowCount(Long timescaleDbRowCount) { this.timescaleDbRowCount = timescaleDbRowCount; }
        public Long getTimescaleDbSizeBytes() { return timescaleDbSizeBytes; }
        public void setTimescaleDbSizeBytes(Long timescaleDbSizeBytes) { this.timescaleDbSizeBytes = timescaleDbSizeBytes; }
        public Long getFirestoreDocCount() { return firestoreDocCount; }
        public void setFirestoreDocCount(Long firestoreDocCount) { this.firestoreDocCount = firestoreDocCount; }
        public Double getEstimatedCostPerMonth() { return estimatedCostPerMonth; }
        public void setEstimatedCostPerMonth(Double estimatedCostPerMonth) { this.estimatedCostPerMonth = estimatedCostPerMonth; }
    }

    /**
     * Data quality distribution.
     */
    public static class QualityStats {
        @JsonProperty("goodQuality")
        private Integer goodQuality;

        @JsonProperty("partialQuality")
        private Integer partialQuality;

        @JsonProperty("poorQuality")
        private Integer poorQuality;

        // Getters and Setters
        public Integer getGoodQuality() { return goodQuality; }
        public void setGoodQuality(Integer goodQuality) { this.goodQuality = goodQuality; }
        public Integer getPartialQuality() { return partialQuality; }
        public void setPartialQuality(Integer partialQuality) { this.partialQuality = partialQuality; }
        public Integer getPoorQuality() { return poorQuality; }
        public void setPoorQuality(Integer poorQuality) { this.poorQuality = poorQuality; }
    }

    /**
     * Represents a gap in market data.
     */
    public static class DataGap {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("timeframe")
        private String timeframe;

        @JsonProperty("gapStart")
        private String gapStart;

        @JsonProperty("gapEnd")
        private String gapEnd;

        @JsonProperty("missingBars")
        private Integer missingBars;

        // Getters and Setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getTimeframe() { return timeframe; }
        public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
        public String getGapStart() { return gapStart; }
        public void setGapStart(String gapStart) { this.gapStart = gapStart; }
        public String getGapEnd() { return gapEnd; }
        public void setGapEnd(String gapEnd) { this.gapEnd = gapEnd; }
        public Integer getMissingBars() { return missingBars; }
        public void setMissingBars(Integer missingBars) { this.missingBars = missingBars; }
    }

    // Getters and Setters
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(String calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Integer getTotalSymbols() {
        return totalSymbols;
    }

    public void setTotalSymbols(Integer totalSymbols) {
        this.totalSymbols = totalSymbols;
    }

    public Integer getTotalTimeframes() {
        return totalTimeframes;
    }

    public void setTotalTimeframes(Integer totalTimeframes) {
        this.totalTimeframes = totalTimeframes;
    }

    public Map<String, TimeframeCoverage> getByTimeframe() {
        return byTimeframe;
    }

    public void setByTimeframe(Map<String, TimeframeCoverage> byTimeframe) {
        this.byTimeframe = byTimeframe;
    }

    public StorageStats getStorage() {
        return storage;
    }

    public void setStorage(StorageStats storage) {
        this.storage = storage;
    }

    public QualityStats getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(QualityStats dataQuality) {
        this.dataQuality = dataQuality;
    }

    public List<DataGap> getGaps() {
        return gaps;
    }

    public void setGaps(List<DataGap> gaps) {
        this.gaps = gaps;
    }
}
