package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Market data
 */
public class MarketData {
    private Map<String, BigDecimal> indexes;
    private Map<String, BigDecimal> trends;

    // Constructors
    public MarketData() {}

    public MarketData(Map<String, BigDecimal> indexes, Map<String, BigDecimal> trends) {
        this.indexes = indexes;
        this.trends = trends;
    }

    // Getters and Setters
    public Map<String, BigDecimal> getIndexes() {
        return indexes;
    }

    public void setIndexes(Map<String, BigDecimal> indexes) {
        this.indexes = indexes;
    }

    public Map<String, BigDecimal> getTrends() {
        return trends;
    }

    public void setTrends(Map<String, BigDecimal> trends) {
        this.trends = trends;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketData that = (MarketData) o;
        return Objects.equals(indexes, that.indexes) &&
               Objects.equals(trends, that.trends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexes, trends);
    }

    @Override
    public String toString() {
        return "MarketData{" +
               "indexes=" + indexes +
               ", trends=" + trends +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, BigDecimal> indexes;
        private Map<String, BigDecimal> trends;

        public Builder withIndexes(Map<String, BigDecimal> indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder withTrends(Map<String, BigDecimal> trends) {
            this.trends = trends;
            return this;
        }

        public MarketData build() {
            return new MarketData(indexes, trends);
        }
    }
}
