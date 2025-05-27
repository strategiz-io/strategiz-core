package io.strategiz.service.dashboard.model.riskanalysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Data model for portfolio risk analysis.
 */
@Data
public class RiskAnalysisData {
    
    /**
     * Overall portfolio volatility score
     */
    private VolatilityMetric volatilityMetric;
    
    /**
     * Portfolio diversification score
     */
    private DiversificationMetric diversificationMetric;
    
    /**
     * Correlation metrics between assets
     */
    private CorrelationMetric correlationMetric;
    
    /**
     * Volatility metric
     */
    @Data
    public static class VolatilityMetric {
        /**
         * Volatility score from 0 (lowest) to 100 (highest)
         */
        private BigDecimal score;
        
        /**
         * Volatility category (Low, Medium, High, Very High)
         */
        private String category;
        
        /**
         * Standard deviation of portfolio returns
         */
        private BigDecimal standardDeviation;
        
        /**
         * Maximum drawdown percentage in the analyzed period
         */
        private BigDecimal maxDrawdown;
    }
    
    /**
     * Diversification metric
     */
    @Data
    public static class DiversificationMetric {
        /**
         * Diversification score from 0 (lowest) to 100 (highest)
         */
        private BigDecimal score;
        
        /**
         * Diversification category (Poor, Fair, Good, Excellent)
         */
        private String category;
        
        /**
         * Number of unique assets in portfolio
         */
        private int assetCount;
        
        /**
         * Percentage of portfolio in largest single asset
         */
        private BigDecimal largestAllocation;
        
        /**
         * Herfindahl-Hirschman Index (concentration measure)
         */
        private BigDecimal concentrationIndex;
    }
    
    /**
     * Correlation metric
     */
    @Data
    public static class CorrelationMetric {
        /**
         * Average correlation between assets in portfolio
         */
        private BigDecimal averageCorrelation;
        
        /**
         * Correlation category (Low, Moderate, High)
         */
        private String category;
    }
}
