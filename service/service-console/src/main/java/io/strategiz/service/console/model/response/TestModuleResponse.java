package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for test module summary
 */
@Schema(description = "Test module summary")
public class TestModuleResponse {

	@Schema(description = "Module ID", example = "e2e-tests")
	private String id;

	@Schema(description = "Parent app ID", example = "frontend-web")
	private String appId;

	@Schema(description = "Module name", example = "E2E Tests")
	private String name;

	@Schema(description = "Module description", example = "Playwright end-to-end tests")
	private String description;

	@Schema(description = "Module path", example = "/apps/web/tests/e2e")
	private String path;

	@Schema(description = "Test framework", example = "PLAYWRIGHT")
	private String framework;

	@Schema(description = "Number of test suites in this module")
	private int suiteCount;

	@Schema(description = "Total number of individual tests")
	private int totalTestCount;

	@Schema(description = "Estimated duration in seconds")
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
	public TestModuleResponse() {
	}

	public TestModuleResponse(String id, String appId, String name, String description, String path, String framework,
			int suiteCount, int totalTestCount, long estimatedDurationSeconds, Instant lastExecutedAt,
			String lastExecutionStatus, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.appId = appId;
		this.name = name;
		this.description = description;
		this.path = path;
		this.framework = framework;
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

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
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
