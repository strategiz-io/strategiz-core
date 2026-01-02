package io.strategiz.data.testing.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a test suite within a module
 * Examples:
 * - Playwright: "Smoke", "Auth Journey", "Trading Journey"
 * - JUnit: "CreateStrategyControllerTest", "ExecuteStrategyControllerTest"
 * - pytest: "TestBasicExecution", "TestSecuritySandbox"
 * Stored in: tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}
 */
@Entity
@Table(name = "test_suites")
@Collection("suites")
public class TestSuiteEntity extends BaseEntity {

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

    @PropertyName("displayName")
    @JsonProperty("displayName")
    @NotBlank(message = "Display name is required")
    @Column(name = "display_name", nullable = false)
    private String displayName; // "Smoke Tests", "CreateStrategyControllerTest"

    @PropertyName("description")
    @JsonProperty("description")
    @Column(name = "description")
    private String description;

    @PropertyName("filePath")
    @JsonProperty("filePath")
    @Column(name = "file_path")
    private String filePath; // Relative path to test file

    @PropertyName("className")
    @JsonProperty("className")
    @Column(name = "class_name")
    private String className; // For JUnit/pytest test classes

    @PropertyName("totalTests")
    @JsonProperty("totalTests")
    @Column(name = "total_tests")
    private Integer totalTests = 0;

    @PropertyName("estimatedDurationSeconds")
    @JsonProperty("estimatedDurationSeconds")
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds = 0;

    // Constructors
    public TestSuiteEntity() {
        super();
    }

    public TestSuiteEntity(String appId, String moduleId, String displayName) {
        super();
        this.appId = appId;
        this.moduleId = moduleId;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(Integer totalTests) {
        this.totalTests = totalTests;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }
}
