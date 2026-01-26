package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.business.marketdata.MarketDataCollectionService;
import io.strategiz.data.marketdata.constants.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Daily end-of-day sync job for market data collection.
 *
 * Purpose: Ensure no data gaps by running daily after market close
 * Execution: Scheduled at 5 PM ET (22:00 UTC) Monday-Friday
 * Target: All symbols for daily timeframe with 48-hour lookback
 *
 * This job complements the incremental job by catching any data
 * that might have been missed during the day (e.g., if Cloud Run
 * instance was scaled down).
 *
 * Configuration:
 * - marketdata.batch.daily-sync-enabled: Enable/disable daily sync
 * - marketdata.batch.daily-sync-cron: Cron schedule (default: 5 PM ET)
 * - marketdata.batch.daily-sync-lookback-hours: Lookback period (default: 48)
 */
@Component
@Profile("scheduler")
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class MarketDataDailySyncJob {

	private static final Logger log = LoggerFactory.getLogger(MarketDataDailySyncJob.class);

	private final MarketDataCollectionService collectionService;
	private final JobExecutionHistoryBusiness jobExecutionHistoryBusiness;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Value("${marketdata.batch.daily-sync-enabled:false}")
	private boolean dailySyncEnabled;

	@Value("${marketdata.batch.daily-sync-lookback-hours:48}")
	private int lookbackHours;

	// Timeframes to sync daily - focus on daily and longer
	private static final List<String> DAILY_TIMEFRAMES = Arrays.asList("1D", "1W", "1M");

	public MarketDataDailySyncJob(
			MarketDataCollectionService collectionService,
			JobExecutionHistoryBusiness jobExecutionHistoryBusiness) {
		this.collectionService = collectionService;
		this.jobExecutionHistoryBusiness = jobExecutionHistoryBusiness;
		log.info("MarketDataDailySyncJob initialized (enabled: {}, lookback: {} hours, profile: scheduler)",
				dailySyncEnabled, lookbackHours);
	}

	/**
	 * Run on startup if enabled - for catch-up scenarios.
	 * Waits 30 seconds for app to fully initialize before running.
	 */
	@jakarta.annotation.PostConstruct
	public void runOnStartupIfEnabled() {
		if (dailySyncEnabled) {
			log.info("Daily sync enabled - triggering catch-up execution on startup with {} hour lookback", lookbackHours);
			new Thread(() -> {
				try {
					Thread.sleep(30000); // Wait 30s for app to fully initialize
					execute();
				} catch (Exception e) {
					log.error("Failed to run startup daily sync: {}", e.getMessage(), e);
				}
			}, "daily-sync-startup").start();
		} else {
			log.info("Daily sync disabled - skipping startup execution");
		}
	}

	/**
	 * Scheduled execution - runs at 5 PM ET (22:00 UTC) Monday-Friday.
	 * Uses @Scheduled annotation for reliable execution.
	 */
	@Scheduled(cron = "${marketdata.batch.daily-sync-cron:0 0 22 * * MON-FRI}")
	public void scheduledExecution() {
		if (!dailySyncEnabled) {
			log.debug("Daily sync disabled, skipping scheduled execution");
			return;
		}
		execute();
	}

	/**
	 * Public execute method - can be called manually or by scheduler.
	 */
	public void execute() {
		executeDailySync();
	}

	/**
	 * Manual trigger for daily sync from console app.
	 * @return DailySyncResult with execution details
	 */
	public DailySyncResult triggerManualExecution() {
		log.info("Manual daily sync triggered from console");
		return executeDailySync();
	}

	/**
	 * Manual trigger with custom lookback period.
	 * Useful for catching up on missed data.
	 * @param lookbackHours Number of hours to look back
	 * @return DailySyncResult with execution details
	 */
	public DailySyncResult triggerManualExecution(int lookbackHours) {
		log.info("Manual daily sync triggered with {} hour lookback", lookbackHours);
		return executeDailySync(lookbackHours);
	}

	/**
	 * Check if job is currently running
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Execute the daily sync with default lookback
	 */
	private DailySyncResult executeDailySync() {
		return executeDailySync(lookbackHours);
	}

	/**
	 * Execute the daily sync with custom lookback
	 */
	private DailySyncResult executeDailySync(int hoursLookback) {
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Daily sync already running, skipping duplicate execution");
			return new DailySyncResult(false, 0, 0, 0, "Already running");
		}

		log.info("=== Starting Market Data Daily Sync ===");
		log.info("Lookback: {} hours", hoursLookback);
		log.info("Timeframes: {}", DAILY_TIMEFRAMES);

		// Record job execution start
		String executionId = jobExecutionHistoryBusiness.recordJobStart(
			"MARKETDATA_DAILY_SYNC",
			"MarketData_DailySync",
			toJson(DAILY_TIMEFRAMES)
		);

		long startTime = System.currentTimeMillis();

		LocalDateTime endDate = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime startDate = endDate.minusHours(hoursLookback);

		int totalSymbolsProcessed = 0;
		int totalDataPointsStored = 0;
		int totalErrors = 0;
		int timeframesProcessed = 0;

		try {
			for (String timeframe : DAILY_TIMEFRAMES) {
				if (!Timeframe.VALID_TIMEFRAMES.contains(timeframe)) {
					log.warn("Skipping invalid timeframe: {}", timeframe);
					continue;
				}

				try {
					log.info("--- Syncing timeframe: {} (lookback: {} hours) ---", timeframe, hoursLookback);
					long tfStartTime = System.currentTimeMillis();

					// Use the standard backfill method with date range
					MarketDataCollectionService.CollectionResult result = collectionService
						.backfillIntradayData(startDate, endDate, timeframe);

					long tfDuration = (System.currentTimeMillis() - tfStartTime) / 1000;

					totalSymbolsProcessed += result.totalSymbolsProcessed;
					totalDataPointsStored += result.totalDataPointsStored;
					totalErrors += result.errorCount;
					timeframesProcessed++;

					log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---",
							timeframe, tfDuration, result.totalSymbolsProcessed,
							result.totalDataPointsStored, result.errorCount);

				} catch (Exception e) {
					log.error("Failed to sync timeframe {}: {}", timeframe, e.getMessage(), e);
					totalErrors++;
				}
			}

			long duration = (System.currentTimeMillis() - startTime) / 1000;

			log.info("=== Daily Sync Completed in {}s ===", duration);
			log.info("Timeframes processed: {}/{}", timeframesProcessed, DAILY_TIMEFRAMES.size());
			log.info("Total symbols processed: {}", totalSymbolsProcessed);
			log.info("Total data points stored: {}", totalDataPointsStored);
			log.info("Total errors: {}", totalErrors);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(
				executionId,
				"SUCCESS",
				totalSymbolsProcessed,
				totalDataPointsStored,
				totalErrors,
				null
			);

			return new DailySyncResult(true, totalSymbolsProcessed, totalDataPointsStored, totalErrors,
					String.format("Completed in %ds", duration));

		} catch (Exception e) {
			log.error("Daily sync failed: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(
				executionId,
				"FAILED",
				totalSymbolsProcessed,
				totalDataPointsStored,
				totalErrors + 1,
				e.getMessage()
			);

			return new DailySyncResult(false, totalSymbolsProcessed, totalDataPointsStored, totalErrors + 1,
					e.getMessage());

		} finally {
			isRunning.set(false);
		}
	}

	/**
	 * Helper method to convert list to JSON string for history storage.
	 */
	private String toJson(List<String> list) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
		} catch (Exception e) {
			log.warn("Failed to convert to JSON: {}", e.getMessage());
			return list != null ? list.toString() : "[]";
		}
	}

	/**
	 * Result of daily sync execution
	 */
	public static class DailySyncResult {

		public final boolean success;
		public final int symbolsProcessed;
		public final int dataPointsStored;
		public final int errorCount;
		public final String message;

		public DailySyncResult(boolean success, int symbolsProcessed, int dataPointsStored, int errorCount,
				String message) {
			this.success = success;
			this.symbolsProcessed = symbolsProcessed;
			this.dataPointsStored = dataPointsStored;
			this.errorCount = errorCount;
			this.message = message;
		}

	}

}
