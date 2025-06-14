package io.strategiz.api.dashboard.model.assetallocation;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Response model for asset allocation data following Synapse patterns.
 */
public class AssetAllocationResponse extends BaseServiceResponse {
    

    /**
     * List of asset allocations for pie chart
     */
    private List<AssetAllocation> allocations;
    
    /**
     * Default constructor
     */
    public AssetAllocationResponse() {
    }
    
    /**
     * Gets the list of asset allocations
     * 
     * @return List of asset allocations
     */
    public List<AssetAllocation> getAllocations() {
        return allocations;
    }
    
    /**
     * Sets the list of asset allocations
     * 
     * @param allocations List of asset allocations
     */
    public void setAllocations(List<AssetAllocation> allocations) {
        this.allocations = allocations;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssetAllocationResponse that = (AssetAllocationResponse) o;
        return Objects.equals(allocations, that.allocations);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), allocations);
    }
    
    @Override
    public String toString() {
        return "AssetAllocationResponse{" +
                "allocations=" + allocations +
                "}";
    }
    
    /**
     * Asset allocation data
     */
    public static class AssetAllocation {
        /**
         * Asset name
         */
        private String name;
        
        /**
         * Asset symbol
         */
        private String symbol;
        
        /**
         * Value of asset holding in USD
         */
        private BigDecimal value;
        
        /**
         * Percentage of total portfolio value
         */
        private BigDecimal percentage;
        
        /**
         * Source exchange (Coinbase, Kraken, Binance, etc.)
         */
        private String exchange;
        
        /**
         * Color to use for this asset in charts
         */
        private String color;
        
        /**
         * Default constructor
         */
        public AssetAllocation() {
        }
        
        /**
         * Gets asset name
         *
         * @return Asset name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets asset name
         *
         * @param name Asset name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets asset symbol
         *
         * @return Asset symbol
         */
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * Sets asset symbol
         *
         * @param symbol Asset symbol
         */
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * Gets value of asset holding
         *
         * @return Value of asset holding
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets value of asset holding
         *
         * @param value Value of asset holding
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets percentage of total portfolio value
         *
         * @return Percentage of total portfolio value
         */
        public BigDecimal getPercentage() {
            return percentage;
        }
        
        /**
         * Sets percentage of total portfolio value
         *
         * @param percentage Percentage of total portfolio value
         */
        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
        
        /**
         * Gets source exchange
         *
         * @return Source exchange
         */
        public String getExchange() {
            return exchange;
        }
        
        /**
         * Sets source exchange
         *
         * @param exchange Source exchange
         */
        public void setExchange(String exchange) {
            this.exchange = exchange;
        }
        
        /**
         * Gets color for charts
         *
         * @return Color for charts
         */
        public String getColor() {
            return color;
        }
        
        /**
         * Sets color for charts
         *
         * @param color Color for charts
         */
        public void setColor(String color) {
            this.color = color;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetAllocation that = (AssetAllocation) o;
            return Objects.equals(name, that.name) && 
                   Objects.equals(symbol, that.symbol) && 
                   Objects.equals(value, that.value) && 
                   Objects.equals(percentage, that.percentage) && 
                   Objects.equals(exchange, that.exchange) && 
                   Objects.equals(color, that.color);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, symbol, value, percentage, exchange, color);
        }
        
        @Override
        public String toString() {
            return "AssetAllocation{" +
                   "name='" + name + '\'' +
                   ", symbol='" + symbol + '\'' +
                   ", value=" + value +
                   ", percentage=" + percentage +
                   ", exchange='" + exchange + '\'' +
                   ", color='" + color + '\'' +
                   "}";
        }
    }
}
