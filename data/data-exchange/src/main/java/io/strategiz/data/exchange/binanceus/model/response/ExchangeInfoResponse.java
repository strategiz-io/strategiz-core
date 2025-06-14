package io.strategiz.data.exchange.binanceus.model.response;

import java.util.Objects;

/**
 * Response model for exchange information
 */
public class ExchangeInfoResponse {
    private Object exchangeInfo;
    
    // Constructors
    public ExchangeInfoResponse() {}
    
    public ExchangeInfoResponse(Object exchangeInfo) {
        this.exchangeInfo = exchangeInfo;
    }
    
    // Getters and setters
    public Object getExchangeInfo() { return exchangeInfo; }
    public void setExchangeInfo(Object exchangeInfo) { this.exchangeInfo = exchangeInfo; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeInfoResponse that = (ExchangeInfoResponse) o;
        return Objects.equals(exchangeInfo, that.exchangeInfo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(exchangeInfo);
    }
    
    @Override
    public String toString() {
        return "ExchangeInfoResponse{" +
               "exchangeInfo=" + exchangeInfo +
               '}';
    }
}
