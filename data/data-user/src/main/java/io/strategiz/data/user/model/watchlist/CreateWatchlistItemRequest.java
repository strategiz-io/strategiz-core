package io.strategiz.data.user.model.watchlist;

import java.util.Objects;

/**
 * Request model for creating a new watchlist item.
 * Contains only the fields needed for creation.
 */
public class CreateWatchlistItemRequest {
    
    private String symbol;
    private String name;
    private String type;

    // Constructors
    public CreateWatchlistItemRequest() {}

    public CreateWatchlistItemRequest(String symbol, String name, String type) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
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

    /**
     * Converts this request to a MarketWatchlistItem entity.
     * 
     * @param createdBy The user creating this item
     * @return MarketWatchlistItem for persistence
     */
    public MarketWatchlistItem toEntity(String createdBy) {
        return new MarketWatchlistItem(symbol, name, type, createdBy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateWatchlistItemRequest that = (CreateWatchlistItemRequest) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, name, type);
    }

    @Override
    public String toString() {
        return "CreateWatchlistItemRequest{" +
               "symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               '}';
    }
} 