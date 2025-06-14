package io.strategiz.api.dashboard.model.watchlist;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Response model for watchlist data following Synapse patterns.
 * Designed to support the Market Watchlist UI component.
 */
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
     * Default constructor
     */
    public WatchlistResponse() {
    }
    
    /**
     * Gets the list of watchlist items
     * 
     * @return List of watchlist items
     */
    public List<WatchlistItem> getWatchlistItems() {
        return watchlistItems;
    }
    
    /**
     * Sets the list of watchlist items
     * 
     * @param watchlistItems List of watchlist items
     */
    public void setWatchlistItems(List<WatchlistItem> watchlistItems) {
        this.watchlistItems = watchlistItems;
    }
    
    /**
     * Gets available categories for filtering
     * 
     * @return Available categories for filtering
     */
    public List<String> getAvailableCategories() {
        return availableCategories;
    }
    
    /**
     * Sets available categories for filtering
     * 
     * @param availableCategories Available categories for filtering
     */
    public void setAvailableCategories(List<String> availableCategories) {
        this.availableCategories = availableCategories;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WatchlistResponse that = (WatchlistResponse) o;
        return Objects.equals(watchlistItems, that.watchlistItems) && 
               Objects.equals(availableCategories, that.availableCategories);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), watchlistItems, availableCategories);
    }
    
    @Override
    public String toString() {
        return "WatchlistResponse{" +
                "watchlistItems=" + watchlistItems +
                ", availableCategories=" + availableCategories +
                "}";
    }
    
    /**
     * Watchlist item that matches the Market Watchlist UI
     */
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
        
        /**
         * Default constructor
         */
        public WatchlistItem() {
        }
        
        /**
         * Gets asset identifier
         *
         * @return Asset identifier
         */
        public String getId() {
            return id;
        }
        
        /**
         * Sets asset identifier
         *
         * @param id Asset identifier
         */
        public void setId(String id) {
            this.id = id;
        }
        
        /**
         * Gets trading symbol
         *
         * @return Trading symbol
         */
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * Sets trading symbol
         *
         * @param symbol Trading symbol
         */
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * Gets full name of the asset
         *
         * @return Full name of the asset
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets full name of the asset
         *
         * @param name Full name of the asset
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets type of asset
         *
         * @return Type of asset
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Sets type of asset
         *
         * @param category Type of asset
         */
        public void setCategory(String category) {
            this.category = category;
        }
        
        /**
         * Gets current price of the asset
         *
         * @return Current price of the asset
         */
        public BigDecimal getPrice() {
            return price;
        }
        
        /**
         * Sets current price of the asset
         *
         * @param price Current price of the asset
         */
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        /**
         * Gets price change value
         *
         * @return Price change value
         */
        public BigDecimal getChange() {
            return change;
        }
        
        /**
         * Sets price change value
         *
         * @param change Price change value
         */
        public void setChange(BigDecimal change) {
            this.change = change;
        }
        
        /**
         * Gets price change percentage
         *
         * @return Price change percentage
         */
        public BigDecimal getChangePercent() {
            return changePercent;
        }
        
        /**
         * Sets price change percentage
         *
         * @param changePercent Price change percentage
         */
        public void setChangePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
        }
        
        /**
         * Checks if the change is positive
         *
         * @return True if the change is positive, false otherwise
         */
        public boolean isPositiveChange() {
            return isPositiveChange;
        }
        
        /**
         * Sets whether the change is positive
         *
         * @param isPositiveChange Whether the change is positive
         */
        public void setPositiveChange(boolean isPositiveChange) {
            this.isPositiveChange = isPositiveChange;
        }
        
        /**
         * Gets optional icon/logo URL for the asset
         *
         * @return Optional icon/logo URL for the asset
         */
        public String getIconUrl() {
            return iconUrl;
        }
        
        /**
         * Sets optional icon/logo URL for the asset
         *
         * @param iconUrl Optional icon/logo URL for the asset
         */
        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }
        
        /**
         * Gets chart data URL or endpoint if available
         *
         * @return Chart data URL or endpoint if available
         */
        public String getChartDataUrl() {
            return chartDataUrl;
        }
        
        /**
         * Sets chart data URL or endpoint if available
         *
         * @param chartDataUrl Chart data URL or endpoint if available
         */
        public void setChartDataUrl(String chartDataUrl) {
            this.chartDataUrl = chartDataUrl;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchlistItem that = (WatchlistItem) o;
            return isPositiveChange == that.isPositiveChange && 
                   Objects.equals(id, that.id) && 
                   Objects.equals(symbol, that.symbol) && 
                   Objects.equals(name, that.name) && 
                   Objects.equals(category, that.category) && 
                   Objects.equals(price, that.price) && 
                   Objects.equals(change, that.change) && 
                   Objects.equals(changePercent, that.changePercent) && 
                   Objects.equals(iconUrl, that.iconUrl) && 
                   Objects.equals(chartDataUrl, that.chartDataUrl);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id, symbol, name, category, price, change, changePercent, 
                               isPositiveChange, iconUrl, chartDataUrl);
        }
        
        @Override
        public String toString() {
            return "WatchlistItem{" +
                   "id='" + id + '\'' +
                   ", symbol='" + symbol + '\'' +
                   ", name='" + name + '\'' +
                   ", category='" + category + '\'' +
                   ", price=" + price +
                   ", change=" + change +
                   ", changePercent=" + changePercent +
                   ", isPositiveChange=" + isPositiveChange +
                   ", iconUrl='" + iconUrl + '\'' +
                   ", chartDataUrl='" + chartDataUrl + '\'' +
                   "}";
        }
    }
}
