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
 * Represents a top-level application in the test hierarchy Examples: "Web App", "Console
 * App", "Auth App", "Backend API", "Python Microservice" Stored in: tests/apps/{appId}
 */
@Entity
@Table(name = "test_apps")
@Collection("tests/apps")
public class TestAppEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("displayName")
	@JsonProperty("displayName")
	@NotBlank(message = "Display name is required")
	@Column(name = "display_name", nullable = false)
	private String displayName; // "Web App", "Console App", "Backend API"

	@PropertyName("type")
	@JsonProperty("type")
	@Enumerated(EnumType.STRING)
	@NotNull(message = "App type is required")
	@Column(name = "type", nullable = false)
	private TestAppType type; // FRONTEND, BACKEND, MICROSERVICE

	@PropertyName("description")
	@JsonProperty("description")
	@Column(name = "description")
	private String description;

	@PropertyName("codeDirectory")
	@JsonProperty("codeDirectory")
	@Column(name = "code_directory")
	private String codeDirectory; // Path to codebase (for test discovery)

	@PropertyName("totalModules")
	@JsonProperty("totalModules")
	@Column(name = "total_modules")
	private Integer totalModules = 0;

	@PropertyName("totalTests")
	@JsonProperty("totalTests")
	@Column(name = "total_tests")
	private Integer totalTests = 0;

	@PropertyName("estimatedDurationSeconds")
	@JsonProperty("estimatedDurationSeconds")
	@Column(name = "estimated_duration_seconds")
	private Integer estimatedDurationSeconds = 0;

	// Constructors
	public TestAppEntity() {
		super();
	}

	public TestAppEntity(String displayName, TestAppType type) {
		super();
		this.displayName = displayName;
		this.type = type;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public TestAppType getType() {
		return type;
	}

	public void setType(TestAppType type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCodeDirectory() {
		return codeDirectory;
	}

	public void setCodeDirectory(String codeDirectory) {
		this.codeDirectory = codeDirectory;
	}

	public Integer getTotalModules() {
		return totalModules;
	}

	public void setTotalModules(Integer totalModules) {
		this.totalModules = totalModules;
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
