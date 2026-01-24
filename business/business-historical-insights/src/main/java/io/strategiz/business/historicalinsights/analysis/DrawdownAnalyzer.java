package io.strategiz.business.historicalinsights.analysis;

import io.strategiz.business.historicalinsights.model.DrawdownProfile;
import io.strategiz.business.historicalinsights.model.DrawdownProfile.DrawdownEvent;
import io.strategiz.data.marketdata.entity.MarketDataEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Analyzes historical price data to calculate comprehensive drawdown statistics.
 * Drawdowns are peak-to-trough declines during a specific period.
 *
 * Key metrics calculated:
 * - Maximum drawdown (worst peak-to-trough decline)
 * - Average drawdown
 * - Drawdown duration and recovery times
 * - Significant drawdown events
 */
public class DrawdownAnalyzer {

	private static final double MIN_DRAWDOWN_THRESHOLD = 5.0; // 5% minimum to count as significant

	/**
	 * Analyze drawdowns in the given price data.
	 * @param data Market data with OHLCV
	 * @return DrawdownProfile with comprehensive statistics
	 */
	public DrawdownProfile analyze(List<MarketDataEntity> data) {
		DrawdownProfile profile = new DrawdownProfile();

		if (data == null || data.size() < 10) {
			return profile;
		}

		List<DrawdownEvent> drawdowns = detectDrawdowns(data);

		if (drawdowns.isEmpty()) {
			profile.setMaxDrawdownPercent(0);
			profile.setAvgDrawdownPercent(0);
			profile.setTotalDrawdowns(0);
			return profile;
		}

		// Calculate statistics
		double maxDrawdown = 0;
		double sumDrawdown = 0;
		int sumDuration = 0;
		int sumRecovery = 0;
		int recoveryCount = 0;
		DrawdownEvent maxEvent = null;

		for (DrawdownEvent event : drawdowns) {
			sumDrawdown += event.getMagnitude();
			sumDuration += event.getDurationDays();

			if (event.getRecoveryDays() > 0) {
				sumRecovery += event.getRecoveryDays();
				recoveryCount++;
			}

			if (event.getMagnitude() > maxDrawdown) {
				maxDrawdown = event.getMagnitude();
				maxEvent = event;
			}
		}

		// Set profile fields
		profile.setMaxDrawdownPercent(maxDrawdown);
		profile.setAvgDrawdownPercent(sumDrawdown / drawdowns.size());
		profile.setAvgDrawdownDuration(sumDuration / drawdowns.size());
		profile.setAvgRecoveryDays(recoveryCount > 0 ? sumRecovery / recoveryCount : 0);
		profile.setTotalDrawdowns(drawdowns.size());

		if (maxEvent != null) {
			profile.setMaxDrawdownStart(maxEvent.getStart());
			profile.setMaxDrawdownEnd(maxEvent.getTrough());
			profile.setMaxDrawdownDuration(maxEvent.getDurationDays());
		}

		// Get top 5 significant drawdowns
		drawdowns.sort(Comparator.comparingDouble(DrawdownEvent::getMagnitude).reversed());
		profile.setSignificantDrawdowns(drawdowns.subList(0, Math.min(5, drawdowns.size())));

		return profile;
	}

	/**
	 * Detect all drawdown events in the price data.
	 */
	private List<DrawdownEvent> detectDrawdowns(List<MarketDataEntity> data) {
		List<DrawdownEvent> drawdowns = new ArrayList<>();

		double peak = data.get(0).getClose().doubleValue();
		Instant peakTime = Instant.ofEpochMilli(data.get(0).getTimestamp());
		double trough = peak;
		Instant troughTime = peakTime;
		boolean inDrawdown = false;

		for (int i = 1; i < data.size(); i++) {
			MarketDataEntity bar = data.get(i);
			double price = bar.getClose().doubleValue();
			Instant time = Instant.ofEpochMilli(bar.getTimestamp());

			if (price >= peak) {
				// New peak - if we were in a drawdown, record it
				if (inDrawdown) {
					double drawdownPct = ((peak - trough) / peak) * 100;
					if (drawdownPct >= MIN_DRAWDOWN_THRESHOLD) {
						DrawdownEvent event = new DrawdownEvent();
						event.setMagnitude(drawdownPct);
						event.setStart(peakTime);
						event.setTrough(troughTime);
						event.setRecovery(time);
						event.setDurationDays((int) Duration.between(peakTime, troughTime).toDays());
						event.setRecoveryDays((int) Duration.between(troughTime, time).toDays());
						drawdowns.add(event);
					}
					inDrawdown = false;
				}

				// Update peak
				peak = price;
				peakTime = time;
				trough = price;
				troughTime = time;
			}
			else {
				// Price below peak - we're in a drawdown
				inDrawdown = true;
				if (price < trough) {
					trough = price;
					troughTime = time;
				}
			}
		}

		// Handle final drawdown if still in one
		if (inDrawdown) {
			double drawdownPct = ((peak - trough) / peak) * 100;
			if (drawdownPct >= MIN_DRAWDOWN_THRESHOLD) {
				DrawdownEvent event = new DrawdownEvent();
				event.setMagnitude(drawdownPct);
				event.setStart(peakTime);
				event.setTrough(troughTime);
				event.setDurationDays((int) Duration.between(peakTime, troughTime).toDays());
				// No recovery yet
				event.setRecoveryDays(0);
				drawdowns.add(event);
			}
		}

		return drawdowns;
	}

}
