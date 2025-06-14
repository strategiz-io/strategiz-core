package io.strategiz.service.exchange.trading.agent.gemini.model;

import io.strategiz.service.exchange.trading.agent.model.HistoricalPriceData;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured prompt schema for the Gemini AI trading agent
 * Contains all market data and context needed for analysis
 */
public class TradingAgentPrompt {
    
    /**
     * Historical price data for BTC from Coinbase
     */
    private List<HistoricalPriceData> historicalData;
    
    /**
     * Current market conditions and metrics
     */
    private Map<String, Object> marketConditions;
    
    /**
     * Timeframe for analysis
     */
    private String timeframe;
    
    /**
     * Current BTC price
     */
    private double currentPrice;
    
    /**
     * Current market trends description
     */
    private String marketTrends;
    
    /**
     * Recent news sentiment (if available)
     */
    private String newsSentiment;
    
    /**
     * Risk profile for analysis (conservative, moderate, aggressive)
     */
    private String riskProfile;
    
    // Default constructor
    public TradingAgentPrompt() {}
    
    // All-args constructor
    public TradingAgentPrompt(List<HistoricalPriceData> historicalData, Map<String, Object> marketConditions,
                             String timeframe, double currentPrice, String marketTrends, 
                             String newsSentiment, String riskProfile) {
        this.historicalData = historicalData;
        this.marketConditions = marketConditions;
        this.timeframe = timeframe;
        this.currentPrice = currentPrice;
        this.marketTrends = marketTrends;
        this.newsSentiment = newsSentiment;
        this.riskProfile = riskProfile;
    }

    // Getters
    public List<HistoricalPriceData> getHistoricalData() {
        return historicalData;
    }

    public Map<String, Object> getMarketConditions() {
        return marketConditions;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getMarketTrends() {
        return marketTrends;
    }

    public String getNewsSentiment() {
        return newsSentiment;
    }

    public String getRiskProfile() {
        return riskProfile;
    }

    // Setters
    public void setHistoricalData(List<HistoricalPriceData> historicalData) {
        this.historicalData = historicalData;
    }

    public void setMarketConditions(Map<String, Object> marketConditions) {
        this.marketConditions = marketConditions;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setMarketTrends(String marketTrends) {
        this.marketTrends = marketTrends;
    }

    public void setNewsSentiment(String newsSentiment) {
        this.newsSentiment = newsSentiment;
    }

    public void setRiskProfile(String riskProfile) {
        this.riskProfile = riskProfile;
    }

    /**
     * Format this prompt into a structured string for the Gemini AI model
     * @return formatted prompt string
     */
    public String formatPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        // Header
        prompt.append("TRADING ANALYSIS REQUEST\n");
        prompt.append("========================\n\n");
        
        // Current market snapshot
        prompt.append("CURRENT MARKET STATUS:\n");
        prompt.append("- Asset: Bitcoin (BTC)\n");
        prompt.append("- Current Price: $").append(String.format("%.2f", currentPrice)).append("\n");
        prompt.append("- Analysis Timeframe: ").append(timeframe).append("\n");
        prompt.append("- Risk Profile: ").append(riskProfile).append("\n\n");
        
        // Historical data summary
        if (historicalData != null && !historicalData.isEmpty()) {
            prompt.append("HISTORICAL PRICE DATA:\n");
            prompt.append("- Data Points: ").append(historicalData.size()).append(" periods\n");
            
            // Get recent data points
            HistoricalPriceData latest = historicalData.get(historicalData.size() - 1);
            HistoricalPriceData earliest = historicalData.get(0);
            
            prompt.append("- Latest Close: $").append(String.format("%.2f", latest.getClose())).append("\n");
            prompt.append("- Period Range: ").append(earliest.getTimestamp()).append(" to ").append(latest.getTimestamp()).append("\n");
            
            // Calculate basic stats
            double totalVolume = historicalData.stream()
                .mapToDouble(data -> data.getVolume().doubleValue())
                .sum();
            double avgVolume = totalVolume / historicalData.size();
            
            prompt.append("- Average Volume: ").append(String.format("%.2f", avgVolume)).append("\n\n");
        }
        
        // Market conditions
        if (marketConditions != null && !marketConditions.isEmpty()) {
            prompt.append("MARKET CONDITIONS:\n");
            marketConditions.forEach((key, value) -> 
                prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }
        
        // Market trends
        if (marketTrends != null && !marketTrends.trim().isEmpty()) {
            prompt.append("MARKET TRENDS:\n");
            prompt.append(marketTrends).append("\n\n");
        }
        
        // News sentiment
        if (newsSentiment != null && !newsSentiment.trim().isEmpty()) {
            prompt.append("NEWS SENTIMENT:\n");
            prompt.append(newsSentiment).append("\n\n");
        }
        
        // Analysis request
        prompt.append("ANALYSIS REQUEST:\n");
        prompt.append("Based on the above data, please provide:\n");
        prompt.append("1. Trading signal recommendation (BUY/SELL/HOLD)\n");
        prompt.append("2. Confidence level (0.0 to 1.0)\n");
        prompt.append("3. Target price (if buying/selling)\n");
        prompt.append("4. Stop loss recommendation\n");
        prompt.append("5. Key technical indicators supporting your decision\n");
        prompt.append("6. Risk assessment and rationale\n\n");
        
        prompt.append("Please respond with a structured JSON format containing these fields:\n");
        prompt.append("{\n");
        prompt.append("  \"signal\": \"BUY|SELL|HOLD\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"currentPrice\": ").append(currentPrice).append(",\n");
        prompt.append("  \"targetPrice\": number,\n");
        prompt.append("  \"stopLoss\": number,\n");
        prompt.append("  \"timeframe\": \"").append(timeframe).append("\",\n");
        prompt.append("  \"rationale\": \"detailed explanation\",\n");
        prompt.append("  \"keyIndicators\": [\"indicator1\", \"indicator2\"]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradingAgentPrompt that = (TradingAgentPrompt) o;
        return Double.compare(that.currentPrice, currentPrice) == 0 &&
               Objects.equals(historicalData, that.historicalData) &&
               Objects.equals(marketConditions, that.marketConditions) &&
               Objects.equals(timeframe, that.timeframe) &&
               Objects.equals(marketTrends, that.marketTrends) &&
               Objects.equals(newsSentiment, that.newsSentiment) &&
               Objects.equals(riskProfile, that.riskProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historicalData, marketConditions, timeframe, currentPrice, 
                           marketTrends, newsSentiment, riskProfile);
    }

    @Override
    public String toString() {
        return "TradingAgentPrompt{" +
               "historicalData=" + (historicalData != null ? historicalData.size() + " data points" : "null") +
               ", marketConditions=" + marketConditions +
               ", timeframe='" + timeframe + '\'' +
               ", currentPrice=" + currentPrice +
               ", marketTrends='" + marketTrends + '\'' +
               ", newsSentiment='" + newsSentiment + '\'' +
               ", riskProfile='" + riskProfile + '\'' +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<HistoricalPriceData> historicalData;
        private Map<String, Object> marketConditions;
        private String timeframe;
        private double currentPrice;
        private String marketTrends;
        private String newsSentiment;
        private String riskProfile;

        public Builder historicalData(List<HistoricalPriceData> historicalData) {
            this.historicalData = historicalData;
            return this;
        }

        public Builder marketConditions(Map<String, Object> marketConditions) {
            this.marketConditions = marketConditions;
            return this;
        }

        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }

        public Builder currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public Builder marketTrends(String marketTrends) {
            this.marketTrends = marketTrends;
            return this;
        }

        public Builder newsSentiment(String newsSentiment) {
            this.newsSentiment = newsSentiment;
            return this;
        }

        public Builder riskProfile(String riskProfile) {
            this.riskProfile = riskProfile;
            return this;
        }

        public TradingAgentPrompt build() {
            return new TradingAgentPrompt(historicalData, marketConditions, timeframe, 
                                        currentPrice, marketTrends, newsSentiment, riskProfile);
        }
    }
}
