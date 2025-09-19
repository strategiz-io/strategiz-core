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
 * Entity representing provider portfolio data stored in Firestore
 * Collection: users/{userId}/provider_data/{providerId}
 */
@Collection("provider_data")
public class ProviderDataEntity extends BaseEntity {

    @DocumentId
    private String documentId;

    @PropertyName("provider_id")
    private String providerId;

    @PropertyName("provider_name")
    private String providerName;

    @PropertyName("account_type")
    private String accountType; // crypto, stock, forex

    @PropertyName("total_value")
    private BigDecimal totalValue;

    @PropertyName("day_change")
    private BigDecimal dayChange;

    @PropertyName("day_change_percent")
    private BigDecimal dayChangePercent;

    @PropertyName("total_profit_loss")
    private BigDecimal totalProfitLoss;

    @PropertyName("total_profit_loss_percent")
    private BigDecimal totalProfitLossPercent;

    @PropertyName("cash_balance")
    private BigDecimal cashBalance;

    @PropertyName("holdings")
    private List<Holding> holdings;

    @PropertyName("balances")
    private Map<String, Object> balances; // Raw balance data from provider

    @PropertyName("transactions")
    private List<Transaction> transactions;

    @PropertyName("last_updated_at")
    private Instant lastUpdatedAt;

    @PropertyName("sync_status")
    private String syncStatus; // success, error, syncing

    @PropertyName("error_message")
    private String errorMessage;

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
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

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings;
    }

    public Map<String, Object> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, Object> balances) {
        this.balances = balances;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Nested class for individual holdings
     */
    public static class Holding {
        
        @PropertyName("asset")
        private String asset;

        @PropertyName("name")
        private String name;

        @PropertyName("quantity")
        private BigDecimal quantity;

        @PropertyName("current_price")
        private BigDecimal currentPrice;

        @PropertyName("current_value")
        private BigDecimal currentValue;

        @PropertyName("cost_basis")
        private BigDecimal costBasis;

        @PropertyName("profit_loss")
        private BigDecimal profitLoss;

        @PropertyName("profit_loss_percent")
        private BigDecimal profitLossPercent;

        @PropertyName("average_buy_price")
        private BigDecimal averageBuyPrice;

        @PropertyName("price_change_24h")
        private BigDecimal priceChange24h;

        @PropertyName("sector")
        private String sector; // For stocks: technology, healthcare, etc.
        
        // Enrichment fields added during data enrichment phase
        @PropertyName("asset_type")
        private String assetType; // crypto, fiat, stablecoin
        
        @PropertyName("category")
        private String category; // Layer1, DeFi, Gaming, etc.
        
        @PropertyName("market_cap_rank")
        private Integer marketCapRank;
        
        @PropertyName("is_staked")
        private Boolean isStaked;
        
        @PropertyName("staking_apr")
        private BigDecimal stakingAPR;
        
        @PropertyName("original_symbol")
        private String originalSymbol; // Original Kraken symbol before normalization

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

        public BigDecimal getCostBasis() {
            return costBasis;
        }

        public void setCostBasis(BigDecimal costBasis) {
            this.costBasis = costBasis;
        }

        public BigDecimal getProfitLoss() {
            return profitLoss;
        }

        public void setProfitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss;
        }

        public BigDecimal getProfitLossPercent() {
            return profitLossPercent;
        }

        public void setProfitLossPercent(BigDecimal profitLossPercent) {
            this.profitLossPercent = profitLossPercent;
        }

        public BigDecimal getAverageBuyPrice() {
            return averageBuyPrice;
        }

        public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
            this.averageBuyPrice = averageBuyPrice;
        }

        public BigDecimal getPriceChange24h() {
            return priceChange24h;
        }

        public void setPriceChange24h(BigDecimal priceChange24h) {
            this.priceChange24h = priceChange24h;
        }

        public String getSector() {
            return sector;
        }

        public void setSector(String sector) {
            this.sector = sector;
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
        
        public Integer getMarketCapRank() {
            return marketCapRank;
        }
        
        public void setMarketCapRank(Integer marketCapRank) {
            this.marketCapRank = marketCapRank;
        }
        
        public Boolean getIsStaked() {
            return isStaked;
        }
        
        public void setIsStaked(Boolean isStaked) {
            this.isStaked = isStaked;
        }
        
        public BigDecimal getStakingAPR() {
            return stakingAPR;
        }
        
        public void setStakingAPR(BigDecimal stakingAPR) {
            this.stakingAPR = stakingAPR;
        }
        
        public String getOriginalSymbol() {
            return originalSymbol;
        }
        
        public void setOriginalSymbol(String originalSymbol) {
            this.originalSymbol = originalSymbol;
        }
    }

    /**
     * Nested class for transactions
     */
    public static class Transaction {
        
        @PropertyName("transaction_id")
        private String transactionId;

        @PropertyName("type")
        private String type; // buy, sell, deposit, withdrawal

        @PropertyName("asset")
        private String asset;

        @PropertyName("quantity")
        private BigDecimal quantity;

        @PropertyName("price")
        private BigDecimal price;

        @PropertyName("total_value")
        private BigDecimal totalValue;

        @PropertyName("fee")
        private BigDecimal fee;

        @PropertyName("timestamp")
        private Instant timestamp;

        // Getters and Setters
        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAsset() {
            return asset;
        }

        public void setAsset(String asset) {
            this.asset = asset;
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

        public BigDecimal getTotalValue() {
            return totalValue;
        }

        public void setTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
        }
        
        public BigDecimal getFee() {
            return fee;
        }

        public void setFee(BigDecimal fee) {
            this.fee = fee;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
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