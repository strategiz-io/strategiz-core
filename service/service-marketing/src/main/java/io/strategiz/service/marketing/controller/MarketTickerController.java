package io.strategiz.service.marketing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;

import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.service.marketing.model.response.MarketTickerResponse;
import io.strategiz.service.marketing.model.response.TickerItem;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketing.exception.ServiceMarketingErrorDetails;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Public controller for market ticker data.
 * No authentication required - provides real-time market data for the landing page.
 */
@RestController
@RequestMapping("/v1/market/tickers")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class MarketTickerController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.MARKETING_MODULE;
    }
    
    private static final Logger log = LoggerFactory.getLogger(MarketTickerController.class);
    
    // Popular symbols to display
    private static final List<String> CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH", "SOL", "BNB", "ADA");
    private static final List<String> STOCK_SYMBOLS = Arrays.asList("TSLA", "GOOG", "AAPL", "AMZN", "META", "NVDA", "NFLX", "AXP");
    
    private final CoinbaseClient coinbaseClient;
    private final CoinGeckoClient coinGeckoClient;
    private final AlphaVantageClient alphaVantageClient; // Keep for backtesting
    
    public MarketTickerController(
            CoinbaseClient coinbaseClient,
            CoinGeckoClient coinGeckoClient,
            AlphaVantageClient alphaVantageClient) {
        this.coinbaseClient = coinbaseClient;
        this.coinGeckoClient = coinGeckoClient;
        this.alphaVantageClient = alphaVantageClient;
    }
    
    /**
     * Get market ticker data for popular assets.
     * Cached for 30 seconds to avoid rate limits.
     * 
     * @return Market ticker data with popular crypto and stock prices
     */
    @GetMapping
    @Cacheable(value = "marketTicker", cacheManager = "cacheManager")
    public ResponseEntity<MarketTickerResponse> getMarketTicker() {
        log.info("Fetching market ticker data");
        
        try {
            // Fetch data in parallel for better performance
            CompletableFuture<List<TickerItem>> cryptoFuture = CompletableFuture.supplyAsync(this::fetchCryptoData);
            CompletableFuture<List<TickerItem>> stockFuture = CompletableFuture.supplyAsync(this::fetchStockData);
            
            // Wait for both to complete
            List<TickerItem> cryptoItems = cryptoFuture.get();
            List<TickerItem> stockItems = stockFuture.get();
            
            // Combine results
            List<TickerItem> allItems = new ArrayList<>();
            allItems.addAll(cryptoItems);
            allItems.addAll(stockItems);
            
            // Create response
            MarketTickerResponse response = new MarketTickerResponse(
                allItems,
                System.currentTimeMillis(),
                30 // cache duration in seconds
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching market ticker data", e);
            // Return demo data as fallback
            return ResponseEntity.ok(getDemoTickerData());
        }
    }
    
    /**
     * Fetch crypto data from available clients
     */
    private List<TickerItem> fetchCryptoData() {
        List<TickerItem> items = new ArrayList<>();
        
        try {
            // Use CoinGecko to fetch real crypto data
            log.info("Fetching real crypto data from CoinGecko");
            
            // Convert our symbol list to CoinGecko IDs
            Map<String, String> symbolToId = new HashMap<>();
            symbolToId.put("BTC", "bitcoin");
            symbolToId.put("ETH", "ethereum");
            symbolToId.put("SOL", "solana");
            symbolToId.put("BNB", "binancecoin");
            symbolToId.put("ADA", "cardano");
            
            List<String> coinIds = CRYPTO_SYMBOLS.stream()
                .map(symbol -> symbolToId.getOrDefault(symbol, symbol.toLowerCase()))
                .collect(Collectors.toList());
            
            var cryptoData = coinGeckoClient.getCryptocurrencyMarketData(coinIds, "usd");
            
            for (var crypto : cryptoData) {
                // Find the original symbol
                String symbol = CRYPTO_SYMBOLS.stream()
                    .filter(s -> symbolToId.getOrDefault(s, s.toLowerCase()).equals(crypto.getId()))
                    .findFirst()
                    .orElse(crypto.getSymbol().toUpperCase());
                
                items.add(new TickerItem(
                    symbol,
                    crypto.getName(),
                    "crypto",
                    crypto.getCurrentPrice(),
                    crypto.getPriceChange24h(),
                    crypto.getPriceChangePercentage24h(),
                    crypto.getPriceChange24h() != null && crypto.getPriceChange24h().compareTo(BigDecimal.ZERO) >= 0
                ));
            }
            
            log.info("Successfully fetched {} crypto items from CoinGecko", items.size());
            
        } catch (Exception e) {
            log.error("Error fetching crypto data from CoinGecko", e);
        }
        
        // Return demo data if no real data available
        if (items.isEmpty()) {
            log.warn("No real crypto data available, using demo data");
            items.addAll(getDemoCryptoData());
        }
        
        return items;
    }
    
    /**
     * Fetch stock data (using demo data temporarily until business service integration)
     */
    private List<TickerItem> fetchStockData() {
        List<TickerItem> items = new ArrayList<>();

        // TODO: Business logic should be moved to service/business module
        // For now, using demo data as requested
        log.info("Using demo stock data (business logic should be in service/business module)");
        items.addAll(getDemoStockData());

        return items;
    }
    
    /**
     * Demo data fallback
     */
    private MarketTickerResponse getDemoTickerData() {
        List<TickerItem> items = new ArrayList<>();
        items.addAll(getDemoCryptoData());
        items.addAll(getDemoStockData());
        
        return new MarketTickerResponse(
            items,
            System.currentTimeMillis(),
            30
        );
    }
    
    private List<TickerItem> getDemoCryptoData() {
        return Arrays.asList(
            new TickerItem("BTC", "Bitcoin", "crypto", new BigDecimal("43250.00"), 
                new BigDecimal("1250.00"), new BigDecimal("2.95"), true),
            new TickerItem("ETH", "Ethereum", "crypto", new BigDecimal("2650.00"), 
                new BigDecimal("-85.00"), new BigDecimal("-3.10"), false),
            new TickerItem("SOL", "Solana", "crypto", new BigDecimal("98.50"), 
                new BigDecimal("3.25"), new BigDecimal("3.42"), true)
        );
    }
    
    private List<TickerItem> getDemoStockData() {
        return Arrays.asList(
            new TickerItem("TSLA", "Tesla Inc.", "stock", new BigDecimal("248.50"), 
                new BigDecimal("-5.20"), new BigDecimal("-2.05"), false),
            new TickerItem("GOOG", "Alphabet Inc.", "stock", new BigDecimal("178.25"), 
                new BigDecimal("2.85"), new BigDecimal("1.62"), true),
            new TickerItem("AAPL", "Apple Inc.", "stock", new BigDecimal("193.50"), 
                new BigDecimal("2.25"), new BigDecimal("1.18"), true),
            new TickerItem("AMZN", "Amazon.com Inc.", "stock", new BigDecimal("155.80"), 
                new BigDecimal("-1.90"), new BigDecimal("-1.20"), false),
            new TickerItem("META", "Meta Platforms Inc.", "stock", new BigDecimal("485.20"), 
                new BigDecimal("8.75"), new BigDecimal("1.84"), true),
            new TickerItem("NVDA", "NVIDIA Corp.", "stock", new BigDecimal("875.30"), 
                new BigDecimal("15.60"), new BigDecimal("1.81"), true),
            new TickerItem("NFLX", "Netflix Inc.", "stock", new BigDecimal("685.40"), 
                new BigDecimal("-12.30"), new BigDecimal("-1.76"), false),
            new TickerItem("AXP", "American Express Co.", "stock", new BigDecimal("245.75"), 
                new BigDecimal("3.45"), new BigDecimal("1.42"), true)
        );
    }
    
    // Helper methods would calculate actual changes based on 24h data
    private BigDecimal calculateDailyChange(Object price) {
        // This would need 24h price data to calculate
        return new BigDecimal("0.00");
    }
    
    private BigDecimal calculateDailyChangePercent(Object price) {
        // This would need 24h price data to calculate
        return new BigDecimal("0.00");
    }
}