package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Sentiment indicator (from MarketSentimentData)
 */
public class MarketSentimentIndicator {
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

    // Constructors
    public MarketSentimentIndicator() {}

    public MarketSentimentIndicator(BigDecimal score, String category, LocalDateTime timestamp) {
        this.score = score;
        this.category = category;
        this.timestamp = timestamp;
    }

    // Getters and Setters
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
        MarketSentimentIndicator that = (MarketSentimentIndicator) o;
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
        return "MarketSentimentIndicator{" +
               "score=" + score +
               ", category='" + category + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal score;
        private String category;
        private LocalDateTime timestamp;

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

        public MarketSentimentIndicator build() {
            return new MarketSentimentIndicator(score, category, timestamp);
        }
    }
}
