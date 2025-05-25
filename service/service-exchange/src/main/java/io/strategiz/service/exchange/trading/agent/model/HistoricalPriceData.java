package io.strategiz.service.exchange.trading.agent.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents historical price data point for a cryptocurrency asset
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPriceData {
    private String assetSymbol;     // e.g., "BTC"
    private LocalDateTime timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private double quoteVolume;     // Volume in quote currency (usually USD)
    
    /**
     * Calculate typical price (average of high, low, close)
     * @return typical price
     */
    public double getTypicalPrice() {
        return (high + low + close) / 3.0;
    }
    
    /**
     * Calculate price range for this period
     * @return price range (high - low)
     */
    public double getPriceRange() {
        return high - low;
    }
    
    /**
     * Calculate the percentage price change during this candle
     * @return price change percentage
     */
    public double getPriceChangePercent() {
        if (open == 0) return 0;
        return ((close - open) / open) * 100.0;
    }
}
