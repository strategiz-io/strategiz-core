package io.strategiz.api.dashboard.model.portfoliosummary;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Response model for portfolio summary data following Synapse patterns.
 * Designed to support the Portfolio Summary UI component.
 */
public class PortfolioSummaryResponse extends BaseServiceResponse {
    
    /**
     * Total portfolio value
     */
    private BigDecimal totalValue;
    
    /**
     * Daily change in portfolio value
     */
    private BigDecimal dailyChange;
    
    /**
     * Daily change percentage
     */
    private BigDecimal dailyChangePercent;
    
    /**
     * Map of exchange ID to exchange data
     */
    private Map<String, ExchangeData> exchanges;
    
    /**
     * Flag indicating if any exchange connections exist
     */
    private boolean hasExchangeConnections;
    
    /**
     * Message to display when no exchange connections are found
     */
    private String statusMessage;
    
    /**
     * Flag indicating if the user needs to configure API keys
     */
    private boolean needsApiKeyConfiguration;
    
    /**
     * Default constructor
     */
    public PortfolioSummaryResponse() {
    }
    
    /**
     * Gets the total portfolio value
     * 
     * @return Total portfolio value
     */
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    /**
     * Sets the total portfolio value
     * 
     * @param totalValue Total portfolio value
     */
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    /**
     * Gets the daily change in portfolio value
     * 
     * @return Daily change in portfolio value
     */
    public BigDecimal getDailyChange() {
        return dailyChange;
    }
    
    /**
     * Sets the daily change in portfolio value
     * 
     * @param dailyChange Daily change in portfolio value
     */
    public void setDailyChange(BigDecimal dailyChange) {
        this.dailyChange = dailyChange;
    }
    
    /**
     * Gets the daily change percentage
     * 
     * @return Daily change percentage
     */
    public BigDecimal getDailyChangePercent() {
        return dailyChangePercent;
    }
    
    /**
     * Sets the daily change percentage
     * 
     * @param dailyChangePercent Daily change percentage
     */
    public void setDailyChangePercent(BigDecimal dailyChangePercent) {
        this.dailyChangePercent = dailyChangePercent;
    }
    
    /**
     * Gets the exchanges
     * 
     * @return Map of exchange ID to exchange data
     */
    public Map<String, ExchangeData> getExchanges() {
        return exchanges;
    }
    
    /**
     * Sets the exchanges
     * 
     * @param exchanges Map of exchange ID to exchange data
     */
    public void setExchanges(Map<String, ExchangeData> exchanges) {
        this.exchanges = exchanges;
    }
    
    /**
     * Checks if exchange connections exist
     * 
     * @return True if exchange connections exist, false otherwise
     */
    public boolean isHasExchangeConnections() {
        return hasExchangeConnections;
    }
    
    /**
     * Sets whether exchange connections exist
     * 
     * @param hasExchangeConnections True if exchange connections exist, false otherwise
     */
    public void setHasExchangeConnections(boolean hasExchangeConnections) {
        this.hasExchangeConnections = hasExchangeConnections;
    }
    
    /**
     * Gets the status message
     * 
     * @return Status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * Sets the status message
     * 
     * @param statusMessage Status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    /**
     * Checks if API key configuration is needed
     * 
     * @return True if API key configuration is needed, false otherwise
     */
    public boolean isNeedsApiKeyConfiguration() {
        return needsApiKeyConfiguration;
    }
    
    /**
     * Sets whether API key configuration is needed
     * 
     * @param needsApiKeyConfiguration True if API key configuration is needed, false otherwise
     */
    public void setNeedsApiKeyConfiguration(boolean needsApiKeyConfiguration) {
        this.needsApiKeyConfiguration = needsApiKeyConfiguration;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PortfolioSummaryResponse that = (PortfolioSummaryResponse) o;
        return hasExchangeConnections == that.hasExchangeConnections &&
               needsApiKeyConfiguration == that.needsApiKeyConfiguration &&
               Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(dailyChange, that.dailyChange) &&
               Objects.equals(dailyChangePercent, that.dailyChangePercent) &&
               Objects.equals(exchanges, that.exchanges) &&
               Objects.equals(statusMessage, that.statusMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), totalValue, dailyChange, dailyChangePercent,
                exchanges, hasExchangeConnections, statusMessage, needsApiKeyConfiguration);
    }
    
    @Override
    public String toString() {
        return "PortfolioSummaryResponse{" +
               "totalValue=" + totalValue +
               ", dailyChange=" + dailyChange +
               ", dailyChangePercent=" + dailyChangePercent +
               ", exchanges=" + exchanges +
               ", hasExchangeConnections=" + hasExchangeConnections +
               ", statusMessage='" + statusMessage + '\'' +
               ", needsApiKeyConfiguration=" + needsApiKeyConfiguration +
               ", " + super.toString() +
               "}";
    }
    
    /**
     * Exchange data
     */
    public static class ExchangeData {
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
        
        /**
         * Default constructor
         */
        public ExchangeData() {
        }
        
        /**
         * Gets the exchange name
         * 
         * @return Exchange name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets the exchange name
         * 
         * @param name Exchange name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets the exchange value
         * 
         * @return Exchange value
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets the exchange value
         * 
         * @param value Exchange value
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets the assets
         * 
         * @return Map of asset symbol to asset data
         */
        public Map<String, AssetData> getAssets() {
            return assets;
        }
        
        /**
         * Sets the assets
         * 
         * @param assets Map of asset symbol to asset data
         */
        public void setAssets(Map<String, AssetData> assets) {
            this.assets = assets;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExchangeData that = (ExchangeData) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(assets, that.assets);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, value, assets);
        }
        
        @Override
        public String toString() {
            return "ExchangeData{" +
                   "name='" + name + '\'' +
                   ", value=" + value +
                   ", assets=" + assets +
                   "}";
        }
    }
    
    /**
     * Asset data
     */
    public static class AssetData {
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;
        
        /**
         * Default constructor
         */
        public AssetData() {
        }
        
        /**
         * Gets the asset symbol
         * 
         * @return Asset symbol
         */
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * Sets the asset symbol
         * 
         * @param symbol Asset symbol
         */
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * Gets the asset name
         * 
         * @return Asset name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets the asset name
         * 
         * @param name Asset name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets the asset quantity
         * 
         * @return Asset quantity
         */
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        /**
         * Sets the asset quantity
         * 
         * @param quantity Asset quantity
         */
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        /**
         * Gets the asset price
         * 
         * @return Asset price
         */
        public BigDecimal getPrice() {
            return price;
        }
        
        /**
         * Sets the asset price
         * 
         * @param price Asset price
         */
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        /**
         * Gets the asset value
         * 
         * @return Asset value
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets the asset value
         * 
         * @param value Asset value
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets the allocation percentage
         * 
         * @return Allocation percentage
         */
        public BigDecimal getAllocationPercent() {
            return allocationPercent;
        }
        
        /**
         * Sets the allocation percentage
         * 
         * @param allocationPercent Allocation percentage
         */
        public void setAllocationPercent(BigDecimal allocationPercent) {
            this.allocationPercent = allocationPercent;
        }
        
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
                   "}";
        }
    }
}
