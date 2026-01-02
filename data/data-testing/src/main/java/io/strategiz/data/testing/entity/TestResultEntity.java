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
import java.util.ArrayList;
import java.util.List;

/**
 * Individual test result within a test run
 * Stored in: test-runs/{runId}/results/{resultId}
 */
@Entity
@Table(name = "test_results")
@Collection("results")
public class TestResultEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    @Column(name = "id")
    private String id;

    @PropertyName("runId")
    @JsonProperty("runId")
    @NotBlank(message = "Run ID is required")
    @Column(name = "run_id", nullable = false)
    private String runId; // Parent test run ID

    @PropertyName("testName")
    @JsonProperty("testName")
    @NotBlank(message = "Test name is required")
    @Column(name = "test_name", nullable = false)
    private String testName; // Display name of the test

    @PropertyName("className")
    @JsonProperty("className")
    @Column(name = "class_name")
    private String className; // For JUnit/pytest

    @PropertyName("methodName")
    @JsonProperty("methodName")
    @Column(name = "method_name")
    private String methodName; // Actual method name

    @PropertyName("status")
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    @Column(name = "status", nullable = false)
    private TestResultStatus status; // PASSED, FAILED, SKIPPED, ERROR

    @PropertyName("durationMs")
    @JsonProperty("durationMs")
    @Column(name = "duration_ms")
    private Long durationMs;

    @PropertyName("errorMessage")
    @JsonProperty("errorMessage")
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Failure message

    @PropertyName("stackTrace")
    @JsonProperty("stackTrace")
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace; // Full stack trace if failed

    @PropertyName("screenshots")
    @JsonProperty("screenshots")
    @ElementCollection
    @Column(name = "screenshots")
    private List<String> screenshots; // URLs to screenshot files (for Playwright)

    @PropertyName("videos")
    @JsonProperty("videos")
    @ElementCollection
    @Column(name = "videos")
    private List<String> videos; // URLs to video files (for Playwright)

    @PropertyName("startTime")
    @JsonProperty("startTime")
    @Column(name = "start_time")
    private Instant startTime;

    @PropertyName("endTime")
    @JsonProperty("endTime")
    @Column(name = "end_time")
    private Instant endTime;

    // Constructors
    public TestResultEntity() {
        super();
        this.screenshots = new ArrayList<>();
        this.videos = new ArrayList<>();
    }

    public TestResultEntity(String runId, String testName, TestResultStatus status) {
        super();
        this.runId = runId;
        this.testName = testName;
        this.status = status;
        this.screenshots = new ArrayList<>();
        this.videos = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
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

    public TestResultStatus getStatus() {
        return status;
    }

    public void setStatus(TestResultStatus status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public List<String> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public List<String> getVideos() {
        return videos;
    }

    public void setVideos(List<String> videos) {
        this.videos = videos;
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
}
