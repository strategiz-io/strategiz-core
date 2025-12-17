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
import org.springframework.beans.factory.annotation.Value;
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
 * Service for collecting and managing market data from Alpaca API
 *
 * Features:
 * - Multi-threaded backfill with configurable concurrency (default 10 threads)
 * - Batch Firestore saves (500 entities per batch)
 * - Comprehensive field mapping from Alpaca to MarketDataEntity
 * - Symbol metadata enrichment from Assets API
 * - Intraday support with 1-minute bars
 * - Automatic pagination handling
 * - Rate limiting and error recovery
 *
 * Target: ~500 symbols (S&P 500 + top crypto/ETFs)
 * Storage: ~3GB for 3 months of 1Min data
 */
@Service
public class AlpacaCollectionService {

    private static final Logger log = LoggerFactory.getLogger(AlpacaCollectionService.class);
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

    @Autowired
    public AlpacaCollectionService(
            AlpacaHistoricalClient historicalClient,
            AlpacaAssetsClient assetsClient,
            MarketDataRepository marketDataRepository,
            SymbolService symbolService,
            @Value("${alpaca.batch.thread-pool-size:2}") int threadPoolSize,
            @Value("${alpaca.batch.batch-size:500}") int batchSize,
            @Value("${alpaca.batch.backfill-months:3}") int backfillMonths,
            @Value("${alpaca.batch.backfill-timeout-minutes:240}") int backfillTimeoutMinutes) {

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

        log.info("AlpacaCollectionService initialized with {} threads, batch size {}, timeout {} min",
                threadPoolSize, batchSize, backfillTimeoutMinutes);
    }

    /**
     * Get symbols to collect from Firestore SymbolService.
     * Returns provider-formatted symbols for Alpaca (e.g., "AAPL", "BTC/USD").
     */
    private List<String> getSymbolsToCollect() {
        List<String> symbols = symbolService.getProviderSymbolsForCollection(DATA_SOURCE);
        log.info("Loaded {} symbols from Firestore for {} collection", symbols.size(), DATA_SOURCE);
        return symbols;
    }

    /**
     * Backfill historical intraday data for all symbols from Firestore.
     * Uses multi-threading for parallel symbol processing.
     *
     * @param timeframe Bar interval ("1Min", "5Min", "15Min", "1Hour", "1Day")
     * @return Collection result with statistics
     */
    public CollectionResult backfillIntradayData(String timeframe) {
        List<String> symbols = getSymbolsToCollect();
        log.info("Starting Alpaca backfill for {} symbols, timeframe: {}, lookback: {} months",
                symbols.size(), timeframe, backfillMonths);

        LocalDateTime endDate = LocalDateTime.now();
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
                        return processSymbol(symbol, startDate, endDate, timeframe, assetMetadata.get(symbol));
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
            allFutures.get(backfillTimeoutMinutes, TimeUnit.MINUTES); // Configurable timeout (default 240 min / 4 hours)

            // Aggregate results
            for (CompletableFuture<SymbolResult> future : futures) {
                SymbolResult result = future.get();
                if (result.success) {
                    symbolsProcessed.incrementAndGet();
                    dataPointsStored.addAndGet(result.barsStored);
                }
            }

        } catch (TimeoutException e) {
            log.error("Backfill timed out after {} minutes", backfillTimeoutMinutes);
            errorCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Error waiting for backfill completion: {}", e.getMessage(), e);
            errorCount.incrementAndGet();
        }

        log.info("Backfill completed: {} symbols, {} bars stored, {} errors",
                symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());

        return new CollectionResult(symbolsProcessed.get(), dataPointsStored.get(), errorCount.get());
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
                    .map(bar -> convertAlpacaBar(symbol, bar, timeframe, assetMetadata))
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
            // Get all tradable US equities from Alpaca
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
     * Convert AlpacaBar to MarketDataEntity with comprehensive field mapping
     */
    private MarketDataEntity convertAlpacaBar(String symbol, AlpacaBar bar, String timeframe,
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
            entity.setTimestampFromLocalDateTime(timestamp);  // Converts LocalDateTime to Timestamp and auto-generates ID

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
            entity.getMetadata().put("dataFeed", "IEX");  // Free tier uses IEX feed
            entity.getMetadata().put("schemaVersion", "1.0");

            // Enrich with asset metadata if available
            if (assetMetadata != null) {
                entity.setAssetType(assetMetadata.getAssetClass());
                entity.setExchange(assetMetadata.getExchange());
                entity.getMetadata().put("assetName", assetMetadata.getName());
                entity.getMetadata().put("status", assetMetadata.getStatus());

                // Trading capabilities - store in metadata
                entity.getMetadata().put("tradable", assetMetadata.getTradable());
                entity.getMetadata().put("marginable", assetMetadata.getMarginable());
                entity.getMetadata().put("shortable", assetMetadata.getShortable());
                entity.getMetadata().put("easyToBorrow", assetMetadata.getEasyToBorrow());
                entity.getMetadata().put("fractionable", assetMetadata.getFractionable());

                // Trading constraints - store in metadata
                entity.getMetadata().put("minOrderSize", assetMetadata.getMinOrderSizeDecimal());
                entity.getMetadata().put("priceIncrement", assetMetadata.getPriceIncrementDecimal());
            } else {
                // Default values when metadata unavailable
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
     * Save entities in batches to Firestore
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
                // Save batch using repository
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
     * Shutdown thread pool gracefully
     */
    public void shutdown() {
        log.info("Shutting down AlpacaCollectionService thread pool");
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
}
