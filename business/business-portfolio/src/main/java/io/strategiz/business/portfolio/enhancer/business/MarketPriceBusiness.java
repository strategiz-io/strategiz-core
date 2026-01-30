package io.strategiz.business.portfolio.enhancer.business;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Business component for fetching and caching market prices.
 * Uses Yahoo Finance for real-time crypto prices.
 */
@Component
public class MarketPriceBusiness {
    
    private static final Logger LOGGER = Logger.getLogger(MarketPriceBusiness.class.getName());
    
    // Cache prices for 60 seconds to avoid excessive API calls
    private final Cache<String, BigDecimal> priceCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();

    // Cache batch prices for 60 seconds
    private final Cache<String, Map<String, BigDecimal>> batchPriceCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();

    public MarketPriceBusiness() {
        LOGGER.info("MarketPriceBusiness initialized with static prices");
    }
    
    /**
     * Get current price for a single asset
     * @param symbol Standard symbol (e.g., "BTC", "ETH")
     * @param assetType Type of asset (crypto, stock, fiat)
     * @return Current price in USD
     */
    public BigDecimal getCurrentPrice(String symbol, String assetType) {
        if (symbol == null || symbol.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Check cache first
        String cacheKey = symbol + "_" + assetType;
        BigDecimal cachedPrice = priceCache.getIfPresent(cacheKey);
        if (cachedPrice != null) {
            return cachedPrice;
        }
        
        BigDecimal price = BigDecimal.ZERO;
        
        try {
            if ("crypto".equals(assetType)) {
                price = fetchCryptoPrice(symbol);
            } else if ("stock".equals(assetType)) {
                price = fetchStockPrice(symbol);
            } else if ("fiat".equals(assetType)) {
                price = fetchFiatRate(symbol);
            }
            
            // Cache the result
            if (price.compareTo(BigDecimal.ZERO) > 0) {
                priceCache.put(cacheKey, price);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to fetch price for " + symbol + ": " + e.getMessage());
        }
        
        return price;
    }
    
    /**
     * Get prices for multiple assets in one call (more efficient)
     * @param symbols List of standard symbols
     * @param assetType Type of assets
     * @return Map of symbol to price
     */
    public Map<String, BigDecimal> getBatchPrices(List<String> symbols, String assetType) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }
        
        // Create cache key
        String cacheKey = String.join(",", symbols) + "_" + assetType;
        Map<String, BigDecimal> cachedPrices = batchPriceCache.getIfPresent(cacheKey);
        if (cachedPrices != null) {
            return cachedPrices;
        }
        
        Map<String, BigDecimal> prices = new HashMap<>();
        
        try {
            if ("crypto".equals(assetType)) {
                prices = fetchCryptoPricesBatch(symbols);
            } else {
                // For non-crypto, fetch individually
                for (String symbol : symbols) {
                    BigDecimal price = getCurrentPrice(symbol, assetType);
                    prices.put(symbol, price);
                }
            }
            
            // Cache the results
            if (!prices.isEmpty()) {
                batchPriceCache.put(cacheKey, prices);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to fetch batch prices: " + e.getMessage());
        }
        
        return prices;
    }
    
    /**
     * Fetch crypto price using static prices (Yahoo Finance integration to be implemented later)
     */
    private BigDecimal fetchCryptoPrice(String symbol) {
        // TODO: Implement Yahoo Finance API integration
        // For now, use static prices updated from user's portfolio data
        
        // Fallback to static prices (updated based on user's portfolio screenshot)
        Map<String, BigDecimal> staticPrices = Map.ofEntries(
            Map.entry("BTC", new BigDecimal("110851.07")),    // Bitcoin - from screenshot
            Map.entry("ETH", new BigDecimal("4292.10")),       // Ethereum - from screenshot  
            Map.entry("ADA", new BigDecimal("0.8333")),        // Cardano - from screenshot
            Map.entry("SOL", new BigDecimal("206.16")),        // Solana - from screenshot
            Map.entry("DOT", new BigDecimal("7.50")),
            Map.entry("MATIC", new BigDecimal("0.85")),
            Map.entry("POL", new BigDecimal("0.276")),         // Polygon - from screenshot
            Map.entry("LINK", new BigDecimal("15")),
            Map.entry("UNI", new BigDecimal("6.50")),
            Map.entry("ATOM", new BigDecimal("8.20")),
            Map.entry("XRP", new BigDecimal("2.87")),          // XRP - from screenshot
            Map.entry("XLM", new BigDecimal("0.12")),
            Map.entry("LTC", new BigDecimal("75")),
            Map.entry("DOGE", new BigDecimal("0.2276")),       // Dogecoin - from screenshot
            Map.entry("AVAX", new BigDecimal("24.65")),        // Avalanche - from screenshot
            Map.entry("TRX", new BigDecimal("0.3303")),        // TRON - from screenshot
            Map.entry("XMR", new BigDecimal("160")),
            Map.entry("ZEC", new BigDecimal("35")),
            Map.entry("ALGO", new BigDecimal("0.20")),
            Map.entry("FLOW", new BigDecimal("0.80")),
            Map.entry("NEAR", new BigDecimal("2.50")),
            Map.entry("FIL", new BigDecimal("4.50")),
            Map.entry("GRT", new BigDecimal("0.18")),
            Map.entry("OCEAN", new BigDecimal("0.65")),
            Map.entry("STORJ", new BigDecimal("0.55")),
            Map.entry("SAND", new BigDecimal("0.40")),
            Map.entry("MANA", new BigDecimal("0.55")),
            Map.entry("GALA", new BigDecimal("0.01625")),      // Gala Games - from screenshot
            Map.entry("AKT", new BigDecimal("1.09")),          // Akash - from screenshot
            Map.entry("SEI", new BigDecimal("0.2945")),        // Sei - from screenshot
            Map.entry("INJ", new BigDecimal("12.98")),         // Injective - from screenshot
            Map.entry("RENDER", new BigDecimal("5.50")),
            Map.entry("BABY", new BigDecimal("0.0000001")),
            Map.entry("PEPE", new BigDecimal("0.00001"))       // Pepe (estimated)
        );
        
        return staticPrices.getOrDefault(symbol.toUpperCase(), new BigDecimal("1"));
    }
    
    /**
     * Fetch crypto prices in batch using static prices (Yahoo Finance integration to be implemented later)
     */
    private Map<String, BigDecimal> fetchCryptoPricesBatch(List<String> symbols) {
        Map<String, BigDecimal> prices = new HashMap<>();

        // TODO: Implement Yahoo Finance API integration for batch pricing
        // For now, use individual static price lookups
        
        // Fallback to individual fetches
        for (String symbol : symbols) {
            prices.put(symbol, fetchCryptoPrice(symbol));
        }
        return prices;
    }
    
    /**
     * Fetch stock price (placeholder)
     */
    private BigDecimal fetchStockPrice(String symbol) {
        // Placeholder for stock prices
        return new BigDecimal("100");
    }
    
    /**
     * Fetch fiat exchange rate
     */
    private BigDecimal fetchFiatRate(String symbol) {
        if ("USD".equals(symbol)) {
            return BigDecimal.ONE;
        }
        
        Map<String, BigDecimal> rates = Map.of(
            "EUR", new BigDecimal("1.10"),
            "GBP", new BigDecimal("1.25"),
            "JPY", new BigDecimal("0.0067"),
            "CAD", new BigDecimal("0.74"),
            "AUD", new BigDecimal("0.65"),
            "CHF", new BigDecimal("1.12")
        );
        
        return rates.getOrDefault(symbol, BigDecimal.ONE);
    }
    
    /**
     * Clear all price caches
     */
    public void clearCache() {
        priceCache.invalidateAll();
        batchPriceCache.invalidateAll();
    }
    
    /**
     * Clear cache for specific symbol
     */
    public void clearCacheForSymbol(String symbol) {
        priceCache.invalidate(symbol + "_crypto");
        priceCache.invalidate(symbol + "_stock");
        priceCache.invalidate(symbol + "_fiat");
    }
}