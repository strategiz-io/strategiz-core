package io.strategiz.data.provider.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing aggregated portfolio summary stored in Firestore
 * Collection: users/{userId}/portfolio_summary/current
 */
@Collection("portfolio_summary")
public class PortfolioSummaryEntity extends BaseEntity {

    @DocumentId
    private String documentId;

    @PropertyName("total_value")
    private BigDecimal totalValue;

    @PropertyName("total_return")
    private BigDecimal totalReturn;

    @PropertyName("total_return_percent")
    private BigDecimal totalReturnPercent;

    @PropertyName("cash_available")
    private BigDecimal cashAvailable;

    @PropertyName("day_change")
    private BigDecimal dayChange;

    @PropertyName("day_change_percent")
    private BigDecimal dayChangePercent;

    @PropertyName("week_change")
    private BigDecimal weekChange;

    @PropertyName("week_change_percent")
    private BigDecimal weekChangePercent;

    @PropertyName("month_change")
    private BigDecimal monthChange;

    @PropertyName("month_change_percent")
    private BigDecimal monthChangePercent;

    @PropertyName("asset_allocation")
    private AssetAllocation assetAllocation;

    @PropertyName("account_performance")
    private Map<String, BigDecimal> accountPerformance; // providerId -> value

    @PropertyName("top_performers")
    private List<TopPerformer> topPerformers;

    @PropertyName("top_losers")
    private List<TopPerformer> topLosers;

    @PropertyName("performance_history")
    private List<PerformanceHistory> performanceHistory;

    @PropertyName("last_synced_at")
    private Instant lastSyncedAt;

    @PropertyName("providers_count")
    private Integer providersCount;

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public BigDecimal getTotalReturnPercent() {
        return totalReturnPercent;
    }

    public void setTotalReturnPercent(BigDecimal totalReturnPercent) {
        this.totalReturnPercent = totalReturnPercent;
    }

    public BigDecimal getCashAvailable() {
        return cashAvailable;
    }

    public void setCashAvailable(BigDecimal cashAvailable) {
        this.cashAvailable = cashAvailable;
    }

    public BigDecimal getDayChange() {
        return dayChange;
    }

    public void setDayChange(BigDecimal dayChange) {
        this.dayChange = dayChange;
    }

    public BigDecimal getDayChangePercent() {
        return dayChangePercent;
    }

    public void setDayChangePercent(BigDecimal dayChangePercent) {
        this.dayChangePercent = dayChangePercent;
    }

    public BigDecimal getWeekChange() {
        return weekChange;
    }

    public void setWeekChange(BigDecimal weekChange) {
        this.weekChange = weekChange;
    }

    public BigDecimal getWeekChangePercent() {
        return weekChangePercent;
    }

    public void setWeekChangePercent(BigDecimal weekChangePercent) {
        this.weekChangePercent = weekChangePercent;
    }

    public BigDecimal getMonthChange() {
        return monthChange;
    }

    public void setMonthChange(BigDecimal monthChange) {
        this.monthChange = monthChange;
    }

    public BigDecimal getMonthChangePercent() {
        return monthChangePercent;
    }

    public void setMonthChangePercent(BigDecimal monthChangePercent) {
        this.monthChangePercent = monthChangePercent;
    }

    public AssetAllocation getAssetAllocation() {
        return assetAllocation;
    }

    public void setAssetAllocation(AssetAllocation assetAllocation) {
        this.assetAllocation = assetAllocation;
    }

    public Map<String, BigDecimal> getAccountPerformance() {
        return accountPerformance;
    }

    public void setAccountPerformance(Map<String, BigDecimal> accountPerformance) {
        this.accountPerformance = accountPerformance;
    }

    public List<TopPerformer> getTopPerformers() {
        return topPerformers;
    }

    public void setTopPerformers(List<TopPerformer> topPerformers) {
        this.topPerformers = topPerformers;
    }

    public List<TopPerformer> getTopLosers() {
        return topLosers;
    }

    public void setTopLosers(List<TopPerformer> topLosers) {
        this.topLosers = topLosers;
    }

    public List<PerformanceHistory> getPerformanceHistory() {
        return performanceHistory;
    }

    public void setPerformanceHistory(List<PerformanceHistory> performanceHistory) {
        this.performanceHistory = performanceHistory;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Integer getProvidersCount() {
        return providersCount;
    }

    public void setProvidersCount(Integer providersCount) {
        this.providersCount = providersCount;
    }

    /**
     * Nested class for asset allocation
     */
    public static class AssetAllocation {
        
        @PropertyName("crypto")
        private BigDecimal crypto;

        @PropertyName("crypto_percent")
        private BigDecimal cryptoPercent;

        @PropertyName("stocks")
        private BigDecimal stocks;

        @PropertyName("stocks_percent")
        private BigDecimal stocksPercent;

        @PropertyName("forex")
        private BigDecimal forex;

        @PropertyName("forex_percent")
        private BigDecimal forexPercent;

        @PropertyName("commodities")
        private BigDecimal commodities;

        @PropertyName("commodities_percent")
        private BigDecimal commoditiesPercent;

        @PropertyName("sectors")
        private Map<String, BigDecimal> sectors; // sector -> percentage

        // Getters and Setters
        public BigDecimal getCrypto() {
            return crypto;
        }

        public void setCrypto(BigDecimal crypto) {
            this.crypto = crypto;
        }

        public BigDecimal getCryptoPercent() {
            return cryptoPercent;
        }

        public void setCryptoPercent(BigDecimal cryptoPercent) {
            this.cryptoPercent = cryptoPercent;
        }

        public BigDecimal getStocks() {
            return stocks;
        }

        public void setStocks(BigDecimal stocks) {
            this.stocks = stocks;
        }

        public BigDecimal getStocksPercent() {
            return stocksPercent;
        }

        public void setStocksPercent(BigDecimal stocksPercent) {
            this.stocksPercent = stocksPercent;
        }

        public BigDecimal getForex() {
            return forex;
        }

        public void setForex(BigDecimal forex) {
            this.forex = forex;
        }

        public BigDecimal getForexPercent() {
            return forexPercent;
        }

        public void setForexPercent(BigDecimal forexPercent) {
            this.forexPercent = forexPercent;
        }

        public BigDecimal getCommodities() {
            return commodities;
        }

        public void setCommodities(BigDecimal commodities) {
            this.commodities = commodities;
        }

        public BigDecimal getCommoditiesPercent() {
            return commoditiesPercent;
        }

        public void setCommoditiesPercent(BigDecimal commoditiesPercent) {
            this.commoditiesPercent = commoditiesPercent;
        }

        public Map<String, BigDecimal> getSectors() {
            return sectors;
        }

        public void setSectors(Map<String, BigDecimal> sectors) {
            this.sectors = sectors;
        }
    }

    /**
     * Nested class for top performers/losers
     */
    public static class TopPerformer {
        
        @PropertyName("asset")
        private String asset;

        @PropertyName("name")
        private String name;

        @PropertyName("provider")
        private String provider;

        @PropertyName("change_percent")
        private BigDecimal changePercent;

        @PropertyName("change_amount")
        private BigDecimal changeAmount;

        // Getters and Setters
        public String getAsset() {
            return asset;
        }

        public void setAsset(String asset) {
            this.asset = asset;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public BigDecimal getChangePercent() {
            return changePercent;
        }

        public void setChangePercent(BigDecimal changePercent) {
            this.changePercent = changePercent;
        }

        public BigDecimal getChangeAmount() {
            return changeAmount;
        }

        public void setChangeAmount(BigDecimal changeAmount) {
            this.changeAmount = changeAmount;
        }
    }

    /**
     * Nested class for performance history
     */
    public static class PerformanceHistory {
        
        @PropertyName("date")
        private Instant date;

        @PropertyName("value")
        private BigDecimal value;

        @PropertyName("change")
        private BigDecimal change;

        @PropertyName("change_percent")
        private BigDecimal changePercent;

        // Getters and Setters
        public Instant getDate() {
            return date;
        }

        public void setDate(Instant date) {
            this.date = date;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
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
    }
    
    // Required BaseEntity methods
    @Override
    public String getId() {
        return documentId;
    }

    @Override
    public void setId(String id) {
        this.documentId = id;
    }
}
