package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Asset data
 */
public class AssetData {
    private String symbol;
    private String name;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal value;
    private BigDecimal allocationPercent;

    // Constructors
    public AssetData() {}

    public AssetData(String symbol, String name, BigDecimal quantity, BigDecimal price,
                    BigDecimal value, BigDecimal allocationPercent) {
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.value = value;
        this.allocationPercent = allocationPercent;
    }

    // Getters and Setters
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getAllocationPercent() {
        return allocationPercent;
    }

    public void setAllocationPercent(BigDecimal allocationPercent) {
        this.allocationPercent = allocationPercent;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetData assetData = (AssetData) o;
        return Objects.equals(symbol, assetData.symbol) &&
               Objects.equals(name, assetData.name) &&
               Objects.equals(quantity, assetData.quantity) &&
               Objects.equals(price, assetData.price) &&
               Objects.equals(value, assetData.value) &&
               Objects.equals(allocationPercent, assetData.allocationPercent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, name, quantity, price, value, allocationPercent);
    }

    @Override
    public String toString() {
        return "AssetData{" +
               "symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", quantity=" + quantity +
               ", price=" + price +
               ", value=" + value +
               ", allocationPercent=" + allocationPercent +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;

        public Builder withSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withQuantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder withValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder withAllocationPercent(BigDecimal allocationPercent) {
            this.allocationPercent = allocationPercent;
            return this;
        }

        public AssetData build() {
            return new AssetData(symbol, name, quantity, price, value, allocationPercent);
        }
    }
}
