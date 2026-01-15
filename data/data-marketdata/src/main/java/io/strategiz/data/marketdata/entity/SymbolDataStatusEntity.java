package io.strategiz.data.marketdata.entity;

import java.time.Instant;

/**
 * Entity for symbol data status tracking in ClickHouse. Tracks the last update time and
 * data coverage for each symbol/timeframe combination.
 */
public class SymbolDataStatusEntity {

	private String symbol;

	private String timeframe;

	private Instant lastUpdate;

	private Instant lastBarTimestamp;

	private Long recordCount;

	private Integer consecutiveFailures;

	private String lastError;

	private String status;

	private Instant updatedAt;

	// Constructors
	public SymbolDataStatusEntity() {
	}

	public SymbolDataStatusEntity(String symbol, String timeframe, Instant lastUpdate) {
		this.symbol = symbol;
		this.timeframe = timeframe;
		this.lastUpdate = lastUpdate;
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

}
