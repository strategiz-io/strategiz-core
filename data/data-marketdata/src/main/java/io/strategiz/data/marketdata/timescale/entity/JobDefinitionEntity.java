package io.strategiz.data.marketdata.timescale.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for job definitions stored in TimescaleDB.
 * Maps to the 'jobs' table which stores job metadata and schedules.
 *
 * This enables database-driven job scheduling instead of hardcoded @Scheduled annotations.
 * Jobs can be enabled/disabled and schedules updated from the admin console.
 */
@Entity
@Table(name = "jobs",
	indexes = { @Index(name = "idx_jobs_enabled", columnList = "enabled"),
			@Index(name = "idx_jobs_group", columnList = "job_group") })
public class JobDefinitionEntity {

	@Id
	@Column(name = "job_id", length = 50, nullable = false)
	private String jobId;

	@Column(name = "display_name", length = 100, nullable = false)
	private String displayName;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "job_group", length = 50)
	private String jobGroup; // MARKETDATA or FUNDAMENTALS

	@Column(name = "schedule_type", length = 20, nullable = false)
	private String scheduleType; // CRON or MANUAL

	@Column(name = "schedule_cron", length = 100)
	private String scheduleCron; // Null if MANUAL

	@Column(name = "enabled", nullable = false)
	private Boolean enabled;

	@Column(name = "job_class", length = 200)
	private String jobClass; // Fully qualified Java class name

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// Default constructor
	public JobDefinitionEntity() {
		this.enabled = true;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	// Builder-style constructor
	public JobDefinitionEntity(String jobId, String displayName, String scheduleType) {
		this();
		this.jobId = jobId;
		this.displayName = displayName;
		this.scheduleType = scheduleType;
	}

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
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
		return String.format("JobDefinition[%s: %s, type=%s, enabled=%s]", jobId, displayName, scheduleType,
				enabled);
	}

}
