package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for test suite summary
 */
@Schema(description = "Test suite summary")
public class TestSuiteResponse {

	@Schema(description = "Suite ID", example = "smoke-tests")
	private String id;

	@Schema(description = "Parent app ID", example = "frontend-web")
	private String appId;

	@Schema(description = "Parent module ID", example = "e2e-tests")
	private String moduleId;

	@Schema(description = "Suite name", example = "Smoke Tests")
	private String name;

	@Schema(description = "Suite description", example = "Critical path smoke tests")
	private String description;

	@Schema(description = "Suite file path", example = "/apps/web/tests/e2e/smoke.spec.ts")
	private String filePath;

	@Schema(description = "Test framework", example = "PLAYWRIGHT")
	private String framework;

	@Schema(description = "Number of individual tests in this suite")
	private int testCount;

	@Schema(description = "Estimated duration in seconds")
	private long estimatedDurationSeconds;

	@Schema(description = "Last execution timestamp")
	private Instant lastExecutedAt;

	@Schema(description = "Last execution status", example = "PASSED")
	private String lastExecutionStatus;

	@Schema(description = "Tags for categorization", example = "[\"smoke\", \"critical\"]")
	private String[] tags;

	@Schema(description = "Created timestamp")
	private Instant createdAt;

	@Schema(description = "Updated timestamp")
	private Instant updatedAt;

	// Constructors
	public TestSuiteResponse() {
	}

	public TestSuiteResponse(String id, String appId, String moduleId, String name, String description,
			String filePath, String framework, int testCount, long estimatedDurationSeconds, Instant lastExecutedAt,
			String lastExecutionStatus, String[] tags, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.appId = appId;
		this.moduleId = moduleId;
		this.name = name;
		this.description = description;
		this.filePath = filePath;
		this.framework = framework;
		this.testCount = testCount;
		this.estimatedDurationSeconds = estimatedDurationSeconds;
		this.lastExecutedAt = lastExecutedAt;
		this.lastExecutionStatus = lastExecutionStatus;
		this.tags = tags;
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

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public int getTestCount() {
		return testCount;
	}

	public void setTestCount(int testCount) {
		this.testCount = testCount;
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

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
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
