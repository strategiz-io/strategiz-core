package io.strategiz.service.dashboard.model.watchlist;

import java.util.Objects;

/**
 * Asset data class for watchlist
 */
public class WatchlistAsset {
    private String id;
    private String symbol;
    private String name;
    private String category;

    // Constructors
    public WatchlistAsset() {}

    public WatchlistAsset(String id, String symbol, String name, String category) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.category = category;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistAsset that = (WatchlistAsset) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, category);
    }

    @Override
    public String toString() {
        return "WatchlistAsset{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               '}';
    }
}
