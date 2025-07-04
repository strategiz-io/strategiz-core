package io.strategiz.data.user.model.watchlist;

import java.util.Date;
import java.util.Objects;

/**
 * Entity representing a market watchlist item.
 * Maps to Firestore document structure: users/{userId}/market_watchlist/{assetId}
 */
public class MarketWatchlistItem {
    
    private String id;
    private String symbol;
    private String name;
    private String type;
    private Date addedAt;
    private Date createdAt;
    private Date modifiedAt;
    private String createdBy;
    private String modifiedBy;
    private Integer version;
    private Boolean isActive;

    // Constructors
    public MarketWatchlistItem() {
        this.addedAt = new Date();
        this.createdAt = new Date();
        this.modifiedAt = new Date();
        this.version = 1;
        this.isActive = true;
    }

    public MarketWatchlistItem(String symbol, String name, String type) {
        this();
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.id = generateId(symbol, type);
    }

    // Getters and Setters
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    /**
     * Performs a soft delete by setting isActive to false.
     */
    public void softDelete() {
        this.isActive = false;
        this.modifiedAt = new Date();
        this.version++;
    }

    /**
     * Updates the modification timestamp and version.
     */
    public void markAsModified() {
        this.modifiedAt = new Date();
        this.version++;
    }

    /**
     * Updates the modification timestamp, version, and modified by user.
     */
    public void markAsModified(String modifiedBy) {
        this.modifiedAt = new Date();
        this.modifiedBy = modifiedBy;
        this.version++;
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
               ", createdBy='" + createdBy + '\'' +
               ", modifiedBy='" + modifiedBy + '\'' +
               ", isActive=" + isActive +
               ", version=" + version +
               '}';
    }
} 