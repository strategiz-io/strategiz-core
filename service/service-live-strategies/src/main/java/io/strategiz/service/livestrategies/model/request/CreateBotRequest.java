package io.strategiz.service.livestrategies.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request model for deploying a new strategy bot.
 * Corresponds to the "Deploy Bot Dialog" in the UX spec.
 */
public class CreateBotRequest {

    @NotBlank(message = "Strategy ID is required")
    @JsonProperty("strategyId")
    private String strategyId;

    @NotBlank(message = "Bot name is required")
    @Size(max = 50, message = "Bot name must not exceed 50 characters")
    @JsonProperty("botName")
    private String botName;

    @NotEmpty(message = "At least one symbol is required")
    @Size(max = 10, message = "Maximum 10 symbols allowed")
    @JsonProperty("symbols")
    private List<String> symbols;

    @NotBlank(message = "Provider ID is required")
    @JsonProperty("providerId")
    private String providerId;

    @NotBlank(message = "Exchange is required")
    @JsonProperty("exchange")
    private String exchange; // NYSE, NASDAQ, CRYPTO

    @NotBlank(message = "Environment is required")
    @JsonProperty("environment")
    private String environment; // PAPER or LIVE

    @NotNull(message = "Max position size is required")
    @Positive(message = "Max position size must be positive")
    @JsonProperty("maxPositionSize")
    private Double maxPositionSize;

    @JsonProperty("stopLossPercent")
    private Double stopLossPercent; // Optional

    @JsonProperty("takeProfitPercent")
    private Double takeProfitPercent; // Optional

    @JsonProperty("maxDailyLoss")
    private Double maxDailyLoss; // Optional

    @JsonProperty("autoExecute")
    private Boolean autoExecute = true;

    // Getters and Setters
    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
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
}
