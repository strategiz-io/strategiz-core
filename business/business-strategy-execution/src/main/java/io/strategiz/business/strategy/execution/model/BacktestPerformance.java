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

    // NEW: Actual backtest period tracking (not hardcoded)
    private String testPeriod;           // Formatted period (e.g., "5.3Y", "18M", "Since Jan 2020")
    private String backtestStartDate;    // ISO timestamp of first data point
    private String backtestEndDate;      // ISO timestamp of last data point
    private Integer backtestPeriodDays;  // Actual days tested

    // NEW: Buy-and-hold comparison metrics
    private Double buyAndHoldReturn;         // $ return if just buying and holding
    private Double buyAndHoldReturnPercent;  // % return if just buying and holding
    private Double outperformance;           // Strategy return - Buy&Hold return (%)

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

    public String getTestPeriod() {
        return testPeriod;
    }

    public void setTestPeriod(String testPeriod) {
        this.testPeriod = testPeriod;
    }

    public String getBacktestStartDate() {
        return backtestStartDate;
    }

    public void setBacktestStartDate(String backtestStartDate) {
        this.backtestStartDate = backtestStartDate;
    }

    public String getBacktestEndDate() {
        return backtestEndDate;
    }

    public void setBacktestEndDate(String backtestEndDate) {
        this.backtestEndDate = backtestEndDate;
    }

    public Integer getBacktestPeriodDays() {
        return backtestPeriodDays;
    }

    public void setBacktestPeriodDays(Integer backtestPeriodDays) {
        this.backtestPeriodDays = backtestPeriodDays;
    }

    public Double getBuyAndHoldReturn() {
        return buyAndHoldReturn;
    }

    public void setBuyAndHoldReturn(Double buyAndHoldReturn) {
        this.buyAndHoldReturn = buyAndHoldReturn;
    }

    public Double getBuyAndHoldReturnPercent() {
        return buyAndHoldReturnPercent;
    }

    public void setBuyAndHoldReturnPercent(Double buyAndHoldReturnPercent) {
        this.buyAndHoldReturnPercent = buyAndHoldReturnPercent;
    }

    public Double getOutperformance() {
        return outperformance;
    }

    public void setOutperformance(Double outperformance) {
        this.outperformance = outperformance;
    }
}
