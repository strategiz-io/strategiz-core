package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Market trends data.
 */
public class MarketTrends {
    private String direction; // "up", "down", "sideways"
    private BigDecimal momentum;
    private List<String> supportLevels;
    private List<String> resistanceLevels;
    private String trendStrength;
    private BigDecimal volatility;

    // Constructors
    public MarketTrends() {}

    public MarketTrends(String direction, BigDecimal momentum, List<String> supportLevels,
                       List<String> resistanceLevels, String trendStrength, BigDecimal volatility) {
        this.direction = direction;
        this.momentum = momentum;
        this.supportLevels = supportLevels;
        this.resistanceLevels = resistanceLevels;
        this.trendStrength = trendStrength;
        this.volatility = volatility;
    }

    // Getters and Setters
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getMomentum() {
        return momentum;
    }

    public void setMomentum(BigDecimal momentum) {
        this.momentum = momentum;
    }

    public List<String> getSupportLevels() {
        return supportLevels;
    }

    public void setSupportLevels(List<String> supportLevels) {
        this.supportLevels = supportLevels;
    }

    public List<String> getResistanceLevels() {
        return resistanceLevels;
    }

    public void setResistanceLevels(List<String> resistanceLevels) {
        this.resistanceLevels = resistanceLevels;
    }

    public String getTrendStrength() {
        return trendStrength;
    }

    public void setTrendStrength(String trendStrength) {
        this.trendStrength = trendStrength;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public void setVolatility(BigDecimal volatility) {
        this.volatility = volatility;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketTrends that = (MarketTrends) o;
        return Objects.equals(direction, that.direction) &&
               Objects.equals(momentum, that.momentum) &&
               Objects.equals(supportLevels, that.supportLevels) &&
               Objects.equals(resistanceLevels, that.resistanceLevels) &&
               Objects.equals(trendStrength, that.trendStrength) &&
               Objects.equals(volatility, that.volatility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, momentum, supportLevels, resistanceLevels, trendStrength, volatility);
    }

    @Override
    public String toString() {
        return "MarketTrends{" +
               "direction='" + direction + '\'' +
               ", momentum=" + momentum +
               ", supportLevels=" + supportLevels +
               ", resistanceLevels=" + resistanceLevels +
               ", trendStrength='" + trendStrength + '\'' +
               ", volatility=" + volatility +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String direction;
        private BigDecimal momentum;
        private List<String> supportLevels;
        private List<String> resistanceLevels;
        private String trendStrength;
        private BigDecimal volatility;

        public Builder withDirection(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder withMomentum(BigDecimal momentum) {
            this.momentum = momentum;
            return this;
        }

        public Builder withSupportLevels(List<String> supportLevels) {
            this.supportLevels = supportLevels;
            return this;
        }

        public Builder withResistanceLevels(List<String> resistanceLevels) {
            this.resistanceLevels = resistanceLevels;
            return this;
        }

        public Builder withTrendStrength(String trendStrength) {
            this.trendStrength = trendStrength;
            return this;
        }

        public Builder withVolatility(BigDecimal volatility) {
            this.volatility = volatility;
            return this;
        }

        public MarketTrends build() {
            return new MarketTrends(direction, momentum, supportLevels, resistanceLevels, trendStrength, volatility);
        }
    }
}
