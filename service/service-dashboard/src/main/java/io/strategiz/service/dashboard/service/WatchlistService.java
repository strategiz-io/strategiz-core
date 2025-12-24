package io.strategiz.service.dashboard.service;

import io.strategiz.service.dashboard.model.watchlist.WatchlistItem;
import io.strategiz.service.dashboard.model.watchlist.WatchlistAsset;
import io.strategiz.service.dashboard.model.watchlist.WatchlistResponse;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for watchlist operations following SOLID principles.
 * Single responsibility: Orchestrates watchlist data retrieval and composition.
 */
@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final YahooFinanceClient yahooFinanceClient;
    private final CoinGeckoClient coinGeckoClient;

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

    @Autowired
    public WatchlistService(YahooFinanceClient yahooFinanceClient,
                           CoinGeckoClient coinGeckoClient) {
        this.yahooFinanceClient = yahooFinanceClient;
        this.coinGeckoClient = coinGeckoClient;
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

        // For stocks/ETFs, Yahoo Finance requires cookies - not supported yet
        // Throw exception to prevent saving without market data
        log.error("Stock/ETF market data fetch not implemented yet for {}", symbol);
        throw new StrategizException(
            ServiceDashboardErrorDetails.DASHBOARD_ERROR,
            "service-dashboard",
            "Market data fetch for stocks/ETFs not yet available. Only crypto (CRYPTO type) is supported."
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
            throw new RuntimeException("No quoteSummary in Yahoo Finance response");
        }

        List<Map<String, Object>> result = (List<Map<String, Object>>) quoteSummary.get("result");
        if (result == null || result.isEmpty()) {
            throw new RuntimeException("No result in Yahoo Finance quoteSummary");
        }

        Map<String, Object> firstResult = result.get(0);
        Map<String, Object> price = (Map<String, Object>) firstResult.get("price");
        if (price == null) {
            throw new RuntimeException("No price data in Yahoo Finance result");
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
            throw new RuntimeException("No data from CoinGecko for " + coinId);
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
