package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Response model for strategy alert.
 * Corresponds to the "Alert Card" in the UX spec.
 */
public class AlertResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("strategyName")
    private String strategyName; // Fetched from Strategy entity

    @JsonProperty("alertName")
    private String alertName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("providerId")
    private String providerId;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("notificationChannels")
    private List<String> notificationChannels;

    @JsonProperty("status")
    private String status; // ACTIVE, PAUSED, ERROR, STOPPED

    @JsonProperty("triggerCount")
    private Integer triggerCount;

    @JsonProperty("lastTriggeredAt")
    private Timestamp lastTriggeredAt;

    @JsonProperty("lastSignal")
    private LastSignalInfo lastSignal;

    @JsonProperty("deployedAt")
    private Timestamp deployedAt;

    @JsonProperty("subscriptionTier")
    private String subscriptionTier;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Nested class for last signal info
    public static class LastSignalInfo {
        @JsonProperty("signal")
        private String signal; // BUY, SELL, HOLD

        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("price")
        private Double price;

        // Getters and Setters
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
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getAlertName() {
        return alertName;
    }

    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public List<String> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(List<String> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }

    public Timestamp getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Timestamp lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public LastSignalInfo getLastSignal() {
        return lastSignal;
    }

    public void setLastSignal(LastSignalInfo lastSignal) {
        this.lastSignal = lastSignal;
    }

    public Timestamp getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Timestamp deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
