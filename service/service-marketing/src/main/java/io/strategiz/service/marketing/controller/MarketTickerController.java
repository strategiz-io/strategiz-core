package io.strategiz.service.marketing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.service.marketing.model.response.MarketTickerResponse;
import io.strategiz.service.marketing.model.response.TickerItem;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
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
        return "service-marketing";
    }
    
    private static final Logger log = LoggerFactory.getLogger(MarketTickerController.class);
    
    // Popular symbols to display
    private static final List<String> CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH", "SOL", "DOGE");
    private static final List<String> STOCK_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOG", "AMZN", "NVDA", "META", "TSLA", "BRK.B",
        "JPM", "V", "MA", "DIS", "NFLX", "AMD", "CRM", "ORCL"
    );
    
    private final CoinGeckoClient coinGeckoClient;

    public MarketTickerController(CoinGeckoClient coinGeckoClient) {
        this.coinGeckoClient = coinGeckoClient;
    }
    
    /**
     * Get market ticker data for popular assets.
     * Cached for 30 seconds to avoid rate limits.
     * 
     * @return Market ticker data with popular crypto and stock prices
     */
    @GetMapping
    // TODO: Re-enable caching once cache configuration is fixed
    // @Cacheable(value = "marketTicker", key = "'ticker'", cacheManager = "cacheManager")
    public ResponseEntity<MarketTickerResponse> getMarketTicker() {
        log.info("Fetching market ticker data");
        
        try {
            // Fetch data sequentially
            List<TickerItem> cryptoItems = fetchCryptoData();
            List<TickerItem> stockItems = fetchStockData();

            // Combine results - stocks first, then crypto
            List<TickerItem> allItems = new ArrayList<>();
            allItems.addAll(stockItems);
            allItems.addAll(cryptoItems);
            
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
     * Fetch crypto data from CoinGecko.
     * Returns empty list if CoinGecko fails (no fallback - banner won't show crypto).
     */
    private List<TickerItem> fetchCryptoData() {
        try {
            List<TickerItem> items = fetchCryptoFromCoinGecko();
            if (!items.isEmpty()) {
                return items;
            }
        } catch (Exception e) {
            log.warn("CoinGecko failed, crypto data unavailable: {}", e.getMessage());
        }

        // No fallback - return empty list (banner won't show crypto section)
        return Collections.emptyList();
    }

    /**
     * Fetch crypto data from CoinGecko API
     */
    private List<TickerItem> fetchCryptoFromCoinGecko() {
        log.info("Fetching real crypto data from CoinGecko");
        List<TickerItem> items = new ArrayList<>();

        // Convert our symbol list to CoinGecko IDs
        Map<String, String> symbolToId = new HashMap<>();
        symbolToId.put("BTC", "bitcoin");
        symbolToId.put("ETH", "ethereum");
        symbolToId.put("SOL", "solana");
        symbolToId.put("DOGE", "dogecoin");

        List<String> coinIds = CRYPTO_SYMBOLS.stream()
            .map(symbol -> symbolToId.getOrDefault(symbol, symbol.toLowerCase()))
            .collect(Collectors.toList());

        List<CryptoCurrency> cryptoData = coinGeckoClient.getCryptocurrencyMarketData(coinIds, "usd");

        for (CryptoCurrency crypto : cryptoData) {
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
        items.addAll(getDemoStockData());
        items.addAll(getDemoCryptoData());

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
            new TickerItem("AAPL", "Apple Inc.", "stock", new BigDecimal("257.50"),
                new BigDecimal("2.25"), new BigDecimal("0.88"), true),
            new TickerItem("MSFT", "Microsoft Corp.", "stock", new BigDecimal("438.20"),
                new BigDecimal("5.80"), new BigDecimal("1.34"), true),
            new TickerItem("GOOG", "Alphabet Inc.", "stock", new BigDecimal("192.75"),
                new BigDecimal("2.85"), new BigDecimal("1.50"), true),
            new TickerItem("AMZN", "Amazon.com Inc.", "stock", new BigDecimal("227.80"),
                new BigDecimal("-1.90"), new BigDecimal("-0.83"), false),
            new TickerItem("NVDA", "NVIDIA Corp.", "stock", new BigDecimal("134.30"),
                new BigDecimal("3.60"), new BigDecimal("2.75"), true),
            new TickerItem("META", "Meta Platforms Inc.", "stock", new BigDecimal("612.20"),
                new BigDecimal("8.75"), new BigDecimal("1.45"), true),
            new TickerItem("TSLA", "Tesla Inc.", "stock", new BigDecimal("421.50"),
                new BigDecimal("-5.20"), new BigDecimal("-1.22"), false),
            new TickerItem("BRK.B", "Berkshire Hathaway", "stock", new BigDecimal("465.30"),
                new BigDecimal("2.15"), new BigDecimal("0.46"), true),
            new TickerItem("JPM", "JPMorgan Chase", "stock", new BigDecimal("243.80"),
                new BigDecimal("1.95"), new BigDecimal("0.81"), true),
            new TickerItem("V", "Visa Inc.", "stock", new BigDecimal("317.40"),
                new BigDecimal("3.20"), new BigDecimal("1.02"), true),
            new TickerItem("MA", "Mastercard Inc.", "stock", new BigDecimal("528.60"),
                new BigDecimal("4.50"), new BigDecimal("0.86"), true),
            new TickerItem("DIS", "Walt Disney Co.", "stock", new BigDecimal("112.80"),
                new BigDecimal("-0.85"), new BigDecimal("-0.75"), false),
            new TickerItem("NFLX", "Netflix Inc.", "stock", new BigDecimal("925.40"),
                new BigDecimal("12.30"), new BigDecimal("1.35"), true),
            new TickerItem("AMD", "AMD Inc.", "stock", new BigDecimal("124.60"),
                new BigDecimal("2.40"), new BigDecimal("1.96"), true),
            new TickerItem("CRM", "Salesforce Inc.", "stock", new BigDecimal("342.20"),
                new BigDecimal("-2.80"), new BigDecimal("-0.81"), false),
            new TickerItem("ORCL", "Oracle Corp.", "stock", new BigDecimal("172.90"),
                new BigDecimal("1.65"), new BigDecimal("0.96"), true)
        );
    }
}