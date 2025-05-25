package strategiz.service.exchange.trading.agent.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import strategiz.service.exchange.trading.agent.gemini.model.GeminiTradingSignal;
import strategiz.service.exchange.trading.agent.gemini.model.TradingAgentPrompt;
import strategiz.service.exchange.trading.agent.model.HistoricalPriceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini AI Trading Agent Service
 * Uses Google Agent Development Kit (ADK) with Vertex AI to analyze Bitcoin price trends
 * and generate intelligent trading signals based on real Coinbase data
 */
@Service
@Slf4j
public class GeminiTradingAgent {

    private final CoinbaseDataFetcher dataFetcher;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public GeminiTradingAgent(CoinbaseDataFetcher dataFetcher) {
        this.dataFetcher = dataFetcher;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate a trading signal using Gemini AI and real Coinbase data
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @param timeframe Timeframe for analysis (e.g., "1d", "6h", "1h", "15m")
     * @param riskProfile Risk profile for analysis ("conservative", "moderate", "aggressive")
     * @return AI-generated trading signal
     */
    public GeminiTradingSignal generateTradingSignal(
            String apiKey, 
            String privateKey, 
            String timeframe,
            String riskProfile) {
        
        log.info("Generating Gemini AI trading signal for BTC on {} timeframe with {} risk profile", 
                timeframe, riskProfile);
        
        try {
            // Step 1: Fetch real historical data from Coinbase
            int dataLimit = dataFetcher.getDefaultLimitForTimeframe(timeframe);
            List<HistoricalPriceData> historicalData = 
                    dataFetcher.fetchBitcoinHistoricalData(apiKey, privateKey, timeframe, dataLimit);
            
            if (historicalData.isEmpty()) {
                throw new RuntimeException("Failed to fetch historical BTC data from Coinbase");
            }
            
            double currentPrice = historicalData.get(0).getClose();
            log.info("Successfully fetched {} data points from Coinbase. Current BTC price: ${}", 
                    historicalData.size(), currentPrice);
            
            // Step 2: Calculate key market indicators
            Map<String, Object> marketIndicators = calculateMarketIndicators(historicalData);
            
            // Step 3: Prepare structured prompt for Gemini
            TradingAgentPrompt prompt = TradingAgentPrompt.builder()
                    .historicalData(historicalData)
                    .marketConditions(marketIndicators)
                    .timeframe(timeframe)
                    .currentPrice(currentPrice)
                    .riskProfile(riskProfile)
                    .marketTrends(detectMarketTrends(historicalData))
                    .build();
            
            // Step 4: Call Gemini model using ADK
            String formattedPrompt = prompt.formatPrompt();
            log.debug("Gemini prompt prepared: {} characters", formattedPrompt.length());
            
            GeminiTradingSignal signal = callGeminiAgent(formattedPrompt);
            
            // Step 5: Enrich signal with additional metadata
            signal.setTimeframe(timeframe);
            signal.setTimestamp(LocalDateTime.now());
            
            log.info("Successfully generated {} signal with {:.0f}% confidence", 
                    signal.getSignal(), signal.getConfidence() * 100);
            
            return signal;
            
        } catch (Exception e) {
            log.error("Error generating Gemini trading signal: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI trading signal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a trading signal based on the provided prompt
     */
    private GeminiTradingSignal callGeminiAgent(String prompt) {
        try {
            // For now, this is a placeholder implementation that returns a mock trading signal
            // In a real implementation, we would call the Gemini API
            log.info("Would normally call Gemini API with prompt of length: {}", prompt.length());
            
            // Create a mock trading signal for now
            GeminiTradingSignal mockSignal = GeminiTradingSignal.builder()
                .signal("HOLD")
                .confidence(0.85)
                .currentPrice(60000.00)
                .targetPrice(65000.00)
                .stopLoss(58000.00)
                .rationale("Mock trading signal until Gemini API integration is completed")
                .build();
                
            return mockSignal;
            
        } catch (Exception e) {
            log.error("Error processing mock response: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing AI response: " + e.getMessage(), e);
        }
    }
    

    
    /**
     * Calculate key market indicators from historical price data
     */
    private Map<String, Object> calculateMarketIndicators(List<HistoricalPriceData> data) {
        Map<String, Object> indicators = new HashMap<>();
        
        // Simple Moving Averages (SMA)
        indicators.put("SMA7", calculateSMA(data, 7));
        indicators.put("SMA25", calculateSMA(data, 25));
        indicators.put("SMA99", calculateSMA(data, 99));
        
        // Price relative to SMAs
        double currentPrice = data.get(0).getClose();
        double sma7 = (double) indicators.get("SMA7");
        double sma25 = (double) indicators.get("SMA25");
        
        indicators.put("PriceToSMA7Ratio", currentPrice / sma7);
        indicators.put("PriceToSMA25Ratio", currentPrice / sma25);
        
        // Volatility
        indicators.put("24hVolatility", calculateVolatility(data, 24));
        
        // Price change percentages
        indicators.put("24hPriceChange", calculatePriceChange(data, 24));
        indicators.put("7dPriceChange", calculatePriceChange(data, 168));
        indicators.put("30dPriceChange", calculatePriceChange(data, 720));
        
        // Volume analysis
        indicators.put("AverageVolume7d", calculateAverageVolume(data, 7));
        indicators.put("VolumeChange24h", calculateVolumeChange(data, 24));
        
        // RSI
        if (data.size() >= 14) {
            indicators.put("RSI14", calculateRSI(data, 14));
        }
        
        return indicators;
    }
    
    /**
     * Detect market trends from historical data
     */
    private String detectMarketTrends(List<HistoricalPriceData> data) {
        if (data.size() < 7) {
            return "Insufficient data for trend analysis";
        }
        
        // Calculate short-term trend (3 days)
        double shortTermChange = calculatePriceChange(data, 3);
        
        // Calculate medium-term trend (14 days)
        double mediumTermChange = calculatePriceChange(data, 14);
        
        // Calculate long-term trend (30 days)
        double longTermChange = calculatePriceChange(data, 30);
        
        // Recent volatility
        double recentVolatility = calculateVolatility(data, 7);
        
        // Determine trend type
        StringBuilder trendBuilder = new StringBuilder();
        
        // Short-term trend
        if (shortTermChange > 5.0) {
            trendBuilder.append("Strong short-term uptrend (").append(String.format("%.1f", shortTermChange)).append("%). ");
        } else if (shortTermChange > 2.0) {
            trendBuilder.append("Moderate short-term uptrend (").append(String.format("%.1f", shortTermChange)).append("%). ");
        } else if (shortTermChange < -5.0) {
            trendBuilder.append("Strong short-term downtrend (").append(String.format("%.1f", shortTermChange)).append("%). ");
        } else if (shortTermChange < -2.0) {
            trendBuilder.append("Moderate short-term downtrend (").append(String.format("%.1f", shortTermChange)).append("%). ");
        } else {
            trendBuilder.append("Neutral short-term trend (").append(String.format("%.1f", shortTermChange)).append("%). ");
        }
        
        // Medium-term trend
        if (mediumTermChange > 15.0) {
            trendBuilder.append("Strong medium-term uptrend (").append(String.format("%.1f", mediumTermChange)).append("%). ");
        } else if (mediumTermChange > 5.0) {
            trendBuilder.append("Moderate medium-term uptrend (").append(String.format("%.1f", mediumTermChange)).append("%). ");
        } else if (mediumTermChange < -15.0) {
            trendBuilder.append("Strong medium-term downtrend (").append(String.format("%.1f", mediumTermChange)).append("%). ");
        } else if (mediumTermChange < -5.0) {
            trendBuilder.append("Moderate medium-term downtrend (").append(String.format("%.1f", mediumTermChange)).append("%). ");
        } else {
            trendBuilder.append("Neutral medium-term trend (").append(String.format("%.1f", mediumTermChange)).append("%). ");
        }
        
        // Volatility context
        if (recentVolatility > 0.03) {
            trendBuilder.append("High volatility environment (").append(String.format("%.1f", recentVolatility * 100)).append("%). ");
        } else if (recentVolatility < 0.01) {
            trendBuilder.append("Low volatility environment (").append(String.format("%.1f", recentVolatility * 100)).append("%). ");
        } else {
            trendBuilder.append("Moderate volatility environment (").append(String.format("%.1f", recentVolatility * 100)).append("%). ");
        }
        
        return trendBuilder.toString();
    }
    
    /**
     * Calculate Simple Moving Average
     */
    private double calculateSMA(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            return data.stream().mapToDouble(HistoricalPriceData::getClose).average().orElse(0);
        }
        
        return data.stream()
            .limit(period)
            .mapToDouble(HistoricalPriceData::getClose)
            .average()
            .orElse(0);
    }
    
    /**
     * Calculate price change percentage over a period
     */
    private double calculatePriceChange(List<HistoricalPriceData> data, int period) {
        if (data.size() < period + 1) {
            period = data.size() - 1;
            if (period <= 0) {
                return 0.0;
            }
        }
        
        double currentPrice = data.get(0).getClose();
        double oldPrice = data.get(Math.min(period, data.size() - 1)).getClose();
        
        return ((currentPrice / oldPrice) - 1.0) * 100.0;
    }
    
    /**
     * Calculate price volatility over a period
     */
    private double calculateVolatility(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            period = data.size();
        }
        
        if (period <= 1) {
            return 0.0;
        }
        
        List<Double> returns = new ArrayList<>();
        for (int i = 0; i < period - 1; i++) {
            double todayPrice = data.get(i).getClose();
            double yesterdayPrice = data.get(i + 1).getClose();
            double dailyReturn = (todayPrice / yesterdayPrice) - 1.0;
            returns.add(dailyReturn);
        }
        
        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSquaredDeviations = returns.stream()
            .mapToDouble(r -> Math.pow(r - meanReturn, 2))
            .sum();
        
        return Math.sqrt(sumSquaredDeviations / (period - 1));
    }
    
    /**
     * Calculate average volume over a period
     */
    private double calculateAverageVolume(List<HistoricalPriceData> data, int period) {
        if (data.size() < period) {
            period = data.size();
        }
        
        return data.stream()
            .limit(period)
            .mapToDouble(HistoricalPriceData::getVolume)
            .average()
            .orElse(0);
    }
    
    /**
     * Calculate volume change percentage
     */
    private double calculateVolumeChange(List<HistoricalPriceData> data, int period) {
        if (data.size() < period + 1) {
            return 0.0;
        }
        
        double currentVolume = data.get(0).getVolume();
        double oldVolume = data.get(Math.min(period, data.size() - 1)).getVolume();
        
        if (oldVolume == 0) {
            return 0.0;
        }
        
        return ((currentVolume / oldVolume) - 1.0) * 100.0;
    }
    
    /**
     * Calculate Relative Strength Index
     */
    private double calculateRSI(List<HistoricalPriceData> data, int period) {
        if (data.size() <= period) {
            return 50.0; // Neutral RSI if not enough data
        }
        
        double sumGain = 0;
        double sumLoss = 0;
        
        // Calculate initial average gain/loss
        for (int i = 0; i < period; i++) {
            double change = data.get(i).getClose() - data.get(i + 1).getClose();
            if (change >= 0) {
                sumGain += change;
            } else {
                sumLoss -= change;
            }
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        
        // Calculate RSI
        double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
