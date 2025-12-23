package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.MarketDataCollectionService;
import io.strategiz.data.marketdata.constants.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled job for historical market data backfill.
 *
 * Purpose: Load historical dataset for all symbols across multiple timeframes
 * Execution: Manual trigger via console or scheduled (disabled by default)
 * Target: All symbols from SymbolService, configurable timeframes
 *
 * Usage:
 * - Initial system setup: Backfill all symbols with historical data
 * - Manual trigger from console app for on-demand backfills
 * - REST API for specific symbol/timeframe backfills
 *
 * Configuration:
 * - marketdata.batch.backfill-enabled: Enable/disable scheduled backfill (default: false)
 * - marketdata.batch.backfill-years: Historical lookback period (default: 7)
 * - marketdata.batch.backfill-timeframes: Comma-separated timeframes (default: 1Day,1Hour,1Week,1Month)
 * - marketdata.batch.thread-pool-size: Concurrent symbol processing (default: 2)
 *
 * Architecture: Only runs when "scheduler" profile is active
 */
@Component
@Profile("scheduler")
public class MarketDataBackfillJob {

	private static final Logger log = LoggerFactory.getLogger(MarketDataBackfillJob.class);

	private final MarketDataCollectionService collectionService;

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Value("${marketdata.batch.backfill-enabled:false}")
	private boolean backfillEnabled;

	@Value("${marketdata.batch.backfill-years:7}")
	private int backfillYears;

	@Value("${marketdata.batch.backfill-timeframes:1Day,1Hour,1Week,1Month}")
	private String backfillTimeframes;

	public MarketDataBackfillJob(MarketDataCollectionService collectionService) {
		this.collectionService = collectionService;
		log.info("MarketDataBackfillJob initialized (enabled: {}, profile: scheduler)", backfillEnabled);
	}

	/**
	 * Scheduled backfill job Runs on startup if enabled (disabled by default)
	 *
	 * WARNING: This is resource-intensive and should only be run once Use REST API
	 * or console trigger for manual backfills instead
	 */
	@Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE) // Run once on startup, 1 min
												// delay
	public void executeScheduledBackfill() {
		if (!backfillEnabled) {
			log.debug("Scheduled backfill is disabled, skipping");
			return;
		}

		executeBackfill();
	}

	/**
	 * Manual trigger for backfill job from console app. Can be called directly
	 * without scheduler profile.
	 * @return BackfillResult with execution details
	 */
	public BackfillResult triggerManualExecution() {
		log.info("Manual backfill triggered from console");
		return executeBackfill();
	}

	/**
	 * Check if job is currently running
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Execute the backfill across all configured timeframes
	 */
	private BackfillResult executeBackfill() {
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Backfill already running, skipping duplicate execution");
			return new BackfillResult(false, 0, 0, 0, "Already running");
		}

		log.info("=== Starting Market Data Backfill ===");
		log.info("Timeframes: {}", backfillTimeframes);
		log.info("Lookback: {} years", backfillYears);

		long startTime = System.currentTimeMillis();

		LocalDateTime endDate = LocalDateTime.now();
		LocalDateTime startDate = endDate.minusYears(backfillYears);

		List<String> timeframes = Arrays.asList(backfillTimeframes.split(","));

		int totalSymbolsProcessed = 0;
		int totalDataPointsStored = 0;
		int totalErrors = 0;
		int timeframesProcessed = 0;

		try {
			for (String timeframe : timeframes) {
				String tf = timeframe.trim();
				if (!Timeframe.VALID_TIMEFRAMES.contains(tf)) {
					log.warn("Skipping invalid timeframe: {}", tf);
					continue;
				}

				try {
					log.info("--- Backfilling timeframe: {} ---", tf);
					long tfStartTime = System.currentTimeMillis();

					MarketDataCollectionService.CollectionResult result = collectionService
						.backfillIntradayData(startDate, endDate, tf);

					long tfDuration = (System.currentTimeMillis() - tfStartTime) / 1000;

					totalSymbolsProcessed += result.totalSymbolsProcessed;
					totalDataPointsStored += result.totalDataPointsStored;
					totalErrors += result.errorCount;
					timeframesProcessed++;

					log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---", tf, tfDuration,
							result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount);

				}
				catch (Exception e) {
					log.error("Failed to backfill timeframe {}: {}", tf, e.getMessage(), e);
					totalErrors++;
				}
			}

			long duration = (System.currentTimeMillis() - startTime) / 1000;

			log.info("=== Backfill Completed in {}s ===", duration);
			log.info("Timeframes processed: {}/{}", timeframesProcessed, timeframes.size());
			log.info("Total symbols processed: {}", totalSymbolsProcessed);
			log.info("Total data points stored: {}", totalDataPointsStored);
			log.info("Total errors: {}", totalErrors);

			return new BackfillResult(true, totalSymbolsProcessed, totalDataPointsStored, totalErrors,
					String.format("Completed in %ds", duration));

		}
		catch (Exception e) {
			log.error("Backfill failed: {}", e.getMessage(), e);
			return new BackfillResult(false, totalSymbolsProcessed, totalDataPointsStored, totalErrors + 1,
					e.getMessage());
		}
		finally {
			isRunning.set(false);
		}
	}

	/**
	 * Result of backfill execution
	 */
	public static class BackfillResult {

		public final boolean success;

		public final int symbolsProcessed;

		public final int dataPointsStored;

		public final int errorCount;

		public final String message;

		public BackfillResult(boolean success, int symbolsProcessed, int dataPointsStored, int errorCount,
				String message) {
			this.success = success;
			this.symbolsProcessed = symbolsProcessed;
			this.dataPointsStored = dataPointsStored;
			this.errorCount = errorCount;
			this.message = message;
		}

	}

}
