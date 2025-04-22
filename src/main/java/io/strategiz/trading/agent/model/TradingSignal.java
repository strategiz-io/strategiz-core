package io.strategiz.trading.agent.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a trading signal with recommendation to buy or sell Bitcoin
 * based on historical price analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSignal {
    
    /**
     * Type of signal
     */
    public enum SignalType {
        BUY,
        SELL,
        HOLD
    }
    
    /**
     * Strength of the signal from 0.0 to 1.0
     */
    public enum SignalStrength {
        WEAK(0.25),
        MODERATE(0.5),
        STRONG(0.75),
        VERY_STRONG(0.9);
        
        private final double value;
        
        SignalStrength(double value) {
            this.value = value;
        }
        
        public double getValue() {
            return value;
        }
    }
    
    private String assetSymbol;
    private SignalType signalType;
    private SignalStrength strength;
    private LocalDateTime timestamp;
    private double currentPrice;
    private double targetPrice;
    private String rationale;
    private String timeframe; // e.g., "short-term", "medium-term", "long-term"
    private Map<String, Object> additionalMetrics;
    
    /**
     * Returns a formatted string representation of the signal for user-friendly display
     */
    public String getFormattedSignal() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s SIGNAL for %s (%.2f%% confidence): ", 
                signalType, assetSymbol, strength.getValue() * 100));
        
        if (signalType == SignalType.BUY) {
            sb.append(String.format("Buy at $%.2f with target of $%.2f", currentPrice, targetPrice));
        } else if (signalType == SignalType.SELL) {
            sb.append(String.format("Sell at $%.2f", currentPrice));
        } else {
            sb.append("Hold position");
        }
        
        sb.append("\nRationale: ").append(rationale);
        sb.append("\nTimeframe: ").append(timeframe);
        sb.append("\nGenerated at: ").append(timestamp);
        
        return sb.toString();
    }
}
