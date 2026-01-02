package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for detailed test application information
 */
@Schema(description = "Detailed test application information with nested modules")
public class TestAppDetailResponse {

	@Schema(description = "App ID", example = "frontend-web")
	private String id;

	@Schema(description = "App name", example = "Web Application")
	private String name;

	@Schema(description = "App description", example = "Main web frontend with Playwright E2E tests")
	private String description;

	@Schema(description = "Test framework", example = "PLAYWRIGHT")
	private String framework;

	@Schema(description = "Base directory path", example = "/apps/web")
	private String basePath;

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

	@Schema(description = "Last execution run ID")
	private String lastRunId;

	@Schema(description = "Nested test modules")
	private List<TestModuleResponse> modules;

	@Schema(description = "Created timestamp")
	private Instant createdAt;

	@Schema(description = "Updated timestamp")
	private Instant updatedAt;

	@Schema(description = "Created by user ID")
	private String createdBy;

	// Constructors
	public TestAppDetailResponse() {
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

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
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

	public String getLastRunId() {
		return lastRunId;
	}

	public void setLastRunId(String lastRunId) {
		this.lastRunId = lastRunId;
	}

	public List<TestModuleResponse> getModules() {
		return modules;
	}

	public void setModules(List<TestModuleResponse> modules) {
		this.modules = modules;
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

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

}
