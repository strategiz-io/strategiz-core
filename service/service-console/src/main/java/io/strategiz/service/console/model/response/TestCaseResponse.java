package io.strategiz.service.console.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for individual test case
 */
@Schema(description = "Individual test case")
public class TestCaseResponse {

	@Schema(description = "Test ID", example = "test-login-success")
	private String id;

	@Schema(description = "Parent app ID", example = "frontend-web")
	private String appId;

	@Schema(description = "Parent module ID", example = "e2e-tests")
	private String moduleId;

	@Schema(description = "Parent suite ID", example = "auth-tests")
	private String suiteId;

	@Schema(description = "Test name", example = "should login with valid credentials")
	private String name;

	@Schema(description = "Test description", example = "Verifies successful login flow with email and password")
	private String description;

	@Schema(description = "Class name (for Java tests)", example = "AuthenticationTest")
	private String className;

	@Schema(description = "Method name (for Java/pytest)", example = "testLoginSuccess")
	private String methodName;

	@Schema(description = "File path", example = "/apps/web/tests/e2e/auth.spec.ts")
	private String filePath;

	@Schema(description = "Line number in file", example = "42")
	private Integer lineNumber;

	@Schema(description = "Test framework", example = "PLAYWRIGHT")
	private String framework;

	@Schema(description = "Estimated duration in seconds")
	private long estimatedDurationSeconds;

	@Schema(description = "Last execution timestamp")
	private Instant lastExecutedAt;

	@Schema(description = "Last execution status", example = "PASSED")
	private String lastExecutionStatus;

	@Schema(description = "Tags for categorization", example = "[\"auth\", \"critical\"]")
	private String[] tags;

	@Schema(description = "Created timestamp")
	private Instant createdAt;

	@Schema(description = "Updated timestamp")
	private Instant updatedAt;

	// Constructors
	public TestCaseResponse() {
	}

	public TestCaseResponse(String id, String appId, String moduleId, String suiteId, String name, String description,
			String className, String methodName, String filePath, Integer lineNumber, String framework,
			long estimatedDurationSeconds, Instant lastExecutedAt, String lastExecutionStatus, String[] tags,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.appId = appId;
		this.moduleId = moduleId;
		this.suiteId = suiteId;
		this.name = name;
		this.description = description;
		this.className = className;
		this.methodName = methodName;
		this.filePath = filePath;
		this.lineNumber = lineNumber;
		this.framework = framework;
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

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
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

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
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
