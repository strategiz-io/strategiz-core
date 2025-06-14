package io.strategiz.data.exchange.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Model class representing a Coinbase account
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
    private String id;
    private String name;
    private String currency;
    private Balance balance;
    private String type;
    private boolean primary;
    private boolean active;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    // Additional fields for calculated values (not part of raw API response)
    private double usdValue;
    
    // Constructors
    public Account() {}
    
    public Account(String id, String name, String currency, Balance balance, String type, 
                   boolean primary, boolean active, String createdAt, String updatedAt, double usdValue) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.balance = balance;
        this.type = type;
        this.primary = primary;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.usdValue = usdValue;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public Balance getBalance() { return balance; }
    public String getType() { return type; }
    public boolean isPrimary() { return primary; }
    public boolean isActive() { return active; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public double getUsdValue() { return usdValue; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setBalance(Balance balance) { this.balance = balance; }
    public void setType(String type) { this.type = type; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setUsdValue(double usdValue) { this.usdValue = usdValue; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return primary == account.primary &&
               active == account.active &&
               Double.compare(account.usdValue, usdValue) == 0 &&
               Objects.equals(id, account.id) &&
               Objects.equals(name, account.name) &&
               Objects.equals(currency, account.currency) &&
               Objects.equals(balance, account.balance) &&
               Objects.equals(type, account.type) &&
               Objects.equals(createdAt, account.createdAt) &&
               Objects.equals(updatedAt, account.updatedAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, currency, balance, type, primary, active, createdAt, updatedAt, usdValue);
    }
    
    @Override
    public String toString() {
        return "Account{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", currency='" + currency + '\'' +
               ", balance=" + balance +
               ", type='" + type + '\'' +
               ", primary=" + primary +
               ", active=" + active +
               ", createdAt='" + createdAt + '\'' +
               ", updatedAt='" + updatedAt + '\'' +
               ", usdValue=" + usdValue +
               '}';
    }
}
