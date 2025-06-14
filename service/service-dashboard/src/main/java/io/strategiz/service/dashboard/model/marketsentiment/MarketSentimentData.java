package io.strategiz.service.dashboard.model.marketsentiment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * Market sentiment data containing sentiment metrics and trends
 */
public class MarketSentimentData {
    
    /**
     * Overall market sentiment indicators
     */
    @JsonProperty("sentiment_indicators")
    @NotNull
    private List<SentimentIndicator> sentimentIndicators;
    
    /**
     * Individual asset sentiment data
     */
    @JsonProperty("asset_sentiments")
    @NotNull
    private List<AssetSentiment> assetSentiments;
    
    /**
     * Market trends data
     */
    @JsonProperty("market_trends")
    @NotNull
    private MarketTrends marketTrends;

    // Constructors
    public MarketSentimentData() {}

    public MarketSentimentData(List<SentimentIndicator> sentimentIndicators, 
                             List<AssetSentiment> assetSentiments, 
                             MarketTrends marketTrends) {
        this.sentimentIndicators = sentimentIndicators;
        this.assetSentiments = assetSentiments;
        this.marketTrends = marketTrends;
    }

    // Getters and Setters
    public List<SentimentIndicator> getSentimentIndicators() {
        return sentimentIndicators;
    }

    public void setSentimentIndicators(List<SentimentIndicator> sentimentIndicators) {
        this.sentimentIndicators = sentimentIndicators;
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
        MarketSentimentData that = (MarketSentimentData) o;
        return Objects.equals(sentimentIndicators, that.sentimentIndicators) &&
               Objects.equals(assetSentiments, that.assetSentiments) &&
               Objects.equals(marketTrends, that.marketTrends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sentimentIndicators, assetSentiments, marketTrends);
    }

    @Override
    public String toString() {
        return "MarketSentimentData{" +
               "sentimentIndicators=" + sentimentIndicators +
               ", assetSentiments=" + assetSentiments +
               ", marketTrends=" + marketTrends +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SentimentIndicator> sentimentIndicators;
        private List<AssetSentiment> assetSentiments;
        private MarketTrends marketTrends;

        public Builder withSentimentIndicators(List<SentimentIndicator> sentimentIndicators) {
            this.sentimentIndicators = sentimentIndicators;
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

        public MarketSentimentData build() {
            return new MarketSentimentData(sentimentIndicators, assetSentiments, marketTrends);
        }
    }
}
