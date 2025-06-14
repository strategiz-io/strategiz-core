package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response model for market sentiment data.
 */
public class MarketSentimentResponse {
    
    private FearGreedIndex fearGreedIndex;
    private List<SentimentIndicator> indicators;
    private Map<String, TrendingAsset> trendingAssets;
    private LocalDateTime lastUpdated;
    private SentimentIndicator overallSentiment;
    private List<AssetSentiment> assetSentiments;
    private MarketTrends marketTrends;

    // Constructors
    public MarketSentimentResponse() {}

    public MarketSentimentResponse(FearGreedIndex fearGreedIndex, List<SentimentIndicator> indicators,
                                  Map<String, TrendingAsset> trendingAssets, LocalDateTime lastUpdated,
                                  SentimentIndicator overallSentiment, List<AssetSentiment> assetSentiments,
                                  MarketTrends marketTrends) {
        this.fearGreedIndex = fearGreedIndex;
        this.indicators = indicators;
        this.trendingAssets = trendingAssets;
        this.lastUpdated = lastUpdated;
        this.overallSentiment = overallSentiment;
        this.assetSentiments = assetSentiments;
        this.marketTrends = marketTrends;
    }

    // Getters and Setters
    public FearGreedIndex getFearGreedIndex() {
        return fearGreedIndex;
    }

    public void setFearGreedIndex(FearGreedIndex fearGreedIndex) {
        this.fearGreedIndex = fearGreedIndex;
    }

    public List<SentimentIndicator> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<SentimentIndicator> indicators) {
        this.indicators = indicators;
    }

    public Map<String, TrendingAsset> getTrendingAssets() {
        return trendingAssets;
    }

    public void setTrendingAssets(Map<String, TrendingAsset> trendingAssets) {
        this.trendingAssets = trendingAssets;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public SentimentIndicator getOverallSentiment() {
        return overallSentiment;
    }

    public void setOverallSentiment(SentimentIndicator overallSentiment) {
        this.overallSentiment = overallSentiment;
    }

    public List<AssetSentiment> getAssetSentiments() {
        return assetSentiments;
    }

    public void setAssetSentiments(List<AssetSentiment> assetSentiments) {
        this.assetSentiments = assetSentiments;
    }

    public MarketTrends getMarketTrends() {
        return marketTrends;
    }

    public void setMarketTrends(MarketTrends marketTrends) {
        this.marketTrends = marketTrends;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketSentimentResponse that = (MarketSentimentResponse) o;
        return Objects.equals(fearGreedIndex, that.fearGreedIndex) &&
               Objects.equals(indicators, that.indicators) &&
               Objects.equals(trendingAssets, that.trendingAssets) &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(overallSentiment, that.overallSentiment) &&
               Objects.equals(assetSentiments, that.assetSentiments) &&
               Objects.equals(marketTrends, that.marketTrends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fearGreedIndex, indicators, trendingAssets, lastUpdated, 
                           overallSentiment, assetSentiments, marketTrends);
    }

    @Override
    public String toString() {
        return "MarketSentimentResponse{" +
               "fearGreedIndex=" + fearGreedIndex +
               ", indicators=" + indicators +
               ", trendingAssets=" + trendingAssets +
               ", lastUpdated=" + lastUpdated +
               ", overallSentiment=" + overallSentiment +
               ", assetSentiments=" + assetSentiments +
               ", marketTrends=" + marketTrends +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FearGreedIndex fearGreedIndex;
        private List<SentimentIndicator> indicators;
        private Map<String, TrendingAsset> trendingAssets;
        private LocalDateTime lastUpdated;
        private SentimentIndicator overallSentiment;
        private List<AssetSentiment> assetSentiments;
        private MarketTrends marketTrends;

        public Builder withFearGreedIndex(FearGreedIndex fearGreedIndex) {
            this.fearGreedIndex = fearGreedIndex;
            return this;
        }

        public Builder withIndicators(List<SentimentIndicator> indicators) {
            this.indicators = indicators;
            return this;
        }

        public Builder withTrendingAssets(Map<String, TrendingAsset> trendingAssets) {
            this.trendingAssets = trendingAssets;
            return this;
        }

        public Builder withLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withOverallSentiment(SentimentIndicator overallSentiment) {
            this.overallSentiment = overallSentiment;
            return this;
        }

        public Builder withAssetSentiments(List<AssetSentiment> assetSentiments) {
            this.assetSentiments = assetSentiments;
            return this;
        }

        public Builder withMarketTrends(MarketTrends marketTrends) {
            this.marketTrends = marketTrends;
            return this;
        }

        public MarketSentimentResponse build() {
            return new MarketSentimentResponse(fearGreedIndex, indicators, trendingAssets, 
                                             lastUpdated, overallSentiment, assetSentiments, marketTrends);
        }
    }
    
    /**
     * Fear and Greed Index data.
     */
    public static class FearGreedIndex {
        private int value;
        private String classification;
        private BigDecimal previousClose;
        private BigDecimal weekAgo;
        private BigDecimal monthAgo;

        // Constructors
        public FearGreedIndex() {}

        public FearGreedIndex(int value, String classification, BigDecimal previousClose, 
                             BigDecimal weekAgo, BigDecimal monthAgo) {
            this.value = value;
            this.classification = classification;
            this.previousClose = previousClose;
            this.weekAgo = weekAgo;
            this.monthAgo = monthAgo;
        }

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        public BigDecimal getPreviousClose() {
            return previousClose;
        }

        public void setPreviousClose(BigDecimal previousClose) {
            this.previousClose = previousClose;
        }

        public BigDecimal getWeekAgo() {
            return weekAgo;
        }

        public void setWeekAgo(BigDecimal weekAgo) {
            this.weekAgo = weekAgo;
        }

        public BigDecimal getMonthAgo() {
            return monthAgo;
        }

        public void setMonthAgo(BigDecimal monthAgo) {
            this.monthAgo = monthAgo;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FearGreedIndex that = (FearGreedIndex) o;
            return value == that.value &&
                   Objects.equals(classification, that.classification) &&
                   Objects.equals(previousClose, that.previousClose) &&
                   Objects.equals(weekAgo, that.weekAgo) &&
                   Objects.equals(monthAgo, that.monthAgo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, classification, previousClose, weekAgo, monthAgo);
        }

        @Override
        public String toString() {
            return "FearGreedIndex{" +
                   "value=" + value +
                   ", classification='" + classification + '\'' +
                   ", previousClose=" + previousClose +
                   ", weekAgo=" + weekAgo +
                   ", monthAgo=" + monthAgo +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int value;
            private String classification;
            private BigDecimal previousClose;
            private BigDecimal weekAgo;
            private BigDecimal monthAgo;

            public Builder withValue(int value) {
                this.value = value;
                return this;
            }

            public Builder withClassification(String classification) {
                this.classification = classification;
                return this;
            }

            public Builder withPreviousClose(BigDecimal previousClose) {
                this.previousClose = previousClose;
                return this;
            }

            public Builder withWeekAgo(BigDecimal weekAgo) {
                this.weekAgo = weekAgo;
                return this;
            }

            public Builder withMonthAgo(BigDecimal monthAgo) {
                this.monthAgo = monthAgo;
                return this;
            }

            public FearGreedIndex build() {
                return new FearGreedIndex(value, classification, previousClose, weekAgo, monthAgo);
            }
        }
    }
    
    /**
     * Individual sentiment indicator.
     */
    public static class SentimentIndicator {
        private String name;
        private BigDecimal value;
        private String signal; // "bullish", "neutral", "bearish"
        private String description;
        private BigDecimal score;
        private String category;
        private LocalDateTime timestamp;

        // Constructors
        public SentimentIndicator() {}

        public SentimentIndicator(String name, BigDecimal value, String signal, String description,
                                 BigDecimal score, String category, LocalDateTime timestamp) {
            this.name = name;
            this.value = value;
            this.signal = signal;
            this.description = description;
            this.score = score;
            this.category = category;
            this.timestamp = timestamp;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public String getSignal() {
            return signal;
        }

        public void setSignal(String signal) {
            this.signal = signal;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SentimentIndicator that = (SentimentIndicator) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(value, that.value) &&
                   Objects.equals(signal, that.signal) &&
                   Objects.equals(description, that.description) &&
                   Objects.equals(score, that.score) &&
                   Objects.equals(category, that.category) &&
                   Objects.equals(timestamp, that.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, signal, description, score, category, timestamp);
        }

        @Override
        public String toString() {
            return "SentimentIndicator{" +
                   "name='" + name + '\'' +
                   ", value=" + value +
                   ", signal='" + signal + '\'' +
                   ", description='" + description + '\'' +
                   ", score=" + score +
                   ", category='" + category + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private BigDecimal value;
            private String signal;
            private String description;
            private BigDecimal score;
            private String category;
            private LocalDateTime timestamp;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withValue(BigDecimal value) {
                this.value = value;
                return this;
            }

            public Builder withSignal(String signal) {
                this.signal = signal;
                return this;
            }

            public Builder withDescription(String description) {
                this.description = description;
                return this;
            }

            public Builder withScore(BigDecimal score) {
                this.score = score;
                return this;
            }

            public Builder withCategory(String category) {
                this.category = category;
                return this;
            }

            public Builder withTimestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public SentimentIndicator build() {
                return new SentimentIndicator(name, value, signal, description, score, category, timestamp);
            }
        }
    }
    
    /**
     * Data for trending assets in the market.
     */
    public static class TrendingAsset {
        private String id;
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal changePercent24h;
        private BigDecimal marketCap;
        private BigDecimal volume24h;
        private int rank;

        // Constructors
        public TrendingAsset() {}

        public TrendingAsset(String id, String symbol, String name, BigDecimal price,
                           BigDecimal changePercent24h, BigDecimal marketCap, BigDecimal volume24h, int rank) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.changePercent24h = changePercent24h;
            this.marketCap = marketCap;
            this.volume24h = volume24h;
            this.rank = rank;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getChangePercent24h() {
            return changePercent24h;
        }

        public void setChangePercent24h(BigDecimal changePercent24h) {
            this.changePercent24h = changePercent24h;
        }

        public BigDecimal getMarketCap() {
            return marketCap;
        }

        public void setMarketCap(BigDecimal marketCap) {
            this.marketCap = marketCap;
        }

        public BigDecimal getVolume24h() {
            return volume24h;
        }

        public void setVolume24h(BigDecimal volume24h) {
            this.volume24h = volume24h;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrendingAsset that = (TrendingAsset) o;
            return rank == that.rank &&
                   Objects.equals(id, that.id) &&
                   Objects.equals(symbol, that.symbol) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(price, that.price) &&
                   Objects.equals(changePercent24h, that.changePercent24h) &&
                   Objects.equals(marketCap, that.marketCap) &&
                   Objects.equals(volume24h, that.volume24h);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, symbol, name, price, changePercent24h, marketCap, volume24h, rank);
        }

        @Override
        public String toString() {
            return "TrendingAsset{" +
                   "id='" + id + '\'' +
                   ", symbol='" + symbol + '\'' +
                   ", name='" + name + '\'' +
                   ", price=" + price +
                   ", changePercent24h=" + changePercent24h +
                   ", marketCap=" + marketCap +
                   ", volume24h=" + volume24h +
                   ", rank=" + rank +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String id;
            private String symbol;
            private String name;
            private BigDecimal price;
            private BigDecimal changePercent24h;
            private BigDecimal marketCap;
            private BigDecimal volume24h;
            private int rank;

            public Builder withId(String id) {
                this.id = id;
                return this;
            }

            public Builder withSymbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withPrice(BigDecimal price) {
                this.price = price;
                return this;
            }

            public Builder withChangePercent24h(BigDecimal changePercent24h) {
                this.changePercent24h = changePercent24h;
                return this;
            }

            public Builder withMarketCap(BigDecimal marketCap) {
                this.marketCap = marketCap;
                return this;
            }

            public Builder withVolume24h(BigDecimal volume24h) {
                this.volume24h = volume24h;
                return this;
            }

            public Builder withRank(int rank) {
                this.rank = rank;
                return this;
            }

            public TrendingAsset build() {
                return new TrendingAsset(id, symbol, name, price, changePercent24h, marketCap, volume24h, rank);
            }
        }
    }
    
    /**
     * Sentiment data for a specific asset.
     */
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

        // Constructors
        public AssetSentiment() {}

        public AssetSentiment(String assetId, String symbol, String name, String sentiment,
                            BigDecimal sentimentScore, String color, List<String> signals,
                            Map<String, BigDecimal> technicalIndicators, String sentimentCategory) {
            this.assetId = assetId;
            this.symbol = symbol;
            this.name = name;
            this.sentiment = sentiment;
            this.sentimentScore = sentimentScore;
            this.color = color;
            this.signals = signals;
            this.technicalIndicators = technicalIndicators;
            this.sentimentCategory = sentimentCategory;
        }

        // Getters and Setters
        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(String sentiment) {
            this.sentiment = sentiment;
        }

        public BigDecimal getSentimentScore() {
            return sentimentScore;
        }

        public void setSentimentScore(BigDecimal sentimentScore) {
            this.sentimentScore = sentimentScore;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public List<String> getSignals() {
            return signals;
        }

        public void setSignals(List<String> signals) {
            this.signals = signals;
        }

        public Map<String, BigDecimal> getTechnicalIndicators() {
            return technicalIndicators;
        }

        public void setTechnicalIndicators(Map<String, BigDecimal> technicalIndicators) {
            this.technicalIndicators = technicalIndicators;
        }

        public String getSentimentCategory() {
            return sentimentCategory;
        }

        public void setSentimentCategory(String sentimentCategory) {
            this.sentimentCategory = sentimentCategory;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AssetSentiment that = (AssetSentiment) o;
            return Objects.equals(assetId, that.assetId) &&
                   Objects.equals(symbol, that.symbol) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(sentiment, that.sentiment) &&
                   Objects.equals(sentimentScore, that.sentimentScore) &&
                   Objects.equals(color, that.color) &&
                   Objects.equals(signals, that.signals) &&
                   Objects.equals(technicalIndicators, that.technicalIndicators) &&
                   Objects.equals(sentimentCategory, that.sentimentCategory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(assetId, symbol, name, sentiment, sentimentScore, color, 
                              signals, technicalIndicators, sentimentCategory);
        }

        @Override
        public String toString() {
            return "AssetSentiment{" +
                   "assetId='" + assetId + '\'' +
                   ", symbol='" + symbol + '\'' +
                   ", name='" + name + '\'' +
                   ", sentiment='" + sentiment + '\'' +
                   ", sentimentScore=" + sentimentScore +
                   ", color='" + color + '\'' +
                   ", signals=" + signals +
                   ", technicalIndicators=" + technicalIndicators +
                   ", sentimentCategory='" + sentimentCategory + '\'' +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String assetId;
            private String symbol;
            private String name;
            private String sentiment;
            private BigDecimal sentimentScore;
            private String color;
            private List<String> signals;
            private Map<String, BigDecimal> technicalIndicators;
            private String sentimentCategory;

            public Builder withAssetId(String assetId) {
                this.assetId = assetId;
                return this;
            }

            public Builder withSymbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withSentiment(String sentiment) {
                this.sentiment = sentiment;
                return this;
            }

            public Builder withSentimentScore(BigDecimal sentimentScore) {
                this.sentimentScore = sentimentScore;
                return this;
            }

            public Builder withColor(String color) {
                this.color = color;
                return this;
            }

            public Builder withSignals(List<String> signals) {
                this.signals = signals;
                return this;
            }

            public Builder withTechnicalIndicators(Map<String, BigDecimal> technicalIndicators) {
                this.technicalIndicators = technicalIndicators;
                return this;
            }

            public Builder withSentimentCategory(String sentimentCategory) {
                this.sentimentCategory = sentimentCategory;
                return this;
            }

            public AssetSentiment build() {
                return new AssetSentiment(assetId, symbol, name, sentiment, sentimentScore, 
                                        color, signals, technicalIndicators, sentimentCategory);
            }
        }
    }
    
    /**
     * Market trend data.
     */
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

        // Constructors
        public MarketTrends() {}

        public MarketTrends(BigDecimal cryptoMarketCap, BigDecimal cryptoVolume24h, BigDecimal btcDominance,
                          BigDecimal stockMarketCap, BigDecimal stockVolume24h, Map<String, BigDecimal> indexPerformance,
                          Map<String, BigDecimal> sectorPerformance, BigDecimal fearGreedIndex, String fearGreedCategory,
                          BigDecimal uptrendPercentage, BigDecimal downtrendPercentage, BigDecimal neutralTrendPercentage) {
            this.cryptoMarketCap = cryptoMarketCap;
            this.cryptoVolume24h = cryptoVolume24h;
            this.btcDominance = btcDominance;
            this.stockMarketCap = stockMarketCap;
            this.stockVolume24h = stockVolume24h;
            this.indexPerformance = indexPerformance;
            this.sectorPerformance = sectorPerformance;
            this.fearGreedIndex = fearGreedIndex;
            this.fearGreedCategory = fearGreedCategory;
            this.uptrendPercentage = uptrendPercentage;
            this.downtrendPercentage = downtrendPercentage;
            this.neutralTrendPercentage = neutralTrendPercentage;
        }

        // Getters and Setters
        public BigDecimal getCryptoMarketCap() {
            return cryptoMarketCap;
        }

        public void setCryptoMarketCap(BigDecimal cryptoMarketCap) {
            this.cryptoMarketCap = cryptoMarketCap;
        }

        public BigDecimal getCryptoVolume24h() {
            return cryptoVolume24h;
        }

        public void setCryptoVolume24h(BigDecimal cryptoVolume24h) {
            this.cryptoVolume24h = cryptoVolume24h;
        }

        public BigDecimal getBtcDominance() {
            return btcDominance;
        }

        public void setBtcDominance(BigDecimal btcDominance) {
            this.btcDominance = btcDominance;
        }

        public BigDecimal getStockMarketCap() {
            return stockMarketCap;
        }

        public void setStockMarketCap(BigDecimal stockMarketCap) {
            this.stockMarketCap = stockMarketCap;
        }

        public BigDecimal getStockVolume24h() {
            return stockVolume24h;
        }

        public void setStockVolume24h(BigDecimal stockVolume24h) {
            this.stockVolume24h = stockVolume24h;
        }

        public Map<String, BigDecimal> getIndexPerformance() {
            return indexPerformance;
        }

        public void setIndexPerformance(Map<String, BigDecimal> indexPerformance) {
            this.indexPerformance = indexPerformance;
        }

        public Map<String, BigDecimal> getSectorPerformance() {
            return sectorPerformance;
        }

        public void setSectorPerformance(Map<String, BigDecimal> sectorPerformance) {
            this.sectorPerformance = sectorPerformance;
        }

        public BigDecimal getFearGreedIndex() {
            return fearGreedIndex;
        }

        public void setFearGreedIndex(BigDecimal fearGreedIndex) {
            this.fearGreedIndex = fearGreedIndex;
        }

        public String getFearGreedCategory() {
            return fearGreedCategory;
        }

        public void setFearGreedCategory(String fearGreedCategory) {
            this.fearGreedCategory = fearGreedCategory;
        }

        public BigDecimal getUptrendPercentage() {
            return uptrendPercentage;
        }

        public void setUptrendPercentage(BigDecimal uptrendPercentage) {
            this.uptrendPercentage = uptrendPercentage;
        }

        public BigDecimal getDowntrendPercentage() {
            return downtrendPercentage;
        }

        public void setDowntrendPercentage(BigDecimal downtrendPercentage) {
            this.downtrendPercentage = downtrendPercentage;
        }

        public BigDecimal getNeutralTrendPercentage() {
            return neutralTrendPercentage;
        }

        public void setNeutralTrendPercentage(BigDecimal neutralTrendPercentage) {
            this.neutralTrendPercentage = neutralTrendPercentage;
        }

        // equals, hashCode, toString
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarketTrends that = (MarketTrends) o;
            return Objects.equals(cryptoMarketCap, that.cryptoMarketCap) &&
                   Objects.equals(cryptoVolume24h, that.cryptoVolume24h) &&
                   Objects.equals(btcDominance, that.btcDominance) &&
                   Objects.equals(stockMarketCap, that.stockMarketCap) &&
                   Objects.equals(stockVolume24h, that.stockVolume24h) &&
                   Objects.equals(indexPerformance, that.indexPerformance) &&
                   Objects.equals(sectorPerformance, that.sectorPerformance) &&
                   Objects.equals(fearGreedIndex, that.fearGreedIndex) &&
                   Objects.equals(fearGreedCategory, that.fearGreedCategory) &&
                   Objects.equals(uptrendPercentage, that.uptrendPercentage) &&
                   Objects.equals(downtrendPercentage, that.downtrendPercentage) &&
                   Objects.equals(neutralTrendPercentage, that.neutralTrendPercentage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cryptoMarketCap, cryptoVolume24h, btcDominance, stockMarketCap, stockVolume24h,
                              indexPerformance, sectorPerformance, fearGreedIndex, fearGreedCategory,
                              uptrendPercentage, downtrendPercentage, neutralTrendPercentage);
        }

        @Override
        public String toString() {
            return "MarketTrends{" +
                   "cryptoMarketCap=" + cryptoMarketCap +
                   ", cryptoVolume24h=" + cryptoVolume24h +
                   ", btcDominance=" + btcDominance +
                   ", stockMarketCap=" + stockMarketCap +
                   ", stockVolume24h=" + stockVolume24h +
                   ", indexPerformance=" + indexPerformance +
                   ", sectorPerformance=" + sectorPerformance +
                   ", fearGreedIndex=" + fearGreedIndex +
                   ", fearGreedCategory='" + fearGreedCategory + '\'' +
                   ", uptrendPercentage=" + uptrendPercentage +
                   ", downtrendPercentage=" + downtrendPercentage +
                   ", neutralTrendPercentage=" + neutralTrendPercentage +
                   '}';
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
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

            public Builder withCryptoMarketCap(BigDecimal cryptoMarketCap) {
                this.cryptoMarketCap = cryptoMarketCap;
                return this;
            }

            public Builder withCryptoVolume24h(BigDecimal cryptoVolume24h) {
                this.cryptoVolume24h = cryptoVolume24h;
                return this;
            }

            public Builder withBtcDominance(BigDecimal btcDominance) {
                this.btcDominance = btcDominance;
                return this;
            }

            public Builder withStockMarketCap(BigDecimal stockMarketCap) {
                this.stockMarketCap = stockMarketCap;
                return this;
            }

            public Builder withStockVolume24h(BigDecimal stockVolume24h) {
                this.stockVolume24h = stockVolume24h;
                return this;
            }

            public Builder withIndexPerformance(Map<String, BigDecimal> indexPerformance) {
                this.indexPerformance = indexPerformance;
                return this;
            }

            public Builder withSectorPerformance(Map<String, BigDecimal> sectorPerformance) {
                this.sectorPerformance = sectorPerformance;
                return this;
            }

            public Builder withFearGreedIndex(BigDecimal fearGreedIndex) {
                this.fearGreedIndex = fearGreedIndex;
                return this;
            }

            public Builder withFearGreedCategory(String fearGreedCategory) {
                this.fearGreedCategory = fearGreedCategory;
                return this;
            }

            public Builder withUptrendPercentage(BigDecimal uptrendPercentage) {
                this.uptrendPercentage = uptrendPercentage;
                return this;
            }

            public Builder withDowntrendPercentage(BigDecimal downtrendPercentage) {
                this.downtrendPercentage = downtrendPercentage;
                return this;
            }

            public Builder withNeutralTrendPercentage(BigDecimal neutralTrendPercentage) {
                this.neutralTrendPercentage = neutralTrendPercentage;
                return this;
            }

            public MarketTrends build() {
                return new MarketTrends(cryptoMarketCap, cryptoVolume24h, btcDominance, stockMarketCap, stockVolume24h,
                                      indexPerformance, sectorPerformance, fearGreedIndex, fearGreedCategory,
                                      uptrendPercentage, downtrendPercentage, neutralTrendPercentage);
            }
        }
    }
}
