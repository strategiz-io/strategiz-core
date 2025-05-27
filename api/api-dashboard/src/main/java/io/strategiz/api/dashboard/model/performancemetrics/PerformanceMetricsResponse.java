package io.strategiz.api.dashboard.model.performancemetrics;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response model for portfolio performance metrics following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformanceMetricsResponse extends BaseServiceResponse {
    
    /**
     * Historical portfolio value data points
     */
    private List<PortfolioValueDataPoint> historicalValues;
    
    /**
     * Overall portfolio performance summary
     */
    private PerformanceSummary summary;
    
    /**
     * Data point for historical portfolio value
     */
    @Data
    public static class PortfolioValueDataPoint {
        /**
         * Timestamp of the data point
         */
        private LocalDateTime timestamp;
        
        /**
         * Portfolio total value at this timestamp
         */
        private BigDecimal value;
    }
    
    /**
     * Performance summary metrics
     */
    @Data
    public static class PerformanceSummary {
        /**
         * Total unrealized profit/loss in USD
         */
        private BigDecimal totalProfitLoss;
        
        /**
         * Total profit/loss percentage
         */
        private BigDecimal totalProfitLossPercentage;
        
        /**
         * 24-hour change in USD
         */
        private BigDecimal dailyChange;
        
        /**
         * 24-hour change percentage
         */
        private BigDecimal dailyChangePercentage;
        
        /**
         * 7-day change in USD
         */
        private BigDecimal weeklyChange;
        
        /**
         * 7-day change percentage
         */
        private BigDecimal weeklyChangePercentage;
        
        /**
         * 30-day change in USD
         */
        private BigDecimal monthlyChange;
        
        /**
         * 30-day change percentage
         */
        private BigDecimal monthlyChangePercentage;
        
        /**
         * Year-to-date change in USD
         */
        private BigDecimal ytdChange;
        
        /**
         * Year-to-date change percentage
         */
        private BigDecimal ytdChangePercentage;
        
        /**
         * Whether the portfolio is currently profitable
         */
        private boolean profitable;
    }
}
