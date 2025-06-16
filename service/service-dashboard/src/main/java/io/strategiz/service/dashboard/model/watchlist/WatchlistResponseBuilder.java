package io.strategiz.service.dashboard.model.watchlist;

import java.time.LocalDateTime;
import java.util.List;

public class WatchlistResponseBuilder {
    private String userId;
    private List<WatchlistItem> assets;
    private List<String> availableCategories;
    private LocalDateTime lastUpdated;

    public WatchlistResponseBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public WatchlistResponseBuilder withAssets(List<WatchlistItem> assets) {
        this.assets = assets;
        return this;
    }

    public WatchlistResponseBuilder withAvailableCategories(List<String> availableCategories) {
        this.availableCategories = availableCategories;
        return this;
    }

    public WatchlistResponseBuilder withLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public WatchlistResponse build() {
        return new WatchlistResponse(userId, assets, availableCategories, lastUpdated);
    }
}
