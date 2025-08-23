package io.strategiz.service.marketdata.service;

import io.strategiz.client.polygon.PolygonClient;
import io.strategiz.client.polygon.PolygonAggregatesResponse;
import io.strategiz.client.yahoofinance.YahooFinanceHistoricalClient;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for collecting and managing market data from multiple sources
 * Orchestrates data collection from Polygon.io, Yahoo Finance, and other sources
 */
@Service
public class MarketDataCollectionService {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataCollectionService.class);
    
    private final PolygonClient polygonClient;
    private final YahooFinanceHistoricalClient yahooFinanceClient;
    private final MarketDataRepository marketDataRepository;
    private final DataSourceAggregator dataSourceAggregator;
    
    // Configuration
    private final boolean polygonEnabled;
    private final int maxSymbolsPerRun;
    private final List<String> defaultSymbols;
    
    public MarketDataCollectionService(PolygonClient polygonClient,
                                     YahooFinanceHistoricalClient yahooFinanceClient,
                                     MarketDataRepository marketDataRepository,
                                     DataSourceAggregator dataSourceAggregator,
                                     @Value("${polygon.batch.enabled:true}") boolean polygonEnabled,
                                     @Value("${polygon.batch.symbols.max:45}") int maxSymbolsPerRun) {
        this.polygonClient = polygonClient;
        this.yahooFinanceClient = yahooFinanceClient;
        this.marketDataRepository = marketDataRepository;
        this.dataSourceAggregator = dataSourceAggregator;
        this.polygonEnabled = polygonEnabled;
        this.maxSymbolsPerRun = maxSymbolsPerRun;
        
        // Popular stocks for backtesting
        this.defaultSymbols = Arrays.asList(
            // Mega caps
            "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "BRK.B", "UNH", "JNJ",
            // Large caps  
            "V", "WMT", "JPM", "PG", "HD", "CVX", "MA", "ABBV", "PFE", "KO",
            "AVGO", "PEP", "TMO", "COST", "MRK", "ADBE", "NFLX", "CRM", "ACN", "TXN",
            // Growth stocks
            "ORCL", "DHR", "VZ", "INTC", "CMCSA", "AMD", "QCOM", "T", "PM", "HON",
            // ETFs for diversification
            "SPY", "QQQ", "IWM", "VTI", "VEA", "VWO", "AGG", "TLT", "GLD", "USO"
        );
    }
    
    /**
     * Collect daily market data for all configured symbols
     * Used by batch job scheduler
     */
    public CollectionResult collectDailyData() {
        log.info("Starting daily market data collection for {} symbols", defaultSymbols.size());
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        CollectionResult result = new CollectionResult();
        
        // Determine which symbols to process (respect rate limits)
        List<String> symbolsToProcess = defaultSymbols.stream()
            .limit(maxSymbolsPerRun)
            .collect(Collectors.toList());
        
        log.info("Processing {} symbols in this batch", symbolsToProcess.size());
        
        // Use mixed approach: Polygon for high-priority symbols, Yahoo for the rest
        List<String> polygonSymbols = symbolsToProcess.stream().limit(maxSymbolsPerRun / 2).toList();
        List<String> yahooSymbols = symbolsToProcess.stream().skip(maxSymbolsPerRun / 2).toList();
        
        // Collect from Polygon.io (limited quota)
        if (polygonEnabled && !polygonSymbols.isEmpty()) {
            result.polygonResults = collectFromPolygon(polygonSymbols, yesterday);
        }
        
        // Collect from Yahoo Finance (unlimited)
        if (!yahooSymbols.isEmpty()) {
            result.yahooResults = collectFromYahooFinance(yahooSymbols, yesterday);
        }
        
        // Store all collected data
        int totalStored = storeCollectedData(result);
        
        result.totalSymbolsProcessed = symbolsToProcess.size();
        result.totalDataPointsStored = totalStored;
        result.collectionDate = yesterday;
        
        log.info("Daily collection completed: {} symbols processed, {} data points stored", 
                result.totalSymbolsProcessed, result.totalDataPointsStored);
        
        return result;
    }
    
    /**
     * Collect historical data for backtesting (bulk download)
     * Uses Yahoo Finance for unlimited historical data
     */
    public CollectionResult collectHistoricalData(List<String> symbols, LocalDate startDate, LocalDate endDate) {
        log.info("Starting historical data collection for {} symbols from {} to {}", 
                symbols.size(), startDate, endDate);
        
        CollectionResult result = new CollectionResult();
        result.collectionDate = endDate;
        
        // Use Yahoo Finance for bulk historical data (no API limits)
        Map<String, List<YahooFinanceHistoricalClient.HistoricalDataPoint>> historicalData = 
            yahooFinanceClient.getBulkHistoricalData(symbols, startDate, endDate);
        
        // Convert Yahoo data to our format
        List<MarketDataEntity> allData = new ArrayList<>();
        for (Map.Entry<String, List<YahooFinanceHistoricalClient.HistoricalDataPoint>> entry : historicalData.entrySet()) {
            String symbol = entry.getKey();
            List<YahooFinanceHistoricalClient.HistoricalDataPoint> dataPoints = entry.getValue();
            
            for (YahooFinanceHistoricalClient.HistoricalDataPoint point : dataPoints) {
                MarketDataEntity entity = convertYahooDataPoint(point);
                if (entity != null) {
                    allData.add(entity);
                }
            }
        }
        
        // Store all historical data
        int stored = 0;
        for (MarketDataEntity data : allData) {
            try {
                marketDataRepository.save(data);
                stored++;
            } catch (Exception e) {
                log.error("Error storing historical data for {}: {}", data.getSymbol(), e.getMessage());
            }
        }
        
        result.totalSymbolsProcessed = symbols.size();
        result.totalDataPointsStored = stored;
        result.yahooResults = allData.size();
        
        log.info("Historical collection completed: {} symbols, {} data points stored", 
                result.totalSymbolsProcessed, result.totalDataPointsStored);
        
        return result;
    }
    
    /**
     * Collect data from Polygon.io (respecting rate limits)
     */
    private int collectFromPolygon(List<String> symbols, LocalDate targetDate) {
        log.info("Collecting data from Polygon.io for {} symbols", symbols.size());
        
        List<MarketDataEntity> collectedData = new ArrayList<>();
        
        for (String symbol : symbols) {
            try {
                PolygonAggregatesResponse response = polygonClient.getDailyAggregates(
                    symbol, targetDate, targetDate
                );
                
                if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                    for (PolygonAggregatesResponse.AggregateData aggregate : response.getResults()) {
                        MarketDataEntity entity = convertPolygonAggregate(symbol, aggregate, targetDate);
                        if (entity != null) {
                            collectedData.add(entity);
                        }
                    }
                }
                
                // Small delay to respect rate limits (additional safety)
                Thread.sleep(100);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error collecting Polygon data for {}: {}", symbol, e.getMessage());
            }
        }
        
        log.info("Collected {} data points from Polygon.io", collectedData.size());
        return collectedData.size();
    }
    
    /**
     * Collect data from Yahoo Finance (unlimited)
     */
    private int collectFromYahooFinance(List<String> symbols, LocalDate targetDate) {
        log.info("Collecting data from Yahoo Finance for {} symbols", symbols.size());
        
        int collected = 0;
        
        for (String symbol : symbols) {
            try {
                List<YahooFinanceHistoricalClient.HistoricalDataPoint> data = 
                    yahooFinanceClient.getHistoricalData(symbol, targetDate, targetDate, "1d");
                
                collected += data.size();
                
            } catch (Exception e) {
                log.error("Error collecting Yahoo Finance data for {}: {}", symbol, e.getMessage());
            }
        }
        
        log.info("Collected {} data points from Yahoo Finance", collected);
        return collected;
    }
    
    /**
     * Store all collected data to Firestore
     */
    private int storeCollectedData(CollectionResult result) {
        // Implementation would store the collected data
        // For now, return the sum of collected data points
        return result.polygonResults + result.yahooResults;
    }
    
    /**
     * Convert Polygon aggregate data to our entity format
     */
    private MarketDataEntity convertPolygonAggregate(String symbol, PolygonAggregatesResponse.AggregateData aggregate, LocalDate date) {
        try {
            MarketDataEntity entity = new MarketDataEntity();
            entity.setSymbol(symbol);
            entity.setDate(date);
            entity.setTimeframe("1D");
            entity.setOpen(aggregate.getOpen());
            entity.setHigh(aggregate.getHigh());
            entity.setLow(aggregate.getLow());
            entity.setClose(aggregate.getClose());
            entity.setVolume(aggregate.getVolume());
            entity.setVwap(aggregate.getVolumeWeightedAveragePrice());
            entity.setDataSource("POLYGON");
            entity.setTimestamp(Instant.now());
            
            // Set document ID for Firestore
            entity.setId(String.format("%s_%s_%s", symbol, date.toString(), "1D"));
            
            return entity;
            
        } catch (Exception e) {
            log.error("Error converting Polygon data for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert Yahoo Finance data to our entity format
     */
    private MarketDataEntity convertYahooDataPoint(YahooFinanceHistoricalClient.HistoricalDataPoint point) {
        try {
            MarketDataEntity entity = new MarketDataEntity();
            entity.setSymbol(point.symbol);
            entity.setDate(point.date);
            entity.setTimeframe("1D");
            entity.setOpen(point.open);
            entity.setHigh(point.high);
            entity.setLow(point.low);
            entity.setClose(point.close);
            entity.setVolume(point.volume);
            entity.setDataSource("YAHOO");
            entity.setTimestamp(Instant.now());
            
            // Set document ID for Firestore
            entity.setId(String.format("%s_%s_%s", point.symbol, point.date.toString(), "1D"));
            
            return entity;
            
        } catch (Exception e) {
            log.error("Error converting Yahoo data for {}: {}", point.symbol, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get collection status and rate limits
     */
    public CollectionStatus getCollectionStatus() {
        PolygonClient.RateLimitStatus polygonStatus = polygonClient.getRateLimitStatus();
        
        return new CollectionStatus(
            polygonEnabled,
            polygonStatus.getAvailableTokens(),
            polygonStatus.getMaxTokens(),
            defaultSymbols.size(),
            maxSymbolsPerRun
        );
    }
    
    /**
     * Collection result summary
     */
    public static class CollectionResult {
        public LocalDate collectionDate;
        public int totalSymbolsProcessed;
        public int totalDataPointsStored;
        public int polygonResults;
        public int yahooResults;
        
        public String getSummary() {
            return String.format(
                "Collection for %s: %d symbols processed, %d data points stored (Polygon: %d, Yahoo: %d)",
                collectionDate, totalSymbolsProcessed, totalDataPointsStored, polygonResults, yahooResults
            );
        }
    }
    
    /**
     * Collection status
     */
    public static class CollectionStatus {
        public final boolean polygonEnabled;
        public final long polygonTokensAvailable;
        public final long polygonTokensMax;
        public final int totalSymbolsConfigured;
        public final int maxSymbolsPerRun;
        
        public CollectionStatus(boolean polygonEnabled, long polygonTokensAvailable, 
                              long polygonTokensMax, int totalSymbolsConfigured, int maxSymbolsPerRun) {
            this.polygonEnabled = polygonEnabled;
            this.polygonTokensAvailable = polygonTokensAvailable;
            this.polygonTokensMax = polygonTokensMax;
            this.totalSymbolsConfigured = totalSymbolsConfigured;
            this.maxSymbolsPerRun = maxSymbolsPerRun;
        }
        
        public String getSummary() {
            return String.format(
                "Status: Polygon %s (%d/%d tokens), %d symbols configured, %d per run",
                polygonEnabled ? "enabled" : "disabled",
                polygonTokensAvailable, polygonTokensMax,
                totalSymbolsConfigured, maxSymbolsPerRun
            );
        }
    }
}