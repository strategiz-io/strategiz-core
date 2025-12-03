package io.strategiz.client.alpaca.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO representing asset metadata from Alpaca's Assets API
 *
 * Alpaca API Response Format:
 * {
 *   "id": "b0b6dd9d-8b9b-48a9-ba46-b9d54906e415",
 *   "class": "us_equity",
 *   "exchange": "NASDAQ",
 *   "symbol": "AAPL",
 *   "name": "Apple Inc.",
 *   "status": "active",
 *   "tradable": true,
 *   "marginable": true,
 *   "shortable": true,
 *   "easy_to_borrow": true,
 *   "fractionable": true,
 *   "min_order_size": "1",
 *   "min_trade_increment": "0.01",
 *   "price_increment": "0.01"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlpacaAsset {

    @JsonProperty("id")
    private String id;

    @JsonProperty("class")
    private String assetClass;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("tradable")
    private Boolean tradable;

    @JsonProperty("marginable")
    private Boolean marginable;

    @JsonProperty("shortable")
    private Boolean shortable;

    @JsonProperty("easy_to_borrow")
    private Boolean easyToBorrow;

    @JsonProperty("fractionable")
    private Boolean fractionable;

    @JsonProperty("min_order_size")
    private String minOrderSize;

    @JsonProperty("min_trade_increment")
    private String minTradeIncrement;

    @JsonProperty("price_increment")
    private String priceIncrement;

    // Constructors
    public AlpacaAsset() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getTradable() {
        return tradable;
    }

    public void setTradable(Boolean tradable) {
        this.tradable = tradable;
    }

    public Boolean getMarginable() {
        return marginable;
    }

    public void setMarginable(Boolean marginable) {
        this.marginable = marginable;
    }

    public Boolean getShortable() {
        return shortable;
    }

    public void setShortable(Boolean shortable) {
        this.shortable = shortable;
    }

    public Boolean getEasyToBorrow() {
        return easyToBorrow;
    }

    public void setEasyToBorrow(Boolean easyToBorrow) {
        this.easyToBorrow = easyToBorrow;
    }

    public Boolean getFractionable() {
        return fractionable;
    }

    public void setFractionable(Boolean fractionable) {
        this.fractionable = fractionable;
    }

    public String getMinOrderSize() {
        return minOrderSize;
    }

    public void setMinOrderSize(String minOrderSize) {
        this.minOrderSize = minOrderSize;
    }

    public String getMinTradeIncrement() {
        return minTradeIncrement;
    }

    public void setMinTradeIncrement(String minTradeIncrement) {
        this.minTradeIncrement = minTradeIncrement;
    }

    public String getPriceIncrement() {
        return priceIncrement;
    }

    public void setPriceIncrement(String priceIncrement) {
        this.priceIncrement = priceIncrement;
    }

    /**
     * Helper method to convert minOrderSize to BigDecimal
     */
    public BigDecimal getMinOrderSizeDecimal() {
        return minOrderSize != null ? new BigDecimal(minOrderSize) : null;
    }

    /**
     * Helper method to convert priceIncrement to BigDecimal
     */
    public BigDecimal getPriceIncrementDecimal() {
        return priceIncrement != null ? new BigDecimal(priceIncrement) : null;
    }

    @Override
    public String toString() {
        return String.format("AlpacaAsset[symbol=%s, name=%s, exchange=%s, class=%s, tradable=%b]",
            symbol, name, exchange, assetClass, tradable);
    }
}
