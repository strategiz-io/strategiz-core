package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Market data class for watchlist items (from DashboardService)
 */
public class DashboardMarketData {
    private String id;
    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private boolean positiveChange;

    // Constructors
    public DashboardMarketData() {}

    public DashboardMarketData(String id, String symbol, String name, BigDecimal price,
                              BigDecimal change, BigDecimal changePercent, boolean positiveChange) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.positiveChange = positiveChange;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChange() {
        return change;
    }

    public void setChange(BigDecimal change) {
        this.change = change;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public boolean isPositiveChange() {
        return positiveChange;
    }

    public void setPositiveChange(boolean positiveChange) {
        this.positiveChange = positiveChange;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardMarketData that = (DashboardMarketData) o;
        return positiveChange == that.positiveChange &&
               Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(price, that.price) &&
               Objects.equals(change, that.change) &&
               Objects.equals(changePercent, that.changePercent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, price, change, changePercent, positiveChange);
    }

    @Override
    public String toString() {
        return "DashboardMarketData{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", change=" + change +
               ", changePercent=" + changePercent +
               ", positiveChange=" + positiveChange +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
        private boolean positiveChange;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder withChange(BigDecimal change) {
            this.change = change;
            return this;
        }

        public Builder withChangePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
            return this;
        }

        public Builder withPositiveChange(boolean positiveChange) {
            this.positiveChange = positiveChange;
            return this;
        }

        public DashboardMarketData build() {
            return new DashboardMarketData(id, symbol, name, price, change, changePercent, positiveChange);
        }
    }
}
