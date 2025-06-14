package io.strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Model class for Binance US ticker price data
 * This represents price information for a specific trading pair from the Binance US API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerPrice {
    private String symbol;
    private String price;

    public TickerPrice() {
    }

    public TickerPrice(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickerPrice that = (TickerPrice) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, price);
    }

    @Override
    public String toString() {
        return "TickerPrice{" +
                "symbol='" + symbol + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}
