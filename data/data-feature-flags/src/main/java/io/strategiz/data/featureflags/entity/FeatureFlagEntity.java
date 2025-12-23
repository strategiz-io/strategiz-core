package io.strategiz.data.featureflags.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * System-wide feature flag stored in system/feature_flags collection.
 * Used to enable/disable features across the platform.
 */
@Collection("feature_flags")
public class FeatureFlagEntity extends BaseEntity {

    @DocumentId
    @PropertyName("flagId")
    @JsonProperty("flagId")
    private String flagId;

    @PropertyName("name")
    @JsonProperty("name")
    @NotBlank(message = "Feature flag name is required")
    private String name;

    @PropertyName("description")
    @JsonProperty("description")
    private String description;

    @PropertyName("enabled")
    @JsonProperty("enabled")
    private boolean enabled;

    @PropertyName("category")
    @JsonProperty("category")
    private String category; // providers, ai, trading, etc.

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional configuration

    // Constructors
    public FeatureFlagEntity() {
        super();
    }

    public FeatureFlagEntity(String flagId, String name, String description, boolean enabled, String category) {
        super();
        this.flagId = flagId;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.category = category;
    }

    // Getters and Setters
    public String getFlagId() {
        return flagId;
    }

    public void setFlagId(String flagId) {
        this.flagId = flagId;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience method
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return flagId;
    }

    @Override
    public void setId(String id) {
        this.flagId = id;
    }
}
