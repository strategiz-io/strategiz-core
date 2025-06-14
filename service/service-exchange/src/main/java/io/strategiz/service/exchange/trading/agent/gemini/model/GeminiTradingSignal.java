package io.strategiz.service.exchange.trading.agent.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Structured response from Gemini AI trading agent
 */
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
    
    // Default constructor
    public GeminiTradingSignal() {}
    
    // All-args constructor
    public GeminiTradingSignal(String signal, double confidence, double currentPrice, double targetPrice,
                              double stopLoss, String timeframe, String rationale, 
                              Map<String, Object> keyIndicators, LocalDateTime timestamp) {
        this.signal = signal;
        this.confidence = confidence;
        this.currentPrice = currentPrice;
        this.targetPrice = targetPrice;
        this.stopLoss = stopLoss;
        this.timeframe = timeframe;
        this.rationale = rationale;
        this.keyIndicators = keyIndicators;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSignal() {
        return signal;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public String getRationale() {
        return rationale;
    }

    public Map<String, Object> getKeyIndicators() {
        return keyIndicators;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setSignal(String signal) {
        this.signal = signal;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public void setKeyIndicators(Map<String, Object> keyIndicators) {
        this.keyIndicators = keyIndicators;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeminiTradingSignal that = (GeminiTradingSignal) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               Double.compare(that.currentPrice, currentPrice) == 0 &&
               Double.compare(that.targetPrice, targetPrice) == 0 &&
               Double.compare(that.stopLoss, stopLoss) == 0 &&
               Objects.equals(signal, that.signal) &&
               Objects.equals(timeframe, that.timeframe) &&
               Objects.equals(rationale, that.rationale) &&
               Objects.equals(keyIndicators, that.keyIndicators) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(signal, confidence, currentPrice, targetPrice, stopLoss,
                           timeframe, rationale, keyIndicators, timestamp);
    }
    
    @Override
    public String toString() {
        return "GeminiTradingSignal{" +
               "signal='" + signal + '\'' +
               ", confidence=" + confidence +
               ", currentPrice=" + currentPrice +
               ", targetPrice=" + targetPrice +
               ", stopLoss=" + stopLoss +
               ", timeframe='" + timeframe + '\'' +
               ", rationale='" + rationale + '\'' +
               ", keyIndicators=" + keyIndicators +
               ", timestamp=" + timestamp +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String signal;
        private double confidence;
        private double currentPrice;
        private double targetPrice;
        private double stopLoss;
        private String timeframe;
        private String rationale;
        private Map<String, Object> keyIndicators;
        private LocalDateTime timestamp;
        
        public Builder signal(String signal) {
            this.signal = signal;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }
        
        public Builder targetPrice(double targetPrice) {
            this.targetPrice = targetPrice;
            return this;
        }
        
        public Builder stopLoss(double stopLoss) {
            this.stopLoss = stopLoss;
            return this;
        }
        
        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }
        
        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }
        
        public Builder keyIndicators(Map<String, Object> keyIndicators) {
            this.keyIndicators = keyIndicators;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public GeminiTradingSignal build() {
            return new GeminiTradingSignal(signal, confidence, currentPrice, targetPrice,
                                         stopLoss, timeframe, rationale, keyIndicators, timestamp);
        }
    }
}
