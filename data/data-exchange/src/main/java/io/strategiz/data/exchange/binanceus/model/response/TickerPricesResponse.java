package io.strategiz.data.exchange.binanceus.model.response;

import io.strategiz.data.exchange.binanceus.model.TickerPrice;
import java.util.List;
import java.util.Objects;

/**
 * Response model for ticker prices information
 */
public class TickerPricesResponse {
    private List<TickerPrice> tickerPrices;
    
    // Constructors
    public TickerPricesResponse() {}
    
    public TickerPricesResponse(List<TickerPrice> tickerPrices) {
        this.tickerPrices = tickerPrices;
    }
    
    // Getters and setters
    public List<TickerPrice> getTickerPrices() { return tickerPrices; }
    public void setTickerPrices(List<TickerPrice> tickerPrices) { this.tickerPrices = tickerPrices; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickerPricesResponse that = (TickerPricesResponse) o;
        return Objects.equals(tickerPrices, that.tickerPrices);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tickerPrices);
    }
    
    @Override
    public String toString() {
        return "TickerPricesResponse{" +
               "tickerPrices=" + tickerPrices +
               '}';
    }
}
