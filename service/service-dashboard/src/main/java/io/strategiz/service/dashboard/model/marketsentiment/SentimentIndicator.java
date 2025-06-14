package io.strategiz.service.dashboard.model.marketsentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Individual sentiment indicator.
 */
public class SentimentIndicator {
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
