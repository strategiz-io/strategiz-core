package io.strategiz.service.dashboard.model.watchlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for watchlist data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponse {
    
    private String userId;
    private List<WatchlistItem> assets;
    private List<String> availableCategories;
    private LocalDateTime lastUpdated;
    
    /**
     * Alias for setAssets to maintain compatibility with existing code
     * 
     * @param watchlistItems The list of watchlist items
     */
    public void setWatchlistItems(List<WatchlistItem> watchlistItems) {
        this.assets = watchlistItems;
    }
    
    /**
     * Represents an asset in a user's watchlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistItem {
        private String id;
        private String symbol;
        private String name;
        private String category;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
        private boolean positiveChange;
        private String chartDataUrl;
    }
}
