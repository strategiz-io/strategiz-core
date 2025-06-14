package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Market data class for individual assets (stocks/crypto)
 */
public class AssetMarketData {
    private String id;
    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private boolean positiveChange;

    // Constructors
    public AssetMarketData() {}

    public AssetMarketData(String id, String symbol, String name, BigDecimal price, 
                         BigDecimal change, BigDecimal changePercent, boolean positiveChange) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.positiveChange = positiveChange;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetMarketData that = (AssetMarketData) o;
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
        return "AssetMarketData{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", change=" + change +
               ", changePercent=" + changePercent +
               ", positiveChange=" + positiveChange +
               '}';
    }
}
