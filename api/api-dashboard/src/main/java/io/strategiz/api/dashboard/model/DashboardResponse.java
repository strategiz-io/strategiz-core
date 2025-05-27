package io.strategiz.api.dashboard.model;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response model for dashboard data following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardResponse extends BaseServiceResponse {
    
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
    
    /**
     * Portfolio summary data
     */
    @Data
    public static class PortfolioSummary {
        private BigDecimal totalValue;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercent;
        private Map<String, ExchangeData> exchanges;
    }
    
    /**
     * Exchange data
     */
    @Data
    public static class ExchangeData {
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
    }
    
    /**
     * Asset data
     */
    @Data
    public static class AssetData {
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;
    }
    
    /**
     * Market data
     */
    @Data
    public static class MarketData {
        private Map<String, BigDecimal> indexes;
        private Map<String, BigDecimal> trends;
    }
    
    /**
     * Watchlist item
     */
    @Data
    public static class WatchlistItem {
        private String id;
        private String symbol;
        private String name;
        private String type;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
    }
    
    /**
     * Performance metrics
     */
    @Data
    public static class PerformanceMetrics {
        private Map<String, BigDecimal> performance;
        private Map<String, BigDecimal> allocation;
        private Map<String, BigDecimal> risk;
    }
}
