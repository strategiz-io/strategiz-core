package io.strategiz.service.marketplace.model.domain;

import java.util.Map;

/**
 * Transaction model - represents a purchase transaction for a strategy
 * Stored in the 'transactions' collection
 */
public class Transaction {
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