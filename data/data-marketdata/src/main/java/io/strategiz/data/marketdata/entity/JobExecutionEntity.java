package io.strategiz.data.marketdata.entity;

import java.time.Instant;

/**
 * Entity for job execution tracking in ClickHouse.
 * Stores execution history for batch data collection jobs.
 */
public class JobExecutionEntity {

	private String executionId;

	private String jobName;

	private String jobId;

	private String status;

	private Instant startTime;

	private Instant endTime;

	private Long durationMs;

	private Integer symbolsProcessed;

	private Long dataPointsStored;

	private Integer errorCount;

	private String errorDetails;

	private String timeframes;

	private Instant createdAt;

	// Constructors
	public JobExecutionEntity() {
	}

	// Getters and Setters
	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Long durationMs) {
		this.durationMs = durationMs;
	}

	public Integer getSymbolsProcessed() {
		return symbolsProcessed;
	}

	public void setSymbolsProcessed(Integer symbolsProcessed) {
		this.symbolsProcessed = symbolsProcessed;
	}

	public Long getDataPointsStored() {
		return dataPointsStored;
	}

	public void setDataPointsStored(Long dataPointsStored) {
		this.dataPointsStored = dataPointsStored;
	}

	public Integer getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
	}

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}

	public String getTimeframes() {
		return timeframes;
	}

	public void setTimeframes(String timeframes) {
		this.timeframes = timeframes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
