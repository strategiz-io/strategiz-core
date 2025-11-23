package io.strategiz.service.livestrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;
import java.util.Map;

/**
 * Response model for alert trigger history.
 * Corresponds to the "Alert History Side Panel" in the UX spec.
 */
public class AlertHistoryResponse {

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("history")
    private List<HistoryEntry> history;

    // Nested class for individual history entry
    public static class HistoryEntry {
        @JsonProperty("id")
        private String id;

        @JsonProperty("signal")
        private String signal; // BUY, SELL, HOLD

        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("price")
        private Double price;

        @JsonProperty("timestamp")
        private Timestamp timestamp;

        @JsonProperty("notificationSent")
        private Boolean notificationSent;

        @JsonProperty("metadata")
        private Map<String, Object> metadata; // RSI, MACD, etc.

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSignal() {
            return signal;
        }

        public void setSignal(String signal) {
            this.signal = signal;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        public Boolean getNotificationSent() {
            return notificationSent;
        }

        public void setNotificationSent(Boolean notificationSent) {
            this.notificationSent = notificationSent;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    // Getters and Setters
    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryEntry> history) {
        this.history = history;
    }
}
