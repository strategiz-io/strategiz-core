package io.strategiz.service.auth.service.signup;

import io.strategiz.data.base.transaction.FirestoreTransactionTemplate;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.data.watchlist.repository.WatchlistBaseRepository;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling OAuth signup processes
 * Specifically designed for signup flows where profile data comes from OAuth providers
 */
@Service
public class SignupService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    // Symbol to CoinGecko ID mapping for crypto
    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = new HashMap<>();
    static {
        CRYPTO_SYMBOL_TO_ID.put("BTC", "bitcoin");
        CRYPTO_SYMBOL_TO_ID.put("ETH", "ethereum");
        CRYPTO_SYMBOL_TO_ID.put("SOL", "solana");
    }

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final SignupResponseBuilder responseBuilder;
    private final FirestoreTransactionTemplate transactionTemplate;
    private final WatchlistBaseRepository watchlistRepository;
    private final YahooFinanceClient yahooFinanceClient;
    private final CoinGeckoClient coinGeckoClient;

    public SignupService(
        UserRepository userRepository,
        UserFactory userFactory,
        SignupResponseBuilder responseBuilder,
        FirestoreTransactionTemplate transactionTemplate,
        WatchlistBaseRepository watchlistRepository,
        YahooFinanceClient yahooFinanceClient,
        CoinGeckoClient coinGeckoClient
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.responseBuilder = responseBuilder;
        this.transactionTemplate = transactionTemplate;
        this.watchlistRepository = watchlistRepository;
        this.yahooFinanceClient = yahooFinanceClient;
        this.coinGeckoClient = coinGeckoClient;
    }

    /**
     * Process OAuth signup with profile data from external provider.
     * Uses Firestore transaction to ensure atomic user creation with email uniqueness check.
     *
     * @param request OAuth signup request containing OAuth profile data
     * @param deviceId Device ID for token generation
     * @param ipAddress IP address for token generation
     * @return OAuthSignupResponse with user details and authentication tokens
     */
    public OAuthSignupResponse processSignup(OAuthSignupRequest request, String deviceId, String ipAddress) {
        String authMethod = request.getAuthMethod().toLowerCase();
        log.info("Processing OAuth signup for email: {} with auth method: {}", request.getEmail(), authMethod);

        try {
            // Create the user entity with profile information using the factory
            UserEntity user = userFactory.createUser(request);
            String createdBy = request.getEmail();

            // Execute user creation within a Firestore transaction
            // This ensures atomic check-and-create to prevent duplicate users
            UserEntity createdUser = transactionTemplate.execute(transaction -> {
                // Check if user already exists (within transaction for consistency)
                if (userRepository.getUserByEmail(request.getEmail()).isPresent()) {
                    throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "User with email already exists");
                }

                // Create user - this will use the transaction from ThreadLocal
                return userRepository.createUser(user);
            });

            log.info("OAuth user created successfully in transaction: {}", createdUser.getUserId());

            // Initialize default watchlist asynchronously (non-blocking)
            initializeDefaultWatchlist(createdUser.getUserId());

            // Build success response with tokens (outside transaction - tokens don't need atomicity)
            List<String> authMethods = List.of(authMethod);
            return responseBuilder.buildSuccessResponse(
                createdUser,
                "OAuth signup completed successfully",
                authMethods,
                deviceId,
                ipAddress
            );

        } catch (StrategizException e) {
            log.warn("OAuth signup failed for {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth signup for {}: {}", request.getEmail(), e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "OAuth signup failed due to internal error");
        }
    }

    /**
     * Initialize default watchlist for new users.
     * Creates 6 default watchlist items with real market data.
     * Default symbols: TSLA, GOOGL, AMZN, QQQ, SPY, NVDA (stocks/ETF)
     *
     * @param userId The user ID to initialize watchlist for
     */
    private void initializeDefaultWatchlist(String userId) {
        log.info("Initializing default watchlist for user: {}", userId);

        // Define default symbols with types
        List<DefaultSymbol> defaultSymbols = Arrays.asList(
            new DefaultSymbol("TSLA", "STOCK", "Tesla Inc."),
            new DefaultSymbol("GOOGL", "STOCK", "Alphabet Inc."),
            new DefaultSymbol("AMZN", "STOCK", "Amazon.com Inc."),
            new DefaultSymbol("QQQ", "ETF", "Invesco QQQ Trust"),
            new DefaultSymbol("SPY", "ETF", "SPDR S&P 500 ETF"),
            new DefaultSymbol("NVDA", "STOCK", "NVIDIA Corporation")
        );

        int successCount = 0;
        int failCount = 0;

        for (DefaultSymbol defaultSymbol : defaultSymbols) {
            try {
                WatchlistItemEntity entity = new WatchlistItemEntity();
                entity.setSymbol(defaultSymbol.symbol);
                entity.setName(defaultSymbol.name);
                entity.setType(defaultSymbol.type);
                entity.setSortOrder(successCount);

                // Enrich with market data (Yahoo Finance primary, CoinGecko fallback for crypto)
                enrichWatchlistItem(entity);

                // Save to Firestore
                watchlistRepository.save(entity, userId);
                successCount++;
                log.debug("Created default watchlist item {} for user {}", defaultSymbol.symbol, userId);

            } catch (Exception e) {
                failCount++;
                log.warn("Failed to create default watchlist item {} for user {}: {}",
                    defaultSymbol.symbol, userId, e.getMessage());
            }
        }

        log.info("Default watchlist initialization completed for user {}: {} success, {} failed",
            userId, successCount, failCount);
    }

    /**
     * Enrich watchlist item with market data from Yahoo Finance or CoinGecko.
     *
     * @throws RuntimeException if market data cannot be fetched
     */
    private void enrichWatchlistItem(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        String type = entity.getType().toUpperCase();

        try {
            // Use Yahoo Finance for stocks/ETFs
            if ("STOCK".equalsIgnoreCase(type) || "ETF".equalsIgnoreCase(type)) {
                enrichFromYahooFinance(entity);
                log.info("Enriched {} from Yahoo Finance", symbol);
            }
            // Use CoinGecko for crypto
            else if ("CRYPTO".equalsIgnoreCase(type)) {
                enrichFromCoinGecko(entity);
                log.info("Enriched {} from CoinGecko", symbol);
            }

            // Validate that we got the required data
            if (entity.getCurrentPrice() == null) {
                throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                        "Market data fetch returned null price for " + symbol);
            }

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Market data enrichment failed for {}: {}", symbol, e.getMessage());
            throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                    "Cannot fetch market data for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Enrich entity from Yahoo Finance API
     */
    private void enrichFromYahooFinance(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();
        String type = entity.getType().toUpperCase();

        // Format symbol for Yahoo Finance
        String yahooSymbol = "CRYPTO".equalsIgnoreCase(type) ? symbol + "-USD" : symbol;

        // Fetch quote from Yahoo Finance
        Map<String, Object> response = yahooFinanceClient.fetchQuote(yahooSymbol);

        // Parse nested response structure
        Map<String, Object> quoteSummary = (Map<String, Object>) response.get("quoteSummary");
        if (quoteSummary == null) {
            throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                    "Yahoo Finance response missing quoteSummary for " + yahooSymbol);
        }

        List<Map<String, Object>> result = (List<Map<String, Object>>) quoteSummary.get("result");
        if (result == null || result.isEmpty()) {
            throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                    "Yahoo Finance quoteSummary has no results for " + yahooSymbol);
        }

        Map<String, Object> price = (Map<String, Object>) result.get(0).get("price");
        if (price == null) {
            throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                    "Yahoo Finance result missing price data for " + yahooSymbol);
        }

        // Extract and set price data
        entity.setCurrentPrice(extractBigDecimal(price.get("regularMarketPrice")));
        entity.setChange(extractBigDecimal(price.get("regularMarketChange")));
        entity.setChangePercent(extractBigDecimal(price.get("regularMarketChangePercent")));
        entity.setVolume(extractLong(price.get("regularMarketVolume")));
        entity.setMarketCap(extractLong(price.get("marketCap")));

        // Set name from Yahoo Finance if not already set
        if (entity.getName() == null || entity.getName().equals(symbol)) {
            String name = price.get("longName") != null ? price.get("longName").toString() :
                         (price.get("shortName") != null ? price.get("shortName").toString() : symbol);
            entity.setName(name);
        }
    }

    /**
     * Enrich entity from CoinGecko API (crypto only)
     */
    private void enrichFromCoinGecko(WatchlistItemEntity entity) {
        String symbol = entity.getSymbol();

        // Map symbol to CoinGecko coin ID
        String coinId = CRYPTO_SYMBOL_TO_ID.getOrDefault(symbol.toUpperCase(), symbol.toLowerCase());

        // Fetch from CoinGecko
        List<CryptoCurrency> cryptoData = coinGeckoClient.getCryptocurrencyMarketData(
            Arrays.asList(coinId), "usd"
        );

        if (cryptoData == null || cryptoData.isEmpty()) {
            throw new StrategizException(ServiceAuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth",
                    "No data from CoinGecko for " + coinId);
        }

        CryptoCurrency crypto = cryptoData.get(0);

        // Populate entity
        entity.setCurrentPrice(crypto.getCurrentPrice());
        entity.setChange(crypto.getPriceChange24h());
        entity.setChangePercent(crypto.getPriceChangePercentage24h());
        if (crypto.getTotalVolume() != null) entity.setVolume(crypto.getTotalVolume().longValue());
        if (crypto.getMarketCap() != null) entity.setMarketCap(crypto.getMarketCap().longValue());
        if (entity.getName() == null || entity.getName().equals(symbol)) {
            entity.setName(crypto.getName());
        }
    }

    /**
     * Extract BigDecimal from Yahoo Finance response
     */
    private BigDecimal extractBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) {
            Object raw = ((Map<String, Object>) obj).get("raw");
            if (raw instanceof Number) return BigDecimal.valueOf(((Number) raw).doubleValue());
        }
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        if (obj instanceof String) {
            try { return new BigDecimal((String) obj); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /**
     * Extract Long from Yahoo Finance response
     */
    private Long extractLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) {
            Object raw = ((Map<String, Object>) obj).get("raw");
            if (raw instanceof Number) return ((Number) raw).longValue();
        }
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) {
            try { return Long.parseLong((String) obj); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /**
     * Helper class for default symbols
     */
    private static class DefaultSymbol {
        final String symbol;
        final String type;
        final String name;

        DefaultSymbol(String symbol, String type, String name) {
            this.symbol = symbol;
            this.type = type;
            this.name = name;
        }
    }
}