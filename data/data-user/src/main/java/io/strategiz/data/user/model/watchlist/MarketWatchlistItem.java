package io.strategiz.data.user.model.watchlist;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing a market watchlist item.
 * Maps to Firestore document structure: users/{userId}/market_watchlist/{assetId}
 */
public class MarketWatchlistItem extends BaseEntity {
    
    private String id;
    private String symbol;
    private String name;
    private String type;
    private Date addedAt;

    // Constructors
    public MarketWatchlistItem() {
        this.addedAt = new Date();
    }

    public MarketWatchlistItem(String symbol, String name, String type, String createdBy) {
        super(createdBy);
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.addedAt = new Date();
        this.id = generateId(symbol, type);
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

    @Override
    public String getCollectionName() {
        return "market_watchlist";
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

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    /**
     * Gets the creation date from audit fields
     * @return Creation date or null if not available
     */
    public Date getCreatedAt() {
        return getAuditFields() != null && getAuditFields().getCreatedDate() != null ?
                getAuditFields().getCreatedDate().toDate() : null;
    }

    /**
     * Gets the modification date from audit fields
     * @return Modification date or null if not available
     */
    public Date getModifiedAt() {
        return getAuditFields() != null && getAuditFields().getModifiedDate() != null ?
                getAuditFields().getModifiedDate().toDate() : null;
    }


    /**
     * Generates a unique ID for the watchlist item.
     * 
     * @param symbol The symbol
     * @param type The type
     * @return Generated ID
     */
    private String generateId(String symbol, String type) {
        return String.format("%s_%s", symbol.toUpperCase(), type.toUpperCase());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketWatchlistItem that = (MarketWatchlistItem) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, type);
    }

    @Override
    public String toString() {
        return "MarketWatchlistItem{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", addedAt=" + addedAt +
               ", audit=" + getAuditFields() +
               '}';
    }
} 