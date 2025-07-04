package io.strategiz.data.user.model.watchlist;

import java.util.Objects;

/**
 * Request model for deleting a watchlist item.
 * Contains only the fields needed for deletion.
 */
public class DeleteWatchlistItemRequest {
    
    private String id;
    private String symbol; // Optional - can delete by symbol instead of ID
    private Integer version; // For optimistic locking

    // Constructors
    public DeleteWatchlistItemRequest() {}

    public DeleteWatchlistItemRequest(String id) {
        this.id = id;
    }

    public DeleteWatchlistItemRequest(String id, Integer version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Constructor for deleting by symbol.
     * 
     * @param symbol The symbol to delete
     */
    public static DeleteWatchlistItemRequest forSymbol(String symbol) {
        DeleteWatchlistItemRequest request = new DeleteWatchlistItemRequest();
        request.setSymbol(symbol);
        return request;
    }

    /**
     * Constructor for deleting by ID.
     * 
     * @param id The ID to delete
     */
    public static DeleteWatchlistItemRequest forId(String id) {
        DeleteWatchlistItemRequest request = new DeleteWatchlistItemRequest();
        request.setId(id);
        return request;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Checks if this request is valid (has either ID or symbol).
     * 
     * @return true if the request has either ID or symbol
     */
    public boolean isValid() {
        return (id != null && !id.trim().isEmpty()) || 
               (symbol != null && !symbol.trim().isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeleteWatchlistItemRequest that = (DeleteWatchlistItemRequest) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, version);
    }

    @Override
    public String toString() {
        return "DeleteWatchlistItemRequest{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", version=" + version +
               '}';
    }
} 