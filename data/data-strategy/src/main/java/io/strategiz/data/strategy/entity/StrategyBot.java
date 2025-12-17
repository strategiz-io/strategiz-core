package io.strategiz.data.strategy.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Entity representing a deployed strategy bot for automated trading.
 * Links to a Strategy entity and defines execution parameters.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategyBots")
public class StrategyBot extends BaseEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("botName")
    private String botName;

    @JsonProperty("symbols")
    private List<String> symbols;

    @JsonProperty("providerId")
    private String providerId; // alpaca, coinbase, etc.

    @JsonProperty("exchange")
    private String exchange; // NYSE, NASDAQ, CRYPTO, etc.

    // Deployment environment
    @JsonProperty("environment")
    private String environment; // PAPER, LIVE

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

    // Status tracking
    @JsonProperty("status")
    private String status; // ACTIVE, PAUSED, ERROR, STOPPED

    @JsonProperty("lastCheckedAt")
    private Timestamp lastCheckedAt;

    @JsonProperty("lastExecutedAt")
    private Timestamp lastExecutedAt;

    @JsonProperty("totalTrades")
    private Integer totalTrades;

    @JsonProperty("profitableTrades")
    private Integer profitableTrades;

    @JsonProperty("totalPnL")
    private Double totalPnL;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Subscription tier (affects execution frequency)
    @JsonProperty("subscriptionTier")
    private String subscriptionTier; // FREE, STARTER, PRO

    // Circuit breaker fields
    @JsonProperty("consecutiveErrors")
    private Integer consecutiveErrors;

    @JsonProperty("maxConsecutiveErrors")
    private Integer maxConsecutiveErrors;

    @JsonProperty("dailyTradeCount")
    private Integer dailyTradeCount;

    @JsonProperty("dailyTradeLimit")
    private Integer dailyTradeLimit;

    @JsonProperty("lastDailyReset")
    private Timestamp lastDailyReset;

    // Constructors
    public StrategyBot() {
        super();
        this.totalTrades = 0;
        this.profitableTrades = 0;
        this.totalPnL = 0.0;
        this.status = "ACTIVE";
        this.environment = "PAPER";
        this.autoExecute = true;
        this.consecutiveErrors = 0;
        this.maxConsecutiveErrors = 3; // Stricter for bots
        this.dailyTradeCount = 0;
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

    public Timestamp getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Timestamp lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Timestamp getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Timestamp lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
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

    public Integer getConsecutiveErrors() {
        return consecutiveErrors;
    }

    public void setConsecutiveErrors(Integer consecutiveErrors) {
        this.consecutiveErrors = consecutiveErrors;
    }

    public Integer getMaxConsecutiveErrors() {
        return maxConsecutiveErrors;
    }

    public void setMaxConsecutiveErrors(Integer maxConsecutiveErrors) {
        this.maxConsecutiveErrors = maxConsecutiveErrors;
    }

    public Integer getDailyTradeCount() {
        return dailyTradeCount;
    }

    public void setDailyTradeCount(Integer dailyTradeCount) {
        this.dailyTradeCount = dailyTradeCount;
    }

    public Integer getDailyTradeLimit() {
        return dailyTradeLimit;
    }

    public void setDailyTradeLimit(Integer dailyTradeLimit) {
        this.dailyTradeLimit = dailyTradeLimit;
    }

    public Timestamp getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(Timestamp lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    // Convenience methods

    /**
     * Check if this bot is in paper trading mode
     */
    public boolean isPaperTrading() {
        return "PAPER".equals(environment);
    }

    /**
     * Check if this bot is in live trading mode
     */
    public boolean isLiveTrading() {
        return "LIVE".equals(environment);
    }

    /**
     * Calculate win rate
     */
    public double getWinRate() {
        if (totalTrades == null || totalTrades == 0) {
            return 0.0;
        }
        int profitable = profitableTrades != null ? profitableTrades : 0;
        return (profitable * 100.0) / totalTrades;
    }

    /**
     * Check if circuit breaker should trip
     */
    public boolean shouldTripCircuitBreaker() {
        int threshold = maxConsecutiveErrors != null ? maxConsecutiveErrors : 3;
        int errors = consecutiveErrors != null ? consecutiveErrors : 0;
        return errors >= threshold;
    }

    /**
     * Check if daily trade limit is reached
     */
    public boolean isDailyLimitReached() {
        if (dailyTradeLimit == null) {
            return false;
        }
        int count = dailyTradeCount != null ? dailyTradeCount : 0;
        return count >= dailyTradeLimit;
    }
}
