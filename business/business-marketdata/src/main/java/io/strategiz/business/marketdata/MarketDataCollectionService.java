package io.strategiz.business.marketdata;

import io.strategiz.client.yahoofinance.client.YahooFinanceHistoricalClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.symbol.entity.SymbolEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for collecting and managing market data from Yahoo Finance
 * Implements smart incremental collection - checks for latest date and only fetches missing days
 */
@Service
public class MarketDataCollectionService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCollectionService.class);
    private static final String DATA_SOURCE = "YAHOO";

    private final YahooFinanceHistoricalClient yahooFinanceClient;
    private final MarketDataRepository marketDataRepository;
    private final SymbolService symbolService;

    // Configuration
    private final int maxSymbolsPerRun;
    private final int backfillYears;
    private final int delayMs;

    // Fallback symbols (used only if Firestore symbols collection is empty)
    private final List<String> fallbackSymbols;

    @Autowired
    public MarketDataCollectionService(YahooFinanceHistoricalClient yahooFinanceClient,
                                     MarketDataRepository marketDataRepository,
                                     SymbolService symbolService,
                                     @Value("${yahoo.batch.symbols.max:60}") int maxSymbolsPerRun,
                                     @Value("${yahoo.batch.backfill-years:7}") int backfillYears,
                                     @Value("${yahoo.batch.delay-ms:100}") int delayMs) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.marketDataRepository = marketDataRepository;
        this.symbolService = symbolService;
        this.maxSymbolsPerRun = maxSymbolsPerRun;
        this.backfillYears = backfillYears;
        this.delayMs = delayMs;

        // Fallback symbols - only used if Firestore symbols collection is empty
        // TODO: Remove this fallback once symbols are seeded in Firestore
        this.fallbackSymbols = Arrays.asList(
            // Major Tech (FAANG+)
            "AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "META", "NVDA", "TSLA",
            // Semiconductors & Hardware
            "AMD", "INTC", "QCOM", "AVGO", "TXN", "MU", "AMAT", "LRCX", "KLAC", "MRVL",
            // Software & Cloud
            "ADBE", "CRM", "NOW", "INTU", "WDAY", "SNOW", "PANW", "CRWD", "ZS", "DDOG",
            // Enterprise Tech
            "ORCL", "IBM", "CSCO", "HPQ", "DELL", "VMW", "SPLK",
            // Finance & Banks
            "JPM", "BAC", "WFC", "C", "GS", "MS", "SCHW", "BLK", "AXP", "V", "MA", "PYPL",
            // Consumer Discretionary
            "AMZN", "HD", "LOW", "TGT", "COST", "WMT", "SBUX", "MCD", "NKE", "LULU",
            // Consumer Staples
            "PG", "KO", "PEP", "MDLZ", "CL", "KMB", "GIS", "K",
            // Healthcare & Biotech
            "JNJ", "UNH", "PFE", "ABBV", "TMO", "ABT", "DHR", "MRK", "LLY", "AMGN", "GILD", "REGN",
            // Energy
            "XOM", "CVX", "COP", "SLB", "EOG", "PXD", "MPC", "PSX",
            // Industrials
            "BA", "CAT", "GE", "HON", "UPS", "RTX", "LMT", "DE",
            // Telecom & Media
            "T", "VZ", "TMUS", "DIS", "NFLX", "CMCSA", "CHTR",
            // Retail & E-commerce
            "AMZN", "SHOP", "ETSY", "W", "EBAY", "BABA",
            // Electric Vehicles & Clean Energy
            "TSLA", "RIVN", "LCID", "NIO", "F", "GM", "ENPH", "SEDG",
            // Real Estate
            "AMT", "PLD", "CCI", "EQIX", "SPG", "O",
            // Major ETFs (S&P 500)
            "SPY", "VOO", "IVV",
            // Major ETFs (Nasdaq/Tech)
            "QQQ", "VGT", "XLK",
            // Major ETFs (Small/Mid Cap)
            "IWM", "IJH", "MDY",
            // Major ETFs (Dow)
            "DIA",
            // Major ETFs (Total Market)
            "VTI", "ITOT",
            // Sector ETFs
            "XLF", "XLE", "XLV", "XLI", "XLP", "XLY", "XLU", "XLRE",
            // Bond ETFs
            "AGG", "BND", "TLT", "IEF", "LQD",
            // International ETFs
            "EFA", "VEA", "IEMG", "VWO", "EEM",
            // Commodity ETFs
            "GLD", "SLV", "USO", "UNG",
            // Top 20 Crypto (Yahoo Finance format)
            "BTC-USD", "ETH-USD", "BNB-USD", "XRP-USD", "ADA-USD",
            "SOL-USD", "DOGE-USD", "DOT-USD", "MATIC-USD", "AVAX-USD",
            "SHIB-USD", "LTC-USD", "UNI-USD", "LINK-USD", "ATOM-USD",
            "XLM-USD", "ALGO-USD", "VET-USD", "ICP-USD", "FIL-USD"
        );

        log.info("MarketDataCollectionService initialized with {} years backfill", backfillYears);
    }

    /**
     * Get symbols to collect from Firestore, falling back to hardcoded list if empty
     */
    private List<String> getSymbolsToCollect() {
        // Try to get symbols from Firestore via SymbolService
        List<String> firestoreSymbols = symbolService.getProviderSymbolsForCollection(DATA_SOURCE);

        if (!firestoreSymbols.isEmpty()) {
            log.info("Using {} symbols from Firestore for {} collection", firestoreSymbols.size(), DATA_SOURCE);
            return firestoreSymbols;
        }

        // Fallback to hardcoded symbols if Firestore is empty
        log.warn("No symbols found in Firestore for {}, using {} fallback symbols",
                DATA_SOURCE, fallbackSymbols.size());
        return fallbackSymbols;
    }

    /**
     * Main collection method - implements smart incremental logic
     * Checks Firestore for latest date per symbol and only fetches missing days
     *
     * @return CollectionResult with summary statistics
     */
    public CollectionResult collectDailyData() {
        List<String> allSymbols = getSymbolsToCollect();
        log.info("Starting daily market data collection for {} symbols", allSymbols.size());

        int symbolsProcessed = 0;
        int dataPointsStored = 0;
        int errorCount = 0;

        // Determine which symbols to process (limit to maxSymbolsPerRun)
        List<String> symbolsToProcess = allSymbols.stream()
                .limit(maxSymbolsPerRun)
                .collect(Collectors.toList());

        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (String symbol : symbolsToProcess) {
            try {
                log.debug("Processing symbol: {}", symbol);

                // Check Firestore for latest data point for this symbol
                Optional<MarketDataEntity> latestData = marketDataRepository.findLatestBySymbol(symbol);

                LocalDate startDate;
                if (latestData.isPresent()) {
                    // Incremental: fetch from day after last stored date
                    LocalDate latestDate = latestData.get().getTimestampAsLocalDateTime().toLocalDate();
                    startDate = latestDate.plusDays(1);
                    log.debug("Symbol {} has data up to {}, fetching from {}",
                            symbol, latestDate, startDate);
                } else {
                    // Backfill: fetch configured years for new symbol
                    startDate = LocalDate.now().minusYears(backfillYears);
                    log.info("Symbol {} has no data, backfilling {} years from {}",
                            symbol, backfillYears, startDate);
                }

                // Only fetch if we need data (startDate is not after yesterday)
                if (!startDate.isAfter(yesterday)) {
                    // Fetch historical data from Yahoo Finance
                    List<YahooFinanceHistoricalClient.HistoricalDataPoint> data =
                            yahooFinanceClient.getHistoricalData(symbol, startDate, yesterday, "1d");

                    // Store in Firestore
                    int stored = storeHistoricalData(symbol, data);
                    dataPointsStored += stored;

                    log.info("Symbol {}: fetched and stored {} data points", symbol, stored);

                    // Mark symbol as collected (update lastCollectedAt in Firestore)
                    try {
                        String canonicalSymbol = symbolService.getCanonicalSymbol(symbol, DATA_SOURCE);
                        symbolService.markCollected(canonicalSymbol, Instant.now());
                    } catch (Exception e) {
                        log.debug("Could not mark symbol {} as collected: {}", symbol, e.getMessage());
                    }
                } else {
                    log.debug("Symbol {} is up to date, no new data to fetch", symbol);
                }

                symbolsProcessed++;

                // Rate limiting - delay between requests
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }

            } catch (Exception e) {
                log.error("Error processing symbol {}: {}", symbol, e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("Daily collection completed: {} symbols processed, {} data points stored, {} errors",
                symbolsProcessed, dataPointsStored, errorCount);

        return new CollectionResult(symbolsProcessed, dataPointsStored, errorCount);
    }

    /**
     * Store historical data points in Firestore
     *
     * @param symbol Stock/crypto symbol
     * @param dataPoints Historical data from Yahoo Finance
     * @return Number of data points stored
     */
    private int storeHistoricalData(String symbol, List<YahooFinanceHistoricalClient.HistoricalDataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return 0;
        }

        int stored = 0;
        for (YahooFinanceHistoricalClient.HistoricalDataPoint point : dataPoints) {
            try {
                // Convert to MarketDataEntity
                MarketDataEntity entity = convertYahooDataPoint(symbol, point);

                // Save to Firestore (will overwrite if exists)
                marketDataRepository.save(entity);
                stored++;

            } catch (Exception e) {
                log.warn("Failed to store data point for {} on {}: {}",
                        symbol, point.date, e.getMessage());
            }
        }

        return stored;
    }

    /**
     * Convert Yahoo Finance data point to MarketDataEntity
     */
    private MarketDataEntity convertYahooDataPoint(String symbol,
                                                   YahooFinanceHistoricalClient.HistoricalDataPoint point) {
        MarketDataEntity entity = new MarketDataEntity();

        // Set symbol and metadata first
        entity.setSymbol(symbol);
        entity.setTimeframe("1Day");  // Use standardized timeframe format

        // Convert LocalDate to LocalDateTime (start of day in UTC) and set timestamp
        // This will also auto-generate the ID
        LocalDateTime timestampDateTime = point.date.atStartOfDay();
        entity.setTimestampFromLocalDateTime(timestampDateTime);

        entity.setDataSource("YAHOO");

        // Set OHLCV data
        entity.setOpen(point.open);
        entity.setHigh(point.high);
        entity.setLow(point.low);
        entity.setClose(point.close);
        entity.setVolume(point.volume);

        return entity;
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
    }
}
