package io.strategiz.client.kraken.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Model for Kraken balance
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrakenBalance {
    private String asset;
    private String balance;
    
    // Additional fields for UI display
    private double balanceValue;
    private double usdValue;
    
    // Constructors
    public KrakenBalance() {}
    
    public KrakenBalance(String asset, String balance) {
        this.asset = asset;
        this.balance = balance;
    }
    
    public KrakenBalance(String asset, String balance, double balanceValue, double usdValue) {
        this.asset = asset;
        this.balance = balance;
        this.balanceValue = balanceValue;
        this.usdValue = usdValue;
    }
    
    // Getters and setters
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    
    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }
    
    public double getBalanceValue() { return balanceValue; }
    public void setBalanceValue(double balanceValue) { this.balanceValue = balanceValue; }
    
    public double getUsdValue() { return usdValue; }
    public void setUsdValue(double usdValue) { this.usdValue = usdValue; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KrakenBalance that = (KrakenBalance) o;
        return Double.compare(that.balanceValue, balanceValue) == 0 &&
               Double.compare(that.usdValue, usdValue) == 0 &&
               Objects.equals(asset, that.asset) &&
               Objects.equals(balance, that.balance);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(asset, balance, balanceValue, usdValue);
    }
    
    @Override
    public String toString() {
        return "KrakenBalance{" +
               "asset='" + asset + '\'' +
               ", balance='" + balance + '\'' +
               ", balanceValue=" + balanceValue +
               ", usdValue=" + usdValue +
               '}';
    }
}
