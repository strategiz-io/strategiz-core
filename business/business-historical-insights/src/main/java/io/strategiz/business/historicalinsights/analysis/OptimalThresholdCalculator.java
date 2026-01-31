package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.OptimalThresholds;
import io.strategiz.business.historicalinsights.model.PriceTurningPoint;
import io.strategiz.business.historicalinsights.model.PriceTurningPoint.PointType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calculates optimal entry/exit thresholds by analyzing historical turning points.
 *
 * The key insight: by looking at what indicator values were present at actual market
 * peaks and troughs, we can determine thresholds that would have captured most historical
 * opportunities.
 *
 * For example, if RSI was below 32 at 80% of historical troughs, then RSI < 32 is a
 * better entry threshold than the standard RSI < 30.
 */
public class OptimalThresholdCalculator {

	private static final double TARGET_CAPTURE_RATE = 0.80; // Find threshold that
															// captures 80% of points

	/**
	 * Calculate optimal thresholds from turning points.
	 * @param turningPoints List of historical peaks and troughs with indicator values
	 * @return OptimalThresholds with derived values
	 */
	public OptimalThresholds calculate(List<PriceTurningPoint> turningPoints) {
		OptimalThresholds thresholds = new OptimalThresholds();

		if (turningPoints == null || turningPoints.isEmpty()) {
			return thresholds;
		}

		// Separate peaks (sell points) and troughs (buy points)
		List<PriceTurningPoint> troughs = turningPoints.stream()
			.filter(tp -> tp.getType() == PointType.TROUGH)
			.collect(Collectors.toList());

		List<PriceTurningPoint> peaks = turningPoints.stream()
			.filter(tp -> tp.getType() == PointType.PEAK)
			.collect(Collectors.toList());

		thresholds.setTroughsAnalyzed(troughs.size());
		thresholds.setPeaksAnalyzed(peaks.size());

		// Calculate RSI thresholds from troughs (entry points)
		if (!troughs.isEmpty()) {
			List<Double> rsiAtTroughs = troughs.stream()
				.filter(tp -> tp.getRsiAtPoint() > 0)
				.map(PriceTurningPoint::getRsiAtPoint)
				.sorted()
				.collect(Collectors.toList());

			if (!rsiAtTroughs.isEmpty()) {
				// Find RSI threshold that captures TARGET_CAPTURE_RATE of troughs
				int index = (int) (rsiAtTroughs.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, rsiAtTroughs.size() - 1);
				thresholds.setRsiOversold(rsiAtTroughs.get(index));
				thresholds.setRsiOversoldConfidence((double) (index + 1) / rsiAtTroughs.size());
			}

			// Calculate % drop thresholds
			List<Double> dropsAtTroughs = new ArrayList<>();
			for (PriceTurningPoint tp : troughs) {
				double change = tp.getPriceChangeFromPrevious();
				if (change < 0) {
					dropsAtTroughs.add(Math.abs(change));
				}
			}

			if (!dropsAtTroughs.isEmpty()) {
				Collections.sort(dropsAtTroughs);
				int index = (int) (dropsAtTroughs.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, dropsAtTroughs.size() - 1);
				thresholds.setPctDropForEntry(dropsAtTroughs.get(index));
				thresholds.setPctDropConfidence((double) (index + 1) / dropsAtTroughs.size());
			}

			// Calculate Stochastic threshold from troughs
			List<Double> stochAtTroughs = troughs.stream()
				.filter(tp -> tp.getStochasticKAtPoint() > 0)
				.map(PriceTurningPoint::getStochasticKAtPoint)
				.sorted()
				.collect(Collectors.toList());

			if (!stochAtTroughs.isEmpty()) {
				int index = (int) (stochAtTroughs.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, stochAtTroughs.size() - 1);
				thresholds.setStochasticOversold(stochAtTroughs.get(index));
			}

			// Calculate Bollinger Band threshold from troughs
			List<Double> bbLowerPctAtTroughs = troughs.stream()
				.filter(tp -> tp.getPctFromBollingerLower() != 0)
				.map(PriceTurningPoint::getPctFromBollingerLower)
				.sorted()
				.collect(Collectors.toList());

			if (!bbLowerPctAtTroughs.isEmpty()) {
				int index = (int) (bbLowerPctAtTroughs.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, bbLowerPctAtTroughs.size() - 1);
				thresholds.setBbLowerPctForEntry(bbLowerPctAtTroughs.get(index));
			}
		}

		// Calculate RSI thresholds from peaks (exit points)
		if (!peaks.isEmpty()) {
			List<Double> rsiAtPeaks = peaks.stream()
				.filter(tp -> tp.getRsiAtPoint() > 0)
				.map(PriceTurningPoint::getRsiAtPoint)
				.sorted(Collections.reverseOrder())
				.collect(Collectors.toList());

			if (!rsiAtPeaks.isEmpty()) {
				// Find RSI threshold that captures TARGET_CAPTURE_RATE of peaks
				// For peaks, we want RSI values above a threshold, so sort descending
				int index = (int) (rsiAtPeaks.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, rsiAtPeaks.size() - 1);
				thresholds.setRsiOverbought(rsiAtPeaks.get(index));
				thresholds.setRsiOverboughtConfidence((double) (index + 1) / rsiAtPeaks.size());
			}

			// Calculate % gain thresholds
			List<Double> gainsAtPeaks = new ArrayList<>();
			for (PriceTurningPoint tp : peaks) {
				double change = tp.getPriceChangeFromPrevious();
				if (change > 0) {
					gainsAtPeaks.add(change);
				}
			}

			if (!gainsAtPeaks.isEmpty()) {
				Collections.sort(gainsAtPeaks, Collections.reverseOrder());
				int index = (int) (gainsAtPeaks.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, gainsAtPeaks.size() - 1);
				thresholds.setPctGainForExit(gainsAtPeaks.get(index));
				thresholds.setPctGainConfidence((double) (index + 1) / gainsAtPeaks.size());
			}

			// Calculate Stochastic threshold from peaks
			List<Double> stochAtPeaks = peaks.stream()
				.filter(tp -> tp.getStochasticKAtPoint() > 0)
				.map(PriceTurningPoint::getStochasticKAtPoint)
				.sorted(Collections.reverseOrder())
				.collect(Collectors.toList());

			if (!stochAtPeaks.isEmpty()) {
				int index = (int) (stochAtPeaks.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, stochAtPeaks.size() - 1);
				thresholds.setStochasticOverbought(stochAtPeaks.get(index));
			}

			// Calculate Bollinger Band threshold from peaks
			List<Double> bbUpperPctAtPeaks = peaks.stream()
				.filter(tp -> tp.getPctFromBollingerUpper() != 0)
				.map(PriceTurningPoint::getPctFromBollingerUpper)
				.sorted(Collections.reverseOrder())
				.collect(Collectors.toList());

			if (!bbUpperPctAtPeaks.isEmpty()) {
				int index = (int) (bbUpperPctAtPeaks.size() * TARGET_CAPTURE_RATE);
				index = Math.min(index, bbUpperPctAtPeaks.size() - 1);
				thresholds.setBbUpperPctForExit(bbUpperPctAtPeaks.get(index));
			}
		}

		// Calculate volume confirmation threshold
		List<Double> volumeRatios = turningPoints.stream()
			.filter(tp -> tp.getVolumeRatio() > 0)
			.map(PriceTurningPoint::getVolumeRatio)
			.sorted(Collections.reverseOrder())
			.collect(Collectors.toList());

		if (!volumeRatios.isEmpty()) {
			// Find volume ratio at which most turning points had elevated volume
			double sumRatio = 0;
			for (double ratio : volumeRatios) {
				sumRatio += ratio;
			}
			thresholds.setVolumeRatioForConfirmation(sumRatio / volumeRatios.size());
		}

		return thresholds;
	}

}
