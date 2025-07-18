package io.strategiz.service.marketplace.model.domain;

/**
 * Purchase model - represents a user's purchase of a strategy
 * Stored in the 'purchases' subcollection under each user document
 */
public class Purchase {
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