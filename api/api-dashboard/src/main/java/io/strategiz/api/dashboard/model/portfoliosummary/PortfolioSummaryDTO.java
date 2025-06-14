package io.strategiz.api.dashboard.model.portfoliosummary;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.strategiz.service.base.model.BaseServiceResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DTO model for portfolio summary data following Synapse patterns.
 * Includes validation annotations and serialization configuration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioSummaryDTO extends BaseServiceResponse {
    
    /**
     * User ID associated with this portfolio summary
     */
    @NotBlank(message = "User ID is required")
    private String userId;
    
    /**
     * Total portfolio value
     */
    @NotNull(message = "Total value is required")
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
     * Weekly change in portfolio value
     */
    private BigDecimal weeklyChange;
    
    /**
     * Weekly change percentage
     */
    private BigDecimal weeklyChangePercent;
    
    /**
     * Monthly change in portfolio value
     */
    private BigDecimal monthlyChange;
    
    /**
     * Monthly change percentage
     */
    private BigDecimal monthlyChangePercent;
    
    /**
     * Yearly change in portfolio value
     */
    private BigDecimal yearlyChange;
    
    /**
     * Yearly change percentage
     */
    private BigDecimal yearlyChangePercent;
    
    /**
     * List of assets in the portfolio
     */
    @Valid
    private List<AssetDTO> assets;
    
    /**
     * Map of exchange ID to exchange data
     */
    private Map<String, ExchangeDataDTO> exchanges;
    
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
     * Timestamp of last update
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    /**
     * Default constructor
     */
    public PortfolioSummaryDTO() {
    }
    
    /**
     * Gets the user ID
     *
     * @return User ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Sets the user ID
     *
     * @param userId User ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
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
     * Gets the weekly change in portfolio value
     *
     * @return Weekly change in portfolio value
     */
    public BigDecimal getWeeklyChange() {
        return weeklyChange;
    }
    
    /**
     * Sets the weekly change in portfolio value
     *
     * @param weeklyChange Weekly change in portfolio value
     */
    public void setWeeklyChange(BigDecimal weeklyChange) {
        this.weeklyChange = weeklyChange;
    }
    
    /**
     * Gets the weekly change percentage
     *
     * @return Weekly change percentage
     */
    public BigDecimal getWeeklyChangePercent() {
        return weeklyChangePercent;
    }
    
    /**
     * Sets the weekly change percentage
     *
     * @param weeklyChangePercent Weekly change percentage
     */
    public void setWeeklyChangePercent(BigDecimal weeklyChangePercent) {
        this.weeklyChangePercent = weeklyChangePercent;
    }
    
    /**
     * Gets the monthly change in portfolio value
     *
     * @return Monthly change in portfolio value
     */
    public BigDecimal getMonthlyChange() {
        return monthlyChange;
    }
    
    /**
     * Sets the monthly change in portfolio value
     *
     * @param monthlyChange Monthly change in portfolio value
     */
    public void setMonthlyChange(BigDecimal monthlyChange) {
        this.monthlyChange = monthlyChange;
    }
    
    /**
     * Gets the monthly change percentage
     *
     * @return Monthly change percentage
     */
    public BigDecimal getMonthlyChangePercent() {
        return monthlyChangePercent;
    }
    
    /**
     * Sets the monthly change percentage
     *
     * @param monthlyChangePercent Monthly change percentage
     */
    public void setMonthlyChangePercent(BigDecimal monthlyChangePercent) {
        this.monthlyChangePercent = monthlyChangePercent;
    }
    
    /**
     * Gets the yearly change in portfolio value
     *
     * @return Yearly change in portfolio value
     */
    public BigDecimal getYearlyChange() {
        return yearlyChange;
    }
    
    /**
     * Sets the yearly change in portfolio value
     *
     * @param yearlyChange Yearly change in portfolio value
     */
    public void setYearlyChange(BigDecimal yearlyChange) {
        this.yearlyChange = yearlyChange;
    }
    
    /**
     * Gets the yearly change percentage
     *
     * @return Yearly change percentage
     */
    public BigDecimal getYearlyChangePercent() {
        return yearlyChangePercent;
    }
    
    /**
     * Sets the yearly change percentage
     *
     * @param yearlyChangePercent Yearly change percentage
     */
    public void setYearlyChangePercent(BigDecimal yearlyChangePercent) {
        this.yearlyChangePercent = yearlyChangePercent;
    }
    
    /**
     * Gets the list of assets in the portfolio
     *
     * @return List of assets
     */
    public List<AssetDTO> getAssets() {
        return assets;
    }
    
    /**
     * Sets the list of assets in the portfolio
     *
     * @param assets List of assets
     */
    public void setAssets(List<AssetDTO> assets) {
        this.assets = assets;
    }
    
    /**
     * Gets the exchanges
     *
     * @return Map of exchange ID to exchange data
     */
    public Map<String, ExchangeDataDTO> getExchanges() {
        return exchanges;
    }
    
    /**
     * Sets the exchanges
     *
     * @param exchanges Map of exchange ID to exchange data
     */
    public void setExchanges(Map<String, ExchangeDataDTO> exchanges) {
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
    
    /**
     * Gets the timestamp of last update
     *
     * @return Timestamp of last update
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Sets the timestamp of last update
     *
     * @param lastUpdated Timestamp of last update
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PortfolioSummaryDTO that = (PortfolioSummaryDTO) o;
        return hasExchangeConnections == that.hasExchangeConnections &&
               needsApiKeyConfiguration == that.needsApiKeyConfiguration &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(dailyChange, that.dailyChange) &&
               Objects.equals(dailyChangePercent, that.dailyChangePercent) &&
               Objects.equals(weeklyChange, that.weeklyChange) &&
               Objects.equals(weeklyChangePercent, that.weeklyChangePercent) &&
               Objects.equals(monthlyChange, that.monthlyChange) &&
               Objects.equals(monthlyChangePercent, that.monthlyChangePercent) &&
               Objects.equals(yearlyChange, that.yearlyChange) &&
               Objects.equals(yearlyChangePercent, that.yearlyChangePercent) &&
               Objects.equals(assets, that.assets) &&
               Objects.equals(exchanges, that.exchanges) &&
               Objects.equals(statusMessage, that.statusMessage) &&
               Objects.equals(lastUpdated, that.lastUpdated);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, totalValue, dailyChange, dailyChangePercent,
                weeklyChange, weeklyChangePercent, monthlyChange, monthlyChangePercent,
                yearlyChange, yearlyChangePercent, assets, exchanges, hasExchangeConnections,
                statusMessage, needsApiKeyConfiguration, lastUpdated);
    }
    
    @Override
    public String toString() {
        return "PortfolioSummaryDTO{" +
               "userId='" + userId + '\'' +
               ", totalValue=" + totalValue +
               ", dailyChange=" + dailyChange +
               ", dailyChangePercent=" + dailyChangePercent +
               ", weeklyChange=" + weeklyChange +
               ", weeklyChangePercent=" + weeklyChangePercent +
               ", monthlyChange=" + monthlyChange +
               ", monthlyChangePercent=" + monthlyChangePercent +
               ", yearlyChange=" + yearlyChange +
               ", yearlyChangePercent=" + yearlyChangePercent +
               ", assets=" + assets +
               ", exchanges=" + exchanges +
               ", hasExchangeConnections=" + hasExchangeConnections +
               ", statusMessage='" + statusMessage + '\'' +
               ", needsApiKeyConfiguration=" + needsApiKeyConfiguration +
               ", lastUpdated=" + lastUpdated +
               ", " + super.toString() +
               "}";
    }
    
    /**
     * DTO for individual asset in a portfolio.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssetDTO {
        /**
         * Asset identifier
         */
        @NotBlank(message = "Asset ID is required")
        private String id;
        
        /**
         * Asset ticker symbol
         */
        @NotBlank(message = "Symbol is required")
        private String symbol;
        
        /**
         * Asset name
         */
        private String name;
        
        /**
         * Asset type (crypto, stock, etc.)
         */
        private String type;
        
        /**
         * Quantity owned
         */
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        
        /**
         * Current price
         */
        private BigDecimal price;
        
        /**
         * Total value (price * quantity)
         */
        private BigDecimal value;
        
        /**
         * Percentage allocation in portfolio
         */
        private BigDecimal allocation;
        
        /**
         * Daily change in value
         */
        private BigDecimal dailyChange;
        
        /**
         * Daily change percentage
         */
        private BigDecimal dailyChangePercent;
        
        /**
         * Default constructor
         */
        public AssetDTO() {
        }
        
        /**
         * Gets the asset identifier
         *
         * @return Asset identifier
         */
        public String getId() {
            return id;
        }
        
        /**
         * Sets the asset identifier
         *
         * @param id Asset identifier
         */
        public void setId(String id) {
            this.id = id;
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
         * Gets the asset type
         *
         * @return Asset type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Sets the asset type
         *
         * @param type Asset type
         */
        public void setType(String type) {
            this.type = type;
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
        public BigDecimal getAllocation() {
            return allocation;
        }
        
        /**
         * Sets the allocation percentage
         *
         * @param allocation Allocation percentage
         */
        public void setAllocation(BigDecimal allocation) {
            this.allocation = allocation;
        }
        
        /**
         * Gets the daily change in value
         *
         * @return Daily change in value
         */
        public BigDecimal getDailyChange() {
            return dailyChange;
        }
        
        /**
         * Sets the daily change in value
         *
         * @param dailyChange Daily change in value
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
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetDTO assetDTO = (AssetDTO) o;
            return Objects.equals(id, assetDTO.id) &&
                   Objects.equals(symbol, assetDTO.symbol) &&
                   Objects.equals(name, assetDTO.name) &&
                   Objects.equals(type, assetDTO.type) &&
                   Objects.equals(quantity, assetDTO.quantity) &&
                   Objects.equals(price, assetDTO.price) &&
                   Objects.equals(value, assetDTO.value) &&
                   Objects.equals(allocation, assetDTO.allocation) &&
                   Objects.equals(dailyChange, assetDTO.dailyChange) &&
                   Objects.equals(dailyChangePercent, assetDTO.dailyChangePercent);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id, symbol, name, type, quantity, price, value, 
                    allocation, dailyChange, dailyChangePercent);
        }
        
        @Override
        public String toString() {
            return "AssetDTO{" +
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
                   "}";
        }
    }
    
    /**
     * DTO for exchange data
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExchangeDataDTO {
        /**
         * Exchange name
         */
        @NotBlank(message = "Exchange name is required")
        private String name;
        
        /**
         * Total value in this exchange
         */
        private BigDecimal value;
        
        /**
         * Exchange-specific metadata
         */
        private Map<String, String> metadata;
        
        /**
         * Default constructor
         */
        public ExchangeDataDTO() {
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
         * Gets the total value in this exchange
         *
         * @return Total value in this exchange
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets the total value in this exchange
         *
         * @param value Total value in this exchange
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets the exchange-specific metadata
         *
         * @return Exchange-specific metadata
         */
        public Map<String, String> getMetadata() {
            return metadata;
        }
        
        /**
         * Sets the exchange-specific metadata
         *
         * @param metadata Exchange-specific metadata
         */
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExchangeDataDTO that = (ExchangeDataDTO) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(metadata, that.metadata);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, value, metadata);
        }
        
        @Override
        public String toString() {
            return "ExchangeDataDTO{" +
                   "name='" + name + '\'' +
                   ", value=" + value +
                   ", metadata=" + metadata +
                   "}";
        }
    }
}
