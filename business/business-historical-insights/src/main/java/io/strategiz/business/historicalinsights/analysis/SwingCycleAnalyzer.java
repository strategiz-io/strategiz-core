package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.PriceTurningPoint;
import io.strategiz.business.historicalinsights.model.PriceTurningPoint.PointType;
import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyzes swing cycle patterns from historical turning points.
 *
 * Provides insights into: - Average and median swing magnitude (% move between turning
 * points) - Average swing duration (days between turning points) - Optimal holding
 * periods for maximum gains - Win rate at different holding periods
 */
public class SwingCycleAnalyzer {

	/**
	 * Result of swing cycle analysis.
	 */
	public static class SwingCycleResult {

		private double avgSwingMagnitude;

		private double medianSwingMagnitude;

		private int avgSwingDuration;

		private int medianSwingDuration;

		private int optimalHoldingPeriodDays;

		private double optimalHoldingWinRate;

		// Additional statistics
		private int totalSwings;

		private double avgUpSwingMagnitude;

		private double avgDownSwingMagnitude;

		private int avgUpSwingDuration;

		private int avgDownSwingDuration;

		// Getters and Setters

		public double getAvgSwingMagnitude() {
			return avgSwingMagnitude;
		}

		public void setAvgSwingMagnitude(double avgSwingMagnitude) {
			this.avgSwingMagnitude = avgSwingMagnitude;
		}

		public double getMedianSwingMagnitude() {
			return medianSwingMagnitude;
		}

		public void setMedianSwingMagnitude(double medianSwingMagnitude) {
			this.medianSwingMagnitude = medianSwingMagnitude;
		}

		public int getAvgSwingDuration() {
			return avgSwingDuration;
		}

		public void setAvgSwingDuration(int avgSwingDuration) {
			this.avgSwingDuration = avgSwingDuration;
		}

		public int getMedianSwingDuration() {
			return medianSwingDuration;
		}

		public void setMedianSwingDuration(int medianSwingDuration) {
			this.medianSwingDuration = medianSwingDuration;
		}

		public int getOptimalHoldingPeriodDays() {
			return optimalHoldingPeriodDays;
		}

		public void setOptimalHoldingPeriodDays(int optimalHoldingPeriodDays) {
			this.optimalHoldingPeriodDays = optimalHoldingPeriodDays;
		}

		public double getOptimalHoldingWinRate() {
			return optimalHoldingWinRate;
		}

		public void setOptimalHoldingWinRate(double optimalHoldingWinRate) {
			this.optimalHoldingWinRate = optimalHoldingWinRate;
		}

		public int getTotalSwings() {
			return totalSwings;
		}

		public void setTotalSwings(int totalSwings) {
			this.totalSwings = totalSwings;
		}

		public double getAvgUpSwingMagnitude() {
			return avgUpSwingMagnitude;
		}

		public void setAvgUpSwingMagnitude(double avgUpSwingMagnitude) {
			this.avgUpSwingMagnitude = avgUpSwingMagnitude;
		}

		public double getAvgDownSwingMagnitude() {
			return avgDownSwingMagnitude;
		}

		public void setAvgDownSwingMagnitude(double avgDownSwingMagnitude) {
			this.avgDownSwingMagnitude = avgDownSwingMagnitude;
		}

		public int getAvgUpSwingDuration() {
			return avgUpSwingDuration;
		}

		public void setAvgUpSwingDuration(int avgUpSwingDuration) {
			this.avgUpSwingDuration = avgUpSwingDuration;
		}

		public int getAvgDownSwingDuration() {
			return avgDownSwingDuration;
		}

		public void setAvgDownSwingDuration(int avgDownSwingDuration) {
			this.avgDownSwingDuration = avgDownSwingDuration;
		}

	}

	/**
	 * Analyze swing cycles from turning points.
	 * @param turningPoints List of historical peaks and troughs
	 * @param data Original market data for detailed analysis
	 * @return SwingCycleResult with swing statistics
	 */
	public SwingCycleResult analyze(List<PriceTurningPoint> turningPoints, List<MarketDataEntity> data) {
		SwingCycleResult result = new SwingCycleResult();

		if (turningPoints == null || turningPoints.size() < 3) {
			result.setAvgSwingMagnitude(10.0); // Default
			result.setMedianSwingMagnitude(8.0);
			result.setAvgSwingDuration(30);
			result.setOptimalHoldingPeriodDays(20);
			result.setOptimalHoldingWinRate(0.5);
			return result;
		}

		// Sort turning points by timestamp
		List<PriceTurningPoint> sortedPoints = turningPoints.stream()
			.sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
			.collect(Collectors.toList());

		// Calculate swing magnitudes and durations
		List<Double> magnitudes = new ArrayList<>();
		List<Integer> durations = new ArrayList<>();
		List<Double> upMagnitudes = new ArrayList<>();
		List<Double> downMagnitudes = new ArrayList<>();
		List<Integer> upDurations = new ArrayList<>();
		List<Integer> downDurations = new ArrayList<>();

		for (int i = 1; i < sortedPoints.size(); i++) {
			PriceTurningPoint current = sortedPoints.get(i);
			PriceTurningPoint previous = sortedPoints.get(i - 1);

			double pctChange = Math.abs(current.getPriceChangeFromPrevious());
			int days = current.getDaysFromPrevious();

			if (pctChange > 0 && days > 0) {
				magnitudes.add(pctChange);
				durations.add(days);

				// Separate up and down swings
				if (current.getType() == PointType.PEAK) {
					// Swing up to peak
					upMagnitudes.add(pctChange);
					upDurations.add(days);
				}
				else {
					// Swing down to trough
					downMagnitudes.add(pctChange);
					downDurations.add(days);
				}
			}
		}

		result.setTotalSwings(magnitudes.size());

		if (magnitudes.isEmpty()) {
			return result;
		}

		// Calculate averages
		double sumMag = magnitudes.stream().mapToDouble(Double::doubleValue).sum();
		result.setAvgSwingMagnitude(sumMag / magnitudes.size());

		int sumDur = durations.stream().mapToInt(Integer::intValue).sum();
		result.setAvgSwingDuration(sumDur / durations.size());

		// Calculate medians
		Collections.sort(magnitudes);
		result.setMedianSwingMagnitude(magnitudes.get(magnitudes.size() / 2));

		Collections.sort(durations);
		result.setMedianSwingDuration(durations.get(durations.size() / 2));

		// Up/down swing statistics
		if (!upMagnitudes.isEmpty()) {
			result.setAvgUpSwingMagnitude(upMagnitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
			result.setAvgUpSwingDuration((int) upDurations.stream().mapToInt(Integer::intValue).average().orElse(0));
		}

		if (!downMagnitudes.isEmpty()) {
			result
				.setAvgDownSwingMagnitude(downMagnitudes.stream().mapToDouble(Double::doubleValue).average().orElse(0));
			result
				.setAvgDownSwingDuration((int) downDurations.stream().mapToInt(Integer::intValue).average().orElse(0));
		}

		// Calculate optimal holding period
		if (data != null && data.size() > 100) {
			OptimalHoldingResult holdingResult = findOptimalHoldingPeriod(data);
			result.setOptimalHoldingPeriodDays(holdingResult.days);
			result.setOptimalHoldingWinRate(holdingResult.winRate);
		}
		else {
			// Estimate from swing duration
			result.setOptimalHoldingPeriodDays(result.getAvgSwingDuration() / 2);
			result.setOptimalHoldingWinRate(0.55); // Default
		}

		return result;
	}

	/**
	 * Find the optimal holding period by testing various periods.
	 */
	private OptimalHoldingResult findOptimalHoldingPeriod(List<MarketDataEntity> data) {
		int[] periodsToTest = { 5, 10, 15, 20, 30, 45, 60, 90 };
		int bestPeriod = 20;
		double bestWinRate = 0;

		for (int period : periodsToTest) {
			if (period >= data.size()) {
				continue;
			}

			int wins = 0;
			int trades = 0;

			// Test buying and holding for 'period' days at each point
			for (int i = 0; i < data.size() - period; i += period) {
				double entryPrice = data.get(i).getClose().doubleValue();
				double exitPrice = data.get(i + period).getClose().doubleValue();

				if (exitPrice > entryPrice) {
					wins++;
				}
				trades++;
			}

			double winRate = trades > 0 ? (double) wins / trades : 0;

			if (winRate > bestWinRate) {
				bestWinRate = winRate;
				bestPeriod = period;
			}
		}

		return new OptimalHoldingResult(bestPeriod, bestWinRate);
	}

	private static class OptimalHoldingResult {

		int days;

		double winRate;

		OptimalHoldingResult(int days, double winRate) {
			this.days = days;
			this.winRate = winRate;
		}

	}

	/**
	 * Calculate Kelly Criterion for optimal position sizing. Kelly = (Win% * AvgWin -
	 * Loss% * AvgLoss) / AvgWin
	 * @param turningPoints Turning points for historical win/loss analysis
	 * @return Optimal position size as fraction (e.g., 0.25 = 25%)
	 */
	public double calculateKellyCriterion(List<PriceTurningPoint> turningPoints) {
		if (turningPoints == null || turningPoints.size() < 4) {
			return 0.05; // Conservative default 5%
		}

		// Simulate buy at troughs, sell at next peak
		int wins = 0;
		int losses = 0;
		double sumWins = 0;
		double sumLosses = 0;

		List<PriceTurningPoint> sorted = turningPoints.stream()
			.sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
			.collect(Collectors.toList());

		for (int i = 0; i < sorted.size() - 1; i++) {
			PriceTurningPoint current = sorted.get(i);
			PriceTurningPoint next = sorted.get(i + 1);

			// Buy at trough, sell at next point
			if (current.getType() == PointType.TROUGH) {
				double pctChange = ((next.getPrice() - current.getPrice()) / current.getPrice()) * 100;

				if (pctChange > 0) {
					wins++;
					sumWins += pctChange;
				}
				else {
					losses++;
					sumLosses += Math.abs(pctChange);
				}
			}
		}

		int totalTrades = wins + losses;
		if (totalTrades < 4) {
			return 0.05;
		}

		double winRate = (double) wins / totalTrades;
		double avgWin = wins > 0 ? sumWins / wins : 0;
		double avgLoss = losses > 0 ? sumLosses / losses : 1;

		// Kelly formula
		if (avgWin <= 0) {
			return 0.05;
		}

		double kelly = (winRate * avgWin - (1 - winRate) * avgLoss) / avgWin;

		// Clamp to reasonable range and use quarter Kelly for safety
		kelly = Math.max(0, Math.min(0.5, kelly));
		return kelly * 0.25; // Quarter Kelly for risk management
	}

	/**
	 * Calculate volatility-adjusted position size. Higher volatility = smaller position.
	 * @param atrPercent ATR as percentage of price
	 * @param targetRiskPct Target risk per trade as percentage (e.g., 2%)
	 * @return Position size as fraction
	 */
	public double calculateVolatilityAdjustedSize(double atrPercent, double targetRiskPct) {
		if (atrPercent <= 0) {
			return 0.05; // Default
		}

		// Position size = Target Risk / ATR
		// E.g., 2% risk / 3% ATR = 0.67 position size
		double size = targetRiskPct / atrPercent;

		// Clamp to reasonable range
		return Math.max(0.02, Math.min(0.25, size));
	}

}
