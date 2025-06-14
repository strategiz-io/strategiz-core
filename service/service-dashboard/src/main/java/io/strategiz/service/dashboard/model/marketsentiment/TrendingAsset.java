package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data for trending assets in the market.
 */
public class TrendingAsset {
    private String id;
    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal changePercent24h;
    private BigDecimal marketCap;
    private BigDecimal volume24h;
    private int rank;

    // Constructors
    public TrendingAsset() {}

    public TrendingAsset(String id, String symbol, String name, BigDecimal price,
                       BigDecimal changePercent24h, BigDecimal marketCap, BigDecimal volume24h, int rank) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.changePercent24h = changePercent24h;
        this.marketCap = marketCap;
        this.volume24h = volume24h;
        this.rank = rank;
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

    public BigDecimal getChangePercent24h() {
        return changePercent24h;
    }

    public void setChangePercent24h(BigDecimal changePercent24h) {
        this.changePercent24h = changePercent24h;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public BigDecimal getVolume24h() {
        return volume24h;
    }

    public void setVolume24h(BigDecimal volume24h) {
        this.volume24h = volume24h;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrendingAsset that = (TrendingAsset) o;
        return rank == that.rank &&
               Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(price, that.price) &&
               Objects.equals(changePercent24h, that.changePercent24h) &&
               Objects.equals(marketCap, that.marketCap) &&
               Objects.equals(volume24h, that.volume24h);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, price, changePercent24h, marketCap, volume24h, rank);
    }

    @Override
    public String toString() {
        return "TrendingAsset{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", changePercent24h=" + changePercent24h +
               ", marketCap=" + marketCap +
               ", volume24h=" + volume24h +
               ", rank=" + rank +
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
        private BigDecimal changePercent24h;
        private BigDecimal marketCap;
        private BigDecimal volume24h;
        private int rank;

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

        public Builder withChangePercent24h(BigDecimal changePercent24h) {
            this.changePercent24h = changePercent24h;
            return this;
        }

        public Builder withMarketCap(BigDecimal marketCap) {
            this.marketCap = marketCap;
            return this;
        }

        public Builder withVolume24h(BigDecimal volume24h) {
            this.volume24h = volume24h;
            return this;
        }

        public Builder withRank(int rank) {
            this.rank = rank;
            return this;
        }

        public TrendingAsset build() {
            return new TrendingAsset(id, symbol, name, price, changePercent24h, marketCap, volume24h, rank);
        }
    }
}
