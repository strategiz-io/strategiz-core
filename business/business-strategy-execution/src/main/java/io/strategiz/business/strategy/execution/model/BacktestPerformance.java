package io.strategiz.business.strategy.execution.model;

import java.util.List;

/**
 * Model representing backtest performance metrics and trade breakdown.
 */
public class BacktestPerformance {

    private double totalReturn;
    private double totalPnL;
    private double winRate;
    private int totalTrades;
    private int profitableTrades;
    private int buyCount;
    private int sellCount;
    private double avgWin;
    private double avgLoss;
    private double profitFactor;
    private double maxDrawdown;
    private double sharpeRatio;
    private List<BacktestTrade> trades;
    private String lastTestedAt;

    public BacktestPerformance() {
    }

    // Getters and Setters
    public double getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(double totalReturn) {
        this.totalReturn = totalReturn;
    }

    public double getTotalPnL() {
        return totalPnL;
    }

    public void setTotalPnL(double totalPnL) {
        this.totalPnL = totalPnL;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(int totalTrades) {
        this.totalTrades = totalTrades;
    }

    public int getProfitableTrades() {
        return profitableTrades;
    }

    public void setProfitableTrades(int profitableTrades) {
        this.profitableTrades = profitableTrades;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(int buyCount) {
        this.buyCount = buyCount;
    }

    public int getSellCount() {
        return sellCount;
    }

    public void setSellCount(int sellCount) {
        this.sellCount = sellCount;
    }

    public double getAvgWin() {
        return avgWin;
    }

    public void setAvgWin(double avgWin) {
        this.avgWin = avgWin;
    }

    public double getAvgLoss() {
        return avgLoss;
    }

    public void setAvgLoss(double avgLoss) {
        this.avgLoss = avgLoss;
    }

    public double getProfitFactor() {
        return profitFactor;
    }

    public void setProfitFactor(double profitFactor) {
        this.profitFactor = profitFactor;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

    public List<BacktestTrade> getTrades() {
        return trades;
    }

    public void setTrades(List<BacktestTrade> trades) {
        this.trades = trades;
    }

    public String getLastTestedAt() {
        return lastTestedAt;
    }

    public void setLastTestedAt(String lastTestedAt) {
        this.lastTestedAt = lastTestedAt;
    }
}
