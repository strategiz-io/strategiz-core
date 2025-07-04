package io.strategiz.data.user.model.watchlist;

import java.util.Objects;

/**
 * Response model for watchlist operations (create, update, delete).
 * Contains minimal data about the operation result.
 */
public class WatchlistOperationResponse {
    
    private String id;
    private String symbol;
    private String operation;
    private Boolean success;
    private String message;
    private Integer version; // For optimistic locking

    // Constructors
    public WatchlistOperationResponse() {}

    public WatchlistOperationResponse(String id, String symbol, String operation, Boolean success) {
        this.id = id;
        this.symbol = symbol;
        this.operation = operation;
        this.success = success;
    }

    /**
     * Creates a successful create operation response.
     * 
     * @param id The ID of the created item
     * @param symbol The symbol of the created item
     * @return WatchlistOperationResponse
     */
    public static WatchlistOperationResponse createSuccess(String id, String symbol) {
        WatchlistOperationResponse response = new WatchlistOperationResponse(id, symbol, "CREATE", true);
        response.setMessage("Watchlist item created successfully");
        return response;
    }

    /**
     * Creates a successful update operation response.
     * 
     * @param id The ID of the updated item
     * @param symbol The symbol of the updated item
     * @param version The new version after update
     * @return WatchlistOperationResponse
     */
    public static WatchlistOperationResponse updateSuccess(String id, String symbol, Integer version) {
        WatchlistOperationResponse response = new WatchlistOperationResponse(id, symbol, "UPDATE", true);
        response.setMessage("Watchlist item updated successfully");
        response.setVersion(version);
        return response;
    }

    /**
     * Creates a successful delete operation response.
     * 
     * @param id The ID of the deleted item
     * @param symbol The symbol of the deleted item
     * @return WatchlistOperationResponse
     */
    public static WatchlistOperationResponse deleteSuccess(String id, String symbol) {
        WatchlistOperationResponse response = new WatchlistOperationResponse(id, symbol, "DELETE", true);
        response.setMessage("Watchlist item deleted successfully");
        return response;
    }

    /**
     * Creates a failed operation response.
     * 
     * @param operation The operation that failed
     * @param message The error message
     * @return WatchlistOperationResponse
     */
    public static WatchlistOperationResponse failure(String operation, String message) {
        WatchlistOperationResponse response = new WatchlistOperationResponse();
        response.setOperation(operation);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistOperationResponse that = (WatchlistOperationResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(operation, that.operation) &&
               Objects.equals(success, that.success) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, operation, success, version);
    }

    @Override
    public String toString() {
        return "WatchlistOperationResponse{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", operation='" + operation + '\'' +
               ", success=" + success +
               ", message='" + message + '\'' +
               ", version=" + version +
               '}';
    }
} 