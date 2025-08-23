package io.strategiz.service.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Aggregates data from multiple free sources to maximize coverage
 * 
 * FREE DATA SOURCES:
 * 1. Polygon.io - 100 calls/day (best for US stocks)
 * 2. Yahoo Finance - Unlimited (via yfinance, good for everything)
 * 3. Alpha Vantage - 500 calls/day on free tier (good for forex/crypto)
 * 4. CoinGecko - 10-50 calls/minute free (crypto only)
 * 5. Twelve Data - 800 calls/day free
 * 6. IEX Cloud - 50,000 calls/month free
 * 7. Finnhub - 60 calls/minute free
 * 
 * Strategy: Use each source for what it's best at
 */
@Service
public class DataSourceAggregator {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceAggregator.class);
    
    /**
     * Priority order for different asset types
     */
    public enum AssetType {
        STOCK("US Equities"),
        CRYPTO("Cryptocurrency"),
        FOREX("Foreign Exchange"),
        ETF("Exchange Traded Funds");
        
        private final String description;
        AssetType(String description) {
            this.description = description;
        }
    }
    
    /**
     * Optimal data source by asset type and use case
     */
    public DataSourceStrategy getOptimalStrategy(String symbol, AssetType assetType) {
        return switch (assetType) {
            case STOCK -> new DataSourceStrategy(
                List.of(
                    "YAHOO",     // Primary: Unlimited calls
                    "POLYGON",   // Secondary: High quality but limited
                    "ALPHAVANTAGE", // Tertiary: Good backup
                    "TWELVEDATA"    // Quaternary: Additional backup
                ),
                "Yahoo Finance is unlimited and reliable for stocks"
            );
            
            case CRYPTO -> new DataSourceStrategy(
                List.of(
                    "COINGECKO", // Primary: Best for crypto, generous limits
                    "YAHOO",     // Secondary: Has major cryptos
                    "POLYGON",   // Tertiary: Has some crypto pairs
                    "ALPHAVANTAGE" // Quaternary: Crypto support
                ),
                "CoinGecko specializes in crypto with good free tier"
            );
            
            case FOREX -> new DataSourceStrategy(
                List.of(
                    "ALPHAVANTAGE", // Primary: Good forex support
                    "POLYGON",      // Secondary: Has forex data
                    "TWELVEDATA",   // Tertiary: Forex support
                    "YAHOO"         // Quaternary: Major pairs only
                ),
                "Alpha Vantage has dedicated forex endpoints"
            );
            
            case ETF -> new DataSourceStrategy(
                List.of(
                    "YAHOO",     // Primary: Excellent ETF coverage
                    "POLYGON",   // Secondary: Good ETF data
                    "IEX",       // Tertiary: ETF support
                    "ALPHAVANTAGE" // Quaternary: Basic ETF data
                ),
                "Yahoo Finance has comprehensive ETF data"
            );
        };
    }
    
    /**
     * Calculate how many symbols we can fetch per day across all sources
     */
    public DailyCapacity calculateDailyCapacity() {
        return new DailyCapacity(
            100,    // Polygon.io
            10000,  // Yahoo Finance (effectively unlimited)
            500,    // Alpha Vantage
            800,    // Twelve Data
            1600,   // IEX Cloud (50K/month ÷ 30)
            86400,  // Finnhub (60/min × 1440 min)
            14400   // CoinGecko (10/min × 1440 min, conservative)
        );
    }
    
    /**
     * Smart batching strategy for twice-daily runs
     */
    public BatchAllocation allocateForTwiceDailyRuns(List<String> prioritySymbols, 
                                                      Map<String, AssetType> symbolTypes) {
        BatchAllocation allocation = new BatchAllocation();
        
        // Morning run (2 AM) - Focus on previous day's close data
        allocation.morningRun = Map.of(
            "POLYGON", prioritySymbols.stream().limit(45).toList(),
            "YAHOO", prioritySymbols.stream().skip(45).limit(200).toList(),
            "ALPHAVANTAGE", filterByType(symbolTypes, AssetType.FOREX).stream().limit(250).toList()
        );
        
        // Afternoon run (2 PM) - Focus on intraday updates
        allocation.afternoonRun = Map.of(
            "POLYGON", prioritySymbols.stream().skip(245).limit(45).toList(),
            "YAHOO", prioritySymbols.stream().skip(290).limit(200).toList(),
            "COINGECKO", filterByType(symbolTypes, AssetType.CRYPTO).stream().limit(100).toList()
        );
        
        return allocation;
    }
    
    private List<String> filterByType(Map<String, AssetType> symbolTypes, AssetType type) {
        return symbolTypes.entrySet().stream()
            .filter(e -> e.getValue() == type)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Data source strategy
     */
    public static class DataSourceStrategy {
        private final List<String> priorityOrder;
        private final String reasoning;
        
        public DataSourceStrategy(List<String> priorityOrder, String reasoning) {
            this.priorityOrder = priorityOrder;
            this.reasoning = reasoning;
        }
        
        public List<String> getPriorityOrder() { return priorityOrder; }
        public String getReasoning() { return reasoning; }
    }
    
    /**
     * Daily capacity across all sources
     */
    public static class DailyCapacity {
        private final int polygon;
        private final int yahoo;
        private final int alphaVantage;
        private final int twelveData;
        private final int iexCloud;
        private final int finnhub;
        private final int coinGecko;
        
        public DailyCapacity(int polygon, int yahoo, int alphaVantage, int twelveData,
                           int iexCloud, int finnhub, int coinGecko) {
            this.polygon = polygon;
            this.yahoo = yahoo;
            this.alphaVantage = alphaVantage;
            this.twelveData = twelveData;
            this.iexCloud = iexCloud;
            this.finnhub = finnhub;
            this.coinGecko = coinGecko;
        }
        
        public int getTotalDailyCapacity() {
            return polygon + yahoo + alphaVantage + twelveData + iexCloud + finnhub + coinGecko;
        }
        
        public String getSummary() {
            return String.format(
                "Total daily capacity: %,d symbols\n" +
                "- Polygon: %d\n" +
                "- Yahoo: %,d (unlimited)\n" +
                "- Alpha Vantage: %d\n" +
                "- Twelve Data: %d\n" +
                "- IEX Cloud: %,d\n" +
                "- Finnhub: %,d\n" +
                "- CoinGecko: %,d",
                getTotalDailyCapacity(), polygon, yahoo, alphaVantage, 
                twelveData, iexCloud, finnhub, coinGecko
            );
        }
    }
    
    /**
     * Batch allocation for twice-daily runs
     */
    public static class BatchAllocation {
        public Map<String, List<String>> morningRun;
        public Map<String, List<String>> afternoonRun;
        
        public int getTotalSymbols() {
            return morningRun.values().stream().mapToInt(List::size).sum() +
                   afternoonRun.values().stream().mapToInt(List::size).sum();
        }
    }
}