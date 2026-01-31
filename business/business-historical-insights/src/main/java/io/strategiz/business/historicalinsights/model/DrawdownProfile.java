package io.strategiz.business.historicalinsights.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive drawdown analysis profile. Provides insights into historical risk
 * characteristics including maximum drawdowns, average drawdowns, recovery times, and
 * drawdown frequency.
 */
public class DrawdownProfile {

	private double maxDrawdownPercent;

	private Instant maxDrawdownStart;

	private Instant maxDrawdownEnd;

	private int maxDrawdownDuration; // days

	private double avgDrawdownPercent;

	private int avgDrawdownDuration; // days

	private int avgRecoveryDays; // days to recover from drawdown

	private int totalDrawdowns; // number of drawdowns > 5%

	private List<DrawdownEvent> significantDrawdowns; // Top 5 drawdowns

	public DrawdownProfile() {
		this.significantDrawdowns = new ArrayList<>();
	}

	/**
	 * Represents a single drawdown event.
	 */
	public static class DrawdownEvent {

		private double magnitude; // % drop from peak

		private Instant start;

		private Instant trough;

		private Instant recovery;

		private int durationDays;

		private int recoveryDays;

		public DrawdownEvent() {
		}

		public DrawdownEvent(double magnitude, Instant start, Instant trough, int durationDays) {
			this.magnitude = magnitude;
			this.start = start;
			this.trough = trough;
			this.durationDays = durationDays;
		}

		public double getMagnitude() {
			return magnitude;
		}

		public void setMagnitude(double magnitude) {
			this.magnitude = magnitude;
		}

		public Instant getStart() {
			return start;
		}

		public void setStart(Instant start) {
			this.start = start;
		}

		public Instant getTrough() {
			return trough;
		}

		public void setTrough(Instant trough) {
			this.trough = trough;
		}

		public Instant getRecovery() {
			return recovery;
		}

		public void setRecovery(Instant recovery) {
			this.recovery = recovery;
		}

		public int getDurationDays() {
			return durationDays;
		}

		public void setDurationDays(int durationDays) {
			this.durationDays = durationDays;
		}

		public int getRecoveryDays() {
			return recoveryDays;
		}

		public void setRecoveryDays(int recoveryDays) {
			this.recoveryDays = recoveryDays;
		}

		@Override
		public String toString() {
			return String.format("Drawdown[%.1f%% over %d days, recovery=%d days]", magnitude, durationDays,
					recoveryDays);
		}

	}

	// Getters and Setters

	public double getMaxDrawdownPercent() {
		return maxDrawdownPercent;
	}

	public void setMaxDrawdownPercent(double maxDrawdownPercent) {
		this.maxDrawdownPercent = maxDrawdownPercent;
	}

	public Instant getMaxDrawdownStart() {
		return maxDrawdownStart;
	}

	public void setMaxDrawdownStart(Instant maxDrawdownStart) {
		this.maxDrawdownStart = maxDrawdownStart;
	}

	public Instant getMaxDrawdownEnd() {
		return maxDrawdownEnd;
	}

	public void setMaxDrawdownEnd(Instant maxDrawdownEnd) {
		this.maxDrawdownEnd = maxDrawdownEnd;
	}

	public int getMaxDrawdownDuration() {
		return maxDrawdownDuration;
	}

	public void setMaxDrawdownDuration(int maxDrawdownDuration) {
		this.maxDrawdownDuration = maxDrawdownDuration;
	}

	public double getAvgDrawdownPercent() {
		return avgDrawdownPercent;
	}

	public void setAvgDrawdownPercent(double avgDrawdownPercent) {
		this.avgDrawdownPercent = avgDrawdownPercent;
	}

	public int getAvgDrawdownDuration() {
		return avgDrawdownDuration;
	}

	public void setAvgDrawdownDuration(int avgDrawdownDuration) {
		this.avgDrawdownDuration = avgDrawdownDuration;
	}

	public int getAvgRecoveryDays() {
		return avgRecoveryDays;
	}

	public void setAvgRecoveryDays(int avgRecoveryDays) {
		this.avgRecoveryDays = avgRecoveryDays;
	}

	public int getTotalDrawdowns() {
		return totalDrawdowns;
	}

	public void setTotalDrawdowns(int totalDrawdowns) {
		this.totalDrawdowns = totalDrawdowns;
	}

	public List<DrawdownEvent> getSignificantDrawdowns() {
		return significantDrawdowns;
	}

	public void setSignificantDrawdowns(List<DrawdownEvent> significantDrawdowns) {
		this.significantDrawdowns = significantDrawdowns;
	}

	/**
	 * Calculate recommended stop loss based on historical drawdown patterns. Uses the
	 * average drawdown as a baseline with a safety margin.
	 */
	public double getRecommendedStopLoss() {
		// Stop loss should be larger than average drawdown to avoid false triggers
		// but smaller than max drawdown to limit losses
		double avgBased = avgDrawdownPercent * 1.2;
		double maxBased = maxDrawdownPercent * 0.5;
		return Math.min(avgBased, maxBased);
	}

	@Override
	public String toString() {
		return String.format(
				"DrawdownProfile[max=%.1f%% over %d days, avg=%.1f%% over %d days, recovery=%d days, count=%d]",
				maxDrawdownPercent, maxDrawdownDuration, avgDrawdownPercent, avgDrawdownDuration, avgRecoveryDays,
				totalDrawdowns);
	}

}
