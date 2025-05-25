package io.strategiz.service.exchange.trading.agent.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structured response from Gemini AI trading agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiTradingSignal {
    
    /**
     * Trading signal (BUY, SELL, HOLD)
     */
    private String signal;
    
    /**
     * Confidence score (0.0-1.0)
     */
    private double confidence;
    
    /**
     * Current BTC price when signal was generated
     */
    @JsonProperty("currentPrice")
    private double currentPrice;
    
    /**
     * Target price for the signal
     */
    @JsonProperty("targetPrice")
    private double targetPrice;
    
    /**
     * Recommended stop loss price
     */
    @JsonProperty("stopLoss")
    private double stopLoss;
    
    /**
     * Timeframe used for analysis
     */
    private String timeframe;
    
    /**
     * Detailed explanation for the trading signal
     */
    private String rationale;
    
    /**
     * Key technical indicators identified by Gemini
     */
    @JsonProperty("keyIndicators")
    private Map<String, Object> keyIndicators;
    
    /**
     * Timestamp when signal was generated
     */
    private LocalDateTime timestamp;
    
    /**
     * Returns a formatted human-readable signal
     */
    public String getFormattedSignal() {
        StringBuilder sb = new StringBuilder();
        
        // Signal type and confidence
        sb.append(String.format("*%s* Signal for BTC (%.0f%% confidence)\n", 
                signal.toUpperCase(), confidence * 100));
        
        // Price information
        sb.append(String.format("Current Price: $%.2f\n", currentPrice));
        
        if ("BUY".equalsIgnoreCase(signal)) {
            sb.append(String.format("Target Price: $%.2f (%.1f%% potential upside)\n", 
                    targetPrice, ((targetPrice / currentPrice) - 1) * 100));
            sb.append(String.format("Stop Loss: $%.2f (%.1f%% downside risk)\n", 
                    stopLoss, ((currentPrice - stopLoss) / currentPrice) * 100));
        } else if ("SELL".equalsIgnoreCase(signal)) {
            sb.append(String.format("Target Price: $%.2f (%.1f%% potential downside)\n", 
                    targetPrice, ((currentPrice - targetPrice) / currentPrice) * 100));
        }
        
        // Key indicators
        if (keyIndicators != null && !keyIndicators.isEmpty()) {
            sb.append("\nKey Indicators:\n");
            if (keyIndicators.containsKey("trend")) {
                sb.append("• Trend: ").append(keyIndicators.get("trend")).append("\n");
            }
            if (keyIndicators.containsKey("momentum")) {
                sb.append("• Momentum: ").append(keyIndicators.get("momentum")).append("\n");
            }
            if (keyIndicators.containsKey("patternDetected") && 
                    !"none".equalsIgnoreCase(keyIndicators.get("patternDetected").toString())) {
                sb.append("• Pattern: ").append(keyIndicators.get("patternDetected")).append("\n");
            }
            if (keyIndicators.containsKey("supportLevel")) {
                sb.append("• Support: $").append(formatNumber(keyIndicators.get("supportLevel"))).append("\n");
            }
            if (keyIndicators.containsKey("resistanceLevel")) {
                sb.append("• Resistance: $").append(formatNumber(keyIndicators.get("resistanceLevel"))).append("\n");
            }
        }
        
        // Rationale
        sb.append("\nRationale:\n").append(rationale);
        
        // Timestamp and timeframe
        sb.append("\n\nTimeframe: ").append(timeframe);
        sb.append("\nGenerated: ").append(timestamp);
        
        return sb.toString();
    }
    
    private String formatNumber(Object value) {
        if (value instanceof Number) {
            return String.format("%.2f", ((Number) value).doubleValue());
        }
        return value.toString();
    }
}
