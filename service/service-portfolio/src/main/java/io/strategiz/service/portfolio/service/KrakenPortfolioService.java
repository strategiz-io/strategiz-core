package io.strategiz.service.portfolio.service;

import io.strategiz.business.portfolio.enhancer.PortfolioEnhancementOrchestrator;
import io.strategiz.business.portfolio.enhancer.model.EnhancedAsset;
import io.strategiz.business.portfolio.enhancer.model.EnhancedPortfolio;
import io.strategiz.client.kraken.auth.portfolio.KrakenApiPortfolioClient;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.data.provider.repository.UpdateProviderDataRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.PortfolioPositionResponse;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for handling Kraken-specific portfolio operations.
 * Single Responsibility: Manages only Kraken portfolio data.
 * Dependency Inversion: Depends on abstractions (repositories, clients).
 */
@Service
public class KrakenPortfolioService {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenPortfolioService.class);
    
    private final KrakenApiPortfolioClient krakenApiPortfolioClient;
    private final ReadProviderDataRepository readProviderDataRepository;
    private final CreateProviderDataRepository createProviderDataRepository;
    private final UpdateProviderDataRepository updateProviderDataRepository;
    private final UserRepository userRepository;
    private final PortfolioEnhancementOrchestrator portfolioEnhancer;
    
    // Cache for crypto prices (symbol -> price)
    private final Map<String, BigDecimal> priceCache = new ConcurrentHashMap<>();
    private final Map<String, Long> priceCacheTimestamp = new ConcurrentHashMap<>();
    private static final long PRICE_CACHE_TTL = 60000; // 1 minute
    
    // Kraken to standard symbol mapping
    private static final Map<String, String> SYMBOL_MAPPING = Map.of(
        "XXBT", "BTC",
        "XBT", "BTC",
        "XETH", "ETH",
        "XXRP", "XRP",
        "ZUSD", "USD",
        "ZEUR", "EUR",
        "ZGBP", "GBP",
        "ZCAD", "CAD",
        "ZJPY", "JPY"
    );
    
    @Autowired
    public KrakenPortfolioService(
            @Autowired(required = false) KrakenApiPortfolioClient krakenApiPortfolioClient,
            ReadProviderDataRepository readProviderDataRepository,
            @Autowired(required = false) CreateProviderDataRepository createProviderDataRepository,
            @Autowired(required = false) UpdateProviderDataRepository updateProviderDataRepository,
            UserRepository userRepository,
            @Autowired(required = false) PortfolioEnhancementOrchestrator portfolioEnhancer) {
        this.krakenApiPortfolioClient = krakenApiPortfolioClient;
        this.readProviderDataRepository = readProviderDataRepository;
        this.createProviderDataRepository = createProviderDataRepository;
        this.updateProviderDataRepository = updateProviderDataRepository;
        this.userRepository = userRepository;
        this.portfolioEnhancer = portfolioEnhancer;
    }
    
    /**
     * Get Kraken portfolio data for a user.
     * 
     * @param userId User ID
     * @return Kraken portfolio response
     */
    public ProviderPortfolioResponse getKrakenPortfolio(String userId) {
        log.info("Fetching Kraken portfolio for user: {}", userId);
        
        try {
            // Check if user is in demo mode
            boolean isInDemoMode = isDemoMode(userId);
            log.info("User {} demo mode status: {}", userId, isInDemoMode);
            
            if (isInDemoMode) {
                // Return demo data
                return getDemoKrakenData(userId);
            }
            
            // Fetch data (prioritizes cached data from Firestore, then real-time from Kraken API)
            ProviderPortfolioResponse response = fetchRealTimeKrakenData(userId);
            
            // Log what we're returning
            if (response != null && response.getPositions() != null) {
                log.info("Returning Kraken portfolio with {} positions, total value: {}", 
                    response.getPositions().size(), response.getTotalValue());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error fetching Kraken portfolio for user {}: {}", userId, e.getMessage(), e);
            return createErrorResponse("Failed to fetch Kraken portfolio: " + e.getMessage());
        }
    }
    
    /**
     * Get Kraken account balances.
     * 
     * @param userId User ID
     * @return Account balances
     */
    public Map<String, Object> getKrakenBalances(String userId) {
        log.debug("Fetching Kraken balances for user: {}", userId);
        
        if (krakenApiPortfolioClient == null) {
            log.warn("Kraken API client not available");
            return Map.of("error", "Kraken API client not configured");
        }
        
        try {
            Map<String, Object> balances = krakenApiPortfolioClient.getBalances(userId);
            if (balances == null) {
                return Map.of("error", "No balances found");
            }
            
            // Process and normalize balances
            Map<String, Object> normalizedBalances = new HashMap<>();
            for (Map.Entry<String, Object> entry : balances.entrySet()) {
                String symbol = normalizeSymbol(entry.getKey());
                normalizedBalances.put(symbol, entry.getValue());
            }
            
            return normalizedBalances;
            
        } catch (Exception e) {
            log.error("Error fetching Kraken balances for user {}: {}", userId, e.getMessage(), e);
            return Map.of("error", "Failed to fetch balances: " + e.getMessage());
        }
    }
    
    /**
     * Get Kraken open positions.
     * 
     * @param userId User ID
     * @return Open positions
     */
    public Map<String, Object> getKrakenPositions(String userId) {
        log.debug("Fetching Kraken positions for user: {}", userId);
        
        if (krakenApiPortfolioClient == null) {
            log.warn("Kraken API client not available");
            return Map.of("error", "Kraken API client not configured");
        }
        
        try {
            Map<String, Object> positions = krakenApiPortfolioClient.getOpenPositions(userId);
            return positions != null ? positions : Map.of();
            
        } catch (Exception e) {
            log.error("Error fetching Kraken positions for user {}: {}", userId, e.getMessage(), e);
            return Map.of("error", "Failed to fetch positions: " + e.getMessage());
        }
    }
    
    /**
     * Get Kraken trade history.
     * 
     * @param userId User ID
     * @param limit Number of trades to return
     * @return Trade history
     */
    public Map<String, Object> getKrakenTradeHistory(String userId, Integer limit) {
        log.debug("Fetching Kraken trade history for user: {}, limit: {}", userId, limit);
        
        if (krakenApiPortfolioClient == null) {
            log.warn("Kraken API client not available");
            return Map.of("error", "Kraken API client not configured");
        }
        
        try {
            Map<String, Object> trades = krakenApiPortfolioClient.getTradeHistory(userId);
            
            if (trades != null && trades.containsKey("trades")) {
                // Apply limit if specified
                if (limit != null && limit > 0) {
                    Map<String, Object> tradesMap = (Map<String, Object>) trades.get("trades");
                    Map<String, Object> limitedTrades = tradesMap.entrySet().stream()
                        .limit(limit)
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                        ));
                    trades.put("trades", limitedTrades);
                }
            }
            
            return trades != null ? trades : Map.of();
            
        } catch (Exception e) {
            log.error("Error fetching Kraken trades for user {}: {}", userId, e.getMessage(), e);
            return Map.of("error", "Failed to fetch trades: " + e.getMessage());
        }
    }
    
    /**
     * Refresh Kraken portfolio data.
     * 
     * @param userId User ID
     * @return Success status
     */
    public boolean refreshKrakenData(String userId) {
        log.info("Refreshing Kraken data for user: {}", userId);
        
        try {
            // Fetch fresh data
            ProviderPortfolioResponse freshData = fetchRealTimeKrakenData(userId);
            
            if (freshData != null && freshData.getTotalValue() != null) {
                // Store the fresh data
                storeProviderData(userId, freshData);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error refreshing Kraken data for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fetch real-time Kraken portfolio data.
     */
    private ProviderPortfolioResponse fetchRealTimeKrakenData(String userId) {
        log.info("Fetching Kraken portfolio data for user: {}", userId);
        
        // Get enriched data from Firestore provider_data subcollection
        ProviderDataEntity storedData = readProviderDataRepository.getProviderData(
            userId, ServicePortfolioConstants.PROVIDER_KRAKEN);
        
        if (storedData != null) {
            log.info("Found enriched Kraken data in Firestore for user: {}", userId);
            
            // Map the enriched data to response
            ProviderPortfolioResponse response = mapEnrichedDataToResponse(storedData);
            
            // Update with real-time prices if data is older than 5 minutes
            if (storedData.getLastUpdatedAt() != null) {
                long ageInMillis = System.currentTimeMillis() - storedData.getLastUpdatedAt().toEpochMilli();
                if (ageInMillis > 300000 && krakenApiPortfolioClient != null) { // 5 minutes
                    log.info("Data is {} minutes old, updating prices", ageInMillis / 60000);
                    updateRealTimePrices(response, userId);
                } else {
                    log.info("Returning enriched data (age: {} ms)", ageInMillis);
                }
            }
            
            return response;
        }
        
        // If no enriched data exists, return error
        log.error("No enriched Kraken data found for user: {}", userId);
        return createErrorResponse("No Kraken data available. Please reconnect your Kraken account.");
    }
    
    /**
     * Map enriched Kraken data to portfolio response.
     * This method properly maps all enriched fields including normalized symbols.
     */
    private ProviderPortfolioResponse mapEnrichedDataToResponse(ProviderDataEntity entity) {
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        
        // Basic provider info
        response.setProviderId(entity.getProviderId());
        response.setProviderName(entity.getProviderName());
        response.setAccountType(entity.getAccountType());
        response.setConnected(true);
        response.setSyncStatus(entity.getSyncStatus());
        
        // Portfolio totals
        response.setTotalValue(entity.getTotalValue());
        response.setCashBalance(entity.getCashBalance());
        response.setDayChange(entity.getDayChange());
        response.setDayChangePercent(entity.getDayChangePercent());
        response.setTotalProfitLoss(entity.getTotalProfitLoss());
        response.setTotalProfitLossPercent(entity.getTotalProfitLossPercent());
        
        // Sync metadata
        if (entity.getLastUpdatedAt() != null) {
            response.setLastSynced(entity.getLastUpdatedAt().toEpochMilli());
        } else {
            response.setLastSynced(System.currentTimeMillis());
        }
        
        // Map enriched holdings to positions
        if (entity.getHoldings() != null) {
            List<PortfolioPositionResponse> positions = new ArrayList<>();
            
            for (ProviderDataEntity.Holding holding : entity.getHoldings()) {
                PortfolioPositionResponse position = new PortfolioPositionResponse();
                
                // Use the normalized symbol (BTC instead of XXBT)
                position.setSymbol(holding.getAsset());
                position.setName(holding.getName() != null ? holding.getName() : holding.getAsset());
                position.setQuantity(holding.getQuantity());
                position.setCurrentPrice(holding.getCurrentPrice());
                position.setCurrentValue(holding.getCurrentValue());
                
                // Trading metrics
                position.setAverageBuyPrice(holding.getAverageBuyPrice());
                position.setPriceChange24h(holding.getPriceChange24h());
                position.setCostBasis(holding.getCostBasis());
                position.setProfitLoss(holding.getProfitLoss());
                position.setProfitLossPercent(holding.getProfitLossPercent());
                
                // Enrichment metadata
                position.setAssetType(holding.getAssetType() != null ? 
                    holding.getAssetType() : ServicePortfolioConstants.ASSET_TYPE_CRYPTO);
                position.setProvider(ServicePortfolioConstants.PROVIDER_KRAKEN);
                
                // Additional enriched fields for frontend display
                // Note: setNotes doesn't exist in PortfolioPositionResponse
                // We could add it later if needed
                
                positions.add(position);
            }
            
            response.setPositions(positions);
        } else {
            response.setPositions(new ArrayList<>());
        }
        
        // Map raw balances if needed
        if (entity.getBalances() != null) {
            Map<String, BigDecimal> balances = new HashMap<>();
            entity.getBalances().forEach((key, value) -> {
                if (value instanceof Number) {
                    balances.put(key, new BigDecimal(value.toString()));
                }
            });
            response.setBalances(balances);
        }
        
        return response;
    }
    
    /**
     * Update real-time prices for positions.
     * Only updates prices without modifying the enriched structure.
     */
    private void updateRealTimePrices(ProviderPortfolioResponse response, String userId) {
        if (krakenApiPortfolioClient == null || response.getPositions() == null) {
            return;
        }
        
        try {
            // Get fresh price data from Kraken (for now, skip this until we add the method)
            // TODO: Add getCurrentPrices method to KrakenApiPortfolioClient
            Map<String, BigDecimal> currentPrices = new HashMap<>();
            
            if (currentPrices != null && !currentPrices.isEmpty()) {
                for (PortfolioPositionResponse position : response.getPositions()) {
                    BigDecimal newPrice = currentPrices.get(position.getSymbol());
                    if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
                        position.setCurrentPrice(newPrice);
                        position.setCurrentValue(position.getQuantity().multiply(newPrice));
                        
                        // Recalculate profit/loss if we have cost basis
                        if (position.getCostBasis() != null && position.getCostBasis().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal profitLoss = position.getCurrentValue().subtract(position.getCostBasis());
                            position.setProfitLoss(profitLoss);
                            
                            BigDecimal profitLossPercent = profitLoss.divide(position.getCostBasis(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                            position.setProfitLossPercent(profitLossPercent);
                        }
                    }
                }
                
                // Recalculate totals
                BigDecimal newTotalValue = response.getPositions().stream()
                    .map(PortfolioPositionResponse::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(response.getCashBalance() != null ? response.getCashBalance() : BigDecimal.ZERO);
                
                response.setTotalValue(newTotalValue);
                log.info("Updated real-time prices for {} positions", response.getPositions().size());
            }
        } catch (Exception e) {
            log.warn("Failed to update real-time prices: {}", e.getMessage());
            // Continue with cached prices
        }
    }
    
    /**
     * Get demo Kraken data.
     */
    private ProviderPortfolioResponse getDemoKrakenData(String userId) {
        log.info("Returning demo Kraken data for user: {}", userId);
        
        // First try to get stored demo data
        ProviderDataEntity storedData = readProviderDataRepository.getProviderData(
            userId, ServicePortfolioConstants.PROVIDER_KRAKEN);
        
        if (storedData != null) {
            return mapToProviderResponse(storedData);
        }
        
        // Create demo data
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        response.setProviderId(ServicePortfolioConstants.PROVIDER_KRAKEN);
        response.setProviderName("Kraken (Demo)");
        response.setAccountType("crypto");
        response.setConnected(true);
        response.setSyncStatus("demo");
        response.setLastSynced(System.currentTimeMillis());
        
        // Create demo positions
        List<PortfolioPositionResponse> positions = new ArrayList<>();
        
        // Bitcoin position
        PortfolioPositionResponse btc = new PortfolioPositionResponse();
        btc.setSymbol("BTC");
        btc.setName("Bitcoin");
        btc.setQuantity(new BigDecimal("0.5"));
        btc.setCurrentPrice(new BigDecimal("45000"));
        btc.setCurrentValue(new BigDecimal("22500"));
        btc.setCostBasis(new BigDecimal("20000"));
        btc.setProfitLoss(new BigDecimal("2500"));
        btc.setProfitLossPercent(new BigDecimal("12.5"));
        btc.setAssetType(ServicePortfolioConstants.ASSET_TYPE_CRYPTO);
        btc.setProvider(ServicePortfolioConstants.PROVIDER_KRAKEN);
        positions.add(btc);
        
        // Ethereum position
        PortfolioPositionResponse eth = new PortfolioPositionResponse();
        eth.setSymbol("ETH");
        eth.setName("Ethereum");
        eth.setQuantity(new BigDecimal("5"));
        eth.setCurrentPrice(new BigDecimal("2500"));
        eth.setCurrentValue(new BigDecimal("12500"));
        eth.setCostBasis(new BigDecimal("10000"));
        eth.setProfitLoss(new BigDecimal("2500"));
        eth.setProfitLossPercent(new BigDecimal("25"));
        eth.setAssetType(ServicePortfolioConstants.ASSET_TYPE_CRYPTO);
        eth.setProvider(ServicePortfolioConstants.PROVIDER_KRAKEN);
        positions.add(eth);
        
        response.setPositions(positions);
        response.setTotalValue(new BigDecimal("35000"));
        response.setCashBalance(new BigDecimal("0"));
        response.setDayChange(new BigDecimal("1000"));
        response.setDayChangePercent(new BigDecimal("2.94"));
        response.setTotalProfitLoss(new BigDecimal("5000"));
        response.setTotalProfitLossPercent(new BigDecimal("16.67"));
        
        return response;
    }
    
    /**
     * Get crypto price from cache or API.
     */
    private BigDecimal getCryptoPrice(String symbol) {
        // Check cache first
        Long cachedTime = priceCacheTimestamp.get(symbol);
        if (cachedTime != null && (System.currentTimeMillis() - cachedTime) < PRICE_CACHE_TTL) {
            BigDecimal cachedPrice = priceCache.get(symbol);
            if (cachedPrice != null) {
                return cachedPrice;
            }
        }
        
        // Default prices for common cryptos (would be replaced with real API calls)
        Map<String, BigDecimal> defaultPrices = Map.of(
            "BTC", new BigDecimal("45000"),
            "ETH", new BigDecimal("2500"),
            "XRP", new BigDecimal("0.65"),
            "ADA", new BigDecimal("0.45"),
            "DOT", new BigDecimal("7.50"),
            "SOL", new BigDecimal("35"),
            "LINK", new BigDecimal("15"),
            "MATIC", new BigDecimal("0.85")
        );
        
        BigDecimal price = defaultPrices.getOrDefault(symbol, BigDecimal.ONE);
        
        // Cache the price
        priceCache.put(symbol, price);
        priceCacheTimestamp.put(symbol, System.currentTimeMillis());
        
        return price;
    }
    
    /**
     * Normalize Kraken symbol to standard format.
     */
    private String normalizeSymbol(String krakenSymbol) {
        // Remove .S suffix for staked assets
        String symbol = krakenSymbol.replaceAll("\\.S$", "");
        
        // Apply mapping
        return SYMBOL_MAPPING.getOrDefault(symbol, symbol);
    }
    
    /**
     * Check if symbol is a fiat currency.
     */
    private boolean isFiatCurrency(String symbol) {
        Set<String> fiatCurrencies = Set.of("USD", "EUR", "GBP", "CAD", "JPY", "AUD", "CHF");
        return fiatCurrencies.contains(symbol);
    }
    
    /**
     * Get display name for asset.
     */
    private String getAssetName(String symbol) {
        Map<String, String> assetNames = Map.of(
            "BTC", "Bitcoin",
            "ETH", "Ethereum",
            "XRP", "Ripple",
            "ADA", "Cardano",
            "DOT", "Polkadot",
            "SOL", "Solana",
            "LINK", "Chainlink",
            "MATIC", "Polygon"
        );
        return assetNames.getOrDefault(symbol, symbol);
    }
    
    /**
     * Check if user is in demo mode.
     */
    private boolean isDemoMode(String userId) {
        try {
            return userRepository.findById(userId)
                .map(user -> {
                    if (user.getProfile() != null && user.getProfile().getDemoMode() != null) {
                        return user.getProfile().getDemoMode();
                    }
                    // Default to demo mode if not set
                    return true;
                })
                .orElse(true);
        } catch (Exception e) {
            log.warn("Error checking demo mode for user {}, defaulting to true: {}", userId, e.getMessage());
            return true;
        }
    }
    
    /**
     * Convert balances to BigDecimal map.
     */
    private Map<String, BigDecimal> convertBalances(Map<String, Object> balances) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : balances.entrySet()) {
            String symbol = normalizeSymbol(entry.getKey());
            Object value = entry.getValue();
            
            if (value instanceof Number) {
                result.put(symbol, new BigDecimal(value.toString()));
            } else if (value instanceof String) {
                try {
                    result.put(symbol, new BigDecimal((String) value));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse balance value for {}: {}", symbol, value);
                }
            }
        }
        return result;
    }
    
    /**
     * Store provider data for caching.
     */
    private void storeProviderData(String userId, ProviderPortfolioResponse response) {
        if (createProviderDataRepository == null && updateProviderDataRepository == null) {
            log.debug("Provider data repositories not available, skipping storage");
            return;
        }
        
        try {
            // Create ProviderDataEntity from response
            ProviderDataEntity entity = new ProviderDataEntity();
            entity.setProviderId(ServicePortfolioConstants.PROVIDER_KRAKEN);
            entity.setProviderName(response.getProviderName());
            entity.setAccountType(response.getAccountType());
            entity.setTotalValue(response.getTotalValue());
            entity.setDayChange(response.getDayChange());
            entity.setDayChangePercent(response.getDayChangePercent());
            entity.setTotalProfitLoss(response.getTotalProfitLoss());
            entity.setTotalProfitLossPercent(response.getTotalProfitLossPercent());
            entity.setCashBalance(response.getCashBalance());
            entity.setSyncStatus(response.getSyncStatus());
            entity.setLastUpdatedAt(Instant.now());
            
            // Convert positions to holdings
            if (response.getPositions() != null) {
                List<ProviderDataEntity.Holding> holdings = response.getPositions().stream()
                    .map(this::convertPositionToHolding)
                    .collect(Collectors.toList());
                entity.setHoldings(holdings);
            }
            
            // Convert balances
            if (response.getBalances() != null) {
                Map<String, Object> balances = new HashMap<>();
                response.getBalances().forEach((key, value) -> balances.put(key, value.toString()));
                entity.setBalances(balances);
            }
            
            // Check if entity exists
            ProviderDataEntity existingEntity = readProviderDataRepository.getProviderData(
                userId, ServicePortfolioConstants.PROVIDER_KRAKEN);
            
            if (existingEntity != null && updateProviderDataRepository != null) {
                // Update existing
                entity.setId(existingEntity.getId());
                updateProviderDataRepository.updateProviderData(userId, ServicePortfolioConstants.PROVIDER_KRAKEN, entity);
                log.debug("Updated Kraken provider data for user: {}", userId);
            } else if (createProviderDataRepository != null) {
                // Create new
                createProviderDataRepository.createProviderData(userId, ServicePortfolioConstants.PROVIDER_KRAKEN, entity);
                log.debug("Created Kraken provider data for user: {}", userId);
            }
            
        } catch (Exception e) {
            log.warn("Failed to store provider data: {}", e.getMessage());
        }
    }
    
    /**
     * Convert PortfolioPositionResponse to Holding.
     */
    private ProviderDataEntity.Holding convertPositionToHolding(PortfolioPositionResponse position) {
        ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
        holding.setAsset(position.getSymbol());
        holding.setName(position.getName());
        holding.setQuantity(position.getQuantity());
        holding.setCurrentPrice(position.getCurrentPrice());
        holding.setCurrentValue(position.getCurrentValue());
        holding.setCostBasis(position.getCostBasis());
        holding.setProfitLoss(position.getProfitLoss());
        holding.setProfitLossPercent(position.getProfitLossPercent());
        holding.setAverageBuyPrice(position.getAverageBuyPrice());
        holding.setPriceChange24h(position.getPriceChange24h());
        
        // Store metadata if available
        if (position.getMetadata() != null) {
            Boolean isStaked = (Boolean) position.getMetadata().get("isStaked");
            if (isStaked != null) {
                holding.setIsStaked(isStaked);
            }
            BigDecimal stakingAPR = (BigDecimal) position.getMetadata().get("stakingAPR");
            if (stakingAPR != null) {
                holding.setStakingAPR(stakingAPR);
            }
            String originalSymbol = (String) position.getMetadata().get("originalSymbol");
            if (originalSymbol != null) {
                holding.setOriginalSymbol(originalSymbol);
            }
        }
        
        return holding;
    }
    
    /**
     * Enhance and map ProviderDataEntity to response.
     * Applies the enhancement layer to transform raw Kraken data into user-friendly format.
     */
    private ProviderPortfolioResponse enhanceAndMapToResponse(ProviderDataEntity entity, String userId) {
        try {
            // Convert entity balances and holdings to raw data format for enhancer
            Map<String, Object> rawData = new HashMap<>();
            
            // Add balances
            if (entity.getBalances() != null) {
                rawData.put("balances", entity.getBalances());
            }
            
            // Add holdings if available
            if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
                Map<String, Object> holdings = new HashMap<>();
                for (ProviderDataEntity.Holding holding : entity.getHoldings()) {
                    holdings.put(holding.getAsset(), holding.getQuantity());
                }
                if (!holdings.isEmpty()) {
                    rawData.put("holdings", holdings);
                }
            }
            
            log.info("Enhancing cached data with raw balances: {}", rawData.get("balances"));
            
            // Apply enhancement
            EnhancedPortfolio enhanced = portfolioEnhancer.enhanceProviderPortfolio(
                userId, "kraken", rawData);
            
            log.info("Enhanced cached portfolio - Total value: {}, Assets count: {}", 
                enhanced.getTotalValue(), 
                enhanced.getAssets() != null ? enhanced.getAssets().size() : 0);
            
            // Convert enhanced portfolio to response
            return convertEnhancedToResponse(enhanced);
            
        } catch (Exception e) {
            log.error("Error enhancing cached data, falling back to raw mapping: {}", e.getMessage(), e);
            return mapToProviderResponse(entity);
        }
    }
    
    /**
     * Map ProviderDataEntity to ProviderPortfolioResponse.
     */
    private ProviderPortfolioResponse mapToProviderResponse(ProviderDataEntity entity) {
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        
        response.setProviderId(entity.getProviderId());
        response.setProviderName(entity.getProviderName());
        response.setAccountType(entity.getAccountType());
        response.setConnected(true);
        response.setTotalValue(entity.getTotalValue());
        response.setDayChange(entity.getDayChange());
        response.setDayChangePercent(entity.getDayChangePercent());
        response.setTotalProfitLoss(entity.getTotalProfitLoss());
        response.setTotalProfitLossPercent(entity.getTotalProfitLossPercent());
        response.setCashBalance(entity.getCashBalance());
        response.setSyncStatus(entity.getSyncStatus());
        response.setErrorMessage(entity.getErrorMessage());
        
        // Convert last updated Instant to timestamp
        if (entity.getLastUpdatedAt() != null) {
            response.setLastSynced(entity.getLastUpdatedAt().toEpochMilli());
        } else {
            response.setLastSynced(System.currentTimeMillis());
        }
        
        // Map holdings to positions
        if (entity.getHoldings() != null) {
            List<PortfolioPositionResponse> positions = entity.getHoldings().stream()
                .map(holding -> mapHoldingToPosition(holding, entity.getProviderId()))
                .collect(Collectors.toList());
            response.setPositions(positions);
        } else {
            response.setPositions(new ArrayList<>());
        }
        
        // Map raw balances
        if (entity.getBalances() != null) {
            Map<String, BigDecimal> balances = new HashMap<>();
            entity.getBalances().forEach((key, value) -> {
                if (value instanceof Number) {
                    balances.put(key, new BigDecimal(value.toString()));
                }
            });
            response.setBalances(balances);
        }
        
        return response;
    }
    
    /**
     * Map a Holding to PortfolioPositionResponse.
     */
    private PortfolioPositionResponse mapHoldingToPosition(ProviderDataEntity.Holding holding, String providerId) {
        PortfolioPositionResponse position = new PortfolioPositionResponse();
        
        position.setSymbol(holding.getAsset());
        position.setName(holding.getName());
        position.setQuantity(holding.getQuantity());
        position.setCurrentPrice(holding.getCurrentPrice());
        position.setCurrentValue(holding.getCurrentValue());
        position.setCostBasis(holding.getCostBasis());
        position.setProfitLoss(holding.getProfitLoss());
        position.setProfitLossPercent(holding.getProfitLossPercent());
        position.setAverageBuyPrice(holding.getAverageBuyPrice());
        position.setPriceChange24h(holding.getPriceChange24h());
        position.setProvider(providerId);
        position.setAssetType(ServicePortfolioConstants.ASSET_TYPE_CRYPTO);
        
        // Add metadata for staking if available
        if (holding.getIsStaked() != null && holding.getIsStaked()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("isStaked", true);
            metadata.put("stakingAPR", holding.getStakingAPR());
            metadata.put("originalSymbol", holding.getOriginalSymbol());
            position.setMetadata(metadata);
        }
        
        return position;
    }
    
    /**
     * Create empty response for disconnected provider.
     */
    private ProviderPortfolioResponse createEmptyResponse() {
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        response.setProviderId(ServicePortfolioConstants.PROVIDER_KRAKEN);
        response.setProviderName("Kraken");
        response.setConnected(false);
        response.setTotalValue(BigDecimal.ZERO);
        response.setPositions(new ArrayList<>());
        response.setSyncStatus("disconnected");
        response.setLastSynced(System.currentTimeMillis());
        return response;
    }
    
    /**
     * Create error response.
     */
    private ProviderPortfolioResponse createErrorResponse(String errorMessage) {
        ProviderPortfolioResponse response = createEmptyResponse();
        response.setSyncStatus(ServicePortfolioConstants.SYNC_STATUS_ERROR);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    /**
     * Convert EnhancedPortfolio to ProviderPortfolioResponse.
     */
    private ProviderPortfolioResponse convertEnhancedToResponse(EnhancedPortfolio enhanced) {
        ProviderPortfolioResponse response = new ProviderPortfolioResponse();
        
        response.setProviderId(enhanced.getProviderId());
        response.setProviderName(enhanced.getProviderName());
        response.setAccountType("crypto");
        response.setConnected(true);
        response.setSyncStatus(enhanced.getSyncStatus() != null ? enhanced.getSyncStatus() : "synced");
        response.setLastSynced(enhanced.getLastUpdated() != null ? enhanced.getLastUpdated() : System.currentTimeMillis());
        response.setErrorMessage(enhanced.getErrorMessage());
        
        // Set totals
        response.setTotalValue(enhanced.getTotalValue());
        response.setCashBalance(enhanced.getCashBalance());
        response.setTotalProfitLoss(enhanced.getTotalProfitLoss());
        response.setTotalProfitLossPercent(enhanced.getTotalProfitLossPercent());
        
        // Convert enhanced assets to positions
        List<PortfolioPositionResponse> positions = new ArrayList<>();
        if (enhanced.getAssets() != null) {
            for (EnhancedAsset asset : enhanced.getAssets()) {
                PortfolioPositionResponse position = new PortfolioPositionResponse();
                
                position.setSymbol(asset.getSymbol());
                position.setName(asset.getName());
                position.setQuantity(asset.getQuantity());
                position.setCurrentPrice(asset.getCurrentPrice());
                position.setCurrentValue(asset.getValue());
                position.setCostBasis(asset.getCostBasis() != null ? asset.getCostBasis() : asset.getValue());
                position.setProfitLoss(asset.getProfitLoss() != null ? asset.getProfitLoss() : BigDecimal.ZERO);
                position.setProfitLossPercent(asset.getProfitLossPercent() != null ? asset.getProfitLossPercent() : BigDecimal.ZERO);
                
                // Set average buy price (calculate from cost basis if not available)
                if (asset.getCostBasis() != null && asset.getQuantity() != null && 
                    asset.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avgPrice = asset.getCostBasis().divide(asset.getQuantity(), 2, RoundingMode.HALF_UP);
                    position.setAverageBuyPrice(avgPrice);
                }
                
                // Price change 24h would come from enrichment service
                position.setPriceChange24h(BigDecimal.ZERO); // TODO: Get from ticker API
                
                position.setAssetType(asset.getAssetType());
                position.setProvider(asset.getProvider());
                
                // Add additional metadata if available
                if (asset.isStaked()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("isStaked", true);
                    metadata.put("stakingAPR", asset.getStakingAPR());
                    position.setMetadata(metadata);
                }
                
                positions.add(position);
            }
        }
        response.setPositions(positions);
        
        // Create balances map from enhanced assets
        Map<String, BigDecimal> balances = new HashMap<>();
        if (enhanced.getAssets() != null) {
            for (EnhancedAsset asset : enhanced.getAssets()) {
                // Use raw symbol for balances to maintain original Kraken format
                String balanceKey = asset.getRawSymbol() != null ? asset.getRawSymbol() : asset.getSymbol();
                balances.put(balanceKey, asset.getQuantity());
            }
        }
        response.setBalances(balances);
        
        // Day change (would need historical data for accurate calculation)
        response.setDayChange(BigDecimal.ZERO);
        response.setDayChangePercent(BigDecimal.ZERO);
        
        return response;
    }
}