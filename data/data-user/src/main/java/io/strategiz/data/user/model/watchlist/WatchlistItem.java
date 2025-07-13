package io.strategiz.data.user.model.watchlist;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.Date;
import java.util.Objects;

/**
 * Domain entity representing an item in a watchlist.
 * This is the improved version that properly follows domain principles.
 */
public class WatchlistItem extends BaseEntity {
    
    private String id;
    private String watchlistId;
    private String symbol;
    private String name;
    private String type; // STOCK, CRYPTO, FOREX, etc.
    private String exchange;
    private Date addedAt;
    private double lastPrice;
    private String currency;
    private boolean alertsEnabled;
    
    public WatchlistItem() {}
    
    public WatchlistItem(String watchlistId, String symbol, String name, String type, String createdBy) {
        super(createdBy);
        this.watchlistId = watchlistId;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.addedAt = new Date();
        this.alertsEnabled = false;
        this.id = generateId(watchlistId, symbol);
    }
    
    public WatchlistItem(String watchlistId, String symbol, String name, String type, String exchange, String currency, String createdBy) {
        super(createdBy);
        this.watchlistId = watchlistId;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.exchange = exchange;
        this.currency = currency;
        this.addedAt = new Date();
        this.alertsEnabled = false;
        this.id = generateId(watchlistId, symbol);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getCollectionName() {
        return "watchlist_items";
    }
    
    public String getWatchlistId() {
        return watchlistId;
    }
    
    public void setWatchlistId(String watchlistId) {
        this.watchlistId = watchlistId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public Date getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }
    
    public double getLastPrice() {
        return lastPrice;
    }
    
    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }
    
    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }
    
    public void updatePrice(double newPrice) {
        this.lastPrice = newPrice;
    }
    
    public void enableAlerts() {
        this.alertsEnabled = true;
    }
    
    public void disableAlerts() {
        this.alertsEnabled = false;
    }
    
    private String generateId(String watchlistId, String symbol) {
        return String.format("%s_%s", watchlistId, symbol.toUpperCase());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WatchlistItem that = (WatchlistItem) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(watchlistId, that.watchlistId) &&
               Objects.equals(symbol, that.symbol);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, watchlistId, symbol);
    }
    
    @Override
    public String toString() {
        return "WatchlistItem{" +
               "id='" + id + '\'' +
               ", watchlistId='" + watchlistId + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", lastPrice=" + lastPrice +
               ", currency='" + currency + '\'' +
               ", alertsEnabled=" + alertsEnabled +
               ", audit=" + getAuditFields() +
               '}';
    }
}