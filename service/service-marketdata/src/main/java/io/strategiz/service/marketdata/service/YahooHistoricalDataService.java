package io.strategiz.service.marketdata.service;

import io.strategiz.client.yahoofinance.YahooFinanceHistoricalClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Yahoo Finance Historical Data Collection Service
 * 
 * UNLIMITED & FREE - Perfect for backtesting!
 * - Downloads years of historical OHLCV data in bulk
 * - No API key required
 * - No rate limits (just respectful delays)
 * - Supports stocks, ETFs, indices, forex, crypto
 */
@Service
public class YahooHistoricalDataService {
    
    private static final Logger log = LoggerFactory.getLogger(YahooHistoricalDataService.class);
    
    private final YahooFinanceHistoricalClient yahooClient;
    private final MarketDataRepository marketDataRepository;
    private final boolean yahooEnabled;
    private final int historicalYears;
    private final ExecutorService executor;
    
    // Popular symbols for backtesting (can be extended)
    private final List<String> DEFAULT_SYMBOLS = Arrays.asList(
        // Major US Stocks
        "AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "NVDA", "META", "TSLA", "BRK-B", "UNH",
        "JNJ", "V", "WMT", "JPM", "PG", "HD", "CVX", "MA", "ABBV", "PFE",
        "KO", "AVGO", "PEP", "TMO", "COST", "MRK", "ADBE", "NFLX", "CRM", "ACN",
        "TXN", "ORCL", "DHR", "VZ", "INTC", "CMCSA", "AMD", "QCOM", "T", "PM",
        "HON", "IBM", "COP", "GS", "CAT", "BA", "MCD", "MMM", "AXP", "DIS",
        
        // ETFs for diversification 
        "SPY", "QQQ", "IWM", "VTI", "VTV", "VUG", "VEA", "VWO", "BND", "AGG",
        "TLT", "GLD", "SLV", "USO", "UNG", "EEM", "FXI", "EWJ", "EWZ", "RSX",
        
        // Major Indices
        "^GSPC", "^DJI", "^IXIC", "^RUT", "^VIX",
        
        // Popular Crypto (on Yahoo)
        "BTC-USD", "ETH-USD", "BNB-USD", "XRP-USD", "ADA-USD", "SOL-USD", "DOGE-USD",
        
        // Major Forex pairs
        "EURUSD=X", "GBPUSD=X", "USDJPY=X", "AUDUSD=X", "USDCAD=X", "USDCHF=X"
    );
    
    public YahooHistoricalDataService(YahooFinanceHistoricalClient yahooClient,
                                    MarketDataRepository marketDataRepository,
                                    @Value("${yahoo.batch.enabled:true}") boolean yahooEnabled,
                                    @Value("${yahoo.batch.historical.years:5}") int historicalYears) {
        this.yahooClient = yahooClient;
        this.marketDataRepository = marketDataRepository;
        this.yahooEnabled = yahooEnabled;
        this.historicalYears = historicalYears;
        this.executor = Executors.newFixedThreadPool(4); // Parallel processing
        
        log.info("Yahoo Historical Data Service initialized: enabled={}, years={}, symbols={}", 
                yahooEnabled, historicalYears, DEFAULT_SYMBOLS.size());
    }
    
    /**
     * Collect historical data for all default symbols
     * This will download YEARS of data in one go!
     */
    public CollectionSummary collectAllHistoricalData() {
        return collectHistoricalData(DEFAULT_SYMBOLS, historicalYears);
    }
    
    /**
     * Collect historical data for specific symbols
     */
    public CollectionSummary collectHistoricalData(List<String> symbols, int years) {
        if (!yahooEnabled) {
            log.warn("Yahoo Finance collection is disabled");
            return new CollectionSummary(0, 0, 0, "DISABLED");
        }
        
        log.info("Starting historical data collection for {} symbols, {} years back", symbols.size(), years);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(years);
        
        CollectionSummary summary = new CollectionSummary(symbols.size(), 0, 0, "IN_PROGRESS");
        
        // Process symbols in parallel batches
        List<CompletableFuture<SymbolResult>> futures = symbols.stream()
            .map(symbol -> CompletableFuture.supplyAsync(() -> 
                processSymbol(symbol, startDate, endDate), executor))
            .toList();
        
        // Wait for all to complete and collect results
        for (CompletableFuture<SymbolResult> future : futures) {
            try {
                SymbolResult result = future.get();
                summary.totalDataPoints += result.dataPoints;
                summary.successfulSymbols += result.success ? 1 : 0;
                
                log.info("Processed {}: {} data points", result.symbol, result.dataPoints);
                
            } catch (Exception e) {
                log.error("Error processing symbol: {}", e.getMessage());
            }
        }
        
        summary.status = "COMPLETED";
        log.info("Historical collection completed: {} symbols, {} data points stored", 
                summary.successfulSymbols, summary.totalDataPoints);
        
        return summary;
    }
    
    /**
     * Process a single symbol (download + store)
     */
    private SymbolResult processSymbol(String symbol, LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Downloading historical data for {} from {} to {}", symbol, startDate, endDate);
            
            // Download bulk historical data from Yahoo Finance
            List<YahooFinanceHistoricalClient.HistoricalDataPoint> data = 
                yahooClient.getHistoricalData(symbol, startDate, endDate, "1d");
            
            if (data.isEmpty()) {
                log.warn("No data received for symbol: {}", symbol);
                return new SymbolResult(symbol, 0, false);
            }
            
            // Convert and store each data point
            int stored = 0;
            for (YahooFinanceHistoricalClient.HistoricalDataPoint point : data) {
                MarketDataEntity entity = convertToMarketDataEntity(point);
                if (entity != null && entity.isValid()) {
                    try {
                        marketDataRepository.save(entity);
                        stored++;
                    } catch (Exception e) {
                        log.error("Error storing data point for {}: {}", symbol, e.getMessage());
                    }
                }
            }
            
            log.info("Successfully stored {} data points for {}", stored, symbol);
            return new SymbolResult(symbol, stored, true);
            
        } catch (Exception e) {
            log.error("Error processing symbol {}: {}", symbol, e.getMessage());
            return new SymbolResult(symbol, 0, false);
        }
    }
    
    /**
     * Convert Yahoo Finance data point to our MarketDataEntity
     */
    private MarketDataEntity convertToMarketDataEntity(YahooFinanceHistoricalClient.HistoricalDataPoint point) {
        try {
            MarketDataEntity entity = new MarketDataEntity();
            
            // Basic info
            entity.setSymbol(point.symbol);
            entity.setDate(point.date);
            entity.setTimeframe("1D");
            
            // OHLCV data
            entity.setOpen(point.open);
            entity.setHigh(point.high);
            entity.setLow(point.low);
            entity.setClose(point.close);
            entity.setVolume(point.volume != null ? new BigDecimal(point.volume) : BigDecimal.ZERO);
            
            // Source info
            entity.setDataSource("YAHOO");
            entity.setDataQuality("HISTORICAL");
            entity.setCollectedAt(Instant.now());
            
            // Determine asset type from symbol
            entity.setAssetType(determineAssetType(point.symbol));
            entity.setExchange(determineExchange(point.symbol));
            
            // Auto-generate document ID
            entity.setId(MarketDataEntity.createId(point.symbol, point.date, "1D"));
            
            return entity;
            
        } catch (Exception e) {
            log.error("Error converting data point for {}: {}", point.symbol, e.getMessage());
            return null;
        }
    }
    
    /**
     * Determine asset type from symbol pattern
     */
    private String determineAssetType(String symbol) {
        if (symbol.contains("-USD") || symbol.contains("BTC") || symbol.contains("ETH")) {
            return "CRYPTO";
        } else if (symbol.contains("=X")) {
            return "FOREX";
        } else if (symbol.startsWith("^")) {
            return "INDEX";
        } else if (Arrays.asList("SPY", "QQQ", "IWM", "VTI", "BND", "GLD").contains(symbol)) {
            return "ETF";
        } else {
            return "STOCK";
        }
    }
    
    /**
     * Determine exchange from symbol
     */
    private String determineExchange(String symbol) {
        if (symbol.contains("-USD")) return "CRYPTO";
        if (symbol.contains("=X")) return "FOREX";
        if (symbol.startsWith("^")) return "INDEX";
        return "US"; // Default for US stocks
    }
    
    /**
     * Get current collection status
     */
    public CollectionStatus getStatus() {
        return new CollectionStatus(
            yahooEnabled,
            DEFAULT_SYMBOLS.size(),
            historicalYears,
            "Yahoo Finance provides unlimited historical data for backtesting"
        );
    }
    
    /**
     * Get available symbols
     */
    public List<String> getAvailableSymbols() {
        return new ArrayList<>(DEFAULT_SYMBOLS);
    }
    
    /**
     * Collection summary result
     */
    public static class CollectionSummary {
        public final int totalSymbols;
        public int successfulSymbols;
        public int totalDataPoints;
        public String status;
        
        public CollectionSummary(int totalSymbols, int successfulSymbols, int totalDataPoints, String status) {
            this.totalSymbols = totalSymbols;
            this.successfulSymbols = successfulSymbols;
            this.totalDataPoints = totalDataPoints;
            this.status = status;
        }
        
        public String getSummary() {
            return String.format("Collected %d data points for %d/%d symbols (%s)", 
                    totalDataPoints, successfulSymbols, totalSymbols, status);
        }
    }
    
    /**
     * Result for a single symbol
     */
    private static class SymbolResult {
        final String symbol;
        final int dataPoints;
        final boolean success;
        
        SymbolResult(String symbol, int dataPoints, boolean success) {
            this.symbol = symbol;
            this.dataPoints = dataPoints;
            this.success = success;
        }
    }
    
    /**
     * Collection status
     */
    public static class CollectionStatus {
        public final boolean enabled;
        public final int symbolCount;
        public final int historicalYears;
        public final String description;
        
        public CollectionStatus(boolean enabled, int symbolCount, int historicalYears, String description) {
            this.enabled = enabled;
            this.symbolCount = symbolCount;
            this.historicalYears = historicalYears;
            this.description = description;
        }
        
        public String getSummary() {
            return String.format("Yahoo Finance: %s, %d symbols, %d years, %s", 
                    enabled ? "enabled" : "disabled", symbolCount, historicalYears, description);
        }
    }
}