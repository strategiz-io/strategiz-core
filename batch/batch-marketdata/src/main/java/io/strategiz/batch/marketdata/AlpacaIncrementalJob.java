package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.AlpacaCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job for incremental intraday data collection from Alpaca
 *
 * Purpose: Keep market data up-to-date with latest bars
 * Execution: Auto-scheduled every 5 minutes during market hours
 * Target: Fetch latest bars for all tracked symbols
 *
 * Behavior:
 * - Checks latest bar timestamp in Firestore for each symbol
 * - Fetches only new bars since last collection
 * - Handles market hours automatically (9:30 AM - 4:00 PM ET)
 * - Runs on weekdays only (Monday-Friday)
 *
 * Configuration:
 * - alpaca.batch.incremental-enabled: Enable/disable incremental updates (default: false)
 * - alpaca.batch.incremental-timeframe: Bar interval (default: 1Min)
 * - alpaca.batch.incremental-cron: Cron schedule (default: every 5 minutes)
 *
 * Architecture: Only runs when "scheduler" profile is active
 */
@Component
@Profile("scheduler")
public class AlpacaIncrementalJob {

    private static final Logger log = LoggerFactory.getLogger(AlpacaIncrementalJob.class);

    private final AlpacaCollectionService collectionService;

    @Value("${alpaca.batch.incremental-enabled:false}")
    private boolean incrementalEnabled;

    @Value("${alpaca.batch.incremental-timeframe:1Min}")
    private String defaultTimeframe;

    @Value("${alpaca.batch.incremental-lookback-hours:2}")
    private int lookbackHours;

    public AlpacaIncrementalJob(AlpacaCollectionService collectionService) {
        this.collectionService = collectionService;
        log.info("AlpacaIncrementalJob initialized (enabled: {}, profile: scheduler)", incrementalEnabled);
    }

    /**
     * Scheduled incremental collection
     * Runs every 5 minutes by default (configurable via cron)
     *
     * Default schedule: 0 *&#47;5 * * * MON-FRI (every 5 minutes, weekdays only)
     * Market hours: 9:30 AM - 4:00 PM ET
     */
    @Scheduled(cron = "${alpaca.batch.incremental-cron:0 */5 * * * MON-FRI}")
    public void executeScheduledIncremental() {
        if (!incrementalEnabled) {
            log.debug("Incremental collection is disabled, skipping");
            return;
        }

        // Check if we're in market hours (9:30 AM - 4:00 PM ET)
        if (!isMarketHours()) {
            log.debug("Outside market hours, skipping incremental collection");
            return;
        }

        log.info("=== Starting Scheduled Incremental Collection ===");

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(lookbackHours);

        long startTime = System.currentTimeMillis();

        try {
            AlpacaCollectionService.CollectionResult result =
                    collectionService.backfillIntradayData(startDate, endDate, defaultTimeframe);

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            log.info("=== Incremental Collection Completed in {}s ===", duration);
            log.info("Symbols processed: {}", result.totalSymbolsProcessed);
            log.info("Data points stored: {}", result.totalDataPointsStored);
            log.info("Errors: {}", result.errorCount);

        } catch (Exception e) {
            log.error("Incremental collection failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if current time is within market hours (9:30 AM - 4:00 PM ET)
     * US stock markets operate Monday-Friday
     *
     * Note: This is a simplified check. For production, consider:
     * - Market holidays
     * - Early closures
     * - Extended hours trading
     * - Using exchange calendar API
     */
    private boolean isMarketHours() {
        LocalDateTime now = LocalDateTime.now();

        // Weekday check (Monday = 1, Sunday = 7)
        int dayOfWeek = now.getDayOfWeek().getValue();
        if (dayOfWeek > 5) {  // Saturday or Sunday
            return false;
        }

        // Time check (simplified - assumes ET timezone)
        // For production, convert to ET timezone explicitly
        int hour = now.getHour();
        int minute = now.getMinute();

        // Market open: 9:30 AM
        boolean afterOpen = (hour > 9) || (hour == 9 && minute >= 30);

        // Market close: 4:00 PM
        boolean beforeClose = (hour < 16);

        return afterOpen && beforeClose;
    }
}
