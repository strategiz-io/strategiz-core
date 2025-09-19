package io.strategiz.data.watchlist.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Watchlist item for users/{userId}/watchlist subcollection
 * Represents a financial instrument the user is tracking
 */
@Collection("watchlist")
public class WatchlistItemEntity extends BaseEntity {

    @DocumentId
    @PropertyName("itemId")
    @JsonProperty("itemId")
    private String itemId;

    @PropertyName("symbol")
    @JsonProperty("symbol")
    @NotBlank(message = "Symbol is required")
    private String symbol;

    @PropertyName("name")
    @JsonProperty("name")
    private String name;

    @PropertyName("type")
    @JsonProperty("type")
    @NotBlank(message = "Asset type is required")
    private String type; // STOCK, CRYPTO, ETF, etc.

    @PropertyName("exchange")
    @JsonProperty("exchange")
    private String exchange;

    @PropertyName("currentPrice")
    @JsonProperty("currentPrice")
    private BigDecimal currentPrice;

    @PropertyName("previousClose")
    @JsonProperty("previousClose")
    private BigDecimal previousClose;

    @PropertyName("change")
    @JsonProperty("change")
    private BigDecimal change;

    @PropertyName("changePercent")
    @JsonProperty("changePercent")
    private BigDecimal changePercent;

    @PropertyName("volume")
    @JsonProperty("volume")
    private Long volume;

    @PropertyName("marketCap")
    @JsonProperty("marketCap")
    private Long marketCap;

    @PropertyName("sortOrder")
    @JsonProperty("sortOrder")
    private Integer sortOrder = 0;

    @PropertyName("alertEnabled")
    @JsonProperty("alertEnabled")
    private Boolean alertEnabled = false;

    @PropertyName("alertPrice")
    @JsonProperty("alertPrice")
    private BigDecimal alertPrice;

    // Constructors
    public WatchlistItemEntity() {
        super();
    }

    public WatchlistItemEntity(String symbol, String type) {
        super();
        this.symbol = symbol;
        this.type = type;
    }

    public WatchlistItemEntity(String symbol, String name, String type, String exchange) {
        super();
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.exchange = exchange;
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public BigDecimal getChange() {
        return change;
    }

    public void setChange(BigDecimal change) {
        this.change = change;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Long marketCap) {
        this.marketCap = marketCap;
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

    public BigDecimal getAlertPrice() {
        return alertPrice;
    }

    public void setAlertPrice(BigDecimal alertPrice) {
        this.alertPrice = alertPrice;
    }

    // Convenience methods
    public boolean isAlertEnabled() {
        return Boolean.TRUE.equals(alertEnabled);
    }

    public boolean isPriceUp() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isPriceDown() {
        return change != null && change.compareTo(BigDecimal.ZERO) < 0;
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return itemId;
    }

    @Override
    public void setId(String id) {
        this.itemId = id;
    }
}