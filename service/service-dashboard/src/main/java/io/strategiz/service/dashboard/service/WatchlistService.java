package io.strategiz.service.dashboard.service;

import io.strategiz.service.dashboard.model.watchlist.WatchlistItem;
import io.strategiz.service.dashboard.model.watchlist.WatchlistAsset;
import io.strategiz.service.dashboard.model.watchlist.WatchlistResponse;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.client.alpaca.client.AlpacaHistoricalClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for watchlist operations following SOLID principles.
 * Single responsibility: Orchestrates watchlist data retrieval and composition.
 */
@Service
public class WatchlistService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-dashboard";
    }

    private final YahooFinanceClient yahooFinanceClient;
    private final CoinGeckoClient coinGeckoClient;
    private final io.strategiz.data.watchlist.repository.WatchlistBaseRepository watchlistRepository;
    private final AlpacaHistoricalClient alpacaHistoricalClient;

    // Default watchlist symbols for new users
    public static class DefaultSymbol {
        public final String symbol;
        public final String type;
        public final String name;

        public DefaultSymbol(String symbol, String type, String name) {
            this.symbol = symbol;
            this.type = type;
            this.name = name;
        }
    }

    private static final List<DefaultSymbol> DEFAULT_WATCHLIST_SYMBOLS = Arrays.asList(
        new DefaultSymbol("TSLA", "STOCK", "Tesla Inc."),
        new DefaultSymbol("GOOGL", "STOCK", "Alphabet Inc."),
        new DefaultSymbol("NVDA", "STOCK", "NVIDIA Corporation"),
        new DefaultSymbol("QQQ", "ETF", "Invesco QQQ Trust"),
        new DefaultSymbol("SPY", "ETF", "SPDR S&P 500 ETF"),
        new DefaultSymbol("BTC", "CRYPTO", "Bitcoin")
    );

    // Symbol to CoinGecko ID mapping for crypto fallback
    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = new HashMap<>();
    static {
        CRYPTO_SYMBOL_TO_ID.put("BTC", "bitcoin");
        CRYPTO_SYMBOL_TO_ID.put("ETH", "ethereum");
        CRYPTO_SYMBOL_TO_ID.put("SOL", "solana");
        CRYPTO_SYMBOL_TO_ID.put("BNB", "binancecoin");
        CRYPTO_SYMBOL_TO_ID.put("ADA", "cardano");
        CRYPTO_SYMBOL_TO_ID.put("XRP", "ripple");
        CRYPTO_SYMBOL_TO_ID.put("DOGE", "dogecoin");
        CRYPTO_SYMBOL_TO_ID.put("DOT", "polkadot");
        CRYPTO_SYMBOL_TO_ID.put("AVAX", "avalanche-2");
        CRYPTO_SYMBOL_TO_ID.put("MATIC", "matic-network");
    }

    /**
     * Get default watchlist symbols for new users.
     * This is the single source of truth for default symbols.
     */
    public List<DefaultSymbol> getDefaultWatchlistSymbols() {
        return DEFAULT_WATCHLIST_SYMBOLS;
    }

    @Autowired
    public WatchlistService(YahooFinanceClient yahooFinanceClient,
                           CoinGeckoClient coinGeckoClient,
                           io.strategiz.data.watchlist.repository.WatchlistBaseRepository watchlistRepository,
                           AlpacaHistoricalClient alpacaHistoricalClient) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.coinGeckoClient = coinGeckoClient;
        this.watchlistRepository = watchlistRepository;
        this.alpacaHistoricalClient = alpacaHistoricalClient;
    }

    /**
     * Gets watchlist data for the authenticated user
     *
     * @param userId The user ID to fetch watchlist data for
     * @return Watchlist response
     */
    public WatchlistResponse getWatchlist(String userId) {
        log.info("Getting watchlist data for user: {}", userId);

        // Validate input
        if (userId == null || userId.trim().isEmpty()) {
            throw new StrategizException(ServiceDashboardErrorDetails.INVALID_PORTFOLIO_DATA, "service-dashboard", "userId", userId, "User ID cannot be null or empty");
        }

        try {
            // TODO: Implement when dependencies are available
            WatchlistResponse response = new WatchlistResponse();
            response.setWatchlistItems(Arrays.asList());
            response.setAvailableCategories(Arrays.asList("All", "Crypto", "Stocks", "ETFs"));
            return response;

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error getting watchlist for user: {}", userId, e);
            throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "get_watchlist", e.getMessage());
        }
    }

    /**
     * Initialize default watchlist for new users with 6 default symbols.
     * Uses parallel enrichment for performance (~1-2 seconds total).
     * FALLBACK STRATEGY: Saves ALL symbols even if enrichment fails.
     * Items without prices will be enriched on subsequent dashboard loads.
     * Idempotent: returns existing items if watchlist already initialized.
     *
     * @param userId The user ID to initialize watchlist for
     * @return List of watchlist items (all default symbols, some may lack price data)
     */
    public List<WatchlistItemEntity> initializeDefaultWatchlist(String userId) {
        log.info("Initializing default watchlist for user: {}", userId);

        // 1. Check if watchlist already has items (idempotency)
        List<WatchlistItemEntity> existing = watchlistRepository.findAllByUserId(userId);
        if (!existing.isEmpty()) {
            log.info("Watchlist already initialized for user {} with {} items", userId, existing.size());
            return existing;
        }

        // 2. Create entities for default symbols and try to enrich in parallel
        List<CompletableFuture<WatchlistItemEntity>> enrichmentFutures = new ArrayList<>();
        for (int i = 0; i < DEFAULT_WATCHLIST_SYMBOLS.size(); i++) {
            DefaultSymbol defaultSymbol = DEFAULT_WATCHLIST_SYMBOLS.get(i);
            int sortOrder = i;

            // Enrich each symbol in parallel
            CompletableFuture<WatchlistItemEntity> future = CompletableFuture.supplyAsync(() -> {
                // Create entity with basic info (always saved regardless of enrichment)
                WatchlistItemEntity entity = new WatchlistItemEntity();
                entity.setSymbol(defaultSymbol.symbol);
                entity.setName(defaultSymbol.name);
                entity.setType(defaultSymbol.type);
                entity.setSortOrder(sortOrder);

                // Try to enrich - if it fails, still return entity without price data
                // Use Alpaca for both stocks and crypto (consistent with working ticker banner)
                try {
                    if ("CRYPTO".equalsIgnoreCase(defaultSymbol.type)) {
                        enrichCryptoFromAlpaca(entity);
                        log.info("Successfully enriched crypto {} from Alpaca", defaultSymbol.symbol);
                    } else {
                        enrichFromAlpaca(entity);
                        log.info("Successfully enriched {} from Alpaca", defaultSymbol.symbol);
                    }
                } catch (Exception e) {
                    // Log warning but STILL return entity - prices can be fetched later
                    log.warn("Failed to enrich symbol {} (will save without price data): {}",
                        defaultSymbol.symbol, e.getMessage());
                }

                return entity; // Always return entity, even without price data
            });

            enrichmentFutures.add(future);
        }

        // 3. Wait for all enrichments to complete
        CompletableFuture.allOf(enrichmentFutures.toArray(new CompletableFuture[0])).join();

        // 4. Collect ALL entities (enriched or not) and save
        List<WatchlistItemEntity> entities = enrichmentFutures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // 5. Save ALL entities to Firestore
        int enrichedCount = 0;
        for (WatchlistItemEntity entity : entities) {
            watchlistRepository.save(entity, userId);
            if (entity.getCurrentPrice() != null) {
                enrichedCount++;
            }
        }

        log.info("Initialized watchlist for user {}: {} symbols saved ({} with price data)",
            userId, entities.size(), enrichedCount);

        return entities;
    }

    /**
     * Enrich entity from Alpaca API for stocks and ETFs.
     * Uses Alpaca's snapshots endpoint (same as ticker banner) for real-time data.
     *
     * @param entity The watchlist item entity to enrich
     * @throws StrategizException if Alpaca data cannot be fetched
     */
    private void enrichFromAlpaca(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        log.info("Enriching {} from Alpaca snapshots", symbol);

        try {
            // Use snapshots endpoint (same as ticker banner - proven to work)
            Map<String, io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote> quotes =
                alpacaHistoricalClient.getLatestStockQuotes(Arrays.asList(symbol));

            if (quotes == null || quotes.isEmpty() || !quotes.containsKey(symbol)) {
                throw new StrategizException(
                    ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                    "service-dashboard",
                    "No snapshot data from Alpaca for " + symbol
                );
            }

            io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote quote = quotes.get(symbol);

            if (quote.getPrice() == null) {
                throw new StrategizException(
                    ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                    "service-dashboard",
                    "Alpaca snapshot missing price for " + symbol
                );
            }

            // Populate entity from snapshot
            entity.setCurrentPrice(quote.getPrice());
            entity.setChange(quote.getChange());
            entity.setChangePercent(quote.getChangePercent());
            if (quote.getVolume() != null) {
                entity.setVolume(quote.getVolume());
            }

            log.info("Successfully enriched {} from Alpaca: price=${}, volume={}", symbol, entity.getCurrentPrice(), quote.getVolume());

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to enrich {} from Alpaca: {}", symbol, e.getMessage());
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                e,
                "Cannot fetch Alpaca data for " + symbol
            );
        }
    }

    /**
     * Enrich crypto entity from Alpaca API (same endpoint used by ticker banner).
     * Uses Alpaca's crypto snapshots endpoint for real-time data.
     *
     * @param entity The watchlist item entity to enrich
     * @throws StrategizException if Alpaca data cannot be fetched
     */
    private void enrichCryptoFromAlpaca(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        log.info("Enriching crypto {} from Alpaca snapshots", symbol);

        try {
            // Use crypto snapshots endpoint (same as ticker banner - proven to work)
            Map<String, io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote> quotes =
                alpacaHistoricalClient.getLatestCryptoQuotes(Arrays.asList(symbol));

            if (quotes == null || quotes.isEmpty() || !quotes.containsKey(symbol)) {
                throw new StrategizException(
                    ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                    "service-dashboard",
                    "No crypto snapshot data from Alpaca for " + symbol
                );
            }

            io.strategiz.client.alpaca.client.AlpacaHistoricalClient.LatestQuote quote = quotes.get(symbol);

            if (quote.getPrice() == null) {
                throw new StrategizException(
                    ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                    "service-dashboard",
                    "Alpaca crypto snapshot missing price for " + symbol
                );
            }

            // Populate entity from snapshot
            entity.setCurrentPrice(quote.getPrice());
            entity.setChange(quote.getChange());
            entity.setChangePercent(quote.getChangePercent());
            if (quote.getVolume() != null) {
                entity.setVolume(quote.getVolume());
            }

            log.info("Successfully enriched crypto {} from Alpaca: price=${}, volume={}", symbol, entity.getCurrentPrice(), quote.getVolume());

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to enrich crypto {} from Alpaca: {}", symbol, e.getMessage());
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                e,
                "Cannot fetch Alpaca crypto data for " + symbol
            );
        }
    }

    /**
     * Enriches a watchlist item with market data from external APIs.
     * Strategy: CoinGecko for crypto, throws exception if data fetch fails.
     *
     * IMPORTANT: This method MUST successfully fetch market data or throw an exception.
     * The caller should NOT save the entity if this method throws an exception.
     *
     * @param entity The watchlist item entity to enrich
     * @return The enriched entity with market data populated
     * @throws StrategizException if market data cannot be fetched
     */
    public WatchlistItemEntity enrichWatchlistItem(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        String type = entity.getType() != null ? entity.getType().toUpperCase() : "STOCK";

        log.info("Enriching watchlist item: {} (type: {})", symbol, type);

        // For crypto, use CoinGecko as primary
        if ("CRYPTO".equalsIgnoreCase(type)) {
            try {
                enrichFromCoinGecko(entity);
                log.info("Successfully enriched {} from CoinGecko", symbol);

                // Validate that we actually got the required data
                if (entity.getCurrentPrice() == null) {
                    throw new StrategizException(
                        ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                        "service-dashboard",
                        "Market data fetch returned null price for " + symbol
                    );
                }

                return entity;
            } catch (Exception e) {
                log.error("CoinGecko enrichment failed for {}: {}", symbol, e.getMessage());
                throw new StrategizException(
                    ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                    "service-dashboard",
                    "Cannot fetch market data for " + symbol + ": " + e.getMessage()
                );
            }
        }

        // For stocks/ETFs, use Alpaca (consistent with initialization)
        if ("STOCK".equalsIgnoreCase(type) || "ETF".equalsIgnoreCase(type)) {
            try {
                enrichFromAlpaca(entity);
                log.info("Successfully enriched {} from Alpaca", symbol);

                // Validate that we actually got the required data
                if (entity.getCurrentPrice() == null) {
                    throw new StrategizException(
                        ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                        "service-dashboard",
                        "Market data fetch returned null price for " + symbol
                    );
                }

                return entity;
            } catch (Exception e) {
                log.error("Alpaca enrichment failed for {}: {}", symbol, e.getMessage());
                throw new StrategizException(
                    ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                    "service-dashboard",
                    "Cannot fetch market data for " + symbol + ": " + e.getMessage()
                );
            }
        }

        // Unknown type
        throw new StrategizException(
            ServiceDashboardErrorDetails.DASHBOARD_ERROR,
            "service-dashboard",
            "Unknown asset type: " + type + " for symbol " + symbol
        );
    }

    /**
     * Enrich entity from Yahoo Finance API (supports both stocks and crypto)
     */
    private void enrichFromYahooFinance(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        String type = entity.getType() != null ? entity.getType().toUpperCase() : "STOCK";

        // Format symbol for Yahoo Finance
        String yahooSymbol = symbol;
        if ("CRYPTO".equalsIgnoreCase(type)) {
            // Crypto needs -USD suffix for Yahoo Finance
            yahooSymbol = symbol + "-USD";
        }

        // Fetch quote from Yahoo Finance
        Map<String, Object> response = yahooFinanceClient.fetchQuote(yahooSymbol);

        // Parse response - Yahoo Finance has complex nested structure
        Map<String, Object> quoteSummary = (Map<String, Object>) response.get("quoteSummary");
        if (quoteSummary == null) {
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                "Yahoo Finance response missing quoteSummary for " + yahooSymbol
            );
        }

        List<Map<String, Object>> result = (List<Map<String, Object>>) quoteSummary.get("result");
        if (result == null || result.isEmpty()) {
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                "Yahoo Finance quoteSummary has no results for " + yahooSymbol
            );
        }

        Map<String, Object> firstResult = result.get(0);
        Map<String, Object> price = (Map<String, Object>) firstResult.get("price");
        if (price == null) {
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                "Yahoo Finance result missing price data for " + yahooSymbol
            );
        }

        // Extract price data from nested structure
        Object regularMarketPriceObj = price.get("regularMarketPrice");
        Object regularMarketChangeObj = price.get("regularMarketChange");
        Object regularMarketChangePercentObj = price.get("regularMarketChangePercent");
        Object marketCapObj = price.get("marketCap");
        Object regularMarketVolumeObj = price.get("regularMarketVolume");
        Object shortNameObj = price.get("shortName");
        Object longNameObj = price.get("longName");

        // Parse price - can be Map with "raw" key or direct number
        BigDecimal currentPrice = extractBigDecimal(regularMarketPriceObj);
        BigDecimal change = extractBigDecimal(regularMarketChangeObj);
        BigDecimal changePercent = extractBigDecimal(regularMarketChangePercentObj);
        Long marketCap = extractLong(marketCapObj);
        Long volume = extractLong(regularMarketVolumeObj);

        // Populate entity
        if (currentPrice != null) {
            entity.setCurrentPrice(currentPrice);
        }
        if (change != null) {
            entity.setChange(change);
        }
        if (changePercent != null) {
            entity.setChangePercent(changePercent);
        }
        if (volume != null) {
            entity.setVolume(volume);
        }
        if (marketCap != null) {
            entity.setMarketCap(marketCap);
        }

        // Set name from Yahoo Finance if not already set
        if (entity.getName() == null || entity.getName().equals(symbol)) {
            String name = longNameObj != null ? longNameObj.toString() :
                         (shortNameObj != null ? shortNameObj.toString() : symbol);
            entity.setName(name);
        }
    }

    /**
     * Enrich entity from CoinGecko API (crypto only, fallback)
     */
    private void enrichFromCoinGecko(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();

        // Map symbol to CoinGecko coin ID
        String coinId = CRYPTO_SYMBOL_TO_ID.get(symbol.toUpperCase());
        if (coinId == null) {
            coinId = symbol.toLowerCase(); // Fallback to lowercase symbol
        }

        // Fetch from CoinGecko
        List<CryptoCurrency> cryptoData = coinGeckoClient.getCryptocurrencyMarketData(
            Arrays.asList(coinId),
            "usd"
        );

        if (cryptoData == null || cryptoData.isEmpty()) {
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "service-dashboard",
                "No data from CoinGecko for " + coinId
            );
        }

        CryptoCurrency crypto = cryptoData.get(0);

        // Populate entity from CoinGecko data
        if (crypto.getCurrentPrice() != null) {
            entity.setCurrentPrice(crypto.getCurrentPrice());
        }
        if (crypto.getPriceChange24h() != null) {
            entity.setChange(crypto.getPriceChange24h());
        }
        if (crypto.getPriceChangePercentage24h() != null) {
            entity.setChangePercent(crypto.getPriceChangePercentage24h());
        }
        if (crypto.getTotalVolume() != null) {
            entity.setVolume(crypto.getTotalVolume().longValue());
        }
        if (crypto.getMarketCap() != null) {
            entity.setMarketCap(crypto.getMarketCap().longValue());
        }

        // Set name from CoinGecko if not already set
        if (entity.getName() == null || entity.getName().equals(symbol)) {
            entity.setName(crypto.getName());
        }
    }

    /**
     * Extract BigDecimal from Yahoo Finance response (handles both Map with "raw" key and direct numbers)
     */
    private BigDecimal extractBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }

        // If it's a Map, extract "raw" value
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Object raw = map.get("raw");
            if (raw instanceof Number) {
                return BigDecimal.valueOf(((Number) raw).doubleValue());
            }
        }

        // If it's a direct number
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }

        // Try parsing as string
        if (obj instanceof String) {
            try {
                return new BigDecimal((String) obj);
            } catch (NumberFormatException e) {
                log.warn("Could not parse BigDecimal from string: {}", obj);
            }
        }

        return null;
    }

    /**
     * Extract Long from Yahoo Finance response (handles both Map with "raw" key and direct numbers)
     */
    private Long extractLong(Object obj) {
        if (obj == null) {
            return null;
        }

        // If it's a Map, extract "raw" value
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Object raw = map.get("raw");
            if (raw instanceof Number) {
                return ((Number) raw).longValue();
            }
        }

        // If it's a direct number
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }

        // Try parsing as string
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                log.warn("Could not parse Long from string: {}", obj);
            }
        }

        return null;
    }
}
