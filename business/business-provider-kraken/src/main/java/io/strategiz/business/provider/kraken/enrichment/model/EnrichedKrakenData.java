package io.strategiz.business.provider.kraken.enrichment.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing enriched Kraken portfolio data.
 * Contains normalized symbols, metadata, pricing, and staking information.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
public class EnrichedKrakenData {
    
    private String userId;
    private String providerId;
    private Map<String, Object> normalizedBalances = new HashMap<>();
    private Map<String, AssetInfo> assetInfo = new HashMap<>();
    private BigDecimal totalValue = BigDecimal.ZERO;
    private BigDecimal cashBalance = BigDecimal.ZERO;
    
    /**
     * Represents enriched information about a single asset
     */
    public static class AssetInfo {
        private String originalSymbol;      // e.g., "XXBT"
        private String normalizedSymbol;    // e.g., "BTC"
        private String fullName;           // e.g., "Bitcoin"
        private String assetType;          // "crypto", "fiat", "stablecoin"
        private String category;           // "Layer1", "DeFi", "Gaming", etc.
        private BigDecimal quantity;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal priceChange24h;
        private BigDecimal averageBuyPrice;  // Average purchase price from trades
        private Integer marketCapRank;
        private boolean isStaked;
        private BigDecimal stakingAPR;
        private String stakingPeriod;
        private boolean isCash;
        
        // Getters and setters
        public String getOriginalSymbol() {
            return originalSymbol;
        }
        
        public void setOriginalSymbol(String originalSymbol) {
            this.originalSymbol = originalSymbol;
        }
        
        public String getNormalizedSymbol() {
            return normalizedSymbol;
        }
        
        public void setNormalizedSymbol(String normalizedSymbol) {
            this.normalizedSymbol = normalizedSymbol;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getAssetType() {
            return assetType;
        }
        
        public void setAssetType(String assetType) {
            this.assetType = assetType;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }
        
        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }
        
        public BigDecimal getCurrentValue() {
            return currentValue;
        }
        
        public void setCurrentValue(BigDecimal currentValue) {
            this.currentValue = currentValue;
        }
        
        public BigDecimal getPriceChange24h() {
            return priceChange24h;
        }
        
        public void setPriceChange24h(BigDecimal priceChange24h) {
            this.priceChange24h = priceChange24h;
        }
        
        public BigDecimal getAverageBuyPrice() {
            return averageBuyPrice;
        }
        
        public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
            this.averageBuyPrice = averageBuyPrice;
        }
        
        public Integer getMarketCapRank() {
            return marketCapRank;
        }
        
        public void setMarketCapRank(Integer marketCapRank) {
            this.marketCapRank = marketCapRank;
        }
        
        public boolean isStaked() {
            return isStaked;
        }
        
        public void setStaked(boolean staked) {
            isStaked = staked;
        }
        
        public BigDecimal getStakingAPR() {
            return stakingAPR;
        }
        
        public void setStakingAPR(BigDecimal stakingAPR) {
            this.stakingAPR = stakingAPR;
        }
        
        public String getStakingPeriod() {
            return stakingPeriod;
        }
        
        public void setStakingPeriod(String stakingPeriod) {
            this.stakingPeriod = stakingPeriod;
        }
        
        public boolean isCash() {
            return isCash;
        }
        
        public void setCash(boolean cash) {
            isCash = cash;
        }
    }
    
    // Main class getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public Map<String, Object> getNormalizedBalances() {
        return normalizedBalances;
    }
    
    public void setNormalizedBalances(Map<String, Object> normalizedBalances) {
        this.normalizedBalances = normalizedBalances;
    }
    
    public Map<String, AssetInfo> getAssetInfo() {
        return assetInfo;
    }
    
    public void setAssetInfo(Map<String, AssetInfo> assetInfo) {
        this.assetInfo = assetInfo;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public BigDecimal getCashBalance() {
        return cashBalance;
    }
    
    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
}