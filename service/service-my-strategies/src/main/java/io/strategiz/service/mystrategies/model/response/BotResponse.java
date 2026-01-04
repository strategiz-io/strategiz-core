package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Response model for strategy bot.
 * Corresponds to the "Bot Card" in the UX spec.
 */
public class BotResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("strategyName")
    private String strategyName;

    @JsonProperty("botName")
    private String botName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("providerId")
    private String providerId;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("environment")
    private String environment; // PAPER or LIVE

    // Risk management
    @JsonProperty("maxPositionSize")
    private Double maxPositionSize;

    @JsonProperty("stopLossPercent")
    private Double stopLossPercent;

    @JsonProperty("takeProfitPercent")
    private Double takeProfitPercent;

    @JsonProperty("maxDailyLoss")
    private Double maxDailyLoss;

    @JsonProperty("autoExecute")
    private Boolean autoExecute;

    // Status
    @JsonProperty("status")
    private String status;

    // Performance metrics
    @JsonProperty("totalTrades")
    private Integer totalTrades;

    @JsonProperty("profitableTrades")
    private Integer profitableTrades;

    @JsonProperty("totalPnL")
    private Double totalPnL;

    @JsonProperty("winRate")
    private Double winRate;

    // Timestamps
    @JsonProperty("lastExecutedAt")
    private Timestamp lastExecutedAt;

    @JsonProperty("deployedAt")
    private Timestamp deployedAt;

    @JsonProperty("subscriptionTier")
    private String subscriptionTier;

    @JsonProperty("errorMessage")
    private String errorMessage;

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

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Double getMaxPositionSize() {
        return maxPositionSize;
    }

    public void setMaxPositionSize(Double maxPositionSize) {
        this.maxPositionSize = maxPositionSize;
    }

    public Double getStopLossPercent() {
        return stopLossPercent;
    }

    public void setStopLossPercent(Double stopLossPercent) {
        this.stopLossPercent = stopLossPercent;
    }

    public Double getTakeProfitPercent() {
        return takeProfitPercent;
    }

    public void setTakeProfitPercent(Double takeProfitPercent) {
        this.takeProfitPercent = takeProfitPercent;
    }

    public Double getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public void setMaxDailyLoss(Double maxDailyLoss) {
        this.maxDailyLoss = maxDailyLoss;
    }

    public Boolean getAutoExecute() {
        return autoExecute;
    }

    public void setAutoExecute(Boolean autoExecute) {
        this.autoExecute = autoExecute;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    public Integer getProfitableTrades() {
        return profitableTrades;
    }

    public void setProfitableTrades(Integer profitableTrades) {
        this.profitableTrades = profitableTrades;
    }

    public Double getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(Double totalPnL) {
        this.totalPnL = totalPnL;
    }

    public Double getWinRate() {
        return winRate;
    }

    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }

    public Timestamp getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Timestamp lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
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
