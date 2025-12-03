package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.AlpacaCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for one-time historical data backfill from Alpaca
 *
 * Purpose: Load initial historical dataset (3 months of 1Min bars)
 * Execution: Auto-scheduled on startup if enabled (disabled by default)
 * Target: ~500 symbols, ~3GB data
 *
 * Usage:
 * - Initial system setup: Backfill all symbols with 3 months of data
 * - Use REST API for manual/one-time backfills instead of this scheduled job
 *
 * Configuration:
 * - alpaca.batch.backfill-enabled: Enable/disable backfill (default: false)
 * - alpaca.batch.backfill-months: Historical lookback period (default: 3)
 * - alpaca.batch.thread-pool-size: Concurrent symbol processing (default: 10)
 *
 * Architecture: Only runs when "scheduler" profile is active
 */
@Component
@Profile("scheduler")
public class AlpacaBackfillJob {

    private static final Logger log = LoggerFactory.getLogger(AlpacaBackfillJob.class);

    private final AlpacaCollectionService collectionService;

    @Value("${alpaca.batch.backfill-enabled:false}")
    private boolean backfillEnabled;

    @Value("${alpaca.batch.backfill-timeframe:1Min}")
    private String defaultTimeframe;

    public AlpacaBackfillJob(AlpacaCollectionService collectionService) {
        this.collectionService = collectionService;
        log.info("AlpacaBackfillJob initialized (enabled: {}, profile: scheduler)", backfillEnabled);
    }

    /**
     * Scheduled backfill job
     * Runs on startup if enabled (disabled by default)
     *
     * WARNING: This is resource-intensive and should only be run once
     * Use REST API for manual backfills instead
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)  // Run once on startup, 1 min delay
    public void executeScheduledBackfill() {
        if (!backfillEnabled) {
            log.debug("Scheduled backfill is disabled, skipping");
            return;
        }

        log.info("=== Starting Scheduled Alpaca Backfill ===");

        long startTime = System.currentTimeMillis();

        try {
            AlpacaCollectionService.CollectionResult result =
                    collectionService.backfillIntradayData(defaultTimeframe);

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            log.info("=== Backfill Completed in {}s ===", duration);
            log.info("Symbols processed: {}", result.totalSymbolsProcessed);
            log.info("Data points stored: {}", result.totalDataPointsStored);
            log.info("Errors: {}", result.errorCount);

        } catch (Exception e) {
            log.error("Scheduled backfill failed: {}", e.getMessage(), e);
        }
    }
}
