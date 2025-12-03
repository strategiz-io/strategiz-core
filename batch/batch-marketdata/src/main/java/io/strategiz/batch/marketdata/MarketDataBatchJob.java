package io.strategiz.batch.marketdata;

import io.strategiz.business.marketdata.MarketDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Daily scheduled job for fetching market data from Polygon.io
 * Runs twice per day at configured times (default: 2 AM and 2 PM EST)
 *
 * The job is designed to:
 * 1. Run twice daily to provide more timely data updates
 * 2. Fetch previous day's market data after market close
 * 3. Process user watchlists and priority symbols
 * 4. Store data for strategy backtesting
 *
 * Architecture: Only runs when "scheduler" profile is active
 */
@Component
@ConditionalOnProperty(name = "polygon.batch.enabled", havingValue = "true", matchIfMissing = true)
@Profile("scheduler")
public class MarketDataBatchJob {

    private static final Logger log = LoggerFactory.getLogger(MarketDataBatchJob.class);

    @Value("${polygon.batch.symbols.max:95}")
    private int maxSymbolsPerDay;

    @Value("${polygon.batch.timezone:America/New_York}")
    private String batchTimezone;

    private final MarketDataCollectionService collectionService;

    public MarketDataBatchJob(MarketDataCollectionService collectionService) {
        this.collectionService = collectionService;
        log.info("MarketDataBatchJob initialized (profile: scheduler)");
    }

    /**
     * Main scheduled job - runs TWICE daily (every 12 hours)
     *
     * Cron expression breakdown:
     * - "0 0 2,14 * * ?" = Run at 2:00 AM and 2:00 PM EST every day
     * - Format: second minute hour day-of-month month day-of-week
     *
     * Why twice daily?
     * - Morning run (2 AM): Gets previous day's finalized data
     * - Afternoon run (2 PM): Gets morning/intraday data for active trading
     * - Splits the 100 daily API calls across two runs (45 symbols each)
     * - Provides more timely data updates
     */
    @Scheduled(cron = "${polygon.batch.cron:0 0 2,14 * * ?}", zone = "${polygon.batch.timezone:America/New_York}")
    public void runDailyMarketDataCollection() {
        log.info("========================================");
        log.info("Starting DAILY Market Data Collection");
        log.info("Time: {} ({})", LocalDateTime.now(), batchTimezone);
        log.info("Max symbols to process: {}", maxSymbolsPerDay);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        try {
            MarketDataCollectionService.CollectionResult result = collectionService.collectDailyData();
            long executionTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

            log.info("========================================");
            log.info("Daily Market Data Collection COMPLETED");
            log.info("Symbols processed: {}", result.totalSymbolsProcessed);
            log.info("Data points stored: {}", result.totalDataPointsStored);
            log.info("Errors encountered: {}", result.errorCount);
            log.info("Time taken: {} seconds", executionTimeSeconds);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Fatal error in market data collection", e);
        }
    }
}
