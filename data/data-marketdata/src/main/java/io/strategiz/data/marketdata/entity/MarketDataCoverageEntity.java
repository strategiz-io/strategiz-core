package io.strategiz.data.marketdata.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.entity.BaseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore entity for storing market data coverage snapshots.
 *
 * Collection structure in Firestore:
 * - Collection: "marketdata_coverage_stats"
 * - Document ID: Snapshot timestamp (e.g., "coverage_2025-12-24T10:00:00Z")
 *
 * Stores aggregate statistics about data completeness:
 * - Coverage percentage by timeframe (how many symbols have data)
 * - Storage metrics (TimescaleDB size, row counts)
 * - Data quality distribution
 * - Identified gaps in data
 *
 * Calculated periodically (daily at 2 AM) or on-demand via API.
 */
@Collection("marketdata_coverage_stats")
public class MarketDataCoverageEntity extends BaseEntity {

    @DocumentId
    @PropertyName("snapshotId")
    @JsonProperty("snapshotId")
    private String snapshotId;  // e.g., "coverage_2025-12-24T10:00:00Z"

    @PropertyName("calculatedAt")
    @JsonProperty("calculatedAt")
    private Timestamp calculatedAt;

    @PropertyName("totalSymbols")
    @JsonProperty("totalSymbols")
    private Integer totalSymbols;

    @PropertyName("totalTimeframes")
    @JsonProperty("totalTimeframes")
    private Integer totalTimeframes;

    @PropertyName("byTimeframe")
    @JsonProperty("byTimeframe")
    private Map<String, TimeframeCoverage> byTimeframe;

    @PropertyName("storage")
    @JsonProperty("storage")
    private StorageStats storage;

    @PropertyName("dataQuality")
    @JsonProperty("dataQuality")
    private QualityStats dataQuality;

    @PropertyName("gaps")
    @JsonProperty("gaps")
    private List<DataGap> gaps;

    // Constructor
    public MarketDataCoverageEntity() {
        this.byTimeframe = new HashMap<>();
        this.gaps = new ArrayList<>();
        this.calculatedAt = Timestamp.now();
    }

    // Nested Classes

    /**
     * Coverage statistics for a single timeframe.
     */
    public static class TimeframeCoverage {
        @PropertyName("symbolsWithData")
        @JsonProperty("symbolsWithData")
        private Integer symbolsWithData;

        @PropertyName("coveragePercent")
        @JsonProperty("coveragePercent")
        private Double coveragePercent;

        @PropertyName("totalBars")
        @JsonProperty("totalBars")
        private Long totalBars;

        @PropertyName("avgBarsPerSymbol")
        @JsonProperty("avgBarsPerSymbol")
        private Long avgBarsPerSymbol;

        @PropertyName("dateRangeStart")
        @JsonProperty("dateRangeStart")
        private String dateRangeStart;  // ISO date string

        @PropertyName("dateRangeEnd")
        @JsonProperty("dateRangeEnd")
        private String dateRangeEnd;  // ISO date string

        @PropertyName("missingSymbols")
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
     * Storage and cost statistics from TimescaleDB.
     */
    public static class StorageStats {
        @PropertyName("timescaleDbRowCount")
        @JsonProperty("timescaleDbRowCount")
        private Long timescaleDbRowCount;

        @PropertyName("timescaleDbSizeBytes")
        @JsonProperty("timescaleDbSizeBytes")
        private Long timescaleDbSizeBytes;

        @PropertyName("firestoreDocCount")
        @JsonProperty("firestoreDocCount")
        private Long firestoreDocCount;  // Fallback storage if TimescaleDB disabled

        @PropertyName("estimatedCostPerMonth")
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
        @PropertyName("goodQuality")
        @JsonProperty("goodQuality")
        private Integer goodQuality;  // Symbols with complete, recent data

        @PropertyName("partialQuality")
        @JsonProperty("partialQuality")
        private Integer partialQuality;  // Symbols with gaps or stale data

        @PropertyName("poorQuality")
        @JsonProperty("poorQuality")
        private Integer poorQuality;  // Symbols with major issues

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
        @PropertyName("symbol")
        @JsonProperty("symbol")
        private String symbol;

        @PropertyName("timeframe")
        @JsonProperty("timeframe")
        private String timeframe;

        @PropertyName("gapStart")
        @JsonProperty("gapStart")
        private String gapStart;  // ISO date string

        @PropertyName("gapEnd")
        @JsonProperty("gapEnd")
        private String gapEnd;  // ISO date string

        @PropertyName("missingBars")
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

    // BaseEntity required methods
    @Override
    public String getId() {
        return snapshotId;
    }

    @Override
    public void setId(String id) {
        this.snapshotId = id;
    }

    // Getters and Setters
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Timestamp getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Timestamp calculatedAt) {
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

    @Override
    public String toString() {
        return String.format("MarketDataCoverage[%s: %d symbols, %d timeframes]",
            snapshotId, totalSymbols, totalTimeframes);
    }
}
