package io.strategiz.api.dashboard.model.watchlist;

import io.strategiz.service.base.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response model for watchlist data following Synapse patterns.
 * Designed to support the Market Watchlist UI component.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WatchlistResponse extends BaseServiceResponse {
    
    /**
     * List of watchlist items
     */
    private List<WatchlistItem> watchlistItems;
    
    /**
     * Available categories for filtering
     */
    private List<String> availableCategories;
    
    /**
     * Watchlist item that matches the Market Watchlist UI
     */
    @Data
    public static class WatchlistItem {
        /**
         * Asset identifier
         */
        private String id;
        
        /**
         * Trading symbol (e.g., BTC, ETH, MSFT)
         */
        private String symbol;
        
        /**
         * Full name of the asset (e.g., Bitcoin, Ethereum, Microsoft)
         */
        private String name;
        
        /**
         * Type of asset (e.g., CRYPTO, STOCK)
         */
        private String category;
        
        /**
         * Current price of the asset
         */
        private BigDecimal price;
        
        /**
         * Price change value
         */
        private BigDecimal change;
        
        /**
         * Price change percentage
         */
        private BigDecimal changePercent;
        
        /**
         * Flag indicating if the change is positive (true) or negative (false)
         */
        private boolean isPositiveChange;
        
        /**
         * Optional icon/logo URL for the asset
         */
        private String iconUrl;
        
        /**
         * Chart data URL or endpoint if available
         */
        private String chartDataUrl;
    }
}
