package io.strategiz.service.dashboard.model.dashboard;

import java.util.Objects;

/**
 * Asset data class for watchlist
 */
public class WatchlistAsset {
    private String id;
    private String symbol;
    private String name;
    private String category;

    // Constructors
    public WatchlistAsset() {}

    public WatchlistAsset(String id, String symbol, String name, String category) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.category = category;
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
        WatchlistAsset that = (WatchlistAsset) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, category);
    }

    @Override
    public String toString() {
        return "WatchlistAsset{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
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
        private String category;

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

        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        public WatchlistAsset build() {
            return new WatchlistAsset(id, symbol, name, category);
        }
    }
}
