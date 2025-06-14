package io.strategiz.service.dashboard.model.dashboard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service model for dashboard data following Synapse patterns.
 */
public class DashboardData implements Serializable {
    
    /**
     * User ID associated with this dashboard data
     */
    private String userId;
    
    /**
     * Portfolio summary data
     */
    private PortfolioSummary portfolio;
    
    /**
     * Market data
     */
    private MarketData market;
    
    /**
     * Watchlist data
     */
    private List<WatchlistItem> watchlist;
    
    /**
     * Portfolio performance metrics
     */
    private PerformanceMetrics metrics;

    // Constructors
    public DashboardData() {}

    public DashboardData(String userId, PortfolioSummary portfolio, MarketData market, 
                        List<WatchlistItem> watchlist, PerformanceMetrics metrics) {
        this.userId = userId;
        this.portfolio = portfolio;
        this.market = market;
        this.watchlist = watchlist;
        this.metrics = metrics;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public PortfolioSummary getPortfolio() {
        return portfolio;
    }

    public MarketData getMarket() {
        return market;
    }

    public List<WatchlistItem> getWatchlist() {
        return watchlist;
    }

    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPortfolio(PortfolioSummary portfolio) {
        this.portfolio = portfolio;
    }

    public void setMarket(MarketData market) {
        this.market = market;
    }

    public void setWatchlist(List<WatchlistItem> watchlist) {
        this.watchlist = watchlist;
    }

    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardData that = (DashboardData) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(portfolio, that.portfolio) &&
               Objects.equals(market, that.market) &&
               Objects.equals(watchlist, that.watchlist) &&
               Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, portfolio, market, watchlist, metrics);
    }

    @Override
    public String toString() {
        return "DashboardData{" +
               "userId='" + userId + '\'' +
               ", portfolio=" + portfolio +
               ", market=" + market +
               ", watchlist=" + watchlist +
               ", metrics=" + metrics +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private PortfolioSummary portfolio;
        private MarketData market;
        private List<WatchlistItem> watchlist;
        private PerformanceMetrics metrics;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder portfolio(PortfolioSummary portfolio) {
            this.portfolio = portfolio;
            return this;
        }

        public Builder market(MarketData market) {
            this.market = market;
            return this;
        }

        public Builder watchlist(List<WatchlistItem> watchlist) {
            this.watchlist = watchlist;
            return this;
        }

        public Builder metrics(PerformanceMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public DashboardData build() {
            return new DashboardData(userId, portfolio, market, watchlist, metrics);
        }
    }
}
