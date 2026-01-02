package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for test application summary
 */
@Schema(description = "Test application summary")
public class TestAppResponse {

	@Schema(description = "App ID", example = "frontend-web")
	private String id;

	@Schema(description = "App name", example = "Web Application")
	private String name;

	@Schema(description = "App description", example = "Main web frontend with Playwright E2E tests")
	private String description;

	@Schema(description = "Test framework", example = "PLAYWRIGHT")
	private String framework;

	@Schema(description = "Total number of test modules")
	private int moduleCount;

	@Schema(description = "Total number of test suites across all modules")
	private int suiteCount;

	@Schema(description = "Total number of individual tests")
	private int totalTestCount;

	@Schema(description = "Estimated total duration in seconds")
	private long estimatedDurationSeconds;

	@Schema(description = "Last execution timestamp")
	private Instant lastExecutedAt;

	@Schema(description = "Last execution status", example = "PASSED")
	private String lastExecutionStatus;

	@Schema(description = "Created timestamp")
	private Instant createdAt;

	@Schema(description = "Updated timestamp")
	private Instant updatedAt;

	// Constructors
	public TestAppResponse() {
	}

	public TestAppResponse(String id, String name, String description, String framework, int moduleCount,
			int suiteCount, int totalTestCount, long estimatedDurationSeconds, Instant lastExecutedAt,
			String lastExecutionStatus, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.framework = framework;
		this.moduleCount = moduleCount;
		this.suiteCount = suiteCount;
		this.totalTestCount = totalTestCount;
		this.estimatedDurationSeconds = estimatedDurationSeconds;
		this.lastExecutedAt = lastExecutedAt;
		this.lastExecutionStatus = lastExecutionStatus;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public int getModuleCount() {
		return moduleCount;
	}

	public void setModuleCount(int moduleCount) {
		this.moduleCount = moduleCount;
	}

	public int getSuiteCount() {
		return suiteCount;
	}

	public void setSuiteCount(int suiteCount) {
		this.suiteCount = suiteCount;
	}

	public int getTotalTestCount() {
		return totalTestCount;
	}

	public void setTotalTestCount(int totalTestCount) {
		this.totalTestCount = totalTestCount;
	}

	public long getEstimatedDurationSeconds() {
		return estimatedDurationSeconds;
	}

	public void setEstimatedDurationSeconds(long estimatedDurationSeconds) {
		this.estimatedDurationSeconds = estimatedDurationSeconds;
	}

	public Instant getLastExecutedAt() {
		return lastExecutedAt;
	}

	public void setLastExecutedAt(Instant lastExecutedAt) {
		this.lastExecutedAt = lastExecutedAt;
	}

	public String getLastExecutionStatus() {
		return lastExecutionStatus;
	}

	public void setLastExecutionStatus(String lastExecutionStatus) {
		this.lastExecutionStatus = lastExecutionStatus;
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

}
