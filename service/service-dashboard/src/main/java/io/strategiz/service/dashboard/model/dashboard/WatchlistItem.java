package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Watchlist item
 */
public class WatchlistItem {
    private String id;
    private String symbol;
    private String name;
    private String type;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;

    // Constructors
    public WatchlistItem() {}

    public WatchlistItem(String id, String symbol, String name, String type,
                        BigDecimal price, BigDecimal change, BigDecimal changePercent) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistItem that = (WatchlistItem) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(price, that.price) &&
               Objects.equals(change, that.change) &&
               Objects.equals(changePercent, that.changePercent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, type, price, change, changePercent);
    }

    @Override
    public String toString() {
        return "WatchlistItem{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", price=" + price +
               ", change=" + change +
               ", changePercent=" + changePercent +
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
        private String type;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;

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

        public Builder withType(String type) {
            this.type = type;
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

        public WatchlistItem build() {
            return new WatchlistItem(id, symbol, name, type, price, change, changePercent);
        }
    }
}
