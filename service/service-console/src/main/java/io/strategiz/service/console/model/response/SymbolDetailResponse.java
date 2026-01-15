package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response model for detailed symbol information. Shows comprehensive status across all
 * timeframes for a single symbol.
 */
public class SymbolDetailResponse {

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("name")
	private String name;

	@JsonProperty("sector")
	private String sector;

	@JsonProperty("assetType")
	private String assetType; // STOCK, CRYPTO, etc.

	@JsonProperty("overallStatus")
	private String overallStatus; // ACTIVE, STALE, FAILED

	@JsonProperty("timeframes")
	private List<TimeframeDetail> timeframes;

	@JsonProperty("recentErrors")
	private List<ErrorRecord> recentErrors;

	// Constructor
	public SymbolDetailResponse() {
	}

	// Nested Classes

	/**
	 * Detailed status for a single timeframe.
	 */
	public static class TimeframeDetail {

		@JsonProperty("timeframe")
		private String timeframe;

		@JsonProperty("status")
		private String status; // ACTIVE, STALE, FAILED

		@JsonProperty("lastUpdate")
		private String lastUpdate; // ISO 8601 string

		@JsonProperty("lastBarTimestamp")
		private String lastBarTimestamp; // ISO 8601 string

		@JsonProperty("recordCount")
		private Long recordCount;

		@JsonProperty("dateRangeStart")
		private String dateRangeStart;

		@JsonProperty("dateRangeEnd")
		private String dateRangeEnd;

		@JsonProperty("consecutiveFailures")
		private Integer consecutiveFailures;

		@JsonProperty("lastError")
		private String lastError;

		@JsonProperty("dataQuality")
		private String dataQuality; // GOOD, PARTIAL, POOR

		public TimeframeDetail() {
		}

		// Getters and Setters
		public String getTimeframe() {
			return timeframe;
		}

		public void setTimeframe(String timeframe) {
			this.timeframe = timeframe;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getLastUpdate() {
			return lastUpdate;
		}

		public void setLastUpdate(String lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		public String getLastBarTimestamp() {
			return lastBarTimestamp;
		}

		public void setLastBarTimestamp(String lastBarTimestamp) {
			this.lastBarTimestamp = lastBarTimestamp;
		}

		public Long getRecordCount() {
			return recordCount;
		}

		public void setRecordCount(Long recordCount) {
			this.recordCount = recordCount;
		}

		public String getDateRangeStart() {
			return dateRangeStart;
		}

		public void setDateRangeStart(String dateRangeStart) {
			this.dateRangeStart = dateRangeStart;
		}

		public String getDateRangeEnd() {
			return dateRangeEnd;
		}

		public void setDateRangeEnd(String dateRangeEnd) {
			this.dateRangeEnd = dateRangeEnd;
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

		public String getDataQuality() {
			return dataQuality;
		}

		public void setDataQuality(String dataQuality) {
			this.dataQuality = dataQuality;
		}

	}

	/**
	 * Recent error record for the symbol.
	 */
	public static class ErrorRecord {

		@JsonProperty("timestamp")
		private String timestamp; // ISO 8601 string

		@JsonProperty("timeframe")
		private String timeframe;

		@JsonProperty("errorMessage")
		private String errorMessage;

		@JsonProperty("errorType")
		private String errorType; // RATE_LIMIT, API_ERROR, etc.

		public ErrorRecord() {
		}

		// Getters and Setters
		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

		public String getTimeframe() {
			return timeframe;
		}

		public void setTimeframe(String timeframe) {
			this.timeframe = timeframe;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public String getErrorType() {
			return errorType;
		}

		public void setErrorType(String errorType) {
			this.errorType = errorType;
		}

	}

	// Getters and Setters
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public String getOverallStatus() {
		return overallStatus;
	}

	public void setOverallStatus(String overallStatus) {
		this.overallStatus = overallStatus;
	}

	public List<TimeframeDetail> getTimeframes() {
		return timeframes;
	}

	public void setTimeframes(List<TimeframeDetail> timeframes) {
		this.timeframes = timeframes;
	}

	public List<ErrorRecord> getRecentErrors() {
		return recentErrors;
	}

	public void setRecentErrors(List<ErrorRecord> recentErrors) {
		this.recentErrors = recentErrors;
	}

}
