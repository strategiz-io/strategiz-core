package io.strategiz.data.strategy.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Entity representing a deployed strategy alert.
 * Links to a Strategy entity and defines how/when to alert the user.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategyAlerts")
public class StrategyAlert extends BaseEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("alertName")
    private String alertName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("providerId")
    private String providerId; // coinbase, schwab, etc.

    @JsonProperty("exchange")
    private String exchange; // NYSE, NASDAQ, CRYPTO, etc.

    @JsonProperty("notificationChannels")
    private List<String> notificationChannels; // email, push, in-app

    @JsonProperty("status")
    private String status; // ACTIVE, PAUSED, ERROR, STOPPED

    @JsonProperty("lastCheckedAt")
    private Timestamp lastCheckedAt;

    @JsonProperty("lastTriggeredAt")
    private Timestamp lastTriggeredAt;

    @JsonProperty("triggerCount")
    private Integer triggerCount;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("subscriptionTier")
    private String subscriptionTier; // FREE, STARTER, PRO

    // Constructors
    public StrategyAlert() {
        super();
        this.triggerCount = 0;
        this.status = "ACTIVE";
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
    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Timestamp getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Timestamp lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Timestamp getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Timestamp lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }
}
