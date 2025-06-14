package io.strategiz.service.dashboard;

import io.strategiz.business.base.BaseService;
import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.client.coincap.CoinCapClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for market sentiment analysis within the Dashboard module.
 * This service provides fear & greed index, trending assets, and sentiment indicators.
 */
@Service
public class MarketSentimentService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(MarketSentimentService.class);

    private final PortfolioManager portfolioManager;
    private final CoinGeckoClient coinGeckoClient;
    private final AlphaVantageClient alphaVantageClient;
    
    // Color mapping for sentiment visualization
    private static final Map<String, String> SENTIMENT_COLORS = new HashMap<>();
    static {
        SENTIMENT_COLORS.put("Bearish", "#e74c3c");
        SENTIMENT_COLORS.put("Slightly Bearish", "#e67e22");
        SENTIMENT_COLORS.put("Neutral", "#f1c40f");
        SENTIMENT_COLORS.put("Slightly Bullish", "#2ecc71");
        SENTIMENT_COLORS.put("Bullish", "#27ae60");
    }

    @Autowired
    public MarketSentimentService(
            PortfolioManager portfolioManager,
            CoinGeckoClient coinGeckoClient,
            AlphaVantageClient alphaVantageClient) {
        this.portfolioManager = portfolioManager;
        this.coinGeckoClient = coinGeckoClient;
        this.alphaVantageClient = alphaVantageClient;
    }

    /**
     * Gets market sentiment data for the user's portfolio
     * 
     * @param userId The user ID to fetch sentiment data for
     * @return Market sentiment response
     */
    public MarketSentimentResponse getMarketSentiment(String userId) {
        log.info("Getting market sentiment for user: {}", userId);
        
        try {
            // Get portfolio data from the business layer
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            
            // Calculate market sentiment
            return calculateMarketSentiment(portfolioData);
        } catch (Exception e) {
            log.error("Error getting market sentiment for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve market sentiment data", e);
        }
    }
    
    /**
     * Calculates market sentiment from portfolio data
     * 
     * @param portfolioData Portfolio data
     * @return Market sentiment response
     */
    private MarketSentimentResponse calculateMarketSentiment(PortfolioData portfolioData) {
        MarketSentimentResponse response = new MarketSentimentResponse();
        
        // Calculate overall market sentiment
        MarketSentimentResponse.SentimentIndicator overallSentiment = calculateOverallSentiment();
        response.setOverallSentiment(overallSentiment);
        
        // Calculate asset-specific sentiment
        List<MarketSentimentResponse.AssetSentiment> assetSentiments = calculateAssetSentiment(portfolioData);
        response.setAssetSentiments(assetSentiments);
        
        // Calculate market trends
        MarketSentimentResponse.MarketTrends marketTrends = calculateMarketTrends();
        response.setMarketTrends(marketTrends);
        
        return response;
    }
    
    /**
     * Calculates overall market sentiment
     * 
     * @return Overall sentiment indicator
     */
    private MarketSentimentResponse.SentimentIndicator calculateOverallSentiment() {
        MarketSentimentResponse.SentimentIndicator sentiment = new MarketSentimentResponse.SentimentIndicator();
        
        try {
            // In a real implementation, we would fetch sentiment data from an external API
            // For now, we'll use the CoinGecko client to get market data
            
            // TODO: Replace with real sentiment API call
            // This is a placeholder implementation that generates a score between 0 and 100
            BigDecimal sentimentScore = fetchOverallMarketSentiment();
            String category = categorizeSentiment(sentimentScore);
            
            sentiment.setScore(sentimentScore);
            sentiment.setCategory(category);
            sentiment.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        } catch (Exception e) {
            log.error("Error calculating overall sentiment: {}", e.getMessage(), e);
            
            // Fallback to neutral sentiment
            sentiment.setScore(new BigDecimal("50"));
            sentiment.setCategory("Neutral");
            sentiment.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        }
        
        return sentiment;
    }
    
    /**
     * Calculates asset-specific sentiment for assets in the portfolio
     * 
     * @param portfolioData Portfolio data
     * @return List of asset-specific sentiment indicators
     */
    private List<MarketSentimentResponse.AssetSentiment> calculateAssetSentiment(PortfolioData portfolioData) {
        List<MarketSentimentResponse.AssetSentiment> assetSentiments = new ArrayList<>();
        
        // Check if portfolio has any assets
        if (portfolioData == null || portfolioData.getExchanges() == null || portfolioData.getExchanges().isEmpty()) {
            return assetSentiments;
        }
        
        try {
            // Process each exchange
            for (PortfolioData.ExchangeData exchangeData : portfolioData.getExchanges().values()) {
                if (exchangeData.getAssets() != null) {
                    // Process each asset
                    for (PortfolioData.AssetData assetData : exchangeData.getAssets().values()) {
                        // Skip assets with no value
                        if (assetData.getValue() == null || assetData.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                            continue;
                        }
                        
                        // Calculate sentiment for this asset
                        BigDecimal sentimentScore = fetchAssetSentiment(assetData.getSymbol());
                        String category = categorizeSentiment(sentimentScore);
                        
                        MarketSentimentResponse.AssetSentiment assetSentiment = new MarketSentimentResponse.AssetSentiment();
                        assetSentiment.setSymbol(assetData.getSymbol());
                        assetSentiment.setName(assetData.getName());
                        assetSentiment.setSentimentScore(sentimentScore);
                        assetSentiment.setSentimentCategory(category);
                        assetSentiment.setColor(SENTIMENT_COLORS.getOrDefault(category, "#95a5a6"));
                        
                        assetSentiments.add(assetSentiment);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calculating asset sentiment: {}", e.getMessage(), e);
        }
        
        return assetSentiments;
    }
    
    /**
     * Calculates market trend indicators
     * 
     * @return Market trends data
     */
    private MarketSentimentResponse.MarketTrends calculateMarketTrends() {
        MarketSentimentResponse.MarketTrends trends = new MarketSentimentResponse.MarketTrends();
        
        try {
            // In a real implementation, we would fetch this data from an external API
            // For now, we'll generate mock data
            
            // Fear and Greed Index (0-100)
            BigDecimal fearGreedIndex = fetchFearGreedIndex();
            String fearGreedCategory = categorizeFearGreed(fearGreedIndex);
            
            trends.setFearGreedIndex(fearGreedIndex);
            trends.setFearGreedCategory(fearGreedCategory);
            
            // Market trend percentages
            BigDecimal uptrendPercentage = fetchMarketUptrendPercentage();
            BigDecimal downtrendPercentage = fetchMarketDowntrendPercentage();
            BigDecimal neutralTrendPercentage = new BigDecimal("100")
                .subtract(uptrendPercentage)
                .subtract(downtrendPercentage);
            
            trends.setUptrendPercentage(uptrendPercentage);
            trends.setDowntrendPercentage(downtrendPercentage);
            trends.setNeutralTrendPercentage(neutralTrendPercentage);
        } catch (Exception e) {
            log.error("Error calculating market trends: {}", e.getMessage(), e);
            
            // Fallback to neutral values
            trends.setFearGreedIndex(new BigDecimal("50"));
            trends.setFearGreedCategory("Neutral");
            trends.setUptrendPercentage(new BigDecimal("33.33"));
            trends.setDowntrendPercentage(new BigDecimal("33.33"));
            trends.setNeutralTrendPercentage(new BigDecimal("33.34"));
        }
        
        return trends;
    }
    
    /**
     * Fetches overall market sentiment
     * 
     * @return Sentiment score from 0 (bearish) to 100 (bullish)
     */
    private BigDecimal fetchOverallMarketSentiment() {
        // TODO: Replace with actual API call in production
        // For now, we'll generate a random score
        // In a real implementation, this would be based on external market sentiment data
        
        try {
            // Get market data from CoinGecko
            Map<String, Object> marketData = coinGeckoClient.getGlobalMarketData();
            if (marketData != null && marketData.containsKey("market_cap_change_percentage_24h_usd")) {
                Double marketCapChange = (Double) marketData.get("market_cap_change_percentage_24h_usd");
                
                // Convert market cap change to a sentiment score (0-100)
                // If market cap is up by 5% or more, sentiment is very bullish (80-100)
                // If market cap is down by 5% or more, sentiment is very bearish (0-20)
                if (marketCapChange != null) {
                    double normalizedChange = (marketCapChange + 5.0) / 10.0; // Normalize to 0-1 range
                    normalizedChange = Math.max(0.0, Math.min(1.0, normalizedChange)); // Clamp to 0-1
                    double sentimentValue = normalizedChange * 100.0;
                    return new BigDecimal(String.valueOf(sentimentValue)).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching market data from CoinGecko: {}", e.getMessage(), e);
        }
        
        // Fallback to a random value between 30 and 70
        Random random = new Random();
        double randomValue = 30.0 + (random.nextDouble() * 40.0);
        return new BigDecimal(String.valueOf(randomValue)).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Fetches asset-specific sentiment
     * 
     * @param symbol Asset symbol
     * @return Sentiment score from 0 (bearish) to 100 (bullish)
     */
    private BigDecimal fetchAssetSentiment(String symbol) {
        // TODO: Replace with actual API call in production
        // For now, we'll generate a random score
        // In a real implementation, this would be based on external asset sentiment data
        
        try {
            // Generate a pseudo-random but consistent score based on the symbol
            int hashCode = Math.abs(symbol.hashCode());
            double baseValue = (hashCode % 50) + 25.0; // 25-75 range
            
            // Add some randomness
            Random random = new Random();
            double randomOffset = random.nextDouble() * 10.0 - 5.0; // -5 to +5
            
            double sentimentValue = baseValue + randomOffset;
            sentimentValue = Math.max(0.0, Math.min(100.0, sentimentValue)); // Clamp to 0-100
            
            return new BigDecimal(String.valueOf(sentimentValue)).setScale(2, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            log.error("Error generating sentiment for asset {}: {}", symbol, e.getMessage(), e);
            
            // Fallback to neutral sentiment
            return new BigDecimal("50");
        }
    }
    
    /**
     * Fetches the Fear and Greed Index
     * 
     * @return Fear and Greed Index value from 0 (extreme fear) to 100 (extreme greed)
     */
    private BigDecimal fetchFearGreedIndex() {
        // TODO: Replace with actual API call in production
        // For now, we'll generate a random score
        // In a real implementation, this would fetch the Fear & Greed Index from an API
        
        try {
            // Generate a random value between 20 and 80
            Random random = new Random();
            double randomValue = 20.0 + (random.nextDouble() * 60.0);
            return new BigDecimal(String.valueOf(randomValue)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error fetching Fear and Greed Index: {}", e.getMessage(), e);
            
            // Fallback to neutral value
            return new BigDecimal("50");
        }
    }
    
    /**
     * Fetches the percentage of assets in uptrend across the market
     * 
     * @return Percentage of assets in uptrend
     */
    private BigDecimal fetchMarketUptrendPercentage() {
        // TODO: Replace with actual API call in production
        // For now, we'll generate a random value
        
        try {
            // Generate a random value between 20% and 60%
            Random random = new Random();
            double randomValue = 20.0 + (random.nextDouble() * 40.0);
            return new BigDecimal(String.valueOf(randomValue)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error fetching market uptrend percentage: {}", e.getMessage(), e);
            
            // Fallback to neutral value
            return new BigDecimal("33.33");
        }
    }
    
    /**
     * Fetches the percentage of assets in downtrend across the market
     * 
     * @return Percentage of assets in downtrend
     */
    private BigDecimal fetchMarketDowntrendPercentage() {
        // TODO: Replace with actual API call in production
        // For now, we'll generate a random value
        
        try {
            // Generate a random value between 20% and 60%
            Random random = new Random();
            double randomValue = 20.0 + (random.nextDouble() * 40.0);
            return new BigDecimal(String.valueOf(randomValue)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Error fetching market downtrend percentage: {}", e.getMessage(), e);
            
            // Fallback to neutral value
            return new BigDecimal("33.33");
        }
    }
    
    /**
     * Categorizes a sentiment score into a category
     * 
     * @param score Sentiment score from 0 to 100
     * @return Sentiment category
     */
    private String categorizeSentiment(BigDecimal score) {
        if (score == null) {
            return "Neutral";
        }
        
        if (score.compareTo(new BigDecimal("20")) < 0) {
            return "Bearish";
        } else if (score.compareTo(new BigDecimal("40")) < 0) {
            return "Slightly Bearish";
        } else if (score.compareTo(new BigDecimal("60")) < 0) {
            return "Neutral";
        } else if (score.compareTo(new BigDecimal("80")) < 0) {
            return "Slightly Bullish";
        } else {
            return "Bullish";
        }
    }
    
    /**
     * Categorizes a Fear and Greed Index value into a category
     * 
     * @param index Fear and Greed Index value from 0 to 100
     * @return Fear and Greed category
     */
    private String categorizeFearGreed(BigDecimal index) {
        if (index == null) {
            return "Neutral";
        }
        
        if (index.compareTo(new BigDecimal("20")) < 0) {
            return "Extreme Fear";
        } else if (index.compareTo(new BigDecimal("40")) < 0) {
            return "Fear";
        } else if (index.compareTo(new BigDecimal("60")) < 0) {
            return "Neutral";
        } else if (index.compareTo(new BigDecimal("80")) < 0) {
            return "Greed";
        } else {
            return "Extreme Greed";
        }
    }
}
