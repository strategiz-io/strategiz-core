package io.strategiz.business.watchlist;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.client.fmp.client.FmpQuoteClient;
import io.strategiz.client.fmp.dto.FmpQuote;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for watchlist operations following SOLID principles.
 * Single responsibility: Orchestrates watchlist data retrieval and composition.
 */
@Service
public class WatchlistBusiness {

    private static final Logger log = LoggerFactory.getLogger(WatchlistBusiness.class);

    private final FmpQuoteClient fmpQuoteClient;
    private final CoinGeckoClient coinGeckoClient;

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
        new DefaultSymbol("AMZN", "STOCK", "Amazon.com Inc."),
        new DefaultSymbol("QQQ", "ETF", "Invesco QQQ Trust"),
        new DefaultSymbol("SPY", "ETF", "SPDR S&P 500 ETF"),
        new DefaultSymbol("NVDA", "STOCK", "NVIDIA Corporation")
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
    public WatchlistBusiness(@Autowired(required = false) FmpQuoteClient fmpQuoteClient,
                           CoinGeckoClient coinGeckoClient) {
        this.fmpQuoteClient = fmpQuoteClient;
        this.coinGeckoClient = coinGeckoClient;
    }

    /**

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

        // For stocks/ETFs, use FMP
        if ("STOCK".equalsIgnoreCase(type) || "ETF".equalsIgnoreCase(type)) {
            try {
                enrichFromFmp(entity);
                log.info("Successfully enriched {} from FMP", symbol);

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
                log.error("FMP enrichment failed for {}: {}", symbol, e.getMessage());
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
     * Enrich entity from FMP API (supports stocks and ETFs)
     */
    private void enrichFromFmp(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();

        if (fmpQuoteClient == null) {
            throw new StrategizException(
                ServiceDashboardErrorDetails.MARKET_DATA_UNAVAILABLE,
                "business-watchlist",
                "FMP client is not available"
            );
        }

        FmpQuote quote = fmpQuoteClient.getQuote(symbol);

        if (quote.getPrice() != null) {
            entity.setCurrentPrice(quote.getPrice());
        }
        if (quote.getChange() != null) {
            entity.setChange(quote.getChange());
        }
        if (quote.getChangePercent() != null) {
            entity.setChangePercent(quote.getChangePercent());
        }
        if (quote.getVolume() != null) {
            entity.setVolume(quote.getVolume());
        }
        if (quote.getMarketCap() != null) {
            entity.setMarketCap(quote.getMarketCap().longValue());
        }

        // Set name from FMP if not already set
        if ((entity.getName() == null || entity.getName().equals(symbol)) && quote.getName() != null) {
            entity.setName(quote.getName());
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

}
