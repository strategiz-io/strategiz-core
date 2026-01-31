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

	// Equity curve (NEW)
	private List<EquityPoint> equityCurve;

	// Test period info (NEW)
	private String startDate;

	private String endDate;

	private String testPeriod;

	// Buy & hold comparison (NEW)
	private double buyAndHoldReturn;

	private double buyAndHoldReturnPercent;

	private double outperformance;

	public static PerformanceBuilder builder() {
		return new PerformanceBuilder();
	}

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

		private List<EquityPoint> equityCurve;

		private String startDate;

		private String endDate;

		private String testPeriod;

		private double buyAndHoldReturn;

		private double buyAndHoldReturnPercent;

		private double outperformance;

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

		public PerformanceBuilder equityCurve(List<EquityPoint> equityCurve) {
			this.equityCurve = equityCurve;
			return this;
		}

		public PerformanceBuilder startDate(String startDate) {
			this.startDate = startDate;
			return this;
		}

		public PerformanceBuilder endDate(String endDate) {
			this.endDate = endDate;
			return this;
		}

		public PerformanceBuilder testPeriod(String testPeriod) {
			this.testPeriod = testPeriod;
			return this;
		}

		public PerformanceBuilder buyAndHoldReturn(double buyAndHoldReturn) {
			this.buyAndHoldReturn = buyAndHoldReturn;
			return this;
		}

		public PerformanceBuilder buyAndHoldReturnPercent(double buyAndHoldReturnPercent) {
			this.buyAndHoldReturnPercent = buyAndHoldReturnPercent;
			return this;
		}

		public PerformanceBuilder outperformance(double outperformance) {
			this.outperformance = outperformance;
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
			perf.equityCurve = this.equityCurve;
			perf.startDate = this.startDate;
			perf.endDate = this.endDate;
			perf.testPeriod = this.testPeriod;
			perf.buyAndHoldReturn = this.buyAndHoldReturn;
			perf.buyAndHoldReturnPercent = this.buyAndHoldReturnPercent;
			perf.outperformance = this.outperformance;
			return perf;
		}

	}

	public double getTotalReturn() {
		return totalReturn;
	}

	public double getTotalPnl() {
		return totalPnl;
	}

	public double getWinRate() {
		return winRate;
	}

	public int getTotalTrades() {
		return totalTrades;
	}

	public int getProfitableTrades() {
		return profitableTrades;
	}

	public int getBuyCount() {
		return buyCount;
	}

	public int getSellCount() {
		return sellCount;
	}

	public double getAvgWin() {
		return avgWin;
	}

	public double getAvgLoss() {
		return avgLoss;
	}

	public double getProfitFactor() {
		return profitFactor;
	}

	public double getMaxDrawdown() {
		return maxDrawdown;
	}

	public double getSharpeRatio() {
		return sharpeRatio;
	}

	public List<Trade> getTrades() {
		return trades;
	}

	public String getLastTestedAt() {
		return lastTestedAt;
	}

	public List<EquityPoint> getEquityCurve() {
		return equityCurve;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public String getTestPeriod() {
		return testPeriod;
	}

	public double getBuyAndHoldReturn() {
		return buyAndHoldReturn;
	}

	public double getBuyAndHoldReturnPercent() {
		return buyAndHoldReturnPercent;
	}

	public double getOutperformance() {
		return outperformance;
	}

}
