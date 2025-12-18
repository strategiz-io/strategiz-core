package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.AlpacaCollectionService;
import io.strategiz.data.marketdata.constants.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled job for incremental intraday data collection from Alpaca
 *
 * Purpose: Keep market data up-to-date with latest bars across ALL timeframes
 * Execution: Auto-scheduled every 5 minutes during market hours
 * Target: Fetch latest bars for all tracked symbols across all 9 timeframes
 *
 * Behavior:
 * - Iterates through all 9 canonical timeframes (1Min to 1Month)
 * - Checks latest bar timestamp in Firestore for each symbol/timeframe
 * - Fetches only new bars since last collection (delta collection)
 * - Handles market hours automatically (9:30 AM - 4:00 PM ET)
 * - Runs on weekdays only (Monday-Friday)
 * - Aggregates results across all timeframes
 *
 * Configuration:
 * - alpaca.batch.incremental-enabled: Enable/disable incremental updates (default: false)
 * - alpaca.batch.incremental-cron: Cron schedule (default: every 5 minutes)
 * - alpaca.batch.incremental-lookback-hours: How far back to look for new bars (default: 2)
 *
 * Timeframes collected: 1Min, 5Min, 15Min, 30Min, 1Hour, 4Hour, 1Day, 1Week, 1Month
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

    @Value("${alpaca.batch.incremental-lookback-hours:2}")
    private int lookbackHours;

    // All canonical timeframes to collect
    private static final List<String> ALL_TIMEFRAMES = new ArrayList<>(Timeframe.VALID_TIMEFRAMES);

    public AlpacaIncrementalJob(AlpacaCollectionService collectionService) {
        this.collectionService = collectionService;
        log.info("AlpacaIncrementalJob initialized (enabled: {}, timeframes: {}, profile: scheduler)",
                incrementalEnabled, ALL_TIMEFRAMES.size());
    }

    /**
     * Scheduled incremental collection across ALL timeframes
     * Runs every 5 minutes by default (configurable via cron)
     *
     * Default schedule: 0 *&#47;5 * * * MON-FRI (every 5 minutes, weekdays only)
     * Market hours: 9:30 AM - 4:00 PM ET
     *
     * Collects delta data for all 9 timeframes: 1Min, 5Min, 15Min, 30Min, 1Hour, 4Hour, 1Day, 1Week, 1Month
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

        log.info("=== Starting Scheduled Incremental Collection (All {} Timeframes) ===", ALL_TIMEFRAMES.size());

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(lookbackHours);

        long startTime = System.currentTimeMillis();

        // Aggregate counters across all timeframes
        int totalSymbolsProcessed = 0;
        int totalDataPointsStored = 0;
        int totalErrors = 0;
        int timeframesProcessed = 0;

        for (String timeframe : ALL_TIMEFRAMES) {
            try {
                log.info("--- Collecting timeframe: {} ---", timeframe);
                long tfStartTime = System.currentTimeMillis();

                AlpacaCollectionService.CollectionResult result =
                        collectionService.backfillIntradayData(startDate, endDate, timeframe);

                long tfDuration = (System.currentTimeMillis() - tfStartTime) / 1000;

                totalSymbolsProcessed += result.totalSymbolsProcessed;
                totalDataPointsStored += result.totalDataPointsStored;
                totalErrors += result.errorCount;
                timeframesProcessed++;

                log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---",
                        timeframe, tfDuration, result.totalSymbolsProcessed,
                        result.totalDataPointsStored, result.errorCount);

            } catch (Exception e) {
                log.error("Failed to collect timeframe {}: {}", timeframe, e.getMessage(), e);
                totalErrors++;
            }
        }

        long totalDuration = (System.currentTimeMillis() - startTime) / 1000;

        log.info("=== Incremental Collection Completed in {}s ===", totalDuration);
        log.info("Timeframes processed: {}/{}", timeframesProcessed, ALL_TIMEFRAMES.size());
        log.info("Total symbols processed: {}", totalSymbolsProcessed);
        log.info("Total data points stored: {}", totalDataPointsStored);
        log.info("Total errors: {}", totalErrors);
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
