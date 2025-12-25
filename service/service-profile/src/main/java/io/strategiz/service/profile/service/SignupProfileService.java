package io.strategiz.service.profile.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.data.watchlist.repository.WatchlistBaseRepository;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.constants.ProfileConstants;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling user profile creation during signup
 */
@Service
public class SignupProfileService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-profile";
    }

    // Symbol to CoinGecko ID mapping for crypto
    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = new HashMap<>();
    static {
        CRYPTO_SYMBOL_TO_ID.put("BTC", "bitcoin");
        CRYPTO_SYMBOL_TO_ID.put("ETH", "ethereum");
        CRYPTO_SYMBOL_TO_ID.put("SOL", "solana");
    }

    private final UserRepository userRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final WatchlistBaseRepository watchlistRepository;
    private final YahooFinanceClient yahooFinanceClient;
    private final CoinGeckoClient coinGeckoClient;

    public SignupProfileService(
        UserRepository userRepository,
        SessionAuthBusiness sessionAuthBusiness,
        WatchlistBaseRepository watchlistRepository,
        YahooFinanceClient yahooFinanceClient,
        CoinGeckoClient coinGeckoClient
    ) {
        this.userRepository = userRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.watchlistRepository = watchlistRepository;
        this.yahooFinanceClient = yahooFinanceClient;
        this.coinGeckoClient = coinGeckoClient;
    }
    
    /**
     * Create signup profile
     */
    public CreateProfileResponse createSignupProfile(CreateProfileRequest request) {
        log.info(ProfileConstants.LogMessages.CREATING_SIGNUP_PROFILE + "{}", request.getEmail());
        
        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            
            // User already exists, return existing profile for signup continuation
            log.info(ProfileConstants.LogMessages.USER_EXISTS_CONTINUE + "{}", user.getId());
            
            // Update the profile with new information if provided
            UserProfileEntity profile = user.getProfile();
            if (request.getName() != null && !request.getName().equals(profile.getName())) {
                profile.setName(request.getName());
                user.setProfile(profile);
                user = userRepository.save(user);
            }
            
            // Generate a partial authentication token (ACR=1) for signup flow
            String identityToken = generatePartialAuthToken(user.getId(), user.getProfile().getEmail());
            
            CreateProfileResponse response = new CreateProfileResponse();
            response.setUserId(user.getId());
            response.setName(user.getProfile().getName());
            response.setEmail(user.getProfile().getEmail());
            response.setDemoMode(user.getProfile().getDemoMode());
            response.setIdentityToken(identityToken);

            return response;
        }

        // User doesn't exist, create new profile
        UserEntity user = createProfile(request.getName(), request.getEmail(), request.getDemoMode());

        // Generate a partial authentication token (ACR=1) for signup flow
        String identityToken = generatePartialAuthToken(user.getId(), user.getProfile().getEmail());

        CreateProfileResponse response = new CreateProfileResponse();
        response.setUserId(user.getId());
        response.setName(user.getProfile().getName());
        response.setEmail(user.getProfile().getEmail());
        response.setDemoMode(user.getProfile().getDemoMode());
        response.setIdentityToken(identityToken);

        return response;
    }

    /**
     * Create user profile (used by SignupProfileController)
     */
    public CreateProfileResponse createUserProfile(CreateProfileRequest request) {
        return createSignupProfile(request);
    }

    /**
     * Helper method to create a new user profile
     */
    private UserEntity createProfile(String name, String email, Boolean demoMode) {
        log.info("=== SIGNUP PROFILE SERVICE: createProfile START ===");
        log.info("SignupProfileService.createProfile - Creating profile for email: {}, demoMode: {}", email, demoMode);

        UserEntity user = new UserEntity();
        log.info("SignupProfileService.createProfile - UserEntity created, ID before save: {}", user.getId());

        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(name);
        profile.setEmail(email);
        profile.setIsEmailVerified(ProfileConstants.Defaults.EMAIL_VERIFIED);
        profile.setSubscriptionTier(ProfileConstants.Defaults.SUBSCRIPTION_TIER);
        // Use provided demoMode, or default to true if not specified
        profile.setDemoMode(demoMode != null ? demoMode : ProfileConstants.Defaults.DEMO_MODE);

        user.setProfile(profile);
        log.info("SignupProfileService.createProfile - Profile set, userId still: {}", user.getId());

        // Don't set any ID - let Firestore auto-generate the document ID on save
        // The repository will handle:
        // 1. Auto-generating a UUID document ID
        // 2. Initializing audit fields

        // Use createUser() to ensure proper user creation with UUID
        log.info("SignupProfileService.createProfile - Calling userRepository.createUser()");
        UserEntity savedUser = userRepository.createUser(user);

        log.info("=== SIGNUP PROFILE SERVICE: createProfile END ===");
        log.info("SignupProfileService.createProfile - User saved with ID: {} for email: {}", savedUser.getId(), email);
        log.info("SignupProfileService.createProfile - Full userId value: [{}]", savedUser.getId());

        // Initialize default watchlist for the new user
        // This ensures all signup methods (Passkey, Email/Password, etc.) get a watchlist
        initializeDefaultWatchlist(savedUser.getId());

        return savedUser;
    }
    
    /**
     * Generate an identity token for signup flow
     * This token uses the identity-key (not session-key) and has limited scope
     * It's used to verify identity during the multi-step signup process
     *
     * Two-Phase Token Flow:
     * Phase 1 (Signup): identity token with scope="profile:create", acr="0"
     * Phase 2 (After Auth): session token with full scopes, acr="1"+"
     */
    private String generatePartialAuthToken(String userId, String email) {
        log.info(ProfileConstants.LogMessages.GENERATING_TOKEN, userId);
        log.info("SignupProfileService.generatePartialAuthToken - userId: [{}]", userId);
        
        // ENHANCED LOGGING: Verify UUID format before creating token
        boolean isValidUUID = userId != null && userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        log.info("SignupProfileService.generatePartialAuthToken - userId is valid UUID: {}", isValidUUID);
        if (!isValidUUID) {
            log.error("CRITICAL: Attempting to create identity token with non-UUID userId: [{}]", userId);
            log.error("This will cause incorrect Firestore paths in Step 2/3 when creating subcollections!");
        }

        // Use createIdentityTokenPair to create a proper identity token
        // This uses the identity-key (not session-key) for proper security isolation
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createIdentityTokenPair(userId);

        log.info("SignupProfileService.generatePartialAuthToken - Created identity token for userId: [{}]", userId);

        // Return the identity token (access token from the pair)
        return tokenPair.accessToken();
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

                // Enrich with market data
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
                throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                    "SignupProfileService", "Market data fetch returned null price for " + symbol);
            }

        } catch (Exception e) {
            log.error("Market data enrichment failed for {}: {}", symbol, e.getMessage());
            throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                "SignupProfileService", "Cannot fetch market data for " + symbol + ": " + e.getMessage());
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
            throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                "SignupProfileService", "Yahoo Finance response missing quoteSummary for " + yahooSymbol);
        }

        List<Map<String, Object>> result = (List<Map<String, Object>>) quoteSummary.get("result");
        if (result == null || result.isEmpty()) {
            throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                "SignupProfileService", "Yahoo Finance quoteSummary has no results for " + yahooSymbol);
        }

        Map<String, Object> price = (Map<String, Object>) result.get(0).get("price");
        if (price == null) {
            throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                "SignupProfileService", "Yahoo Finance result missing price data for " + yahooSymbol);
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
            throw new StrategizException(ProfileErrors.MARKET_DATA_FETCH_FAILED,
                "SignupProfileService", "No data from CoinGecko for " + coinId);
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