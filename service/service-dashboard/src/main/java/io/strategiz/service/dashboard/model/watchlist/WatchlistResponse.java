package io.strategiz.service.dashboard.model.watchlist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Response model for watchlist data.
 */
public class WatchlistResponse {

    private String userId;
    private List<WatchlistItem> assets;
    private List<String> availableCategories;
    private LocalDateTime lastUpdated;

    public WatchlistResponse() {
    }

    public WatchlistResponse(String userId, List<WatchlistItem> assets, List<String> availableCategories, LocalDateTime lastUpdated) {
        this.userId = userId;
        this.assets = assets;
        this.availableCategories = availableCategories;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<WatchlistItem> getAssets() {
        return assets;
    }

    public void setAssets(List<WatchlistItem> assets) {
        this.assets = assets;
    }

    public List<String> getAvailableCategories() {
        return availableCategories;
    }

    public void setAvailableCategories(List<String> availableCategories) {
        this.availableCategories = availableCategories;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Alias for setAssets to maintain compatibility with existing code
     *
     * @param watchlistItems The list of watchlist items
     */
    public void setWatchlistItems(List<WatchlistItem> watchlistItems) {
        this.assets = watchlistItems;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistResponse that = (WatchlistResponse) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(assets, that.assets) &&
                Objects.equals(availableCategories, that.availableCategories) &&
                Objects.equals(lastUpdated, that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, assets, availableCategories, lastUpdated);
    }

    @Override
    public String toString() {
        return "WatchlistResponse{" +
                "userId='" + userId + '\'' +
                ", assets=" + assets +
                ", availableCategories=" + availableCategories +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Builder pattern
    public static WatchlistResponseBuilder builder() {
        return new WatchlistResponseBuilder();
    }
}
