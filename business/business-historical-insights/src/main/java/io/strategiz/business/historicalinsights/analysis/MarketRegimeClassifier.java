package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.HurstResult;
import io.strategiz.business.historicalinsights.model.RegimeClassification;
import io.strategiz.business.historicalinsights.model.RegimeClassification.RegimeType;
import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.util.List;

/**
 * Classifies the current market regime based on multiple factors: - Trend direction and
 * strength (using regression and ADX) - Volatility level (using ATR) - Mean-reversion
 * tendency (using Hurst exponent)
 *
 * The regime determines which type of trading strategy is most appropriate.
 */
public class MarketRegimeClassifier {

	private final HurstExponentCalculator hurstCalculator;

	public MarketRegimeClassifier() {
		this.hurstCalculator = new HurstExponentCalculator();
	}

	/**
	 * Classify the market regime based on historical data.
	 * @param data Market data with OHLCV
	 * @return RegimeClassification with regime type and recommendations
	 */
	public RegimeClassification classify(List<MarketDataEntity> data) {
		if (data == null || data.size() < 50) {
			return new RegimeClassification(RegimeType.TRANSITIONAL, 0.3);
		}

		// Calculate trend using linear regression slope
		double trendStrength = calculateTrendStrength(data);
		String trendDirection = getTrendDirection(data);

		// Calculate volatility level
		double volatility = calculateVolatilityLevel(data);

		// Calculate Hurst exponent for mean-reversion detection
		HurstResult hurstResult = hurstCalculator.calculate(data);
		boolean isMeanReverting = hurstResult.isMeanReverting();

		// Calculate ADX for trend strength confirmation
		double adx = calculateADX(data, 14);

		// Determine regime
		RegimeType regime = determineRegime(trendDirection, trendStrength, volatility, isMeanReverting, adx);

		// Build classification result
		RegimeClassification classification = new RegimeClassification(regime, calculateConfidence(adx, trendStrength));
		classification.setTrendStrength(trendStrength);
		classification.setVolatilityLevel(volatility);
		classification.setMeanReverting(isMeanReverting);
		classification.setAdxValue(adx);
		classification.setHurstExponent(hurstResult.getHurstExponent());

		// Calculate ATR percentage for position sizing recommendations
		double atrPct = calculateATRPercent(data);
		classification.setAtrPercent(atrPct);

		return classification;
	}

	private RegimeType determineRegime(String direction, double strength, double volatility, boolean meanReverting,
			double adx) {
		// Strong trend detection
		if (adx > 40 || strength > 0.8) {
			if ("BULLISH".equals(direction)) {
				return RegimeType.STRONG_UPTREND;
			}
			else if ("BEARISH".equals(direction)) {
				return RegimeType.STRONG_DOWNTREND;
			}
		}

		// Moderate trend detection
		if (adx > 25 || strength > 0.5) {
			if ("BULLISH".equals(direction)) {
				return RegimeType.UPTREND;
			}
			else if ("BEARISH".equals(direction)) {
				return RegimeType.DOWNTREND;
			}
		}

		// No clear trend - ranging market
		if (meanReverting || adx < 20) {
			if (volatility > 0.6) {
				return RegimeType.RANGING_VOLATILE;
			}
			else {
				return RegimeType.RANGING_CALM;
			}
		}

		return RegimeType.TRANSITIONAL;
	}

	private double calculateConfidence(double adx, double trendStrength) {
		// Higher ADX and clearer trend = higher confidence
		double adxConfidence = Math.min(adx / 50.0, 1.0);
		double trendConfidence = trendStrength;
		return (adxConfidence * 0.6 + trendConfidence * 0.4);
	}

	/**
	 * Calculate trend strength using linear regression R².
	 */
	private double calculateTrendStrength(List<MarketDataEntity> data) {
		int n = data.size();
		double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

		for (int i = 0; i < n; i++) {
			double x = i;
			double y = data.get(i).getClose().doubleValue();
			sumX += x;
			sumY += y;
			sumXY += x * y;
			sumX2 += x * x;
			sumY2 += y * y;
		}

		// Calculate R²
		double numerator = n * sumXY - sumX * sumY;
		double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

		if (Math.abs(denominator) < 1e-10) {
			return 0.0;
		}

		double correlation = numerator / denominator;
		return Math.abs(correlation); // R² approximation
	}

	/**
	 * Determine trend direction from price movement.
	 */
	private String getTrendDirection(List<MarketDataEntity> data) {
		int lookback = Math.min(50, data.size());
		int startIndex = data.size() - lookback;

		double startPrice = data.get(startIndex).getClose().doubleValue();
		double endPrice = data.get(data.size() - 1).getClose().doubleValue();
		double change = (endPrice - startPrice) / startPrice;

		if (change > 0.05) {
			return "BULLISH";
		}
		else if (change < -0.05) {
			return "BEARISH";
		}
		else {
			return "SIDEWAYS";
		}
	}

	/**
	 * Calculate volatility level (0-1) based on ATR.
	 */
	private double calculateVolatilityLevel(List<MarketDataEntity> data) {
		double atrPct = calculateATRPercent(data);

		// Classify volatility
		if (atrPct < 1.0) {
			return 0.2; // Low
		}
		else if (atrPct < 2.0) {
			return 0.4; // Low-Medium
		}
		else if (atrPct < 3.0) {
			return 0.6; // Medium
		}
		else if (atrPct < 5.0) {
			return 0.8; // High
		}
		else {
			return 1.0; // Extreme
		}
	}

	/**
	 * Calculate Average True Range as percentage of price.
	 */
	private double calculateATRPercent(List<MarketDataEntity> data) {
		if (data.size() < 15) {
			return 2.0; // Default
		}

		double sumTR = 0;
		int period = 14;

		for (int i = data.size() - period; i < data.size(); i++) {
			MarketDataEntity current = data.get(i);
			MarketDataEntity previous = data.get(i - 1);

			double high = current.getHigh().doubleValue();
			double low = current.getLow().doubleValue();
			double prevClose = previous.getClose().doubleValue();

			double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
			sumTR += tr;
		}

		double atr = sumTR / period;
		double avgPrice = data.get(data.size() - 1).getClose().doubleValue();

		return (atr / avgPrice) * 100.0;
	}

	/**
	 * Calculate Average Directional Index (ADX). ADX measures trend strength regardless
	 * of direction.
	 */
	private double calculateADX(List<MarketDataEntity> data, int period) {
		if (data.size() < period * 2) {
			return 25.0; // Default neutral value
		}

		double[] plusDM = new double[data.size()];
		double[] minusDM = new double[data.size()];
		double[] tr = new double[data.size()];

		// Calculate +DM, -DM, and TR
		for (int i = 1; i < data.size(); i++) {
			MarketDataEntity current = data.get(i);
			MarketDataEntity previous = data.get(i - 1);

			double highDiff = current.getHigh().doubleValue() - previous.getHigh().doubleValue();
			double lowDiff = previous.getLow().doubleValue() - current.getLow().doubleValue();

			plusDM[i] = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
			minusDM[i] = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;

			double high = current.getHigh().doubleValue();
			double low = current.getLow().doubleValue();
			double prevClose = previous.getClose().doubleValue();
			tr[i] = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
		}

		// Calculate smoothed values using Wilder's smoothing
		double smoothedPlusDM = 0, smoothedMinusDM = 0, smoothedTR = 0;

		// Initial sum
		for (int i = 1; i <= period; i++) {
			smoothedPlusDM += plusDM[i];
			smoothedMinusDM += minusDM[i];
			smoothedTR += tr[i];
		}

		// Wilder's smoothing
		for (int i = period + 1; i < data.size(); i++) {
			smoothedPlusDM = smoothedPlusDM - (smoothedPlusDM / period) + plusDM[i];
			smoothedMinusDM = smoothedMinusDM - (smoothedMinusDM / period) + minusDM[i];
			smoothedTR = smoothedTR - (smoothedTR / period) + tr[i];
		}

		// Calculate +DI and -DI
		double plusDI = (smoothedTR > 0) ? (smoothedPlusDM / smoothedTR) * 100 : 0;
		double minusDI = (smoothedTR > 0) ? (smoothedMinusDM / smoothedTR) * 100 : 0;

		// Calculate DX
		double diSum = plusDI + minusDI;
		double dx = (diSum > 0) ? Math.abs(plusDI - minusDI) / diSum * 100 : 0;

		// For simplicity, return DX as approximation of ADX
		// (full ADX requires additional smoothing of DX values)
		return dx;
	}

}
