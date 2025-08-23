package io.strategiz.service.marketdata.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Daily batch job for fetching market data from Polygon.io
 * Runs once per day at a configured time (default: 2 AM EST)
 * 
 * The job is designed to:
 * 1. Run only once per day to stay within free tier limits (100 calls/day)
 * 2. Fetch previous day's market data after market close
 * 3. Process user watchlists and priority symbols
 * 4. Store data for strategy backtesting
 */
@Component
@ConditionalOnProperty(name = "polygon.batch.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataBatchJob {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataBatchJob.class);
    
    @Value("${polygon.batch.symbols.max:95}")
    private int maxSymbolsPerDay;
    
    @Value("${polygon.batch.timezone:America/New_York}")
    private String batchTimezone;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger dailyApiCallCount = new AtomicInteger(0);
    private LocalDateTime lastRunTime;
    
    private final MarketDataCollectionService collectionService;
    
    public MarketDataBatchJob(MarketDataCollectionService collectionService) {
        this.collectionService = collectionService;
    }
    
    /**
     * Main batch job - runs TWICE daily (every 12 hours)
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
        // Prevent multiple executions
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Market data batch job is already running, skipping this execution");
            return;
        }
        
        try {
            log.info("========================================");
            log.info("Starting DAILY Market Data Batch Job");
            log.info("Time: {} ({})", LocalDateTime.now(), batchTimezone);
            log.info("Max symbols to process: {}", maxSymbolsPerDay);
            log.info("========================================");
            
            // Reset daily counter at the start of each day
            dailyApiCallCount.set(0);
            lastRunTime = LocalDateTime.now();
            
            // Execute the batch collection
            BatchExecutionResult result = collectionService.collectDailyMarketData(maxSymbolsPerDay);
            
            // Log results
            log.info("========================================");
            log.info("Daily Market Data Batch Job COMPLETED");
            log.info("Symbols processed: {}", result.getSymbolsProcessed());
            log.info("API calls made: {}", result.getApiCallsMade());
            log.info("Errors encountered: {}", result.getErrorCount());
            log.info("Time taken: {} seconds", result.getExecutionTimeSeconds());
            log.info("========================================");
            
            // Update counter
            dailyApiCallCount.set(result.getApiCallsMade());
            
        } catch (Exception e) {
            log.error("Fatal error in market data batch job", e);
        } finally {
            isRunning.set(false);
        }
    }
    
    /**
     * Health check method - can be called to verify the job is configured correctly
     */
    public BatchJobStatus getStatus() {
        return new BatchJobStatus(
            isRunning.get(),
            lastRunTime,
            dailyApiCallCount.get(),
            maxSymbolsPerDay
        );
    }
    
    /**
     * Manual trigger for testing or emergency data collection
     * This should be used sparingly to stay within API limits
     */
    public void triggerManually(List<String> prioritySymbols) {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Batch job is already running");
        }
        
        try {
            log.info("MANUAL trigger of market data collection for {} symbols", prioritySymbols.size());
            
            // Check if we have API calls remaining
            int remainingCalls = maxSymbolsPerDay - dailyApiCallCount.get();
            if (remainingCalls <= 0) {
                throw new IllegalStateException("Daily API limit reached. No calls remaining.");
            }
            
            // Limit symbols to remaining API calls
            List<String> symbolsToProcess = prioritySymbols.stream()
                .limit(remainingCalls)
                .toList();
            
            BatchExecutionResult result = collectionService.collectMarketDataForSymbols(symbolsToProcess);
            dailyApiCallCount.addAndGet(result.getApiCallsMade());
            
            log.info("Manual collection completed. Processed {} symbols", result.getSymbolsProcessed());
            
        } finally {
            isRunning.set(false);
        }
    }
    
    /**
     * Status object for monitoring
     */
    public static class BatchJobStatus {
        private final boolean running;
        private final LocalDateTime lastRun;
        private final int apiCallsToday;
        private final int maxApiCallsPerDay;
        
        public BatchJobStatus(boolean running, LocalDateTime lastRun, int apiCallsToday, int maxApiCallsPerDay) {
            this.running = running;
            this.lastRun = lastRun;
            this.apiCallsToday = apiCallsToday;
            this.maxApiCallsPerDay = maxApiCallsPerDay;
        }
        
        // Getters
        public boolean isRunning() { return running; }
        public LocalDateTime getLastRun() { return lastRun; }
        public int getApiCallsToday() { return apiCallsToday; }
        public int getMaxApiCallsPerDay() { return maxApiCallsPerDay; }
        public int getRemainingApiCalls() { return maxApiCallsPerDay - apiCallsToday; }
    }
    
    /**
     * Result of batch execution
     */
    public static class BatchExecutionResult {
        private final int symbolsProcessed;
        private final int apiCallsMade;
        private final int errorCount;
        private final long executionTimeSeconds;
        
        public BatchExecutionResult(int symbolsProcessed, int apiCallsMade, int errorCount, long executionTimeSeconds) {
            this.symbolsProcessed = symbolsProcessed;
            this.apiCallsMade = apiCallsMade;
            this.errorCount = errorCount;
            this.executionTimeSeconds = executionTimeSeconds;
        }
        
        // Getters
        public int getSymbolsProcessed() { return symbolsProcessed; }
        public int getApiCallsMade() { return apiCallsMade; }
        public int getErrorCount() { return errorCount; }
        public long getExecutionTimeSeconds() { return executionTimeSeconds; }
    }
}