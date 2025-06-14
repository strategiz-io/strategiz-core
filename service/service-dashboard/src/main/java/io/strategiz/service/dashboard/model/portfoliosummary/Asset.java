package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Individual asset in a portfolio.
 */
public class Asset {
    private String id;
    private String symbol;
    private String name;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal value;
    private BigDecimal allocation;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    
    /**
     * Default constructor
     */
    public Asset() {
    }
    
    /**
     * All-args constructor
     */
    public Asset(String id, String symbol, String name, String type, BigDecimal quantity,
                BigDecimal price, BigDecimal value, BigDecimal allocation, BigDecimal dailyChange,
                BigDecimal dailyChangePercent) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.value = value;
        this.allocation = allocation;
        this.dailyChange = dailyChange;
        this.dailyChangePercent = dailyChangePercent;
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
    
    public BigDecimal getAllocation() {
        return allocation;
    }
    
    public void setAllocation(BigDecimal allocation) {
        this.allocation = allocation;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return Objects.equals(id, asset.id) &&
                Objects.equals(symbol, asset.symbol) &&
                Objects.equals(name, asset.name) &&
                Objects.equals(type, asset.type) &&
                Objects.equals(quantity, asset.quantity) &&
                Objects.equals(price, asset.price) &&
                Objects.equals(value, asset.value) &&
                Objects.equals(allocation, asset.allocation) &&
                Objects.equals(dailyChange, asset.dailyChange) &&
                Objects.equals(dailyChangePercent, asset.dailyChangePercent);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, type, quantity, price, value, allocation, dailyChange, dailyChangePercent);
    }
    
    @Override
    public String toString() {
        return "Asset{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", value=" + value +
                ", allocation=" + allocation +
                ", dailyChange=" + dailyChange +
                ", dailyChangePercent=" + dailyChangePercent +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String symbol;
        private String name;
        private String type;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocation;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercent;
        
        public Builder withId(String id) { this.id = id; return this; }
        public Builder withSymbol(String symbol) { this.symbol = symbol; return this; }
        public Builder withName(String name) { this.name = name; return this; }
        public Builder withType(String type) { this.type = type; return this; }
        public Builder withQuantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder withPrice(BigDecimal price) { this.price = price; return this; }
        public Builder withValue(BigDecimal value) { this.value = value; return this; }
        public Builder withAllocation(BigDecimal allocation) { this.allocation = allocation; return this; }
        public Builder withDailyChange(BigDecimal dailyChange) { this.dailyChange = dailyChange; return this; }
        public Builder withDailyChangePercent(BigDecimal dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; return this; }
        
        public Asset build() {
            return new Asset(id, symbol, name, type, quantity, price, value, allocation, dailyChange, dailyChangePercent);
        }
    }
}
