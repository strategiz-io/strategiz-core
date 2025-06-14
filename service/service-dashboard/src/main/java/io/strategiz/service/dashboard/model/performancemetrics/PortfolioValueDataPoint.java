package io.strategiz.service.dashboard.model.performancemetrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data point for historical portfolio value
 */
public class PortfolioValueDataPoint {
    /**
     * Timestamp of the data point
     */
    private LocalDateTime timestamp;
    
    /**
     * Portfolio total value at this timestamp
     */
    private BigDecimal value;

    // Constructors
    public PortfolioValueDataPoint() {}

    public PortfolioValueDataPoint(LocalDateTime timestamp, BigDecimal value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioValueDataPoint that = (PortfolioValueDataPoint) o;
        return Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }

    @Override
    public String toString() {
        return "PortfolioValueDataPoint{" +
               "timestamp=" + timestamp +
               ", value=" + value +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private BigDecimal value;

        public Builder withTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public PortfolioValueDataPoint build() {
            return new PortfolioValueDataPoint(timestamp, value);
        }
    }
}
