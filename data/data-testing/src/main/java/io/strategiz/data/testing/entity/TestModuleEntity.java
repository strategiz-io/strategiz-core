package io.strategiz.data.testing.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a module within an application
 * Examples:
 * - Frontend: "E2E Tests", "Unit Tests", "Integration Tests"
 * - Backend: "service-auth", "service-labs", "service-portfolio"
 * - Python: "test_executor", "test_grpc_integration"
 * Stored in: tests/apps/{appId}/modules/{moduleId}
 */
@Entity
@Table(name = "test_modules")
@Collection("modules")
public class TestModuleEntity extends BaseEntity {

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

    @PropertyName("displayName")
    @JsonProperty("displayName")
    @NotBlank(message = "Display name is required")
    @Column(name = "display_name", nullable = false)
    private String displayName; // "E2E Tests", "service-auth"

    @PropertyName("framework")
    @JsonProperty("framework")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Test framework is required")
    @Column(name = "framework", nullable = false)
    private TestFramework framework; // PLAYWRIGHT, JEST, JUNIT, CUCUMBER, PYTEST

    @PropertyName("description")
    @JsonProperty("description")
    @Column(name = "description")
    private String description;

    @PropertyName("modulePath")
    @JsonProperty("modulePath")
    @Column(name = "module_path")
    private String modulePath; // Relative path to module (for Maven/npm)

    @PropertyName("totalSuites")
    @JsonProperty("totalSuites")
    @Column(name = "total_suites")
    private Integer totalSuites = 0;

    @PropertyName("totalTests")
    @JsonProperty("totalTests")
    @Column(name = "total_tests")
    private Integer totalTests = 0;

    @PropertyName("estimatedDurationSeconds")
    @JsonProperty("estimatedDurationSeconds")
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds = 0;

    // Constructors
    public TestModuleEntity() {
        super();
    }

    public TestModuleEntity(String appId, String displayName, TestFramework framework) {
        super();
        this.appId = appId;
        this.displayName = displayName;
        this.framework = framework;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TestFramework getFramework() {
        return framework;
    }

    public void setFramework(TestFramework framework) {
        this.framework = framework;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public Integer getTotalSuites() {
        return totalSuites;
    }

    public void setTotalSuites(Integer totalSuites) {
        this.totalSuites = totalSuites;
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
