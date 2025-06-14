package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Performance metrics
 */
public class PerformanceMetrics {
    private Map<String, BigDecimal> performance;
    private Map<String, BigDecimal> allocation;
    private Map<String, BigDecimal> risk;

    // Constructors
    public PerformanceMetrics() {}

    public PerformanceMetrics(Map<String, BigDecimal> performance, Map<String, BigDecimal> allocation,
                             Map<String, BigDecimal> risk) {
        this.performance = performance;
        this.allocation = allocation;
        this.risk = risk;
    }

    // Getters and Setters
    public Map<String, BigDecimal> getPerformance() {
        return performance;
    }

    public void setPerformance(Map<String, BigDecimal> performance) {
        this.performance = performance;
    }

    public Map<String, BigDecimal> getAllocation() {
        return allocation;
    }

    public void setAllocation(Map<String, BigDecimal> allocation) {
        this.allocation = allocation;
    }

    public Map<String, BigDecimal> getRisk() {
        return risk;
    }

    public void setRisk(Map<String, BigDecimal> risk) {
        this.risk = risk;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerformanceMetrics that = (PerformanceMetrics) o;
        return Objects.equals(performance, that.performance) &&
               Objects.equals(allocation, that.allocation) &&
               Objects.equals(risk, that.risk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(performance, allocation, risk);
    }

    @Override
    public String toString() {
        return "PerformanceMetrics{" +
               "performance=" + performance +
               ", allocation=" + allocation +
               ", risk=" + risk +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, BigDecimal> performance;
        private Map<String, BigDecimal> allocation;
        private Map<String, BigDecimal> risk;

        public Builder withPerformance(Map<String, BigDecimal> performance) {
            this.performance = performance;
            return this;
        }

        public Builder withAllocation(Map<String, BigDecimal> allocation) {
            this.allocation = allocation;
            return this;
        }

        public Builder withRisk(Map<String, BigDecimal> risk) {
            this.risk = risk;
            return this;
        }

        public PerformanceMetrics build() {
            return new PerformanceMetrics(performance, allocation, risk);
        }
    }
}
