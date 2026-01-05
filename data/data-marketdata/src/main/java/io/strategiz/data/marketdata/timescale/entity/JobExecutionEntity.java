package io.strategiz.data.marketdata.timescale.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for tracking job execution history in TimescaleDB.
 * Maps to the 'job_executions' hypertable partitioned by start_time.
 *
 * Tracks execution metrics for batch jobs like market data backfill and incremental collection.
 */
@Entity
@Table(name = "job_executions",
    indexes = {
        @Index(name = "idx_job_executions_job_time", columnList = "job_name, start_time DESC"),
        @Index(name = "idx_job_executions_status", columnList = "status, start_time DESC")
    }
)
public class JobExecutionEntity {

    @Id
    @Column(name = "execution_id", length = 50, nullable = false)
    private String executionId;

    @Column(name = "job_name", length = 100, nullable = false)
    private String jobName;

    @Column(name = "job_id", length = 50)
    private String jobId;  // Foreign key to jobs table (nullable for backward compatibility)

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 20, nullable = false)
    private String status;  // SUCCESS, FAILED, RUNNING

    @Column(name = "symbols_processed")
    private Integer symbolsProcessed;

    @Column(name = "data_points_stored")
    private Long dataPointsStored;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;  // JSON array of error messages

    @Column(name = "timeframes", columnDefinition = "TEXT")
    private String timeframes;  // JSON array of timeframes processed

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor
    public JobExecutionEntity() {
        this.createdAt = Instant.now();
        this.symbolsProcessed = 0;
        this.dataPointsStored = 0L;
        this.errorCount = 0;
    }

    // Builder-style constructor
    public JobExecutionEntity(String executionId, String jobName, Instant startTime, String status) {
        this();
        this.executionId = executionId;
        this.jobName = jobName;
        this.startTime = startTime;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("JobExecution[%s %s: %s, symbols=%d, points=%d, errors=%d]",
            jobName, executionId, status, symbolsProcessed, dataPointsStored, errorCount);
    }
}
