package io.strategiz.data.testing.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Core entity representing a test execution run Tracks executions at any level: app,
 * module, suite, or individual test Stored in: test-runs/{runId}
 */
@Entity
@Table(name = "test_runs")
@Collection("test-runs")
public class TestRunEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id; // Also serves as runId

	// Hierarchy linkage - which level was executed
	@PropertyName("level")
	@JsonProperty("level")
	@Enumerated(EnumType.STRING)
	@NotNull(message = "Execution level is required")
	@Column(name = "level", nullable = false)
	private TestExecutionLevel level; // APP, MODULE, SUITE, TEST

	@PropertyName("appId")
	@JsonProperty("appId")
	@NotBlank(message = "App ID is required")
	@Column(name = "app_id", nullable = false)
	private String appId;

	@PropertyName("moduleId")
	@JsonProperty("moduleId")
	@Column(name = "module_id")
	private String moduleId; // Null if level = APP

	@PropertyName("suiteId")
	@JsonProperty("suiteId")
	@Column(name = "suite_id")
	private String suiteId; // Null if level = APP or MODULE

	@PropertyName("testId")
	@JsonProperty("testId")
	@Column(name = "test_id")
	private String testId; // Null if level != TEST

	// Execution metadata
	@PropertyName("trigger")
	@JsonProperty("trigger")
	@Enumerated(EnumType.STRING)
	@NotNull(message = "Trigger type is required")
	@Column(name = "trigger", nullable = false)
	private TestTrigger trigger; // MANUAL, CI_CD, SCHEDULED

	@PropertyName("status")
	@JsonProperty("status")
	@Enumerated(EnumType.STRING)
	@NotNull(message = "Status is required")
	@Column(name = "status", nullable = false)
	private TestRunStatus status; // PENDING, RUNNING, PASSED, FAILED, ERROR, CANCELLED

	@PropertyName("startTime")
	@JsonProperty("startTime")
	@Column(name = "start_time")
	private Instant startTime;

	@PropertyName("endTime")
	@JsonProperty("endTime")
	@Column(name = "end_time")
	private Instant endTime;

	@PropertyName("durationMs")
	@JsonProperty("durationMs")
	@Column(name = "duration_ms")
	private Long durationMs;

	// Results summary
	@PropertyName("totalTests")
	@JsonProperty("totalTests")
	@Column(name = "total_tests")
	private Integer totalTests = 0;

	@PropertyName("passedTests")
	@JsonProperty("passedTests")
	@Column(name = "passed_tests")
	private Integer passedTests = 0;

	@PropertyName("failedTests")
	@JsonProperty("failedTests")
	@Column(name = "failed_tests")
	private Integer failedTests = 0;

	@PropertyName("skippedTests")
	@JsonProperty("skippedTests")
	@Column(name = "skipped_tests")
	private Integer skippedTests = 0;

	@PropertyName("errorTests")
	@JsonProperty("errorTests")
	@Column(name = "error_tests")
	private Integer errorTests = 0;

	// CI/CD metadata (for GitHub Actions runs)
	@PropertyName("commitHash")
	@JsonProperty("commitHash")
	@Column(name = "commit_hash")
	private String commitHash;

	@PropertyName("branch")
	@JsonProperty("branch")
	@Column(name = "branch")
	private String branch;

	@PropertyName("workflowRunId")
	@JsonProperty("workflowRunId")
	@Column(name = "workflow_run_id")
	private String workflowRunId; // GitHub Actions run ID

	@PropertyName("workflowRunUrl")
	@JsonProperty("workflowRunUrl")
	@Column(name = "workflow_run_url")
	private String workflowRunUrl;

	// Execution logs (stored here for quick access, full logs in subcollection)
	@PropertyName("logs")
	@JsonProperty("logs")
	@Column(name = "logs", columnDefinition = "TEXT")
	private String logs; // Captured stdout/stderr (truncated to 10MB)

	@PropertyName("errorMessage")
	@JsonProperty("errorMessage")
	@Column(name = "error_message")
	private String errorMessage; // High-level error if run failed

	@PropertyName("executedBy")
	@JsonProperty("executedBy")
	@Column(name = "executed_by")
	private String executedBy; // User ID who triggered manual run

	// Additional metadata (environment vars, parameters, etc.)
	@PropertyName("metadata")
	@JsonProperty("metadata")
	@Column(name = "metadata", columnDefinition = "TEXT")
	private Map<String, Object> metadata;

	// Constructors
	public TestRunEntity() {
		super();
		this.metadata = new HashMap<>();
	}

	public TestRunEntity(TestExecutionLevel level, String appId, TestTrigger trigger) {
		super();
		this.level = level;
		this.appId = appId;
		this.trigger = trigger;
		this.status = TestRunStatus.PENDING;
		this.metadata = new HashMap<>();
	}

	// Helper methods
	public Double getPassRate() {
		if (totalTests == null || totalTests == 0) {
			return 0.0;
		}
		return (passedTests != null ? passedTests : 0) * 100.0 / totalTests;
	}

	public boolean isComplete() {
		return status == TestRunStatus.PASSED || status == TestRunStatus.FAILED || status == TestRunStatus.ERROR
				|| status == TestRunStatus.CANCELLED;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TestExecutionLevel getLevel() {
		return level;
	}

	public void setLevel(TestExecutionLevel level) {
		this.level = level;
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

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}

	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public TestTrigger getTrigger() {
		return trigger;
	}

	public void setTrigger(TestTrigger trigger) {
		this.trigger = trigger;
	}

	public TestRunStatus getStatus() {
		return status;
	}

	public void setStatus(TestRunStatus status) {
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

	public Integer getTotalTests() {
		return totalTests;
	}

	public void setTotalTests(Integer totalTests) {
		this.totalTests = totalTests;
	}

	public Integer getPassedTests() {
		return passedTests;
	}

	public void setPassedTests(Integer passedTests) {
		this.passedTests = passedTests;
	}

	public Integer getFailedTests() {
		return failedTests;
	}

	public void setFailedTests(Integer failedTests) {
		this.failedTests = failedTests;
	}

	public Integer getSkippedTests() {
		return skippedTests;
	}

	public void setSkippedTests(Integer skippedTests) {
		this.skippedTests = skippedTests;
	}

	public Integer getErrorTests() {
		return errorTests;
	}

	public void setErrorTests(Integer errorTests) {
		this.errorTests = errorTests;
	}

	public String getCommitHash() {
		return commitHash;
	}

	public void setCommitHash(String commitHash) {
		this.commitHash = commitHash;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getWorkflowRunId() {
		return workflowRunId;
	}

	public void setWorkflowRunId(String workflowRunId) {
		this.workflowRunId = workflowRunId;
	}

	public String getWorkflowRunUrl() {
		return workflowRunUrl;
	}

	public void setWorkflowRunUrl(String workflowRunUrl) {
		this.workflowRunUrl = workflowRunUrl;
	}

	public String getLogs() {
		return logs;
	}

	public void setLogs(String logs) {
		this.logs = logs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}
