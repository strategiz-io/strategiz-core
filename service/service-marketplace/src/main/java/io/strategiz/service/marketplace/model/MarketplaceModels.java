package io.strategiz.service.marketplace.model;

import java.util.List;
import java.util.Map;

/**
 * Models for the Strategy Marketplace
 * These classes define the structure for storing strategies, applications, reviews, and transactions
 */
public class MarketplaceModels {

    /**
     * Strategy model - represents a strategy in the marketplace
     * Stored in the top-level 'strategies' collection
     */
    public static class Strategy {
        private String id;
        private String name;
        private String description;
        private String creatorId;
        private String creatorName;
        private String creatorEmail;
        private List<String> tags;
        private double price;
        private String currency; // e.g., "USD"
        private boolean isPublic;
        private long createdAt;
        private long updatedAt;
        private Map<String, Object> metadata;
        private Map<String, Object> configuration;
        private String version;
        private List<String> supportedExchanges;
        private long purchaseCount;
        private double averageRating;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCreatorId() { return creatorId; }
        public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
        
        public String getCreatorName() { return creatorName; }
        public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
        
        public String getCreatorEmail() { return creatorEmail; }
        public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<String> getSupportedExchanges() { return supportedExchanges; }
        public void setSupportedExchanges(List<String> supportedExchanges) { this.supportedExchanges = supportedExchanges; }
        
        public long getPurchaseCount() { return purchaseCount; }
        public void setPurchaseCount(long purchaseCount) { this.purchaseCount = purchaseCount; }
        
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    }
    
    /**
     * StrategyApplication model - represents a user's application of a purchased strategy
     * Stored in the 'strategy_applications' subcollection under each user document
     */
    public static class StrategyApplication {
        private String id;
        private String strategyId;
        private String strategyName;
        private String strategyVersion;
        private String userId;
        private String exchangeId; // e.g., "binanceus", "kraken"
        private boolean isActive;
        private Map<String, Object> configuration;
        private long appliedAt;
        private long lastRunAt;
        private String status; // "active", "paused", "error"
        private String errorMessage;
        private Map<String, Object> results;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        
        public String getStrategyVersion() { return strategyVersion; }
        public void setStrategyVersion(String strategyVersion) { this.strategyVersion = strategyVersion; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getExchangeId() { return exchangeId; }
        public void setExchangeId(String exchangeId) { this.exchangeId = exchangeId; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean isActive) { this.isActive = isActive; }
        
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        
        public long getAppliedAt() { return appliedAt; }
        public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }
        
        public long getLastRunAt() { return lastRunAt; }
        public void setLastRunAt(long lastRunAt) { this.lastRunAt = lastRunAt; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Map<String, Object> getResults() { return results; }
        public void setResults(Map<String, Object> results) { this.results = results; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * Review model - represents a user's review of a strategy
     * Stored in the 'reviews' subcollection under each strategy document
     */
    public static class Review {
        private String id;
        private String strategyId;
        private String userId;
        private String userName;
        private int rating; // 1-5
        private String comment;
        private long createdAt;
        private long updatedAt;
        private boolean isVerifiedPurchase;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
        
        public boolean isVerifiedPurchase() { return isVerifiedPurchase; }
        public void setVerifiedPurchase(boolean isVerifiedPurchase) { this.isVerifiedPurchase = isVerifiedPurchase; }
    }
    
    /**
     * Transaction model - represents a purchase transaction for a strategy
     * Stored in the 'transactions' collection
     */
    public static class Transaction {
        private String id;
        private String strategyId;
        private String strategyName;
        private String buyerId;
        private String buyerEmail;
        private String sellerId;
        private String sellerEmail;
        private double amount;
        private String currency;
        private String status; // "pending", "completed", "failed", "refunded"
        private long createdAt;
        private long completedAt;
        private String paymentMethod;
        private String paymentId;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        
        public String getBuyerId() { return buyerId; }
        public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
        
        public String getBuyerEmail() { return buyerEmail; }
        public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
        
        public String getSellerId() { return sellerId; }
        public void setSellerId(String sellerId) { this.sellerId = sellerId; }
        
        public String getSellerEmail() { return sellerEmail; }
        public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * Purchase model - represents a user's purchase of a strategy
     * Stored in the 'purchases' subcollection under each user document
     */
    public static class Purchase {
        private String id;
        private String strategyId;
        private String strategyName;
        private String transactionId;
        private long purchasedAt;
        private String status; // "active", "expired", "refunded"
        private long expiresAt; // null if no expiration
        private double price;
        private String currency;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getStrategyId() { return strategyId; }
        public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
        
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public long getPurchasedAt() { return purchasedAt; }
        public void setPurchasedAt(long purchasedAt) { this.purchasedAt = purchasedAt; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getExpiresAt() { return expiresAt; }
        public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
        
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}
