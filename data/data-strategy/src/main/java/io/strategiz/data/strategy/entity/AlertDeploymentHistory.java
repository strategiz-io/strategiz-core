package io.strategiz.data.strategy.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.Map;

/**
 * Entity representing the history of alert triggers.
 * Records each time a strategy alert generates a signal.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategyAlertHistory")
public class StrategyAlertHistory extends BaseEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("alertId")
    private String alertId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("signal")
    private String signal; // BUY, SELL, HOLD

    @JsonProperty("price")
    private Double price;

    @JsonProperty("timestamp")
    private Timestamp timestamp;

    @JsonProperty("notificationSent")
    private Boolean notificationSent;

    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Store indicator values (RSI, MACD, etc.)

    // Constructors
    public StrategyAlertHistory() {
        super();
        this.timestamp = Timestamp.now();
        this.notificationSent = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    // Getters and Setters
    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
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
