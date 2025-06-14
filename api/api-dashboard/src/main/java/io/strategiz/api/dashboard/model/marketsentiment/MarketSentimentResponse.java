package io.strategiz.api.dashboard.model.marketsentiment;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Response model for market sentiment data following Synapse patterns.
 */
public class MarketSentimentResponse extends BaseServiceResponse {
    
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
     * Default constructor
     */
    public MarketSentimentResponse() {
    }
    
    /**
     * Gets the overall sentiment indicator
     *
     * @return Overall sentiment indicator
     */
    public SentimentIndicator getOverallSentiment() {
        return overallSentiment;
    }
    
    /**
     * Sets the overall sentiment indicator
     *
     * @param overallSentiment Overall sentiment indicator
     */
    public void setOverallSentiment(SentimentIndicator overallSentiment) {
        this.overallSentiment = overallSentiment;
    }
    
    /**
     * Gets the asset sentiments
     *
     * @return Asset sentiments
     */
    public List<AssetSentiment> getAssetSentiments() {
        return assetSentiments;
    }
    
    /**
     * Sets the asset sentiments
     *
     * @param assetSentiments Asset sentiments
     */
    public void setAssetSentiments(List<AssetSentiment> assetSentiments) {
        this.assetSentiments = assetSentiments;
    }
    
    /**
     * Gets the market trends
     *
     * @return Market trends
     */
    public MarketTrends getMarketTrends() {
        return marketTrends;
    }
    
    /**
     * Sets the market trends
     *
     * @param marketTrends Market trends
     */
    public void setMarketTrends(MarketTrends marketTrends) {
        this.marketTrends = marketTrends;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MarketSentimentResponse that = (MarketSentimentResponse) o;
        return Objects.equals(overallSentiment, that.overallSentiment) &&
               Objects.equals(assetSentiments, that.assetSentiments) &&
               Objects.equals(marketTrends, that.marketTrends);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), overallSentiment, assetSentiments, marketTrends);
    }
    
    @Override
    public String toString() {
        return "MarketSentimentResponse{" +
               "overallSentiment=" + overallSentiment +
               ", assetSentiments=" + assetSentiments +
               ", marketTrends=" + marketTrends +
               ", " + super.toString() +
               "}";
    }
    
    /**
     * Sentiment indicator
     */
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
        
        /**
         * Default constructor
         */
        public SentimentIndicator() {
        }
        
        /**
         * Gets the sentiment score
         *
         * @return Sentiment score
         */
        public BigDecimal getScore() {
            return score;
        }
        
        /**
         * Sets the sentiment score
         *
         * @param score Sentiment score
         */
        public void setScore(BigDecimal score) {
            this.score = score;
        }
        
        /**
         * Gets the sentiment category
         *
         * @return Sentiment category
         */
        public String getCategory() {
            return category;
        }
        
        /**
         * Sets the sentiment category
         *
         * @param category Sentiment category
         */
        public void setCategory(String category) {
            this.category = category;
        }
        
        /**
         * Gets the timestamp
         *
         * @return Timestamp
         */
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        /**
         * Sets the timestamp
         *
         * @param timestamp Timestamp
         */
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SentimentIndicator that = (SentimentIndicator) o;
            return Objects.equals(score, that.score) &&
                   Objects.equals(category, that.category) &&
                   Objects.equals(timestamp, that.timestamp);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(score, category, timestamp);
        }
        
        @Override
        public String toString() {
            return "SentimentIndicator{" +
                   "score=" + score +
                   ", category='" + category + '\'' +
                   ", timestamp=" + timestamp +
                   "}";
        }
    }
    
    /**
     * Asset-specific sentiment data
     */
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
        
        /**
         * Default constructor
         */
        public AssetSentiment() {
        }
        
        /**
         * Gets the asset symbol
         *
         * @return Asset symbol
         */
        public String getSymbol() {
            return symbol;
        }
        
        /**
         * Sets the asset symbol
         *
         * @param symbol Asset symbol
         */
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        /**
         * Gets the asset name
         *
         * @return Asset name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Sets the asset name
         *
         * @param name Asset name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Gets the sentiment score
         *
         * @return Sentiment score
         */
        public BigDecimal getSentimentScore() {
            return sentimentScore;
        }
        
        /**
         * Sets the sentiment score
         *
         * @param sentimentScore Sentiment score
         */
        public void setSentimentScore(BigDecimal sentimentScore) {
            this.sentimentScore = sentimentScore;
        }
        
        /**
         * Gets the sentiment category
         *
         * @return Sentiment category
         */
        public String getSentimentCategory() {
            return sentimentCategory;
        }
        
        /**
         * Sets the sentiment category
         *
         * @param sentimentCategory Sentiment category
         */
        public void setSentimentCategory(String sentimentCategory) {
            this.sentimentCategory = sentimentCategory;
        }
        
        /**
         * Gets the visualization color
         *
         * @return Visualization color
         */
        public String getColor() {
            return color;
        }
        
        /**
         * Sets the visualization color
         *
         * @param color Visualization color
         */
        public void setColor(String color) {
            this.color = color;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetSentiment that = (AssetSentiment) o;
            return Objects.equals(symbol, that.symbol) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(sentimentScore, that.sentimentScore) &&
                   Objects.equals(sentimentCategory, that.sentimentCategory) &&
                   Objects.equals(color, that.color);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(symbol, name, sentimentScore, sentimentCategory, color);
        }
        
        @Override
        public String toString() {
            return "AssetSentiment{" +
                   "symbol='" + symbol + '\'' +
                   ", name='" + name + '\'' +
                   ", sentimentScore=" + sentimentScore +
                   ", sentimentCategory='" + sentimentCategory + '\'' +
                   ", color='" + color + '\'' +
                   "}";
        }
    }
    
    /**
     * Market trends data
     */
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
        
        /**
         * Default constructor
         */
        public MarketTrends() {
        }
        
        /**
         * Gets the fear and greed index
         *
         * @return Fear and greed index
         */
        public BigDecimal getFearGreedIndex() {
            return fearGreedIndex;
        }
        
        /**
         * Sets the fear and greed index
         *
         * @param fearGreedIndex Fear and greed index
         */
        public void setFearGreedIndex(BigDecimal fearGreedIndex) {
            this.fearGreedIndex = fearGreedIndex;
        }
        
        /**
         * Gets the fear and greed category
         *
         * @return Fear and greed category
         */
        public String getFearGreedCategory() {
            return fearGreedCategory;
        }
        
        /**
         * Sets the fear and greed category
         *
         * @param fearGreedCategory Fear and greed category
         */
        public void setFearGreedCategory(String fearGreedCategory) {
            this.fearGreedCategory = fearGreedCategory;
        }
        
        /**
         * Gets the uptrend percentage
         *
         * @return Uptrend percentage
         */
        public BigDecimal getUptrendPercentage() {
            return uptrendPercentage;
        }
        
        /**
         * Sets the uptrend percentage
         *
         * @param uptrendPercentage Uptrend percentage
         */
        public void setUptrendPercentage(BigDecimal uptrendPercentage) {
            this.uptrendPercentage = uptrendPercentage;
        }
        
        /**
         * Gets the downtrend percentage
         *
         * @return Downtrend percentage
         */
        public BigDecimal getDowntrendPercentage() {
            return downtrendPercentage;
        }
        
        /**
         * Sets the downtrend percentage
         *
         * @param downtrendPercentage Downtrend percentage
         */
        public void setDowntrendPercentage(BigDecimal downtrendPercentage) {
            this.downtrendPercentage = downtrendPercentage;
        }
        
        /**
         * Gets the neutral trend percentage
         *
         * @return Neutral trend percentage
         */
        public BigDecimal getNeutralTrendPercentage() {
            return neutralTrendPercentage;
        }
        
        /**
         * Sets the neutral trend percentage
         *
         * @param neutralTrendPercentage Neutral trend percentage
         */
        public void setNeutralTrendPercentage(BigDecimal neutralTrendPercentage) {
            this.neutralTrendPercentage = neutralTrendPercentage;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarketTrends that = (MarketTrends) o;
            return Objects.equals(fearGreedIndex, that.fearGreedIndex) &&
                   Objects.equals(fearGreedCategory, that.fearGreedCategory) &&
                   Objects.equals(uptrendPercentage, that.uptrendPercentage) &&
                   Objects.equals(downtrendPercentage, that.downtrendPercentage) &&
                   Objects.equals(neutralTrendPercentage, that.neutralTrendPercentage);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fearGreedIndex, fearGreedCategory, uptrendPercentage, 
                    downtrendPercentage, neutralTrendPercentage);
        }
        
        @Override
        public String toString() {
            return "MarketTrends{" +
                   "fearGreedIndex=" + fearGreedIndex +
                   ", fearGreedCategory='" + fearGreedCategory + '\'' +
                   ", uptrendPercentage=" + uptrendPercentage +
                   ", downtrendPercentage=" + downtrendPercentage +
                   ", neutralTrendPercentage=" + neutralTrendPercentage +
                   "}";
        }
    }
}
