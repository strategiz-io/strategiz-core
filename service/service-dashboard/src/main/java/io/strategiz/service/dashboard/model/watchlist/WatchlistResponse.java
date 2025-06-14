package io.strategiz.service.dashboard.model.watchlist;

import java.math.BigDecimal;
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

    // Constructors
    public WatchlistResponse() {}

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
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private List<WatchlistItem> assets;
        private List<String> availableCategories;
        private LocalDateTime lastUpdated;

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAssets(List<WatchlistItem> assets) {
            this.assets = assets;
            return this;
        }

        public Builder withAvailableCategories(List<String> availableCategories) {
            this.availableCategories = availableCategories;
            return this;
        }

        public Builder withLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public WatchlistResponse build() {
            return new WatchlistResponse(userId, assets, availableCategories, lastUpdated);
        }
    }
    
    /**
     * Represents an asset in a user's watchlist.
     */
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

        // Constructors
        public WatchlistItem() {}

        public WatchlistItem(String id, String symbol, String name, String category, BigDecimal price,
                           BigDecimal change, BigDecimal changePercent, boolean positiveChange, String chartDataUrl) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.category = category;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
            this.positiveChange = positiveChange;
            this.chartDataUrl = chartDataUrl;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getChange() {
            return change;
        }

        public void setChange(BigDecimal change) {
            this.change = change;
        }

        public BigDecimal getChangePercent() {
            return changePercent;
        }

        public void setChangePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
        }

        public boolean isPositiveChange() {
            return positiveChange;
        }

        public void setPositiveChange(boolean positiveChange) {
            this.positiveChange = positiveChange;
        }

        public String getChartDataUrl() {
            return chartDataUrl;
        }

        public void setChartDataUrl(String chartDataUrl) {
            this.chartDataUrl = chartDataUrl;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchlistItem that = (WatchlistItem) o;
            return positiveChange == that.positiveChange &&
                   Objects.equals(id, that.id) &&
                   Objects.equals(symbol, that.symbol) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(category, that.category) &&
                   Objects.equals(price, that.price) &&
                   Objects.equals(change, that.change) &&
                   Objects.equals(changePercent, that.changePercent) &&
                   Objects.equals(chartDataUrl, that.chartDataUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, symbol, name, category, price, change, changePercent, positiveChange, chartDataUrl);
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
                   ", positiveChange=" + positiveChange +
                   ", chartDataUrl='" + chartDataUrl + '\'' +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private String symbol;
            private String name;
            private String category;
            private BigDecimal price;
            private BigDecimal change;
            private BigDecimal changePercent;
            private boolean positiveChange;
            private String chartDataUrl;

            public Builder withId(String id) {
                this.id = id;
                return this;
            }

            public Builder withSymbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withCategory(String category) {
                this.category = category;
                return this;
            }

            public Builder withPrice(BigDecimal price) {
                this.price = price;
                return this;
            }

            public Builder withChange(BigDecimal change) {
                this.change = change;
                return this;
            }

            public Builder withChangePercent(BigDecimal changePercent) {
                this.changePercent = changePercent;
                return this;
            }

            public Builder withPositiveChange(boolean positiveChange) {
                this.positiveChange = positiveChange;
                return this;
            }

            public Builder withChartDataUrl(String chartDataUrl) {
                this.chartDataUrl = chartDataUrl;
                return this;
            }

            public WatchlistItem build() {
                return new WatchlistItem(id, symbol, name, category, price, change, changePercent, positiveChange, chartDataUrl);
            }
        }
    }
}
