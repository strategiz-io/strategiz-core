package io.strategiz.data.marketdata.timescale.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for tracking individual symbol data freshness in TimescaleDB.
 * Maps to the 'symbol_data_status' hypertable partitioned by last_update.
 *
 * Tracks when each symbol/timeframe was last updated, enabling:
 * - Stale symbol detection (symbols needing refresh)
 * - Data quality monitoring (consecutive failures)
 * - Historical freshness trends
 *
 * Queried via 'symbol_latest_status' materialized view for current status.
 */
@Entity
@Table(name = "symbol_data_status",
    indexes = {
        @Index(name = "idx_symbol_status_symbol", columnList = "symbol, timeframe"),
        @Index(name = "idx_symbol_status_stale", columnList = "status, last_update DESC"),
        @Index(name = "idx_symbol_status_timeframe", columnList = "timeframe, last_update DESC")
    }
)
@IdClass(SymbolDataStatusId.class)
public class SymbolDataStatusEntity {

    @Id
    @Column(name = "symbol", length = 20, nullable = false)
    private String symbol;

    @Id
    @Column(name = "timeframe", length = 10, nullable = false)
    private String timeframe;

    @Id
    @Column(name = "last_update", nullable = false)
    private Instant lastUpdate;

    @Column(name = "last_bar_timestamp")
    private Instant lastBarTimestamp;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "status", length = 20)
    private String status;  // ACTIVE, STALE, FAILED

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor
    public SymbolDataStatusEntity() {
        this.updatedAt = Instant.now();
        this.recordCount = 0L;
        this.consecutiveFailures = 0;
        this.status = "ACTIVE";
    }

    // Builder-style constructor
    public SymbolDataStatusEntity(String symbol, String timeframe, Instant lastUpdate) {
        this();
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.lastUpdate = lastUpdate;
    }

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    // Getters and Setters
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

    public Instant getLastBarTimestamp() {
        return lastBarTimestamp;
    }

    public void setLastBarTimestamp(Instant lastBarTimestamp) {
        this.lastBarTimestamp = lastBarTimestamp;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("SymbolDataStatus[%s %s: status=%s, records=%d, failures=%d, lastUpdate=%s]",
            symbol, timeframe, status, recordCount, consecutiveFailures, lastUpdate);
    }
}
