package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for market sentiment data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSentimentResponse {
    
    private FearGreedIndex fearGreedIndex;
    private List<SentimentIndicator> indicators;
    private Map<String, TrendingAsset> trendingAssets;
    private LocalDateTime lastUpdated;
    private SentimentIndicator overallSentiment;
    private List<AssetSentiment> assetSentiments;
    private MarketTrends marketTrends;
    
    /**
     * Fear and Greed Index data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FearGreedIndex {
        private int value;
        private String classification;
        private BigDecimal previousClose;
        private BigDecimal weekAgo;
        private BigDecimal monthAgo;
    }
    
    /**
     * Individual sentiment indicator.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentIndicator {
        private String name;
        private BigDecimal value;
        private String signal; // "bullish", "neutral", "bearish"
        private String description;
        private BigDecimal score;
        private String category;
        private LocalDateTime timestamp;
    }
    
    /**
     * Data for trending assets in the market.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendingAsset {
        private String id;
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal changePercent24h;
        private BigDecimal marketCap;
        private BigDecimal volume24h;
        private int rank;
    }
    
    /**
     * Sentiment data for a specific asset.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetSentiment {
        private String assetId;
        private String symbol;
        private String name;
        private String sentiment;
        private BigDecimal sentimentScore;
        private String color;
        private List<String> signals;
        private Map<String, BigDecimal> technicalIndicators;
        private String sentimentCategory;
    }
    
    /**
     * Market trend data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketTrends {
        private BigDecimal cryptoMarketCap;
        private BigDecimal cryptoVolume24h;
        private BigDecimal btcDominance;
        private BigDecimal stockMarketCap;
        private BigDecimal stockVolume24h;
        private Map<String, BigDecimal> indexPerformance;
        private Map<String, BigDecimal> sectorPerformance;
        private BigDecimal fearGreedIndex;
        private String fearGreedCategory;
        private BigDecimal uptrendPercentage;
        private BigDecimal downtrendPercentage;
        private BigDecimal neutralTrendPercentage;
    }
}
