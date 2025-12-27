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
    private List<Trade> trades;
    private String lastTestedAt;

    public static PerformanceBuilder builder() { return new PerformanceBuilder(); }

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
        private List<Trade> trades;
        private String lastTestedAt;

        public PerformanceBuilder totalReturn(double totalReturn) {
            this.totalReturn = totalReturn;
            return this;
        }

        public PerformanceBuilder totalPnl(double totalPnl) {
            this.totalPnl = totalPnl;
            return this;
        }

        public PerformanceBuilder winRate(double winRate) {
            this.winRate = winRate;
            return this;
        }

        public PerformanceBuilder totalTrades(int totalTrades) {
            this.totalTrades = totalTrades;
            return this;
        }

        public PerformanceBuilder profitableTrades(int profitableTrades) {
            this.profitableTrades = profitableTrades;
            return this;
        }

        public PerformanceBuilder buyCount(int buyCount) {
            this.buyCount = buyCount;
            return this;
        }

        public PerformanceBuilder sellCount(int sellCount) {
            this.sellCount = sellCount;
            return this;
        }

        public PerformanceBuilder avgWin(double avgWin) {
            this.avgWin = avgWin;
            return this;
        }

        public PerformanceBuilder avgLoss(double avgLoss) {
            this.avgLoss = avgLoss;
            return this;
        }

        public PerformanceBuilder profitFactor(double profitFactor) {
            this.profitFactor = profitFactor;
            return this;
        }

        public PerformanceBuilder maxDrawdown(double maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
            return this;
        }

        public PerformanceBuilder sharpeRatio(double sharpeRatio) {
            this.sharpeRatio = sharpeRatio;
            return this;
        }

        public PerformanceBuilder trades(List<Trade> trades) {
            this.trades = trades;
            return this;
        }

        public PerformanceBuilder lastTestedAt(String lastTestedAt) {
            this.lastTestedAt = lastTestedAt;
            return this;
        }

        public Performance build() {
            Performance perf = new Performance();
            perf.totalReturn = this.totalReturn;
            perf.totalPnl = this.totalPnl;
            perf.winRate = this.winRate;
            perf.totalTrades = this.totalTrades;
            perf.profitableTrades = this.profitableTrades;
            perf.buyCount = this.buyCount;
            perf.sellCount = this.sellCount;
            perf.avgWin = this.avgWin;
            perf.avgLoss = this.avgLoss;
            perf.profitFactor = this.profitFactor;
            perf.maxDrawdown = this.maxDrawdown;
            perf.sharpeRatio = this.sharpeRatio;
            perf.trades = this.trades;
            perf.lastTestedAt = this.lastTestedAt;
            return perf;
        }
    }

    public double getTotalReturn() { return totalReturn; }
    public double getTotalPnl() { return totalPnl; }
    public double getWinRate() { return winRate; }
    public int getTotalTrades() { return totalTrades; }
    public int getProfitableTrades() { return profitableTrades; }
    public int getBuyCount() { return buyCount; }
    public int getSellCount() { return sellCount; }
    public double getAvgWin() { return avgWin; }
    public double getAvgLoss() { return avgLoss; }
    public double getProfitFactor() { return profitFactor; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public double getSharpeRatio() { return sharpeRatio; }
    public List<Trade> getTrades() { return trades; }
    public String getLastTestedAt() { return lastTestedAt; }
}
