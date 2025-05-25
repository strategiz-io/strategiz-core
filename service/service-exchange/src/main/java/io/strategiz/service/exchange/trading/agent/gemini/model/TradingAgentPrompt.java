package io.strategiz.service.exchange.trading.agent.gemini.model;

import io.strategiz.service.exchange.trading.agent.model.HistoricalPriceData;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Structured prompt schema for the Gemini AI trading agent
 * Contains all market data and context needed for analysis
 */
@Data
@Builder
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
    
    /**
     * Format the prompt for Gemini model
     */
    public String formatPrompt() {
        StringBuilder promptBuilder = new StringBuilder();
        
        // System prompt
        promptBuilder.append("You are a professional Bitcoin trading analyst with expertise in technical analysis, market trends, and price prediction. ");
        promptBuilder.append("Analyze the following real Bitcoin price data from Coinbase and provide a trading signal (BUY, SELL, or HOLD) ");
        promptBuilder.append("with a confidence score (0.0-1.0) and detailed rationale.\n\n");
        
        // Context
        promptBuilder.append("## Context\n");
        promptBuilder.append("- Timeframe: ").append(timeframe).append("\n");
        promptBuilder.append("- Current BTC price: $").append(String.format("%.2f", currentPrice)).append("\n");
        promptBuilder.append("- Risk profile: ").append(riskProfile).append("\n");
        
        if (marketTrends != null && !marketTrends.isEmpty()) {
            promptBuilder.append("- Market trends: ").append(marketTrends).append("\n");
        }
        
        if (newsSentiment != null && !newsSentiment.isEmpty()) {
            promptBuilder.append("- News sentiment: ").append(newsSentiment).append("\n");
        }
        
        // Market conditions
        promptBuilder.append("\n## Market Indicators\n");
        if (marketConditions != null && !marketConditions.isEmpty()) {
            marketConditions.forEach((key, value) -> 
                promptBuilder.append("- ").append(key).append(": ").append(value).append("\n")
            );
        }
        
        // Historical data summary
        promptBuilder.append("\n## Historical Data (most recent first, ").append(historicalData.size()).append(" data points)\n");
        promptBuilder.append("```\n");
        promptBuilder.append("DateTime,Open,High,Low,Close,Volume\n");
        
        // Include only a limited number of data points in the prompt to avoid token limits
        int recordLimit = Math.min(historicalData.size(), 30);
        for (int i = 0; i < recordLimit; i++) {
            HistoricalPriceData data = historicalData.get(i);
            promptBuilder.append(data.getTimestamp()).append(",")
                         .append(data.getOpen()).append(",")
                         .append(data.getHigh()).append(",")
                         .append(data.getLow()).append(",")
                         .append(data.getClose()).append(",")
                         .append(data.getVolume()).append("\n");
        }
        promptBuilder.append("```\n");
        
        // Instructions
        promptBuilder.append("\n## Instructions\n");
        promptBuilder.append("1. Analyze the price data and indicators\n");
        promptBuilder.append("2. Identify key patterns, trends, and signals\n");
        promptBuilder.append("3. Provide a trading recommendation (BUY, SELL, or HOLD)\n");
        promptBuilder.append("4. Assign a confidence score between 0.0 and 1.0\n");
        promptBuilder.append("5. Explain your rationale in detail\n");
        promptBuilder.append("6. Consider risk profile in your recommendation\n");
        
        // Required output format
        promptBuilder.append("\n## Required Output Format\n");
        promptBuilder.append("Provide your response in valid JSON format with the following structure:\n");
        promptBuilder.append("```json\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"signal\": \"BUY|SELL|HOLD\",\n");
        promptBuilder.append("  \"confidence\": 0.7,\n");
        promptBuilder.append("  \"currentPrice\": 55000.00,\n");
        promptBuilder.append("  \"targetPrice\": 60000.00,\n");
        promptBuilder.append("  \"stopLoss\": 52000.00,\n");
        promptBuilder.append("  \"timeframe\": \"1d\",\n");
        promptBuilder.append("  \"rationale\": \"Detailed explanation...\",\n");
        promptBuilder.append("  \"keyIndicators\": {\n");
        promptBuilder.append("    \"trend\": \"bullish|bearish|neutral\",\n");
        promptBuilder.append("    \"momentum\": \"increasing|decreasing|stable\",\n");
        promptBuilder.append("    \"patternDetected\": \"pattern name or none\",\n");
        promptBuilder.append("    \"volumeAnalysis\": \"high|low|average\",\n");
        promptBuilder.append("    \"supportLevel\": 50000.00,\n");
        promptBuilder.append("    \"resistanceLevel\": 60000.00\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("}\n");
        promptBuilder.append("```\n");
        
        return promptBuilder.toString();
    }
}
