package io.strategiz.business.marketdata;

import io.strategiz.client.yahoofinance.client.YahooFinanceHistoricalClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private final YahooFinanceHistoricalClient yahooFinanceClient;
    private final MarketDataRepository marketDataRepository;

    // Configuration
    private final int maxSymbolsPerRun;
    private final int backfillYears;
    private final int delayMs;
    private final List<String> defaultSymbols;

    public MarketDataCollectionService(YahooFinanceHistoricalClient yahooFinanceClient,
                                     MarketDataRepository marketDataRepository,
                                     @Value("${yahoo.batch.symbols.max:60}") int maxSymbolsPerRun,
                                     @Value("${yahoo.batch.backfill-years:7}") int backfillYears,
                                     @Value("${yahoo.batch.delay-ms:100}") int delayMs) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.marketDataRepository = marketDataRepository;
        this.maxSymbolsPerRun = maxSymbolsPerRun;
        this.backfillYears = backfillYears;
        this.delayMs = delayMs;

        // Default symbols to collect - 160+ popular stocks, ETFs, and crypto
        this.defaultSymbols = Arrays.asList(
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

        log.info("MarketDataCollectionService initialized with {} default symbols, {} years backfill",
                defaultSymbols.size(), backfillYears);
    }

    /**
     * Main collection method - implements smart incremental logic
     * Checks Firestore for latest date per symbol and only fetches missing days
     *
     * @return CollectionResult with summary statistics
     */
    public CollectionResult collectDailyData() {
        log.info("Starting daily market data collection for {} symbols", defaultSymbols.size());

        int symbolsProcessed = 0;
        int dataPointsStored = 0;
        int errorCount = 0;

        // Determine which symbols to process (limit to maxSymbolsPerRun)
        List<String> symbolsToProcess = defaultSymbols.stream()
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
