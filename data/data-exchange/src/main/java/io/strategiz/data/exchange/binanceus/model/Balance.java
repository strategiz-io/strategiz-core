package io.strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Model class for Binance US balance data
 * This represents balance information for a specific asset from the Binance US API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Balance {
    private String asset;
    private String free;
    private String locked;

    // Added fields for calculated values (not part of raw API response)
    private double freeValue;
    private double lockedValue;
    private double totalValue;
    private double usdValue;

    public Balance() {
    }

    public Balance(String asset, String free, String locked, double freeValue, double lockedValue, double totalValue, double usdValue) {
        this.asset = asset;
        this.free = free;
        this.locked = locked;
        this.freeValue = freeValue;
        this.lockedValue = lockedValue;
        this.totalValue = totalValue;
        this.usdValue = usdValue;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getFree() {
        return free;
    }

    public void setFree(String free) {
        this.free = free;
    }

    public String getLocked() {
        return locked;
    }

    public void setLocked(String locked) {
        this.locked = locked;
    }

    public double getFreeValue() {
        return freeValue;
    }

    public void setFreeValue(double freeValue) {
        this.freeValue = freeValue;
    }

    public double getLockedValue() {
        return lockedValue;
    }

    public void setLockedValue(double lockedValue) {
        this.lockedValue = lockedValue;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public double getUsdValue() {
        return usdValue;
    }

    public void setUsdValue(double usdValue) {
        this.usdValue = usdValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return Double.compare(balance.freeValue, freeValue) == 0 &&
                Double.compare(balance.lockedValue, lockedValue) == 0 &&
                Double.compare(balance.totalValue, totalValue) == 0 &&
                Double.compare(balance.usdValue, usdValue) == 0 &&
                Objects.equals(asset, balance.asset) &&
                Objects.equals(free, balance.free) &&
                Objects.equals(locked, balance.locked);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset, free, locked, freeValue, lockedValue, totalValue, usdValue);
    }

    @Override
    public String toString() {
        return "Balance{" +
                "asset='" + asset + '\'' +
                ", free='" + free + '\'' +
                ", locked='" + locked + '\'' +
                ", freeValue=" + freeValue +
                ", lockedValue=" + lockedValue +
                ", totalValue=" + totalValue +
                ", usdValue=" + usdValue +
                '}';
    }
}
