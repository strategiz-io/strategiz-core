package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for portfolio summary data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    
    private String userId;
    private BigDecimal totalValue;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    private BigDecimal weeklyChange;
    private BigDecimal weeklyChangePercent;
    private BigDecimal monthlyChange;
    private BigDecimal monthlyChangePercent;
    private BigDecimal yearlyChange;
    private BigDecimal yearlyChangePercent;
    private List<Asset> assets;
    private LocalDateTime lastUpdated;
    private boolean hasExchangeConnections;
    private String statusMessage;
    private boolean needsApiKeyConfiguration;
    private Map<String, ExchangeData> exchanges;
    
    /**
     * Individual asset in a portfolio.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Asset {
        private String id;
        private String symbol;
        private String name;
        private String type;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocation;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercent;
    }
    
    /**
     * Exchange data with assets
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeData {
        private String id;
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
    }
    
    /**
     * Asset data for exchanges
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetData {
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;
    }
    
    /**
     * Performance metrics for the portfolio.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioMetrics {
        private BigDecimal annualizedReturn;
        private BigDecimal volatility;
        private BigDecimal sharpeRatio;
        private BigDecimal maxDrawdown;
        private BigDecimal beta;
        private BigDecimal alpha;
    }
}
