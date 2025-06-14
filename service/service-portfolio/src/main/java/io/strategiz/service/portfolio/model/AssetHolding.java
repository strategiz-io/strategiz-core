package io.strategiz.service.portfolio.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a holding of a financial asset in a portfolio.
 */
public class AssetHolding {
    private String symbol;
    private String name;
    private String type; // crypto, stock, etc.
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal value;
    private String currency;
    private BigDecimal costBasis;
    private Map<String, Object> additionalData;

    public AssetHolding() {
    }

    public AssetHolding(String symbol, String name, String type, BigDecimal quantity, BigDecimal price, 
                       BigDecimal value, String currency, BigDecimal costBasis, Map<String, Object> additionalData) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.value = value;
        this.currency = currency;
        this.costBasis = costBasis;
        this.additionalData = additionalData;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetHolding that = (AssetHolding) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(quantity, that.quantity) &&
               Objects.equals(price, that.price) &&
               Objects.equals(value, that.value) &&
               Objects.equals(currency, that.currency) &&
               Objects.equals(costBasis, that.costBasis) &&
               Objects.equals(additionalData, that.additionalData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, name, type, quantity, price, value, currency, costBasis, additionalData);
    }

    @Override
    public String toString() {
        return "AssetHolding{" +
               "symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", quantity=" + quantity +
               ", price=" + price +
               ", value=" + value +
               ", currency='" + currency + '\'' +
               ", costBasis=" + costBasis +
               ", additionalData=" + additionalData +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol;
        private String name;
        private String type;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private String currency;
        private BigDecimal costBasis;
        private Map<String, Object> additionalData;

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder costBasis(BigDecimal costBasis) {
            this.costBasis = costBasis;
            return this;
        }

        public Builder additionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public AssetHolding build() {
            return new AssetHolding(symbol, name, type, quantity, price, value, currency, costBasis, additionalData);
        }
    }
}
