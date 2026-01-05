package io.strategiz.data.marketdata.timescale.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for job definitions stored in TimescaleDB.
 * Maps to the 'jobs' table containing job metadata and schedules.
 *
 * Enables database-driven job scheduling where schedules can be modified from
 * the admin console without code changes. DynamicJobSchedulerBusiness reads
 * from this table to schedule jobs at runtime.
 *
 * Job Types:
 * - CRON: Scheduled jobs with cron expression (e.g., "0 */5 * * * MON-FRI")
 * - MANUAL: Jobs triggered manually from admin console or REST API
 *
 * Job Groups:
 * - MARKETDATA: Market data collection jobs (backfill, incremental)
 * - FUNDAMENTALS: Company fundamentals collection jobs
 */
@Entity
@Table(name = "jobs",
    indexes = {
        @Index(name = "idx_jobs_enabled", columnList = "enabled"),
        @Index(name = "idx_jobs_group", columnList = "job_group")
    }
)
public class JobDefinitionEntity {

    @Id
    @Column(name = "job_id", length = 50, nullable = false)
    private String jobId;  // e.g., "MARKETDATA_INCREMENTAL"

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;  // e.g., "Market Data Incremental"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // Detailed description for admin console

    @Column(name = "job_group", length = 50)
    private String jobGroup;  // MARKETDATA or FUNDAMENTALS

    @Column(name = "schedule_type", length = 20, nullable = false)
    private String scheduleType;  // CRON or MANUAL

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;  // e.g., "0 */5 * * * MON-FRI" (null for MANUAL jobs)

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;  // Can be toggled from admin console

    @Column(name = "job_class", length = 200)
    private String jobClass;  // Java class name (for reference/debugging)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor
    public JobDefinitionEntity() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.enabled = true;  // Jobs enabled by default
    }

    // Builder-style constructor for creating new job definitions
    public JobDefinitionEntity(String jobId, String displayName, String jobGroup,
                               String scheduleType, String scheduleCron) {
        this();
        this.jobId = jobId;
        this.displayName = displayName;
        this.jobGroup = jobGroup;
        this.scheduleType = scheduleType;
        this.scheduleCron = scheduleCron;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Helper methods

    /**
     * Check if this is a scheduled job (CRON type).
     */
    public boolean isScheduled() {
        return "CRON".equals(scheduleType) && scheduleCron != null && !scheduleCron.trim().isEmpty();
    }

    /**
     * Check if this is a manual-trigger job.
     */
    public boolean isManual() {
        return "MANUAL".equals(scheduleType);
    }

    /**
     * Check if job is currently enabled and ready to run.
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled);
    }

    // Getters and Setters

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("JobDefinition[%s: %s, type=%s, enabled=%s, cron=%s]",
            jobId, displayName, scheduleType, enabled, scheduleCron);
    }
}
