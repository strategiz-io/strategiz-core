package io.strategiz.client.execution.model;

import java.util.List;

public class Performance {
    private double totalReturn;
    private double totalPnl;
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
    private String lastTestedAt;
    private List<Trade> trades;

    public Performance() {}

    public Performance(double totalReturn, double totalPnl, double winRate, int totalTrades, 
                      int profitableTrades, int buyCount, int sellCount, double avgWin, 
                      double avgLoss, double profitFactor, double maxDrawdown, double sharpeRatio,
                      String lastTestedAt, List<Trade> trades) {
        this.totalReturn = totalReturn;
        this.totalPnl = totalPnl;
        this.winRate = winRate;
        this.totalTrades = totalTrades;
        this.profitableTrades = profitableTrades;
        this.buyCount = buyCount;
        this.sellCount = sellCount;
        this.avgWin = avgWin;
        this.avgLoss = avgLoss;
        this.profitFactor = profitFactor;
        this.maxDrawdown = maxDrawdown;
        this.sharpeRatio = sharpeRatio;
        this.lastTestedAt = lastTestedAt;
        this.trades = trades;
    }

    public static PerformanceBuilder builder() {
        return new PerformanceBuilder();
    }

    public double getTotalReturn() { return totalReturn; }
    public void setTotalReturn(double totalReturn) { this.totalReturn = totalReturn; }
    public double getTotalPnl() { return totalPnl; }
    public void setTotalPnl(double totalPnl) { this.totalPnl = totalPnl; }
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }
    public int getProfitableTrades() { return profitableTrades; }
    public void setProfitableTrades(int profitableTrades) { this.profitableTrades = profitableTrades; }
    public int getBuyCount() { return buyCount; }
    public void setBuyCount(int buyCount) { this.buyCount = buyCount; }
    public int getSellCount() { return sellCount; }
    public void setSellCount(int sellCount) { this.sellCount = sellCount; }
    public double getAvgWin() { return avgWin; }
    public void setAvgWin(double avgWin) { this.avgWin = avgWin; }
    public double getAvgLoss() { return avgLoss; }
    public void setAvgLoss(double avgLoss) { this.avgLoss = avgLoss; }
    public double getProfitFactor() { return profitFactor; }
    public void setProfitFactor(double profitFactor) { this.profitFactor = profitFactor; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(double maxDrawdown) { this.maxDrawdown = maxDrawdown; }
    public double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; }
    public String getLastTestedAt() { return lastTestedAt; }
    public void setLastTestedAt(String lastTestedAt) { this.lastTestedAt = lastTestedAt; }
    public List<Trade> getTrades() { return trades; }
    public void setTrades(List<Trade> trades) { this.trades = trades; }

    public static class PerformanceBuilder {
        private double totalReturn;
        private double totalPnl;
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
        private String lastTestedAt;
        private List<Trade> trades;

        public PerformanceBuilder totalReturn(double totalReturn) { this.totalReturn = totalReturn; return this; }
        public PerformanceBuilder totalPnl(double totalPnl) { this.totalPnl = totalPnl; return this; }
        public PerformanceBuilder winRate(double winRate) { this.winRate = winRate; return this; }
        public PerformanceBuilder totalTrades(int totalTrades) { this.totalTrades = totalTrades; return this; }
        public PerformanceBuilder profitableTrades(int profitableTrades) { this.profitableTrades = profitableTrades; return this; }
        public PerformanceBuilder buyCount(int buyCount) { this.buyCount = buyCount; return this; }
        public PerformanceBuilder sellCount(int sellCount) { this.sellCount = sellCount; return this; }
        public PerformanceBuilder avgWin(double avgWin) { this.avgWin = avgWin; return this; }
        public PerformanceBuilder avgLoss(double avgLoss) { this.avgLoss = avgLoss; return this; }
        public PerformanceBuilder profitFactor(double profitFactor) { this.profitFactor = profitFactor; return this; }
        public PerformanceBuilder maxDrawdown(double maxDrawdown) { this.maxDrawdown = maxDrawdown; return this; }
        public PerformanceBuilder sharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; return this; }
        public PerformanceBuilder lastTestedAt(String lastTestedAt) { this.lastTestedAt = lastTestedAt; return this; }
        public PerformanceBuilder trades(List<Trade> trades) { this.trades = trades; return this; }
        public Performance build() {
            return new Performance(totalReturn, totalPnl, winRate, totalTrades, profitableTrades,
                                 buyCount, sellCount, avgWin, avgLoss, profitFactor, maxDrawdown,
                                 sharpeRatio, lastTestedAt, trades);
        }
    }
}
