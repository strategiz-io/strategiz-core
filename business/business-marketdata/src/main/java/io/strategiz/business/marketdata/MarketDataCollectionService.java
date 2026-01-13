package io.strategiz.business.marketdata;

import io.strategiz.client.alpaca.client.AlpacaAssetsClient;
import io.strategiz.client.alpaca.client.AlpacaHistoricalClient;
import io.strategiz.client.alpaca.model.AlpacaAsset;
import io.strategiz.client.alpaca.model.AlpacaBar;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.strategiz.business.marketdata.exception.MarketDataErrorDetails;
import io.strategiz.framework.exception.StrategizException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for collecting and managing market data from data providers.
 *
 * Features:
 * - Multi-threaded backfill with configurable concurrency (default 2 threads)
 * - Batch saves (500 entities per batch)
 * - Comprehensive field mapping to MarketDataEntity
 * - Symbol metadata enrichment from Assets API
 * - All timeframe support (1Min, 5Min, 15Min, 1Hour, 1Day, 1Week, 1Month)
 * - Automatic pagination handling
 * - Rate limiting and error recovery
 *
 * Target: All symbols from Firestore SymbolService
 * Storage: TimescaleDB (when enabled) or Firestore
 */
@Service
public class MarketDataCollectionService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCollectionService.class);
    private static final String DATA_SOURCE = "ALPACA";

    private final AlpacaHistoricalClient historicalClient;
    private final AlpacaAssetsClient assetsClient;
    private final MarketDataRepository marketDataRepository;
    private final SymbolService symbolService;

    // Configuration
    private final int threadPoolSize;
    private final int batchSize;
    private final int backfillMonths;
    private final int backfillTimeoutMinutes;

    // Thread pool for concurrent execution
    private final ExecutorService executorService;

    // Job status tracking (volatile for thread-safe reads)
    private volatile JobStatus currentJobStatus = new JobStatus();

    @Autowired
    public MarketDataCollectionService(
            AlpacaHistoricalClient historicalClient,
            AlpacaAssetsClient assetsClient,
            MarketDataRepository marketDataRepository,
            SymbolService symbolService,
            @Value("${marketdata.batch.thread-pool-size:2}") int threadPoolSize,
            @Value("${marketdata.batch.batch-size:500}") int batchSize,
            @Value("${marketdata.batch.backfill-months:3}") int backfillMonths,
            @Value("${marketdata.batch.backfill-timeout-minutes:240}") int backfillTimeoutMinutes) {

        this.historicalClient = historicalClient;
        this.assetsClient = assetsClient;
        this.marketDataRepository = marketDataRepository;
        this.symbolService = symbolService;
        this.threadPoolSize = threadPoolSize;
        this.batchSize = batchSize;
        this.backfillMonths = backfillMonths;
        this.backfillTimeoutMinutes = backfillTimeoutMinutes;

        // Initialize thread pool
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        log.info("MarketDataCollectionService initialized with {} threads, batch size {}, timeout {} min",
                threadPoolSize, batchSize, backfillTimeoutMinutes);
    }

    /**
     * Get symbols to collect from Firestore SymbolService.
     * Returns provider-formatted symbols (e.g., "AAPL", "BTC/USD").
     */
    private List<String> getSymbolsToCollect() {
        List<String> symbols = symbolService.getProviderSymbolsForCollection(DATA_SOURCE);
        log.info("Loaded {} symbols from Firestore for {} collection", symbols.size(), DATA_SOURCE);
        return symbols;
    }

    /**
     * Backfill historical data for all symbols from Firestore.
     * Uses multi-threading for parallel symbol processing.
     *
     * @param timeframe Bar interval ("1Min", "5Min", "15Min", "1Hour", "1Day", "1Week", "1Month")
     * @return Collection result with statistics
     */
    public CollectionResult backfillIntradayData(String timeframe) {
        List<String> symbols = getSymbolsToCollect();
        log.info("Starting market data backfill for {} symbols, timeframe: {}, lookback: {} months",
                symbols.size(), timeframe, backfillMonths);

        LocalDateTime endDate = LocalDateTime.now(java.time.ZoneOffset.UTC);
        LocalDateTime startDate = endDate.minusMonths(backfillMonths);

        return backfillIntradayData(symbols, startDate, endDate, timeframe);
    }

    /**
     * Backfill historical data with custom date range for all Firestore symbols.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param timeframe Bar interval
     * @return Collection result with statistics
     */
    public CollectionResult backfillIntradayData(LocalDateTime startDate, LocalDateTime endDate, String timeframe) {
        List<String> symbols = getSymbolsToCollect();
        return backfillIntradayData(symbols, startDate, endDate, timeframe);
    }

    /**
     * Backfill historical data for specific symbols and date range
     *
     * @param symbols List of symbols to fetch
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param timeframe Bar interval
     * @return Collection result with statistics
     */
    public CollectionResult backfillIntradayData(List<String> symbols, LocalDateTime startDate,
                                                  LocalDateTime endDate, String timeframe) {
        log.info("Backfilling {} symbols from {} to {} ({})",
                symbols.size(), startDate, endDate, timeframe);

        // Set job status to RUNNING
        currentJobStatus.setRunning(timeframe, symbols.size());

        try {
            // First, fetch asset metadata for all symbols to enrich market data
            Map<String, AlpacaAsset> assetMetadata = fetchAssetMetadata(symbols);

            // Atomic counters for thread-safe statistics
            AtomicInteger symbolsProcessed = new AtomicInteger(0);
            AtomicInteger dataPointsStored = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Create tasks for each symbol
            List<CompletableFuture<SymbolResult>> futures = symbols.stream()
                    .map(symbol -> CompletableFuture.supplyAsync(() -> {
                        try {
                            // Check for cancellation before processing
                            if (currentJobStatus.isCancelRequested()) {
                                log.info("Cancellation requested, skipping symbol: {}", symbol);
                                return new SymbolResult(symbol, 0, false);
                            }

                            // Update current symbol being processed
                            currentJobStatus.updateProgress(symbolsProcessed.get(), symbol);

                            SymbolResult result = processSymbol(symbol, startDate, endDate, timeframe, assetMetadata.get(symbol));

                            // Update progress after successful processing
                            if (result.success) {
                                symbolsProcessed.incrementAndGet();
                                dataPointsStored.addAndGet(result.barsStored);
                                currentJobStatus.updateProgress(symbolsProcessed.get(), null);
                            }

                            return result;
                        } catch (Exception e) {
                            log.error("Error processing symbol {}: {}", symbol, e.getMessage(), e);
                            errorCount.incrementAndGet();
                            return new SymbolResult(symbol, 0, false);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            try {
                allFutures.get(backfillTimeoutMinutes, TimeUnit.MINUTES);

                // Wait for all futures to complete (they already updated counters in the async tasks)
                for (CompletableFuture<SymbolResult> future : futures) {
                    future.get(); // Just wait, counters already updated
                }

                // Check if job was cancelled
                if (currentJobStatus.isCancelRequested()) {
                    currentJobStatus.setCancelled();
                    log.warn("Backfill cancelled: {} symbols processed, {} bars stored, {} errors",
                            symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());
                } else {
                    // Set status to COMPLETED
                    currentJobStatus.setCompleted();
                    log.info("Backfill completed: {} symbols, {} bars stored, {} errors",
                            symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());
                }

                return new CollectionResult(symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());

            } catch (TimeoutException e) {
                log.error("Backfill timed out after {} minutes", backfillTimeoutMinutes);
                currentJobStatus.setFailed("Backfill timed out after " + backfillTimeoutMinutes + " minutes");
                errorCount.incrementAndGet();
                return new CollectionResult(symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());
            } catch (Exception e) {
                log.error("Error waiting for backfill completion: {}", e.getMessage(), e);
                currentJobStatus.setFailed("Error: " + e.getMessage());
                errorCount.incrementAndGet();
                return new CollectionResult(symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());
            }

        } catch (Exception e) {
            log.error("Backfill failed during initialization: {}", e.getMessage(), e);
            currentJobStatus.setFailed("Initialization failed: " + e.getMessage());
            return new CollectionResult(0, 0, 1);
        }
    }

    /**
     * Start backfill asynchronously for multiple timeframes (non-blocking).
     * Returns immediately while the job runs in the background.
     *
     * Use getBackfillStatus() to check progress.
     *
     * @param timeframes List of timeframes to backfill
     * @param startDate Start date
     * @param endDate End date
     */
    @Async("consoleTaskExecutor")
    public void backfillIntradayDataAsync(List<String> timeframes, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("=== Starting async backfill for {} timeframes ===", timeframes.size());

        for (String timeframe : timeframes) {
            try {
                log.info("--- Backfilling timeframe: {} ---", timeframe);
                CollectionResult result = backfillIntradayData(startDate, endDate, timeframe);
                log.info("--- Timeframe {} completed: {} symbols, {} bars, {} errors ---",
                        timeframe, result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount);
            } catch (Exception e) {
                log.error("Failed to backfill timeframe {}: {}", timeframe, e.getMessage(), e);
            }
        }

        log.info("=== Async backfill completed for all timeframes ===");
    }

    /**
     * Process a single symbol - fetch bars and store in batches
     */
    private SymbolResult processSymbol(String symbol, LocalDateTime startDate, LocalDateTime endDate,
                                       String timeframe, AlpacaAsset assetMetadata) {
        log.info(">>> Processing symbol: {} from {} to {} (timeframe: {})", symbol, startDate, endDate, timeframe);

        try {
            // Fetch all bars for this symbol (handles pagination internally)
            log.info(">>> Fetching bars for {} via historicalClient...", symbol);
            List<AlpacaBar> bars = historicalClient.getBars(symbol, startDate, endDate, timeframe);
            log.info(">>> Got {} bars for {}", bars != null ? bars.size() : "null", symbol);

            if (bars == null || bars.isEmpty()) {
                log.warn("No data returned for symbol: {}", symbol);
                return new SymbolResult(symbol, 0, true);
            }

            // Convert to MarketDataEntity
            log.info(">>> Converting {} bars to entities for {}", bars.size(), symbol);
            List<MarketDataEntity> entities = bars.stream()
                    .map(bar -> convertBar(symbol, bar, timeframe, assetMetadata))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.info(">>> Converted to {} entities for {} (filtered out {})", entities.size(), symbol, bars.size() - entities.size());

            // Save in batches
            log.info(">>> Saving {} entities for {}...", entities.size(), symbol);
            int stored = saveBatch(entities);

            // Mark symbol as collected in Firestore
            try {
                String canonicalSymbol = symbolService.getCanonicalSymbol(symbol, DATA_SOURCE);
                symbolService.markCollected(canonicalSymbol, Instant.now());
            } catch (Exception e) {
                log.debug("Could not mark symbol {} as collected: {}", symbol, e.getMessage());
            }

            log.info(">>> Symbol {}: fetched {} bars, converted {}, stored {}", symbol, bars.size(), entities.size(), stored);
            return new SymbolResult(symbol, stored, true);

        } catch (Exception e) {
            log.error("Failed to process symbol {}: {}", symbol, e.getMessage());
            throw new StrategizException(MarketDataErrorDetails.SYMBOL_PROCESSING_FAILED, "business-marketdata", e, symbol);
        }
    }

    /**
     * Fetch asset metadata for enrichment
     */
    private Map<String, AlpacaAsset> fetchAssetMetadata(List<String> symbols) {
        log.info("Fetching asset metadata for {} symbols", symbols.size());

        Map<String, AlpacaAsset> metadata = new ConcurrentHashMap<>();

        try {
            // Get all tradable US equities
            List<AlpacaAsset> assets = assetsClient.getNyseNasdaqStocks();

            // Build lookup map
            for (AlpacaAsset asset : assets) {
                if (symbols.contains(asset.getSymbol())) {
                    metadata.put(asset.getSymbol(), asset);
                }
            }

            log.info("Retrieved metadata for {}/{} symbols", metadata.size(), symbols.size());

        } catch (Exception e) {
            log.warn("Failed to fetch asset metadata, continuing without enrichment: {}", e.getMessage());
        }

        return metadata;
    }

    /**
     * Convert bar data to MarketDataEntity with comprehensive field mapping
     */
    private MarketDataEntity convertBar(String symbol, AlpacaBar bar, String timeframe,
                                               AlpacaAsset assetMetadata) {
        try {
            MarketDataEntity entity = new MarketDataEntity();

            // Initialize audit fields (required by BaseEntity)
            entity._initAudit("SYSTEM_BATCH");

            // Parse timestamp (RFC-3339 format: "2024-11-23T14:30:00Z")
            Instant instant = Instant.parse(bar.getTimestamp());
            LocalDateTime timestamp = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

            // Set core identifiers and timestamp (triggers auto-ID generation)
            entity.setSymbol(symbol);
            entity.setTimeframe(timeframe);
            entity.setTimestampFromLocalDateTime(timestamp);

            // Set OHLCV data
            entity.setOpen(bar.getOpen());
            entity.setHigh(bar.getHigh());
            entity.setLow(bar.getLow());
            entity.setClose(bar.getClose());
            entity.setVolume(bar.getVolume() != null ? BigDecimal.valueOf(bar.getVolume()) : null);
            entity.setVwap(bar.getVwap());
            entity.setTrades(bar.getTrades());

            // Set data source information
            entity.setDataSource("ALPACA");
            entity.setDataQuality("HISTORICAL");

            // Store additional metadata in metadata field
            entity.getMetadata().put("dataFeed", "IEX");
            entity.getMetadata().put("schemaVersion", "1.0");

            // Enrich with asset metadata if available
            if (assetMetadata != null) {
                entity.setAssetType(assetMetadata.getAssetClass());
                entity.setExchange(assetMetadata.getExchange());
                entity.getMetadata().put("assetName", assetMetadata.getName());
                entity.getMetadata().put("status", assetMetadata.getStatus());
                entity.getMetadata().put("tradable", assetMetadata.getTradable());
                entity.getMetadata().put("marginable", assetMetadata.getMarginable());
                entity.getMetadata().put("shortable", assetMetadata.getShortable());
                entity.getMetadata().put("easyToBorrow", assetMetadata.getEasyToBorrow());
                entity.getMetadata().put("fractionable", assetMetadata.getFractionable());
                entity.getMetadata().put("minOrderSize", assetMetadata.getMinOrderSizeDecimal());
                entity.getMetadata().put("priceIncrement", assetMetadata.getPriceIncrementDecimal());
            } else {
                entity.setAssetType("us_equity");
                entity.setExchange("UNKNOWN");
                entity.getMetadata().put("status", "active");
            }

            return entity;

        } catch (Exception e) {
            log.error("Failed to convert bar for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Save entities in batches to storage
     * Batch size configured via application.properties (default 500)
     */
    private int saveBatch(List<MarketDataEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        int totalSaved = 0;

        // Process in batches
        for (int i = 0; i < entities.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entities.size());
            List<MarketDataEntity> batch = entities.subList(i, endIndex);

            try {
                marketDataRepository.saveAll(batch);
                totalSaved += batch.size();

                log.debug("Saved batch of {} entities ({}/{})",
                        batch.size(), totalSaved, entities.size());

            } catch (Exception e) {
                log.error("Failed to save batch at index {}: {}", i, e.getMessage());

                // Try saving individually as fallback
                for (MarketDataEntity entity : batch) {
                    try {
                        marketDataRepository.save(entity);
                        totalSaved++;
                    } catch (Exception ex) {
                        log.error("Failed to save entity {}: {}", entity.getId(), ex.getMessage());
                    }
                }
            }
        }

        return totalSaved;
    }

    /**
     * Cancel the currently running backfill job.
     * The job will stop processing new symbols but will finish the current symbol.
     *
     * @return true if a job was running and cancellation was requested, false if no job was running
     */
    public boolean cancelCurrentJob() {
        if (currentJobStatus.getStatus() == JobStatus.Status.RUNNING) {
            log.warn("Cancellation requested for running backfill job");
            currentJobStatus.requestCancel();
            return true;
        }
        log.info("No running job to cancel (current status: {})", currentJobStatus.getStatus());
        return false;
    }

    /**
     * Get current configuration values
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getBackfillTimeoutMinutes() {
        return backfillTimeoutMinutes;
    }

    /**
     * Shutdown thread pool gracefully
     */
    public void shutdown() {
        log.info("Shutting down MarketDataCollectionService thread pool");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Result for single symbol processing
     */
    private static class SymbolResult {
        final String symbol;
        final int barsStored;
        final boolean success;

        SymbolResult(String symbol, int barsStored, boolean success) {
            this.symbol = symbol;
            this.barsStored = barsStored;
            this.success = success;
        }
    }

    /**
     * Result summary for batch collection
     */
    public static class CollectionResult {
        public final int totalSymbolsProcessed;
        public final int totalDataPointsStored;
        public final int errorCount;

        public CollectionResult(int symbolsProcessed, int dataPointsStored, int errorCount) {
            this.totalSymbolsProcessed = symbolsProcessed;
            this.totalDataPointsStored = dataPointsStored;
            this.errorCount = errorCount;
        }

        @Override
        public String toString() {
            return String.format("CollectionResult[symbols=%d, dataPoints=%d, errors=%d]",
                    totalSymbolsProcessed, totalDataPointsStored, errorCount);
        }
    }

    /**
     * Current job status for real-time tracking
     */
    public static class JobStatus {
        public enum Status { IDLE, RUNNING, COMPLETED, FAILED, CANCELLED }

        private volatile Status status = Status.IDLE;
        private volatile String timeframe;
        private volatile int symbolsProcessed = 0;
        private volatile int totalSymbols = 0;
        private volatile LocalDateTime startTime;
        private volatile String currentSymbol;
        private volatile String errorMessage;
        private volatile boolean cancelRequested = false;

        public synchronized void setRunning(String timeframe, int totalSymbols) {
            this.status = Status.RUNNING;
            this.timeframe = timeframe;
            this.totalSymbols = totalSymbols;
            this.symbolsProcessed = 0;
            this.startTime = LocalDateTime.now(java.time.ZoneOffset.UTC);
            this.currentSymbol = null;
            this.errorMessage = null;
            this.cancelRequested = false;
        }

        public synchronized void updateProgress(int symbolsProcessed, String currentSymbol) {
            this.symbolsProcessed = symbolsProcessed;
            this.currentSymbol = currentSymbol;
        }

        public synchronized void setCompleted() {
            this.status = Status.COMPLETED;
            this.currentSymbol = null;
        }

        public synchronized void setFailed(String errorMessage) {
            this.status = Status.FAILED;
            this.errorMessage = errorMessage;
        }

        public synchronized void setIdle() {
            this.status = Status.IDLE;
            this.timeframe = null;
            this.symbolsProcessed = 0;
            this.totalSymbols = 0;
            this.startTime = null;
            this.currentSymbol = null;
            this.errorMessage = null;
            this.cancelRequested = false;
        }

        public synchronized void setCancelled() {
            this.status = Status.CANCELLED;
            this.errorMessage = "Job cancelled by admin request";
        }

        public synchronized void requestCancel() {
            this.cancelRequested = true;
        }

        public synchronized boolean isCancelRequested() {
            return this.cancelRequested;
        }

        // Getters
        public Status getStatus() { return status; }
        public String getTimeframe() { return timeframe; }
        public int getSymbolsProcessed() { return symbolsProcessed; }
        public int getTotalSymbols() { return totalSymbols; }
        public LocalDateTime getStartTime() { return startTime; }
        public String getCurrentSymbol() { return currentSymbol; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Get current job status for status endpoint
     */
    public JobStatus getCurrentJobStatus() {
        return currentJobStatus;
    }
}
