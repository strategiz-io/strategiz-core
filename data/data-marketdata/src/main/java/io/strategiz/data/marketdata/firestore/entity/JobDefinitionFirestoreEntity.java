package io.strategiz.data.marketdata.firestore.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import java.time.Instant;

/**
 * Firestore entity for job definitions. Stores job metadata and schedules for
 * batch jobs like market data backfill and incremental updates.
 *
 * Collection: batch_jobs
 *
 * This enables database-driven job scheduling instead of hardcoded @Scheduled annotations.
 * Jobs can be enabled/disabled and schedules updated from the admin console.
 */
@Document(collectionName = "batch_jobs")
public class JobDefinitionFirestoreEntity {

	@DocumentId
	private String jobId;

	private String displayName;

	private String description;

	private String jobGroup; // MARKETDATA or FUNDAMENTALS

	private String scheduleType; // CRON or MANUAL

	private String scheduleCron; // Null if MANUAL

	private Boolean enabled;

	private String jobClass; // Fully qualified Java class name

	private Instant createdAt;

	private Instant updatedAt;

	// Default constructor for Firestore
	public JobDefinitionFirestoreEntity() {
		this.enabled = true;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	// Constructor with required fields
	public JobDefinitionFirestoreEntity(String jobId, String displayName, String scheduleType) {
		this();
		this.jobId = jobId;
		this.displayName = displayName;
		this.scheduleType = scheduleType;
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
		return String.format("JobDefinition[%s: %s, type=%s, enabled=%s]", jobId, displayName, scheduleType, enabled);
	}

}
