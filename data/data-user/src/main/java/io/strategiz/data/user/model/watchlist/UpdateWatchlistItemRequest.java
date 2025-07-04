package io.strategiz.data.user.model.watchlist;

import java.util.Objects;

/**
 * Request model for updating a watchlist item.
 * Contains only the fields that can be updated.
 */
public class UpdateWatchlistItemRequest {
    
    private String id;
    private String symbol;
    private String name;
    private String type;
    private Integer version; // For optimistic locking

    // Constructors
    public UpdateWatchlistItemRequest() {}

    public UpdateWatchlistItemRequest(String id, String symbol, String name, String type) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
    }

    public UpdateWatchlistItemRequest(String id, String symbol, String name, String type, Integer version) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.version = version;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Applies this update to an existing MarketWatchlistItem entity.
     * 
     * @param entity The entity to update
     */
    public void applyTo(MarketWatchlistItem entity) {
        if (symbol != null) {
            entity.setSymbol(symbol);
        }
        if (name != null) {
            entity.setName(name);
        }
        if (type != null) {
            entity.setType(type);
        }
        entity.markAsModified();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateWatchlistItemRequest that = (UpdateWatchlistItemRequest) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, type, version);
    }

    @Override
    public String toString() {
        return "UpdateWatchlistItemRequest{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", version=" + version +
               '}';
    }
} 