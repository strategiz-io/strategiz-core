package io.strategiz.data.user.model.watchlist;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * User preferences and settings for watchlist functionality.
 */
public class WatchlistSettings extends BaseEntity {
    
    private String id;
    private String userId;
    private String defaultWatchlistId;
    private boolean autoRefresh;
    private int refreshIntervalSeconds;
    private boolean soundNotifications;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private String defaultSortBy; // SYMBOL, NAME, PRICE, CHANGE, etc.
    private String defaultSortOrder; // ASC, DESC
    private Map<String, Object> customSettings = new HashMap<>();
    
    public WatchlistSettings() {
        this.autoRefresh = true;
        this.refreshIntervalSeconds = 30;
        this.soundNotifications = false;
        this.emailNotifications = false;
        this.pushNotifications = true;
        this.defaultSortBy = "SYMBOL";
        this.defaultSortOrder = "ASC";
    }
    
    public WatchlistSettings(String userId, String createdBy) {
        super(createdBy);
        this.userId = userId;
        this.id = userId + "_watchlist_settings";
        this.autoRefresh = true;
        this.refreshIntervalSeconds = 30;
        this.soundNotifications = false;
        this.emailNotifications = false;
        this.pushNotifications = true;
        this.defaultSortBy = "SYMBOL";
        this.defaultSortOrder = "ASC";
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getCollectionName() {
        return "watchlist_settings";
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDefaultWatchlistId() {
        return defaultWatchlistId;
    }
    
    public void setDefaultWatchlistId(String defaultWatchlistId) {
        this.defaultWatchlistId = defaultWatchlistId;
    }
    
    public boolean isAutoRefresh() {
        return autoRefresh;
    }
    
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }
    
    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }
    
    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = Math.max(5, Math.min(300, refreshIntervalSeconds)); // 5-300 seconds
    }
    
    public boolean isSoundNotifications() {
        return soundNotifications;
    }
    
    public void setSoundNotifications(boolean soundNotifications) {
        this.soundNotifications = soundNotifications;
    }
    
    public boolean isEmailNotifications() {
        return emailNotifications;
    }
    
    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
    
    public boolean isPushNotifications() {
        return pushNotifications;
    }
    
    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }
    
    public String getDefaultSortBy() {
        return defaultSortBy;
    }
    
    public void setDefaultSortBy(String defaultSortBy) {
        this.defaultSortBy = defaultSortBy;
    }
    
    public String getDefaultSortOrder() {
        return defaultSortOrder;
    }
    
    public void setDefaultSortOrder(String defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }
    
    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }
    
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings != null ? customSettings : new HashMap<>();
    }
    
    public void putCustomSetting(String key, Object value) {
        this.customSettings.put(key, value);
    }
    
    public Object getCustomSetting(String key) {
        return this.customSettings.get(key);
    }
    
    public void removeCustomSetting(String key) {
        this.customSettings.remove(key);
    }
    
    public void enableAllNotifications() {
        this.soundNotifications = true;
        this.emailNotifications = true;
        this.pushNotifications = true;
    }
    
    public void disableAllNotifications() {
        this.soundNotifications = false;
        this.emailNotifications = false;
        this.pushNotifications = false;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WatchlistSettings that = (WatchlistSettings) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, userId);
    }
    
    @Override
    public String toString() {
        return "WatchlistSettings{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", defaultWatchlistId='" + defaultWatchlistId + '\'' +
               ", autoRefresh=" + autoRefresh +
               ", refreshIntervalSeconds=" + refreshIntervalSeconds +
               ", audit=" + getAuditFields() +
               '}';
    }
}