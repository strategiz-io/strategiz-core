package io.strategiz.service.portfolio.model.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete portfolio overview for the main portfolio page.
 * Contains all data needed to render the full portfolio view.
 */
public class PortfolioOverviewResponse {
    
    private BigDecimal totalValue;
    private BigDecimal dayChange;
    private BigDecimal dayChangePercent;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private BigDecimal totalCashBalance;
    private List<ProviderSummary> providers;
    private List<PortfolioPositionResponse> allPositions;
    private AssetAllocation assetAllocation;
    private long lastUpdated;
    
    /**
     * Summary of each connected provider
     */
    public static class ProviderSummary {
        private String providerId;
        private String providerName;
        private boolean connected;
        private BigDecimal totalValue;
        private BigDecimal dayChange;
        private BigDecimal cashBalance;
        private int positionCount;
        private String syncStatus;
        private long lastSynced;
        
        // Getters and Setters
        public String getProviderId() {
            return providerId;
        }
        
        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }
        
        public String getProviderName() {
            return providerName;
        }
        
        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public void setConnected(boolean connected) {
            this.connected = connected;
        }
        
        public BigDecimal getTotalValue() {
            return totalValue;
        }
        
        public void setTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
        }
        
        public BigDecimal getDayChange() {
            return dayChange;
        }
        
        public void setDayChange(BigDecimal dayChange) {
            this.dayChange = dayChange;
        }
        
        public BigDecimal getCashBalance() {
            return cashBalance;
        }
        
        public void setCashBalance(BigDecimal cashBalance) {
            this.cashBalance = cashBalance;
        }
        
        public int getPositionCount() {
            return positionCount;
        }
        
        public void setPositionCount(int positionCount) {
            this.positionCount = positionCount;
        }
        
        public String getSyncStatus() {
            return syncStatus;
        }
        
        public void setSyncStatus(String syncStatus) {
            this.syncStatus = syncStatus;
        }
        
        public long getLastSynced() {
            return lastSynced;
        }
        
        public void setLastSynced(long lastSynced) {
            this.lastSynced = lastSynced;
        }
    }
    
    /**
     * Asset allocation breakdown
     */
    public static class AssetAllocation {
        private BigDecimal cryptoPercent;
        private BigDecimal stockPercent;
        private BigDecimal forexPercent;
        private BigDecimal cashPercent;
        private BigDecimal otherPercent;
        
        // Getters and Setters
        public BigDecimal getCryptoPercent() {
            return cryptoPercent;
        }
        
        public void setCryptoPercent(BigDecimal cryptoPercent) {
            this.cryptoPercent = cryptoPercent;
        }
        
        public BigDecimal getStockPercent() {
            return stockPercent;
        }
        
        public void setStockPercent(BigDecimal stockPercent) {
            this.stockPercent = stockPercent;
        }
        
        public BigDecimal getForexPercent() {
            return forexPercent;
        }
        
        public void setForexPercent(BigDecimal forexPercent) {
            this.forexPercent = forexPercent;
        }
        
        public BigDecimal getCashPercent() {
            return cashPercent;
        }
        
        public void setCashPercent(BigDecimal cashPercent) {
            this.cashPercent = cashPercent;
        }
        
        public BigDecimal getOtherPercent() {
            return otherPercent;
        }
        
        public void setOtherPercent(BigDecimal otherPercent) {
            this.otherPercent = otherPercent;
        }
    }
    
    // Main class getters and setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
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
    
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }
    
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }
    
    public BigDecimal getTotalProfitLossPercent() {
        return totalProfitLossPercent;
    }
    
    public void setTotalProfitLossPercent(BigDecimal totalProfitLossPercent) {
        this.totalProfitLossPercent = totalProfitLossPercent;
    }
    
    public BigDecimal getTotalCashBalance() {
        return totalCashBalance;
    }
    
    public void setTotalCashBalance(BigDecimal totalCashBalance) {
        this.totalCashBalance = totalCashBalance;
    }
    
    public List<ProviderSummary> getProviders() {
        return providers;
    }
    
    public void setProviders(List<ProviderSummary> providers) {
        this.providers = providers;
    }
    
    public List<PortfolioPositionResponse> getAllPositions() {
        return allPositions;
    }
    
    public void setAllPositions(List<PortfolioPositionResponse> allPositions) {
        this.allPositions = allPositions;
    }
    
    public AssetAllocation getAssetAllocation() {
        return assetAllocation;
    }
    
    public void setAssetAllocation(AssetAllocation assetAllocation) {
        this.assetAllocation = assetAllocation;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}