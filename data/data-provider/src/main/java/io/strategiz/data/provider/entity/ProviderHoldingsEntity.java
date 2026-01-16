package io.strategiz.data.provider.entity;

import io.strategiz.data.base.entity.BaseEntity;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing detailed holdings data for a provider.
 * This is the heavy data stored in a subcollection for lazy loading.
 *
 * Firestore path: users/{userId}/portfolio/providers/{providerId}/holdings/current
 *
 * Lightweight provider status is in the parent document:
 * users/{userId}/portfolio/providers/{providerId}
 */
public class ProviderHoldingsEntity extends BaseEntity {

    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    private String id = "current"; // Fixed ID - only one holdings doc per provider

    @PropertyName("providerId")
    @JsonProperty("providerId")
    private String providerId;

    @PropertyName("totalValue")
    @JsonProperty("totalValue")
    private BigDecimal totalValue;

    @PropertyName("dayChange")
    @JsonProperty("dayChange")
    private BigDecimal dayChange;

    @PropertyName("dayChangePercent")
    @JsonProperty("dayChangePercent")
    private BigDecimal dayChangePercent;

    @PropertyName("totalProfitLoss")
    @JsonProperty("totalProfitLoss")
    private BigDecimal totalProfitLoss;

    @PropertyName("totalProfitLossPercent")
    @JsonProperty("totalProfitLossPercent")
    private BigDecimal totalProfitLossPercent;

    @PropertyName("cashBalance")
    @JsonProperty("cashBalance")
    private BigDecimal cashBalance;

    @PropertyName("buyingPower")
    @JsonProperty("buyingPower")
    private BigDecimal buyingPower;

    @PropertyName("marginBalance")
    @JsonProperty("marginBalance")
    private BigDecimal marginBalance;

    @PropertyName("liquidationValue")
    @JsonProperty("liquidationValue")
    private BigDecimal liquidationValue;

    @PropertyName("holdings")
    @JsonProperty("holdings")
    private List<Holding> holdings;

    @PropertyName("balances")
    @JsonProperty("balances")
    private Map<String, Object> balances; // Raw balance data from provider

    @PropertyName("transactions")
    @JsonProperty("transactions")
    private List<Transaction> transactions;

    @PropertyName("lastUpdatedAt")
    @JsonProperty("lastUpdatedAt")
    private Instant lastUpdatedAt;

    @PropertyName("syncStatus")
    @JsonProperty("syncStatus")
    private String syncStatus; // success, error, syncing

    @PropertyName("errorMessage")
    @JsonProperty("errorMessage")
    private String errorMessage;

    // Constructors
    public ProviderHoldingsEntity() {
        super();
        this.id = "current";
    }

    public ProviderHoldingsEntity(String providerId, String userId) {
        super(userId);
        this.id = "current";
        this.providerId = providerId;
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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

    public BigDecimal getBuyingPower() {
        return buyingPower;
    }

    public void setBuyingPower(BigDecimal buyingPower) {
        this.buyingPower = buyingPower;
    }

    public BigDecimal getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(BigDecimal marginBalance) {
        this.marginBalance = marginBalance;
    }

    public BigDecimal getLiquidationValue() {
        return liquidationValue;
    }

    public void setLiquidationValue(BigDecimal liquidationValue) {
        this.liquidationValue = liquidationValue;
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
     * Nested class for individual holdings.
     * Reused from ProviderDataEntity for compatibility.
     */
    public static class Holding {

        @PropertyName("asset")
        private String asset;

        @PropertyName("name")
        private String name;

        @PropertyName("quantity")
        private BigDecimal quantity;

        @PropertyName("currentPrice")
        private BigDecimal currentPrice;

        @PropertyName("currentValue")
        private BigDecimal currentValue;

        @PropertyName("costBasis")
        private BigDecimal costBasis;

        @PropertyName("profitLoss")
        private BigDecimal profitLoss;

        @PropertyName("profitLossPercent")
        private BigDecimal profitLossPercent;

        @PropertyName("averageBuyPrice")
        private BigDecimal averageBuyPrice;

        @PropertyName("priceChange24h")
        private BigDecimal priceChange24h;

        @PropertyName("sector")
        private String sector;

        @PropertyName("assetType")
        private String assetType; // crypto, fiat, stablecoin

        @PropertyName("category")
        private String category; // Layer1, DeFi, Gaming, etc.

        @PropertyName("marketCapRank")
        private Integer marketCapRank;

        @PropertyName("isStaked")
        private Boolean isStaked;

        @PropertyName("stakingAPR")
        private BigDecimal stakingAPR;

        @PropertyName("originalSymbol")
        private String originalSymbol;

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
     * Nested class for transactions.
     * Reused from ProviderDataEntity for compatibility.
     */
    public static class Transaction {

        @PropertyName("transactionId")
        private String transactionId;

        @PropertyName("type")
        private String type; // buy, sell, deposit, withdrawal

        @PropertyName("asset")
        private String asset;

        @PropertyName("quantity")
        private BigDecimal quantity;

        @PropertyName("price")
        private BigDecimal price;

        @PropertyName("totalValue")
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
}
