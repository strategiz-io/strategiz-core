package io.strategiz.api.dashboard.model.dashboard;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response model for dashboard data following Synapse patterns.
 */
public class DashboardResponse extends BaseServiceResponse {
    
    /**
     * Portfolio summary data
     */
    private PortfolioSummary portfolio;
    
    /**
     * Market data
     */
    private MarketData market;
    
    /**
     * Watchlist data
     */
    private List<WatchlistItem> watchlist;
    
    /**
     * Portfolio performance metrics
     */
    private PerformanceMetrics metrics;
    
    /**
     * Default constructor
     */
    public DashboardResponse() {
    }
    
    /**
     * Gets portfolio summary data
     * 
     * @return Portfolio summary data
     */
    public PortfolioSummary getPortfolio() {
        return portfolio;
    }
    
    /**
     * Sets portfolio summary data
     * 
     * @param portfolio Portfolio summary data
     */
    public void setPortfolio(PortfolioSummary portfolio) {
        this.portfolio = portfolio;
    }
    
    /**
     * Gets market data
     * 
     * @return Market data
     */
    public MarketData getMarket() {
        return market;
    }
    
    /**
     * Sets market data
     * 
     * @param market Market data
     */
    public void setMarket(MarketData market) {
        this.market = market;
    }
    
    /**
     * Gets watchlist data
     * 
     * @return Watchlist data
     */
    public List<WatchlistItem> getWatchlist() {
        return watchlist;
    }
    
    /**
     * Sets watchlist data
     * 
     * @param watchlist Watchlist data
     */
    public void setWatchlist(List<WatchlistItem> watchlist) {
        this.watchlist = watchlist;
    }
    
    /**
     * Gets portfolio performance metrics
     * 
     * @return Portfolio performance metrics
     */
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Sets portfolio performance metrics
     * 
     * @param metrics Portfolio performance metrics
     */
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DashboardResponse that = (DashboardResponse) o;
        return Objects.equals(portfolio, that.portfolio) &&
               Objects.equals(market, that.market) &&
               Objects.equals(watchlist, that.watchlist) &&
               Objects.equals(metrics, that.metrics);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), portfolio, market, watchlist, metrics);
    }
    
    @Override
    public String toString() {
        return "DashboardResponse{" +
                "portfolio=" + portfolio +
                ", market=" + market +
                ", watchlist=" + watchlist +
                ", metrics=" + metrics +
                "}";
    }
    
    /**
     * Portfolio summary data
     */
    public static class PortfolioSummary {
        private BigDecimal totalValue;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercent;
        private Map<String, ExchangeData> exchanges;
        
        /**
         * Default constructor
         */
        public PortfolioSummary() {
        }
        
        /**
         * Gets total portfolio value
         * 
         * @return Total portfolio value
         */
        public BigDecimal getTotalValue() {
            return totalValue;
        }
        
        /**
         * Sets total portfolio value
         * 
         * @param totalValue Total portfolio value
         */
        public void setTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
        }
        
        /**
         * Gets daily change value
         * 
         * @return Daily change value
         */
        public BigDecimal getDailyChange() {
            return dailyChange;
        }
        
        /**
         * Sets daily change value
         * 
         * @param dailyChange Daily change value
         */
        public void setDailyChange(BigDecimal dailyChange) {
            this.dailyChange = dailyChange;
        }
        
        /**
         * Gets daily change percentage
         * 
         * @return Daily change percentage
         */
        public BigDecimal getDailyChangePercent() {
            return dailyChangePercent;
        }
        
        /**
         * Sets daily change percentage
         * 
         * @param dailyChangePercent Daily change percentage
         */
        public void setDailyChangePercent(BigDecimal dailyChangePercent) {
            this.dailyChangePercent = dailyChangePercent;
        }
        
        /**
         * Gets map of exchange data
         * 
         * @return Map of exchange data
         */
        public Map<String, ExchangeData> getExchanges() {
            return exchanges;
        }
        
        /**
         * Sets map of exchange data
         * 
         * @param exchanges Map of exchange data
         */
        public void setExchanges(Map<String, ExchangeData> exchanges) {
            this.exchanges = exchanges;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortfolioSummary that = (PortfolioSummary) o;
            return Objects.equals(totalValue, that.totalValue) &&
                   Objects.equals(dailyChange, that.dailyChange) &&
                   Objects.equals(dailyChangePercent, that.dailyChangePercent) &&
                   Objects.equals(exchanges, that.exchanges);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(totalValue, dailyChange, dailyChangePercent, exchanges);
        }
        
        @Override
        public String toString() {
            return "PortfolioSummary{" +
                   "totalValue=" + totalValue +
                   ", dailyChange=" + dailyChange +
                   ", dailyChangePercent=" + dailyChangePercent +
                   ", exchanges=" + exchanges +
                   "}";
        }
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
         * Gets exchange name
         * 
         * @return Exchange name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets exchange name
         * 
         * @param name Exchange name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets exchange value
         * 
         * @return Exchange value
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets exchange value
         * 
         * @param value Exchange value
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets map of assets
         * 
         * @return Map of assets
         */
        public Map<String, AssetData> getAssets() {
            return assets;
        }
        
        /**
         * Sets map of assets
         * 
         * @param assets Map of assets
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
         * Gets asset quantity
         * 
         * @return Asset quantity
         */
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        /**
         * Sets asset quantity
         * 
         * @param quantity Asset quantity
         */
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        /**
         * Gets asset price
         * 
         * @return Asset price
         */
        public BigDecimal getPrice() {
            return price;
        }
        
        /**
         * Sets asset price
         * 
         * @param price Asset price
         */
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        /**
         * Gets asset value
         * 
         * @return Asset value
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets asset value
         * 
         * @param value Asset value
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        /**
         * Gets allocation percentage
         * 
         * @return Allocation percentage
         */
        public BigDecimal getAllocationPercent() {
            return allocationPercent;
        }
        
        /**
         * Sets allocation percentage
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
            AssetData that = (AssetData) o;
            return Objects.equals(symbol, that.symbol) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(quantity, that.quantity) &&
                   Objects.equals(price, that.price) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(allocationPercent, that.allocationPercent);
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
    
    /**
     * Market data
     */
    public static class MarketData {
        private Map<String, BigDecimal> indexes;
        private Map<String, BigDecimal> trends;
        
        /**
         * Default constructor
         */
        public MarketData() {
        }
        
        /**
         * Gets market indexes
         * 
         * @return Market indexes
         */
        public Map<String, BigDecimal> getIndexes() {
            return indexes;
        }
        
        /**
         * Sets market indexes
         * 
         * @param indexes Market indexes
         */
        public void setIndexes(Map<String, BigDecimal> indexes) {
            this.indexes = indexes;
        }
        
        /**
         * Gets market trends
         * 
         * @return Market trends
         */
        public Map<String, BigDecimal> getTrends() {
            return trends;
        }
        
        /**
         * Sets market trends
         * 
         * @param trends Market trends
         */
        public void setTrends(Map<String, BigDecimal> trends) {
            this.trends = trends;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarketData that = (MarketData) o;
            return Objects.equals(indexes, that.indexes) &&
                   Objects.equals(trends, that.trends);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(indexes, trends);
        }
        
        @Override
        public String toString() {
            return "MarketData{" +
                   "indexes=" + indexes +
                   ", trends=" + trends +
                   "}";
        }
    }
    
    /**
     * Watchlist item
     */
    public static class WatchlistItem {
        private String id;
        private String symbol;
        private String name;
        private String type;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
        
        /**
         * Default constructor
         */
        public WatchlistItem() {
        }
        
        /**
         * Gets item id
         * 
         * @return Item id
         */
        public String getId() {
            return id;
        }
        
        /**
         * Sets item id
         * 
         * @param id Item id
         */
        public void setId(String id) {
            this.id = id;
        }
        
        /**
         * Gets item symbol
         * 
         * @return Item symbol
         */
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * Sets item symbol
         * 
         * @param symbol Item symbol
         */
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * Gets item name
         * 
         * @return Item name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets item name
         * 
         * @param name Item name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets item type
         * 
         * @return Item type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Sets item type
         * 
         * @param type Item type
         */
        public void setType(String type) {
            this.type = type;
        }
        
        /**
         * Gets item price
         * 
         * @return Item price
         */
        public BigDecimal getPrice() {
            return price;
        }
        
        /**
         * Sets item price
         * 
         * @param price Item price
         */
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        /**
         * Gets price change
         * 
         * @return Price change
         */
        public BigDecimal getChange() {
            return change;
        }
        
        /**
         * Sets price change
         * 
         * @param change Price change
         */
        public void setChange(BigDecimal change) {
            this.change = change;
        }
        
        /**
         * Gets price change percentage
         * 
         * @return Price change percentage
         */
        public BigDecimal getChangePercent() {
            return changePercent;
        }
        
        /**
         * Sets price change percentage
         * 
         * @param changePercent Price change percentage
         */
        public void setChangePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
        }
        
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
                   "}";
        }
    }
    
    /**
     * Performance metrics
     */
    public static class PerformanceMetrics {
        private Map<String, BigDecimal> performance;
        private Map<String, BigDecimal> allocation;
        private Map<String, BigDecimal> risk;
        
        /**
         * Default constructor
         */
        public PerformanceMetrics() {
        }
        
        /**
         * Gets performance metrics
         * 
         * @return Performance metrics
         */
        public Map<String, BigDecimal> getPerformance() {
            return performance;
        }
        
        /**
         * Sets performance metrics
         * 
         * @param performance Performance metrics
         */
        public void setPerformance(Map<String, BigDecimal> performance) {
            this.performance = performance;
        }
        
        /**
         * Gets allocation metrics
         * 
         * @return Allocation metrics
         */
        public Map<String, BigDecimal> getAllocation() {
            return allocation;
        }
        
        /**
         * Sets allocation metrics
         * 
         * @param allocation Allocation metrics
         */
        public void setAllocation(Map<String, BigDecimal> allocation) {
            this.allocation = allocation;
        }
        
        /**
         * Gets risk metrics
         * 
         * @return Risk metrics
         */
        public Map<String, BigDecimal> getRisk() {
            return risk;
        }
        
        /**
         * Sets risk metrics
         * 
         * @param risk Risk metrics
         */
        public void setRisk(Map<String, BigDecimal> risk) {
            this.risk = risk;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PerformanceMetrics that = (PerformanceMetrics) o;
            return Objects.equals(performance, that.performance) &&
                   Objects.equals(allocation, that.allocation) &&
                   Objects.equals(risk, that.risk);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(performance, allocation, risk);
        }
        
        @Override
        public String toString() {
            return "PerformanceMetrics{" +
                   "performance=" + performance +
                   ", allocation=" + allocation +
                   ", risk=" + risk +
                   "}";
        }
    }
}
