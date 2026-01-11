package io.strategiz.service.mystrategies.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregated performance metrics across all bot deployments of a strategy.
 */
public class AggregatedBotPerformanceDTO {

    @JsonProperty("totalReturn")
    private Double totalReturn; // Average return across all bots

    @JsonProperty("totalPnL")
    private Double totalPnL; // Sum of P&L across all bots

    @JsonProperty("avgWinRate")
    private Double avgWinRate; // Average win rate

    @JsonProperty("totalTrades")
    private Integer totalTrades; // Sum of trades across all bots

    // Getters and Setters
    public Double getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(Double totalReturn) {
        this.totalReturn = totalReturn;
    }

    public Double getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(Double totalPnL) {
        this.totalPnL = totalPnL;
    }

    public Double getAvgWinRate() {
        return avgWinRate;
    }

    public void setAvgWinRate(Double avgWinRate) {
        this.avgWinRate = avgWinRate;
    }

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }
}
