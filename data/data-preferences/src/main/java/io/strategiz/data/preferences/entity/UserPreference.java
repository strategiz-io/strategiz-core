package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * User preference for users/{userId}/preferences subcollection
 * Each document represents a preference category (theme, notifications, etc.)
 */
public class UserPreference extends BaseEntity {

    @DocumentId
    @PropertyName("preferenceId")
    @JsonProperty("preferenceId")
    private String preferenceId;

    @PropertyName("category")
    @JsonProperty("category")
    @NotBlank(message = "Preference category is required")
    private String category; // theme, notifications, trading, etc.

    @PropertyName("settings")
    @JsonProperty("settings")
    private Map<String, Object> settings; // Category-specific settings

    // Constructors
    public UserPreference() {
        super();
    }

    public UserPreference(String category, Map<String, Object> settings) {
        super();
        this.category = category;
        this.settings = settings;
        this.preferenceId = category; // Use category as document ID
    }

    // Getters and Setters
    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    // Convenience methods
    public Object getSetting(String key) {
        return settings != null ? settings.get(key) : null;
    }

    public void setSetting(String key, Object value) {
        if (settings == null) {
            settings = new java.util.HashMap<>();
        }
        settings.put(key, value);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        Object value = getSetting(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public String getStringSetting(String key, String defaultValue) {
        Object value = getSetting(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public Integer getIntegerSetting(String key, Integer defaultValue) {
        Object value = getSetting(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return preferenceId;
    }

    @Override
    public void setId(String id) {
        this.preferenceId = id;
    }

    @Override
    public String getCollectionName() {
        return "preferences";
    }
}