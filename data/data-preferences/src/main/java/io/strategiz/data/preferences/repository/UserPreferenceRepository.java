package io.strategiz.data.preferences.repository;

import io.strategiz.data.preferences.entity.UserPreference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for users/{userId}/preferences subcollection
 */
public interface UserPreferenceRepository {
    
    /**
     * Save user preference
     */
    UserPreference savePreference(String userId, UserPreference preference);
    
    /**
     * Get all preferences for user
     */
    List<UserPreference> findByUserId(String userId);
    
    /**
     * Get specific preference category
     */
    Optional<UserPreference> findByUserIdAndCategory(String userId, String category);
    
    /**
     * Update preference
     */
    UserPreference updatePreference(String userId, String category, UserPreference preference);
    
    /**
     * Update specific setting within a preference
     */
    void updateSetting(String userId, String category, String settingKey, Object settingValue);
    
    /**
     * Update multiple settings within a preference
     */
    void updateSettings(String userId, String category, Map<String, Object> settings);
    
    /**
     * Delete preference category
     */
    void deletePreference(String userId, String category);
    
    /**
     * Check if preference exists
     */
    boolean hasPreference(String userId, String category);
    
    /**
     * Get theme preference
     */
    Optional<UserPreference> getThemePreference(String userId);
    
    /**
     * Get notification preferences
     */
    Optional<UserPreference> getNotificationPreference(String userId);
    
    /**
     * Get trading preferences
     */
    Optional<UserPreference> getTradingPreference(String userId);
    
    /**
     * Set theme
     */
    void setTheme(String userId, String theme);
    
    /**
     * Set notification setting
     */
    void setNotificationSetting(String userId, String notificationType, boolean enabled);
    
    /**
     * Set trading mode
     */
    void setTradingMode(String userId, String tradingMode);
}