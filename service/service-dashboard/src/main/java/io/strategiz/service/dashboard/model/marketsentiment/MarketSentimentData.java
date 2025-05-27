package io.strategiz.service.dashboard.model.marketsentiment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data model for market sentiment.
 */
@Data
public class MarketSentimentData {
    
    /**
     * Overall market sentiment indicator
     */
    private SentimentIndicator overallSentiment;
    
    /**
     * Sentiment data for specific assets in the user's portfolio
     */
    private List<AssetSentiment> assetSentiments;
    
    /**
     * Market trend indicators
     */
    private MarketTrends marketTrends;
    
    /**
     * Sentiment indicator
     */
    @Data
    public static class SentimentIndicator {
        /**
         * Sentiment score from 0 (most bearish) to 100 (most bullish)
         */
        private BigDecimal score;
        
        /**
         * Sentiment category (Bearish, Slightly Bearish, Neutral, Slightly Bullish, Bullish)
         */
        private String category;
        
        /**
         * Timestamp of this sentiment reading
         */
        private LocalDateTime timestamp;
    }
    
    /**
     * Asset-specific sentiment data
     */
    @Data
    public static class AssetSentiment {
        /**
         * Asset symbol
         */
        private String symbol;
        
        /**
         * Asset name
         */
        private String name;
        
        /**
         * Sentiment score from 0 (most bearish) to 100 (most bullish)
         */
        private BigDecimal sentimentScore;
        
        /**
         * Sentiment category (Bearish, Slightly Bearish, Neutral, Slightly Bullish, Bullish)
         */
        private String sentimentCategory;
        
        /**
         * Color for visualization (usually green for bullish, red for bearish)
         */
        private String color;
    }
    
    /**
     * Market trends data
     */
    @Data
    public static class MarketTrends {
        /**
         * Fear and Greed Index value
         */
        private BigDecimal fearGreedIndex;
        
        /**
         * Fear and Greed category
         */
        private String fearGreedCategory;
        
        /**
         * Percentage of assets in uptrend across the market
         */
        private BigDecimal uptrendPercentage;
        
        /**
         * Percentage of assets in downtrend across the market
         */
        private BigDecimal downtrendPercentage;
        
        /**
         * Percentage of assets in sideways/neutral trend
         */
        private BigDecimal neutralTrendPercentage;
    }
}
