package io.strategiz.data.testing.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents an individual test case (test method) Examples: - Playwright: "should login
 * with valid credentials" - JUnit: "testExecutePythonStrategy_Success" - pytest:
 * "test_simple_buy_strategy" Stored in:
 * tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests/{testId}
 */
@Entity
@Table(name = "test_cases")
@Collection("tests")
public class TestCaseEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("appId")
	@JsonProperty("appId")
	@NotBlank(message = "App ID is required")
	@Column(name = "app_id", nullable = false)
	private String appId; // Parent app ID

	@PropertyName("moduleId")
	@JsonProperty("moduleId")
	@NotBlank(message = "Module ID is required")
	@Column(name = "module_id", nullable = false)
	private String moduleId; // Parent module ID

	@PropertyName("suiteId")
	@JsonProperty("suiteId")
	@NotBlank(message = "Suite ID is required")
	@Column(name = "suite_id", nullable = false)
	private String suiteId; // Parent suite ID

	@PropertyName("displayName")
	@JsonProperty("displayName")
	@NotBlank(message = "Display name is required")
	@Column(name = "display_name", nullable = false)
	private String displayName; // "should login with valid credentials"

	@PropertyName("methodName")
	@JsonProperty("methodName")
	@Column(name = "method_name")
	private String methodName; // Actual method name (e.g.,
								// "testExecutePythonStrategy_Success")

	@PropertyName("description")
	@JsonProperty("description")
	@Column(name = "description")
	private String description;

	@PropertyName("estimatedDurationSeconds")
	@JsonProperty("estimatedDurationSeconds")
	@Column(name = "estimated_duration_seconds")
	private Integer estimatedDurationSeconds = 0;

	@PropertyName("className")
	@JsonProperty("className")
	@Column(name = "class_name")
	private String className; // Java class name (e.g., "AuthenticationTest")

	@PropertyName("filePath")
	@JsonProperty("filePath")
	@Column(name = "file_path")
	private String filePath; // File path (e.g., "/apps/web/tests/e2e/auth.spec.ts")

	@PropertyName("lineNumber")
	@JsonProperty("lineNumber")
	@Column(name = "line_number")
	private Integer lineNumber; // Line number in file

	@PropertyName("framework")
	@JsonProperty("framework")
	@Column(name = "framework")
	private String framework; // Test framework (PLAYWRIGHT, JUNIT, PYTEST, etc.)

	@PropertyName("lastExecutedAt")
	@JsonProperty("lastExecutedAt")
	@Column(name = "last_executed_at")
	private java.time.Instant lastExecutedAt; // Last execution timestamp

	@PropertyName("lastExecutionStatus")
	@JsonProperty("lastExecutionStatus")
	@Column(name = "last_execution_status")
	private String lastExecutionStatus; // PASSED, FAILED, SKIPPED, ERROR

	@PropertyName("tags")
	@JsonProperty("tags")
	@Column(name = "tags")
	private String[] tags; // Tags for categorization (e.g., ["auth", "critical"])

	// Constructors
	public TestCaseEntity() {
		super();
	}

	public TestCaseEntity(String appId, String moduleId, String suiteId, String displayName) {
		super();
		this.appId = appId;
		this.moduleId = moduleId;
		this.suiteId = suiteId;
		this.displayName = displayName;
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

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getEstimatedDurationSeconds() {
		return estimatedDurationSeconds;
	}

	public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
		this.estimatedDurationSeconds = estimatedDurationSeconds;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	public java.time.Instant getLastExecutedAt() {
		return lastExecutedAt;
	}

	public void setLastExecutedAt(java.time.Instant lastExecutedAt) {
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

	// Helper method to get display name (used as "name" in responses)
	public String getName() {
		return this.displayName;
	}

	// Helper methods for timestamp conversion
	public java.time.Instant getCreatedAt() {
		com.google.cloud.Timestamp timestamp = getCreatedDate();
		return timestamp != null ? java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()) : null;
	}

	public java.time.Instant getUpdatedAt() {
		com.google.cloud.Timestamp timestamp = getModifiedDate();
		return timestamp != null ? java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()) : null;
	}

}
