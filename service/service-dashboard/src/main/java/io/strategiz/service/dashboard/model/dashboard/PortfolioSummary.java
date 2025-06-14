package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Portfolio summary data
 */
public class PortfolioSummary {
    private BigDecimal totalValue;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    private Map<String, ExchangeData> exchanges;

    // Constructors
    public PortfolioSummary() {}

    public PortfolioSummary(BigDecimal totalValue, BigDecimal dailyChange, 
                           BigDecimal dailyChangePercent, Map<String, ExchangeData> exchanges) {
        this.totalValue = totalValue;
        this.dailyChange = dailyChange;
        this.dailyChangePercent = dailyChangePercent;
        this.exchanges = exchanges;
    }

    // Getters and Setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getDailyChange() {
        return dailyChange;
    }

    public void setDailyChange(BigDecimal dailyChange) {
        this.dailyChange = dailyChange;
    }

    public BigDecimal getDailyChangePercent() {
        return dailyChangePercent;
    }

    public void setDailyChangePercent(BigDecimal dailyChangePercent) {
        this.dailyChangePercent = dailyChangePercent;
    }

    public Map<String, ExchangeData> getExchanges() {
        return exchanges;
    }

    public void setExchanges(Map<String, ExchangeData> exchanges) {
        this.exchanges = exchanges;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioSummary that = (PortfolioSummary) o;
        return Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(dailyChange, that.dailyChange) &&
               Objects.equals(dailyChangePercent, that.dailyChangePercent) &&
               Objects.equals(exchanges, that.exchanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalValue, dailyChange, dailyChangePercent, exchanges);
    }

    @Override
    public String toString() {
        return "PortfolioSummary{" +
               "totalValue=" + totalValue +
               ", dailyChange=" + dailyChange +
               ", dailyChangePercent=" + dailyChangePercent +
               ", exchanges=" + exchanges +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal totalValue;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercent;
        private Map<String, ExchangeData> exchanges;

        public Builder withTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }

        public Builder withDailyChange(BigDecimal dailyChange) {
            this.dailyChange = dailyChange;
            return this;
        }

        public Builder withDailyChangePercent(BigDecimal dailyChangePercent) {
            this.dailyChangePercent = dailyChangePercent;
            return this;
        }

        public Builder withExchanges(Map<String, ExchangeData> exchanges) {
            this.exchanges = exchanges;
            return this;
        }

        public PortfolioSummary build() {
            return new PortfolioSummary(totalValue, dailyChange, dailyChangePercent, exchanges);
        }
    }
}
