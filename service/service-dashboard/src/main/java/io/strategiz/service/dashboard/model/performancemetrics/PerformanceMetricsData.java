package io.strategiz.service.dashboard.model.performancemetrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Data model for portfolio performance metrics.
 */
public class PerformanceMetricsData {

	/**
	 * Historical portfolio value data points
	 */
	private List<PortfolioValueDataPoint> historicalValues;

	/**
	 * Overall portfolio performance summary
	 */
	private PerformanceSummary summary;

	// Constructors
	public PerformanceMetricsData() {
	}

	public PerformanceMetricsData(List<PortfolioValueDataPoint> historicalValues, PerformanceSummary summary) {
		this.historicalValues = historicalValues;
		this.summary = summary;
	}

	// Getters and Setters
	public List<PortfolioValueDataPoint> getHistoricalValues() {
		return historicalValues;
	}

	public void setHistoricalValues(List<PortfolioValueDataPoint> historicalValues) {
		this.historicalValues = historicalValues;
	}

	public PerformanceSummary getSummary() {
		return summary;
	}

	public void setSummary(PerformanceSummary summary) {
		this.summary = summary;
	}

	// equals, hashCode, toString
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PerformanceMetricsData that = (PerformanceMetricsData) o;
		return Objects.equals(historicalValues, that.historicalValues) && Objects.equals(summary, that.summary);
	}

	@Override
	public int hashCode() {
		return Objects.hash(historicalValues, summary);
	}

	@Override
	public String toString() {
		return "PerformanceMetricsData{" + "historicalValues=" + historicalValues + ", summary=" + summary + '}';
	}

	/**
	 * Data point for historical portfolio value
	 */
	public static class PortfolioValueDataPoint {

		/**
		 * Timestamp of the data point
		 */
		private LocalDateTime timestamp;

		/**
		 * Portfolio total value at this timestamp
		 */
		private BigDecimal value;

		// Constructors
		public PortfolioValueDataPoint() {
		}

		public PortfolioValueDataPoint(LocalDateTime timestamp, BigDecimal value) {
			this.timestamp = timestamp;
			this.value = value;
		}

		// Getters and Setters
		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
		}

		public BigDecimal getValue() {
			return value;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		// equals, hashCode, toString
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PortfolioValueDataPoint that = (PortfolioValueDataPoint) o;
			return Objects.equals(timestamp, that.timestamp) && Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(timestamp, value);
		}

		@Override
		public String toString() {
			return "PortfolioValueDataPoint{" + "timestamp=" + timestamp + ", value=" + value + '}';
		}

	}

	/**
	 * Performance summary metrics
	 */
	public static class PerformanceSummary {

		/**
		 * Total unrealized profit/loss in USD
		 */
		private BigDecimal totalProfitLoss;

		/**
		 * Total profit/loss percentage
		 */
		private BigDecimal totalProfitLossPercentage;

		/**
		 * 24-hour change in USD
		 */
		private BigDecimal dailyChange;

		/**
		 * 24-hour change percentage
		 */
		private BigDecimal dailyChangePercentage;

		/**
		 * 7-day change in USD
		 */
		private BigDecimal weeklyChange;

		/**
		 * 7-day change percentage
		 */
		private BigDecimal weeklyChangePercentage;

		/**
		 * 30-day change in USD
		 */
		private BigDecimal monthlyChange;

		/**
		 * 30-day change percentage
		 */
		private BigDecimal monthlyChangePercentage;

		/**
		 * Year-to-date change in USD
		 */
		private BigDecimal ytdChange;

		/**
		 * Year-to-date change percentage
		 */
		private BigDecimal ytdChangePercentage;

		/**
		 * Whether the portfolio is currently profitable
		 */
		private boolean profitable;

		// Constructors
		public PerformanceSummary() {
		}

		public PerformanceSummary(BigDecimal totalProfitLoss, BigDecimal totalProfitLossPercentage,
				BigDecimal dailyChange, BigDecimal dailyChangePercentage, BigDecimal weeklyChange,
				BigDecimal weeklyChangePercentage, BigDecimal monthlyChange, BigDecimal monthlyChangePercentage,
				BigDecimal ytdChange, BigDecimal ytdChangePercentage, boolean profitable) {
			this.totalProfitLoss = totalProfitLoss;
			this.totalProfitLossPercentage = totalProfitLossPercentage;
			this.dailyChange = dailyChange;
			this.dailyChangePercentage = dailyChangePercentage;
			this.weeklyChange = weeklyChange;
			this.weeklyChangePercentage = weeklyChangePercentage;
			this.monthlyChange = monthlyChange;
			this.monthlyChangePercentage = monthlyChangePercentage;
			this.ytdChange = ytdChange;
			this.ytdChangePercentage = ytdChangePercentage;
			this.profitable = profitable;
		}

		// Getters and Setters
		public BigDecimal getTotalProfitLoss() {
			return totalProfitLoss;
		}

		public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
			this.totalProfitLoss = totalProfitLoss;
		}

		public BigDecimal getTotalProfitLossPercentage() {
			return totalProfitLossPercentage;
		}

		public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) {
			this.totalProfitLossPercentage = totalProfitLossPercentage;
		}

		public BigDecimal getDailyChange() {
			return dailyChange;
		}

		public void setDailyChange(BigDecimal dailyChange) {
			this.dailyChange = dailyChange;
		}

		public BigDecimal getDailyChangePercentage() {
			return dailyChangePercentage;
		}

		public void setDailyChangePercentage(BigDecimal dailyChangePercentage) {
			this.dailyChangePercentage = dailyChangePercentage;
		}

		public BigDecimal getWeeklyChange() {
			return weeklyChange;
		}

		public void setWeeklyChange(BigDecimal weeklyChange) {
			this.weeklyChange = weeklyChange;
		}

		public BigDecimal getWeeklyChangePercentage() {
			return weeklyChangePercentage;
		}

		public void setWeeklyChangePercentage(BigDecimal weeklyChangePercentage) {
			this.weeklyChangePercentage = weeklyChangePercentage;
		}

		public BigDecimal getMonthlyChange() {
			return monthlyChange;
		}

		public void setMonthlyChange(BigDecimal monthlyChange) {
			this.monthlyChange = monthlyChange;
		}

		public BigDecimal getMonthlyChangePercentage() {
			return monthlyChangePercentage;
		}

		public void setMonthlyChangePercentage(BigDecimal monthlyChangePercentage) {
			this.monthlyChangePercentage = monthlyChangePercentage;
		}

		public BigDecimal getYtdChange() {
			return ytdChange;
		}

		public void setYtdChange(BigDecimal ytdChange) {
			this.ytdChange = ytdChange;
		}

		public BigDecimal getYtdChangePercentage() {
			return ytdChangePercentage;
		}

		public void setYtdChangePercentage(BigDecimal ytdChangePercentage) {
			this.ytdChangePercentage = ytdChangePercentage;
		}

		public boolean isProfitable() {
			return profitable;
		}

		public void setProfitable(boolean profitable) {
			this.profitable = profitable;
		}

		// equals, hashCode, toString
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PerformanceSummary that = (PerformanceSummary) o;
			return profitable == that.profitable && Objects.equals(totalProfitLoss, that.totalProfitLoss)
					&& Objects.equals(totalProfitLossPercentage, that.totalProfitLossPercentage)
					&& Objects.equals(dailyChange, that.dailyChange)
					&& Objects.equals(dailyChangePercentage, that.dailyChangePercentage)
					&& Objects.equals(weeklyChange, that.weeklyChange)
					&& Objects.equals(weeklyChangePercentage, that.weeklyChangePercentage)
					&& Objects.equals(monthlyChange, that.monthlyChange)
					&& Objects.equals(monthlyChangePercentage, that.monthlyChangePercentage)
					&& Objects.equals(ytdChange, that.ytdChange)
					&& Objects.equals(ytdChangePercentage, that.ytdChangePercentage);
		}

		@Override
		public int hashCode() {
			return Objects.hash(totalProfitLoss, totalProfitLossPercentage, dailyChange, dailyChangePercentage,
					weeklyChange, weeklyChangePercentage, monthlyChange, monthlyChangePercentage, ytdChange,
					ytdChangePercentage, profitable);
		}

		@Override
		public String toString() {
			return "PerformanceSummary{" + "totalProfitLoss=" + totalProfitLoss + ", totalProfitLossPercentage="
					+ totalProfitLossPercentage + ", dailyChange=" + dailyChange + ", dailyChangePercentage="
					+ dailyChangePercentage + ", weeklyChange=" + weeklyChange + ", weeklyChangePercentage="
					+ weeklyChangePercentage + ", monthlyChange=" + monthlyChange + ", monthlyChangePercentage="
					+ monthlyChangePercentage + ", ytdChange=" + ytdChange + ", ytdChangePercentage="
					+ ytdChangePercentage + ", profitable=" + profitable + '}';
		}

	}

}
