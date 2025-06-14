package io.strategiz.service.dashboard.model.riskanalysis;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Volatility metric
 */
public class VolatilityMetric {
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

    // Constructors
    public VolatilityMetric() {}

    public VolatilityMetric(BigDecimal score, String category, BigDecimal standardDeviation, BigDecimal maxDrawdown) {
        this.score = score;
        this.category = category;
        this.standardDeviation = standardDeviation;
        this.maxDrawdown = maxDrawdown;
    }

    // Getters and Setters
    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(BigDecimal standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    // equals, hashCode, toString
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
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal score;
        private String category;
        private BigDecimal standardDeviation;
        private BigDecimal maxDrawdown;

        public Builder withScore(BigDecimal score) {
            this.score = score;
            return this;
        }

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder withStandardDeviation(BigDecimal standardDeviation) {
            this.standardDeviation = standardDeviation;
            return this;
        }

        public Builder withMaxDrawdown(BigDecimal maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
            return this;
        }

        public VolatilityMetric build() {
            return new VolatilityMetric(score, category, standardDeviation, maxDrawdown);
        }
    }
}
