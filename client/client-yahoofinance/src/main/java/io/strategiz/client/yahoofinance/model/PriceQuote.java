package io.strategiz.client.yahoofinance.model;

import io.strategiz.client.base.exception.ClientErrorDetails;
import io.strategiz.framework.exception.StrategizException;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a price quote from Yahoo Finance API.
 * Immutable data object following clean architecture principles.
 */
public class PriceQuote {
    
    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal previousClose;
    private final BigDecimal dayChange;
    private final BigDecimal dayChangePercent;
    private final BigDecimal dayHigh;
    private final BigDecimal dayLow;
    private final Long volume;
    private final Instant timestamp;
    
    private PriceQuote(Builder builder) {
        this.symbol = builder.symbol;
        this.price = builder.price;
        this.previousClose = builder.previousClose;
        this.dayChange = builder.dayChange;
        this.dayChangePercent = builder.dayChangePercent;
        this.dayHigh = builder.dayHigh;
        this.dayLow = builder.dayLow;
        this.volume = builder.volume;
        this.timestamp = builder.timestamp;
    }
    
    // Getters
    public String getSymbol() {
        return symbol;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigDecimal getPreviousClose() {
        return previousClose;
    }
    
    public BigDecimal getDayChange() {
        return dayChange;
    }
    
    public BigDecimal getDayChangePercent() {
        return dayChangePercent;
    }
    
    public BigDecimal getDayHigh() {
        return dayHigh;
    }
    
    public BigDecimal getDayLow() {
        return dayLow;
    }
    
    public Long getVolume() {
        return volume;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Builder for PriceQuote following Builder pattern.
     */
    public static class Builder {
        private String symbol;
        private BigDecimal price;
        private BigDecimal previousClose;
        private BigDecimal dayChange;
        private BigDecimal dayChangePercent;
        private BigDecimal dayHigh;
        private BigDecimal dayLow;
        private Long volume;
        private Instant timestamp;
        
        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }
        
        public Builder previousClose(BigDecimal previousClose) {
            this.previousClose = previousClose;
            return this;
        }
        
        public Builder dayChange(BigDecimal dayChange) {
            this.dayChange = dayChange;
            return this;
        }
        
        public Builder dayChangePercent(BigDecimal dayChangePercent) {
            this.dayChangePercent = dayChangePercent;
            return this;
        }
        
        public Builder dayHigh(BigDecimal dayHigh) {
            this.dayHigh = dayHigh;
            return this;
        }
        
        public Builder dayLow(BigDecimal dayLow) {
            this.dayLow = dayLow;
            return this;
        }
        
        public Builder volume(Long volume) {
            this.volume = volume;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public PriceQuote build() {
            if (symbol == null || symbol.isEmpty()) {
                throw new StrategizException(ClientErrorDetails.INVALID_ARGUMENT,
                    "yahoo-finance", "Symbol is required");
            }
            if (price == null) {
                throw new StrategizException(ClientErrorDetails.INVALID_ARGUMENT,
                    "yahoo-finance", "Price is required");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new PriceQuote(this);
        }
    }
}