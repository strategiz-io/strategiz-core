package io.strategiz.service.dashboard.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request object for creating a new watchlist item
 */
public class CreateWatchlistItemRequest {

    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must be 20 characters or less")
    private String symbol;

    @Size(max = 200, message = "Name must be 200 characters or less")
    private String name;

    @NotBlank(message = "Asset type is required")
    @Size(max = 50, message = "Type must be 50 characters or less")
    private String type; // CRYPTO, STOCK, ETF, etc.

    @Size(max = 100, message = "Exchange must be 100 characters or less")
    private String exchange;

    private Integer sortOrder;

    private Boolean alertEnabled = false;

    // Constructors
    public CreateWatchlistItemRequest() {
    }

    public CreateWatchlistItemRequest(String symbol, String type) {
        this.symbol = symbol;
        this.type = type;
    }

    public CreateWatchlistItemRequest(String symbol, String name, String type, String exchange) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.exchange = exchange;
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

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getAlertEnabled() {
        return alertEnabled;
    }

    public void setAlertEnabled(Boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    @Override
    public String toString() {
        return "CreateWatchlistItemRequest{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", exchange='" + exchange + '\'' +
                ", sortOrder=" + sortOrder +
                ", alertEnabled=" + alertEnabled +
                '}';
    }
} 