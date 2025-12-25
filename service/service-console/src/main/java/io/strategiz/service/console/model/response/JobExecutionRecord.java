package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response model for a single job execution record.
 */
public class JobExecutionRecord {

    @JsonProperty("executionId")
    private String executionId;

    @JsonProperty("jobName")
    private String jobName;

    @JsonProperty("startTime")
    private Instant startTime;

    @JsonProperty("endTime")
    private Instant endTime;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonProperty("status")
    private String status; // SUCCESS, FAILED, RUNNING

    @JsonProperty("symbolsProcessed")
    private Integer symbolsProcessed;

    @JsonProperty("dataPointsStored")
    private Long dataPointsStored;

    @JsonProperty("errorCount")
    private Integer errorCount;

    @JsonProperty("errorDetails")
    private String errorDetails; // JSON array

    @JsonProperty("timeframes")
    private String timeframes; // JSON array

    // Constructors
    public JobExecutionRecord() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
