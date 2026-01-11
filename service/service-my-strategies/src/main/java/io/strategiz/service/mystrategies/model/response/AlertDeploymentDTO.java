package io.strategiz.service.mystrategies.model.response;

import io.strategiz.data.strategy.entity.AlertLivePerformance;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Simplified alert deployment information for nesting within StrategyWithDeploymentsResponse.
 * Contains essential deployment info without full details.
 */
public class AlertDeploymentDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("alertName")
    private String alertName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("status")
    private String status; // ACTIVE, PAUSED, STOPPED, ERROR

    @JsonProperty("triggerCount")
    private Integer triggerCount;

    @JsonProperty("lastTriggeredAt")
    private Timestamp lastTriggeredAt;

    @JsonProperty("deployedAt")
    private Timestamp deployedAt;

    @JsonProperty("notificationChannels")
    private List<String> notificationChannels;

    @JsonProperty("livePerformance")
    private AlertLivePerformance livePerformance;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
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

    public Timestamp getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Timestamp deployedAt) {
        this.deployedAt = deployedAt;
    }

    public List<String> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(List<String> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    public AlertLivePerformance getLivePerformance() {
        return livePerformance;
    }

    public void setLivePerformance(AlertLivePerformance livePerformance) {
        this.livePerformance = livePerformance;
    }
}
