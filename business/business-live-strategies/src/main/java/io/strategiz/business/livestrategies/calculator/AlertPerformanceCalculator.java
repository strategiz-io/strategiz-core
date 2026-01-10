package io.strategiz.business.livestrategies.calculator;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;
import io.strategiz.data.strategy.entity.AlertLivePerformance;
import com.google.cloud.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculator for alert live performance metrics.
 * Analyzes signal history to compute real-time performance statistics.
 */
@Component
public class AlertPerformanceCalculator {

	/**
	 * Calculate live performance from signal history
	 *
	 * @param alertId              The alert deployment ID
	 * @param signalHistory        List of all signals triggered by this alert
	 * @param deploymentStartDate  When the alert was deployed
	 * @return Calculated live performance metrics
	 */
	public AlertLivePerformance calculatePerformance(String alertId, List<AlertDeploymentHistory> signalHistory,
			Instant deploymentStartDate) {

		AlertLivePerformance perf = new AlertLivePerformance();

		// Basic counts
		perf.setTotalSignals(signalHistory.size());
		perf.setDeploymentStartDate(deploymentStartDate);

		if (!signalHistory.isEmpty()) {
			Timestamp lastTimestamp = signalHistory.get(signalHistory.size() - 1).getTimestamp();
			if (lastTimestamp != null) {
				perf.setLastSignalDate(
						Instant.ofEpochSecond(lastTimestamp.getSeconds(), lastTimestamp.getNanos()));
			}
		}

		// Calculate signal breakdown (BUY, SELL, HOLD)
		Map<String, Integer> breakdown = new HashMap<>();
		Map<String, Integer> bySymbol = new HashMap<>();

		Instant now = Instant.now();
		Instant monthAgo = now.minus(30, ChronoUnit.DAYS);
		Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
		Instant today = now.truncatedTo(ChronoUnit.DAYS);

		int monthCount = 0;
		int weekCount = 0;
		int todayCount = 0;

		for (AlertDeploymentHistory signal : signalHistory) {
			// Signal type breakdown
			String signalType = signal.getSignal();
			breakdown.put(signalType, breakdown.getOrDefault(signalType, 0) + 1);

			// By symbol
			String symbol = signal.getSymbol();
			bySymbol.put(symbol, bySymbol.getOrDefault(symbol, 0) + 1);

			// Time-based counts
			Timestamp ts = signal.getTimestamp();
			if (ts != null) {
				Instant timestamp = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
				if (timestamp.isAfter(monthAgo)) {
					monthCount++;
				}
				if (timestamp.isAfter(weekAgo)) {
					weekCount++;
				}
				if (timestamp.isAfter(today)) {
					todayCount++;
				}
			}
		}

		perf.setSignalBreakdown(breakdown);
		perf.setSignalsBySymbol(bySymbol);
		perf.setSignalsThisMonth(monthCount);
		perf.setSignalsThisWeek(weekCount);
		perf.setSignalsToday(todayCount);

		// Days since deployment
		if (deploymentStartDate != null) {
			long days = ChronoUnit.DAYS.between(deploymentStartDate, now);
			perf.setDaysSinceDeployment((int) days);
		}

		return perf;
	}

}
