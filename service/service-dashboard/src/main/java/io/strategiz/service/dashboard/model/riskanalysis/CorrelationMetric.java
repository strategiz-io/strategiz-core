package io.strategiz.service.dashboard.model.riskanalysis;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Correlation metric
 */
public class CorrelationMetric {
    /**
     * Average correlation between assets in portfolio
     */
    private BigDecimal averageCorrelation;
    
    /**
     * Correlation category (Low, Moderate, High)
     */
    private String category;

    // Constructors
    public CorrelationMetric() {}

    public CorrelationMetric(BigDecimal averageCorrelation, String category) {
        this.averageCorrelation = averageCorrelation;
        this.category = category;
    }

    // Getters and Setters
    public BigDecimal getAverageCorrelation() {
        return averageCorrelation;
    }

    public void setAverageCorrelation(BigDecimal averageCorrelation) {
        this.averageCorrelation = averageCorrelation;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // equals, hashCode, toString
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
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal averageCorrelation;
        private String category;

        public Builder withAverageCorrelation(BigDecimal averageCorrelation) {
            this.averageCorrelation = averageCorrelation;
            return this;
        }

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public CorrelationMetric build() {
            return new CorrelationMetric(averageCorrelation, category);
        }
    }
}
