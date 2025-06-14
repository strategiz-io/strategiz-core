package io.strategiz.api.dashboard.model.riskanalysis;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Response model for portfolio risk analysis following Synapse patterns.
 */
public class RiskAnalysisResponse extends BaseServiceResponse {
    
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
     * Default constructor
     */
    public RiskAnalysisResponse() {
    }
    
    /**
     * Gets the volatility metric
     * 
     * @return Volatility metric
     */
    public VolatilityMetric getVolatilityMetric() {
        return volatilityMetric;
    }
    
    /**
     * Sets the volatility metric
     * 
     * @param volatilityMetric Volatility metric
     */
    public void setVolatilityMetric(VolatilityMetric volatilityMetric) {
        this.volatilityMetric = volatilityMetric;
    }
    
    /**
     * Gets the diversification metric
     * 
     * @return Diversification metric
     */
    public DiversificationMetric getDiversificationMetric() {
        return diversificationMetric;
    }
    
    /**
     * Sets the diversification metric
     * 
     * @param diversificationMetric Diversification metric
     */
    public void setDiversificationMetric(DiversificationMetric diversificationMetric) {
        this.diversificationMetric = diversificationMetric;
    }
    
    /**
     * Gets the correlation metric
     * 
     * @return Correlation metric
     */
    public CorrelationMetric getCorrelationMetric() {
        return correlationMetric;
    }
    
    /**
     * Sets the correlation metric
     * 
     * @param correlationMetric Correlation metric
     */
    public void setCorrelationMetric(CorrelationMetric correlationMetric) {
        this.correlationMetric = correlationMetric;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RiskAnalysisResponse that = (RiskAnalysisResponse) o;
        return Objects.equals(volatilityMetric, that.volatilityMetric) &&
               Objects.equals(diversificationMetric, that.diversificationMetric) &&
               Objects.equals(correlationMetric, that.correlationMetric);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), volatilityMetric, diversificationMetric, correlationMetric);
    }
    
    @Override
    public String toString() {
        return "RiskAnalysisResponse{" +
               "volatilityMetric=" + volatilityMetric +
               ", diversificationMetric=" + diversificationMetric +
               ", correlationMetric=" + correlationMetric +
               ", " + super.toString() +
               "}";
    }
    
    /**
     * Volatility metric
     */
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
        
        /**
         * Default constructor
         */
        public VolatilityMetric() {
        }
        
        /**
         * Gets the volatility score
         * 
         * @return Volatility score
         */
        public BigDecimal getScore() {
            return score;
        }
        
        /**
         * Sets the volatility score
         * 
         * @param score Volatility score
         */
        public void setScore(BigDecimal score) {
            this.score = score;
        }
        
        /**
         * Gets the volatility category
         * 
         * @return Volatility category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Sets the volatility category
         * 
         * @param category Volatility category
         */
        public void setCategory(String category) {
            this.category = category;
        }
        
        /**
         * Gets the standard deviation
         * 
         * @return Standard deviation
         */
        public BigDecimal getStandardDeviation() {
            return standardDeviation;
        }
        
        /**
         * Sets the standard deviation
         * 
         * @param standardDeviation Standard deviation
         */
        public void setStandardDeviation(BigDecimal standardDeviation) {
            this.standardDeviation = standardDeviation;
        }
        
        /**
         * Gets the maximum drawdown
         * 
         * @return Maximum drawdown
         */
        public BigDecimal getMaxDrawdown() {
            return maxDrawdown;
        }
        
        /**
         * Sets the maximum drawdown
         * 
         * @param maxDrawdown Maximum drawdown
         */
        public void setMaxDrawdown(BigDecimal maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VolatilityMetric that = (VolatilityMetric) o;
            return Objects.equals(score, that.score) &&
                   Objects.equals(category, that.category) &&
                   Objects.equals(standardDeviation, that.standardDeviation) &&
                   Objects.equals(maxDrawdown, that.maxDrawdown);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(score, category, standardDeviation, maxDrawdown);
        }
        
        @Override
        public String toString() {
            return "VolatilityMetric{" +
                   "score=" + score +
                   ", category='" + category + '\'' +
                   ", standardDeviation=" + standardDeviation +
                   ", maxDrawdown=" + maxDrawdown +
                   "}";
        }
    }
    
    /**
     * Diversification metric
     */
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
        
        /**
         * Default constructor
         */
        public DiversificationMetric() {
        }
        
        /**
         * Gets the diversification score
         * 
         * @return Diversification score
         */
        public BigDecimal getScore() {
            return score;
        }
        
        /**
         * Sets the diversification score
         * 
         * @param score Diversification score
         */
        public void setScore(BigDecimal score) {
            this.score = score;
        }
        
        /**
         * Gets the diversification category
         * 
         * @return Diversification category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Sets the diversification category
         * 
         * @param category Diversification category
         */
        public void setCategory(String category) {
            this.category = category;
        }
        
        /**
         * Gets the asset count
         * 
         * @return Asset count
         */
        public int getAssetCount() {
            return assetCount;
        }
        
        /**
         * Sets the asset count
         * 
         * @param assetCount Asset count
         */
        public void setAssetCount(int assetCount) {
            this.assetCount = assetCount;
        }
        
        /**
         * Gets the largest allocation
         * 
         * @return Largest allocation
         */
        public BigDecimal getLargestAllocation() {
            return largestAllocation;
        }
        
        /**
         * Sets the largest allocation
         * 
         * @param largestAllocation Largest allocation
         */
        public void setLargestAllocation(BigDecimal largestAllocation) {
            this.largestAllocation = largestAllocation;
        }
        
        /**
         * Gets the concentration index
         * 
         * @return Concentration index
         */
        public BigDecimal getConcentrationIndex() {
            return concentrationIndex;
        }
        
        /**
         * Sets the concentration index
         * 
         * @param concentrationIndex Concentration index
         */
        public void setConcentrationIndex(BigDecimal concentrationIndex) {
            this.concentrationIndex = concentrationIndex;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiversificationMetric that = (DiversificationMetric) o;
            return assetCount == that.assetCount &&
                   Objects.equals(score, that.score) &&
                   Objects.equals(category, that.category) &&
                   Objects.equals(largestAllocation, that.largestAllocation) &&
                   Objects.equals(concentrationIndex, that.concentrationIndex);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(score, category, assetCount, largestAllocation, concentrationIndex);
        }
        
        @Override
        public String toString() {
            return "DiversificationMetric{" +
                   "score=" + score +
                   ", category='" + category + '\'' +
                   ", assetCount=" + assetCount +
                   ", largestAllocation=" + largestAllocation +
                   ", concentrationIndex=" + concentrationIndex +
                   "}";
        }
    }
    
    /**
     * Correlation metric
     */
    public static class CorrelationMetric {
        /**
         * Average correlation between assets in portfolio
         */
        private BigDecimal averageCorrelation;
        
        /**
         * Correlation category (Low, Moderate, High)
         */
        private String category;
        
        /**
         * Default constructor
         */
        public CorrelationMetric() {
        }
        
        /**
         * Gets the average correlation
         * 
         * @return Average correlation
         */
        public BigDecimal getAverageCorrelation() {
            return averageCorrelation;
        }
        
        /**
         * Sets the average correlation
         * 
         * @param averageCorrelation Average correlation
         */
        public void setAverageCorrelation(BigDecimal averageCorrelation) {
            this.averageCorrelation = averageCorrelation;
        }
        
        /**
         * Gets the correlation category
         * 
         * @return Correlation category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Sets the correlation category
         * 
         * @param category Correlation category
         */
        public void setCategory(String category) {
            this.category = category;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CorrelationMetric that = (CorrelationMetric) o;
            return Objects.equals(averageCorrelation, that.averageCorrelation) &&
                   Objects.equals(category, that.category);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(averageCorrelation, category);
        }
        
        @Override
        public String toString() {
            return "CorrelationMetric{" +
                   "averageCorrelation=" + averageCorrelation +
                   ", category='" + category + '\'' +
                   "}";
        }
    }
}
