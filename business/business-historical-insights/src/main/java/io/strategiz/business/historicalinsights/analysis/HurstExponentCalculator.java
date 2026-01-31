package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.HurstResult;
import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.util.List;

/**
 * Calculates the Hurst Exponent using Rescaled Range (R/S) analysis.
 *
 * The Hurst exponent H indicates the tendency of a time series: - H < 0.5: Mean-reverting
 * (anti-persistent) - price tends to reverse - H = 0.5: Random walk - no predictable
 * pattern - H > 0.5: Trending (persistent) - price tends to continue in same direction
 *
 * This calculation uses the classical R/S method with multiple time scales and linear
 * regression on log-log plot to estimate H.
 */
public class HurstExponentCalculator {

	private static final int MIN_SAMPLES = 50;

	private static final int[] SCALES = { 8, 16, 32, 64, 128, 256 };

	/**
	 * Calculate the Hurst exponent for the given price data.
	 * @param data Market data with close prices
	 * @return HurstResult with exponent value and interpretation
	 */
	public HurstResult calculate(List<MarketDataEntity> data) {
		if (data == null || data.size() < MIN_SAMPLES) {
			return new HurstResult(0.5, 0.0); // Default to random walk with zero
												// confidence
		}

		// Extract log returns
		double[] logReturns = calculateLogReturns(data);
		if (logReturns.length < MIN_SAMPLES) {
			return new HurstResult(0.5, 0.0);
		}

		// Calculate R/S values for different time scales
		double[] logN = new double[SCALES.length];
		double[] logRS = new double[SCALES.length];
		int validScales = 0;

		for (int i = 0; i < SCALES.length; i++) {
			int n = SCALES[i];
			if (n * 2 > logReturns.length) {
				break; // Not enough data for this scale
			}

			double rs = calculateRescaledRange(logReturns, n);
			if (rs > 0) {
				logN[validScales] = Math.log(n);
				logRS[validScales] = Math.log(rs);
				validScales++;
			}
		}

		if (validScales < 3) {
			return new HurstResult(0.5, 0.3); // Not enough scales for reliable estimate
		}

		// Linear regression on log-log plot: log(R/S) = H * log(n) + c
		// Slope gives us the Hurst exponent
		double[] regression = linearRegression(logN, logRS, validScales);
		double hurst = regression[0]; // slope
		double rSquared = regression[2]; // R² for confidence

		// Clamp to valid range [0, 1]
		hurst = Math.max(0.0, Math.min(1.0, hurst));

		return new HurstResult(hurst, rSquared);
	}

	/**
	 * Calculate log returns from price data.
	 */
	private double[] calculateLogReturns(List<MarketDataEntity> data) {
		double[] returns = new double[data.size() - 1];

		for (int i = 1; i < data.size(); i++) {
			double current = data.get(i).getClose().doubleValue();
			double previous = data.get(i - 1).getClose().doubleValue();

			if (previous > 0 && current > 0) {
				returns[i - 1] = Math.log(current / previous);
			}
			else {
				returns[i - 1] = 0;
			}
		}

		return returns;
	}

	/**
	 * Calculate the Rescaled Range (R/S) for a given time scale. R/S = (max cumulative
	 * deviation - min cumulative deviation) / standard deviation
	 */
	private double calculateRescaledRange(double[] returns, int n) {
		int numSubseries = returns.length / n;
		if (numSubseries < 1) {
			return 0;
		}

		double sumRS = 0;

		for (int subseries = 0; subseries < numSubseries; subseries++) {
			int start = subseries * n;

			// Calculate mean of this subseries
			double mean = 0;
			for (int i = 0; i < n; i++) {
				mean += returns[start + i];
			}
			mean /= n;

			// Calculate cumulative deviations and find range
			double cumDev = 0;
			double maxCumDev = Double.NEGATIVE_INFINITY;
			double minCumDev = Double.POSITIVE_INFINITY;
			double sumSquares = 0;

			for (int i = 0; i < n; i++) {
				double deviation = returns[start + i] - mean;
				cumDev += deviation;
				maxCumDev = Math.max(maxCumDev, cumDev);
				minCumDev = Math.min(minCumDev, cumDev);
				sumSquares += deviation * deviation;
			}

			// Range R
			double range = maxCumDev - minCumDev;

			// Standard deviation S
			double stdDev = Math.sqrt(sumSquares / n);

			// Rescaled range R/S
			if (stdDev > 0) {
				sumRS += range / stdDev;
			}
		}

		return numSubseries > 0 ? sumRS / numSubseries : 0;
	}

	/**
	 * Simple linear regression: y = slope * x + intercept. Returns [slope, intercept, R²]
	 */
	private double[] linearRegression(double[] x, double[] y, int n) {
		double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

		for (int i = 0; i < n; i++) {
			sumX += x[i];
			sumY += y[i];
			sumXY += x[i] * y[i];
			sumX2 += x[i] * x[i];
			sumY2 += y[i] * y[i];
		}

		double denominator = n * sumX2 - sumX * sumX;
		if (Math.abs(denominator) < 1e-10) {
			return new double[] { 0.5, 0, 0 };
		}

		double slope = (n * sumXY - sumX * sumY) / denominator;
		double intercept = (sumY - slope * sumX) / n;

		// Calculate R²
		double ssTotal = sumY2 - (sumY * sumY) / n;
		double ssResidual = 0;
		for (int i = 0; i < n; i++) {
			double predicted = slope * x[i] + intercept;
			double residual = y[i] - predicted;
			ssResidual += residual * residual;
		}

		double rSquared = ssTotal > 0 ? 1 - (ssResidual / ssTotal) : 0;
		rSquared = Math.max(0, Math.min(1, rSquared));

		return new double[] { slope, intercept, rSquared };
	}

}
