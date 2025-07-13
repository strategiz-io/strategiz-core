package io.strategiz.data.user.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.user.model.watchlist.WatchlistSettings;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for WatchlistSettings entities using BaseRepository audit support.
 * Provides CRUD operations with automatic audit field management.
 */
@Repository
public class WatchlistSettingsRepository extends BaseRepository<WatchlistSettings> {
    
    public WatchlistSettingsRepository(Firestore firestore) {
        super(firestore, WatchlistSettings.class);
    }
    
    /**
     * Find settings for a specific user
     * @param userId User ID
     * @return Optional settings
     */
    public Optional<WatchlistSettings> findByUserId(String userId) {
        return findById(userId + "_watchlist_settings");
    }
    
    /**
     * Get or create default settings for a user
     * @param userId User ID
     * @param createdBy Who is creating it
     * @return User's settings (existing or newly created)
     */
    public WatchlistSettings getOrCreateForUser(String userId, String createdBy) {
        Optional<WatchlistSettings> existing = findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        WatchlistSettings defaultSettings = new WatchlistSettings(userId, createdBy);
        return save(defaultSettings, createdBy);
    }
    
    /**
     * Update the default watchlist for a user
     * @param userId User ID
     * @param defaultWatchlistId New default watchlist ID
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings updateDefaultWatchlist(String userId, String defaultWatchlistId, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.setDefaultWatchlistId(defaultWatchlistId);
        return save(settings, modifiedBy);
    }
    
    /**
     * Update refresh settings for a user
     * @param userId User ID
     * @param autoRefresh Enable/disable auto refresh
     * @param intervalSeconds Refresh interval in seconds
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings updateRefreshSettings(String userId, boolean autoRefresh, int intervalSeconds, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.setAutoRefresh(autoRefresh);
        settings.setRefreshIntervalSeconds(intervalSeconds);
        return save(settings, modifiedBy);
    }
    
    /**
     * Update notification settings for a user
     * @param userId User ID
     * @param soundNotifications Enable/disable sound notifications
     * @param emailNotifications Enable/disable email notifications
     * @param pushNotifications Enable/disable push notifications
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings updateNotificationSettings(String userId, boolean soundNotifications, boolean emailNotifications, boolean pushNotifications, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.setSoundNotifications(soundNotifications);
        settings.setEmailNotifications(emailNotifications);
        settings.setPushNotifications(pushNotifications);
        return save(settings, modifiedBy);
    }
    
    /**
     * Update sorting preferences for a user
     * @param userId User ID
     * @param sortBy Sort field (SYMBOL, NAME, PRICE, etc.)
     * @param sortOrder Sort order (ASC, DESC)
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings updateSortingSettings(String userId, String sortBy, String sortOrder, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.setDefaultSortBy(sortBy);
        settings.setDefaultSortOrder(sortOrder);
        return save(settings, modifiedBy);
    }
    
    /**
     * Add or update a custom setting
     * @param userId User ID
     * @param key Setting key
     * @param value Setting value
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings putCustomSetting(String userId, String key, Object value, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.putCustomSetting(key, value);
        return save(settings, modifiedBy);
    }
    
    /**
     * Remove a custom setting
     * @param userId User ID
     * @param key Setting key to remove
     * @param modifiedBy Who is modifying it
     * @return Updated settings
     */
    public WatchlistSettings removeCustomSetting(String userId, String key, String modifiedBy) {
        WatchlistSettings settings = getOrCreateForUser(userId, modifiedBy);
        settings.removeCustomSetting(key);
        return save(settings, modifiedBy);
    }
    
    /**
     * Reset all settings to defaults for a user
     * @param userId User ID
     * @param modifiedBy Who is resetting it
     * @return Reset settings
     */
    public WatchlistSettings resetToDefaults(String userId, String modifiedBy) {
        WatchlistSettings defaultSettings = new WatchlistSettings(userId, modifiedBy);
        return save(defaultSettings, modifiedBy);
    }
}