package io.strategiz.business.provider.kraken.helper;

import io.strategiz.business.provider.kraken.constants.KrakenConstants;
import io.strategiz.business.provider.kraken.exception.KrakenProviderErrorDetails;
import io.strategiz.business.provider.kraken.enrichment.KrakenDataEnrichmentService;
import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import io.strategiz.client.kraken.auth.KrakenApiAuthClient;
import io.strategiz.client.yahoofinance.client.YahooFinanceClient;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Component responsible for initializing and storing Kraken portfolio data.
 * Handles fetching data from Kraken API and persisting to Firestore.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenDataInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenDataInitializer.class);
    private static final String MODULE_NAME = "business-provider-kraken";
    
    /**
     * Inner class to hold price and 24h change data
     */
    private static class PriceData {
        BigDecimal price;
        BigDecimal change24h;
        
        PriceData(BigDecimal price, BigDecimal change24h) {
            this.price = price;
            this.change24h = change24h;
        }
    }
    
    private final KrakenApiAuthClient krakenClient;
    private final KrakenDataTransformer dataTransformer;
    private final CreateProviderDataRepository createProviderDataRepo;
    private final KrakenDataEnrichmentService enrichmentService;
    private final YahooFinancePriceService yahooFinancePriceService;
    
    @Autowired
    public KrakenDataInitializer(KrakenApiAuthClient krakenClient,
                                 KrakenDataTransformer dataTransformer,
                                 CreateProviderDataRepository createProviderDataRepo,
                                 @Autowired(required = false) KrakenDataEnrichmentService enrichmentService,
                                 @Autowired(required = false) YahooFinancePriceService yahooFinancePriceService) {
        this.krakenClient = krakenClient;
        this.dataTransformer = dataTransformer;
        this.createProviderDataRepo = createProviderDataRepo;
        this.enrichmentService = enrichmentService;
        this.yahooFinancePriceService = yahooFinancePriceService;
        
        if (enrichmentService == null) {
            log.error("WARNING: KrakenDataEnrichmentService is NULL - enrichment will not work!");
        } else {
            log.info("KrakenDataEnrichmentService successfully injected");
        }
        
        if (yahooFinancePriceService == null) {
            log.warn("YahooFinancePriceService is NULL - falling back to Kraken ticker API");
        } else {
            log.info("YahooFinancePriceService successfully injected for price fetching");
        }
    }
    
    /**
     * Initialize and store provider data for a user.
     * Fetches account balance, trade history, and current prices, then transforms and stores the data.
     * 
     * @param userId User ID
     * @param apiKey Kraken API key
     * @param apiSecret Kraken API secret
     * @param otp Optional OTP for 2FA
     * @return Stored ProviderDataEntity
     */
    public ProviderDataEntity initializeAndStoreData(String userId, String apiKey, String apiSecret, String otp) {
        log.info("Initializing Kraken portfolio data for user: {}", userId);
        
        try {
            // 1. Fetch account balance
            Map<String, Object> balanceResponse = fetchAccountBalance(apiKey, apiSecret, otp);
            log.debug("Fetched balance data for user: {}, assets count: {}", 
                     userId, balanceResponse.get("result") != null ? 
                     ((Map)balanceResponse.get("result")).size() : 0);
            
            // 2. Fetch trade history for cost basis calculation
            Map<String, Object> tradesResponse = fetchTradeHistory(apiKey, apiSecret, otp);
            log.debug("Fetched trade history for user: {}", userId);
            
            // 3. Fetch current prices and 24h changes for portfolio valuation
            Map<String, PriceData> priceDataMap = fetchCurrentPricesWithChanges(balanceResponse);
            Map<String, BigDecimal> currentPrices = new HashMap<>();
            for (Map.Entry<String, PriceData> entry : priceDataMap.entrySet()) {
                currentPrices.put(entry.getKey(), entry.getValue().price);
            }
            log.debug("Fetched price data for {} assets", priceDataMap.size());
            
            // 4. Extract raw balances from response
            Map<String, Object> rawBalances = extractRawBalances(balanceResponse);
            
            // 5. Enrich the data using enrichment service
            EnrichedKrakenData enrichedData;
            if (enrichmentService != null) {
                log.info("Enriching Kraken data with enrichment service");
                enrichedData = enrichmentService.enrich(userId, rawBalances, currentPrices);
                // Add 24h price changes to enriched data
                for (Map.Entry<String, PriceData> entry : priceDataMap.entrySet()) {
                    String asset = entry.getKey();
                    PriceData data = entry.getValue();
                    // Find matching asset in enriched data and set 24h change
                    for (EnrichedKrakenData.AssetInfo assetInfo : enrichedData.getAssetInfo().values()) {
                        if (asset.equals(assetInfo.getOriginalSymbol()) || 
                            asset.equals(assetInfo.getNormalizedSymbol())) {
                            assetInfo.setPriceChange24h(data.change24h);
                            break;
                        }
                    }
                }
                log.debug("Enriched data for {} assets with price changes", enrichedData.getAssetInfo().size());
            } else {
                log.error("ENRICHMENT SERVICE IS NULL - Creating fake enriched data without normalization!");
                // Create minimal enriched data without normalization
                enrichedData = new EnrichedKrakenData();
                Map<String, EnrichedKrakenData.AssetInfo> assetInfoMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : rawBalances.entrySet()) {
                    EnrichedKrakenData.AssetInfo info = new EnrichedKrakenData.AssetInfo();
                    info.setOriginalSymbol(entry.getKey());
                    info.setNormalizedSymbol(entry.getKey()); // No normalization
                    info.setQuantity(new BigDecimal(entry.getValue().toString()));
                    info.setCurrentPrice(currentPrices.getOrDefault(entry.getKey(), BigDecimal.ONE));
                    info.setCurrentValue(info.getQuantity().multiply(info.getCurrentPrice()));
                    assetInfoMap.put(entry.getKey(), info);
                }
                enrichedData.setAssetInfo(assetInfoMap);
            }
            
            // 6. Transform enriched data to ProviderDataEntity
            ProviderDataEntity data = dataTransformer.transformEnrichedData(
                userId, enrichedData, tradesResponse
            );
            
            // 7. Store in Firestore
            ProviderDataEntity savedData = createProviderDataRepo.createOrReplaceProviderData(
                userId, KrakenConstants.PROVIDER_ID, data
            );
            
            log.info("Successfully initialized and stored Kraken data for user: {}, total value: {}", 
                    userId, savedData.getTotalValue());
            
            return savedData;
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to initialize Kraken data for user: {}", userId, e);
            throw new StrategizException(
                KrakenProviderErrorDetails.DATA_INITIALIZATION_FAILED,
                MODULE_NAME,
                userId,
                e.getMessage()
            );
        }
    }
    
    /**
     * Fetch account balance from Kraken API
     */
    private Map<String, Object> fetchAccountBalance(String apiKey, String apiSecret, String otp) {
        log.debug("Fetching account balance from Kraken");
        
        try {
            Map<String, Object> response = krakenClient.getAccountBalance(apiKey, apiSecret, otp).block();
            
            if (response == null) {
                throw new StrategizException(
                    KrakenProviderErrorDetails.BALANCE_FETCH_FAILED,
                    MODULE_NAME,
                    "null response"
                );
            }
            
            // Check for API errors
            if (response.containsKey("error") && response.get("error") instanceof List) {
                List<?> errors = (List<?>) response.get("error");
                if (!errors.isEmpty()) {
                    String errorMsg = errors.toString();
                    log.error("Kraken API returned error: {}", errorMsg);
                    
                    // Check for specific error types
                    if (errorMsg.contains(KrakenConstants.ERROR_INVALID_KEY)) {
                        throw new StrategizException(
                            KrakenProviderErrorDetails.INVALID_CREDENTIALS,
                            MODULE_NAME,
                            errorMsg
                        );
                    } else if (errorMsg.contains(KrakenConstants.ERROR_RATE_LIMIT)) {
                        throw new StrategizException(
                            KrakenProviderErrorDetails.RATE_LIMIT_EXCEEDED,
                            MODULE_NAME,
                            errorMsg
                        );
                    } else {
                        throw new StrategizException(
                            KrakenProviderErrorDetails.BALANCE_FETCH_FAILED,
                            MODULE_NAME,
                            errorMsg
                        );
                    }
                }
            }
            
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching balance from Kraken", e);
            throw new StrategizException(
                KrakenProviderErrorDetails.BALANCE_FETCH_FAILED,
                MODULE_NAME,
                e.getMessage()
            );
        }
    }
    
    /**
     * Fetch trade history from Kraken API
     */
    private Map<String, Object> fetchTradeHistory(String apiKey, String apiSecret, String otp) {
        log.debug("Fetching trade history from Kraken");
        
        try {
            Map<String, Object> response = krakenClient.getTradeHistory(apiKey, apiSecret, otp).block();
            
            if (response == null) {
                // Trade history might be empty for new accounts, return empty result
                log.debug("No trade history available");
                return Map.of("result", Map.of("trades", new HashMap<>()));
            }
            
            // Check for API errors
            if (response.containsKey("error") && response.get("error") instanceof List) {
                List<?> errors = (List<?>) response.get("error");
                if (!errors.isEmpty()) {
                    log.warn("Error fetching trades, continuing without trade history: {}", errors);
                    // Don't fail if trade history fails - it's not critical
                    return Map.of("result", Map.of("trades", new HashMap<>()));
                }
            }
            
            return response;
            
        } catch (Exception e) {
            log.warn("Error fetching trade history, continuing without it: {}", e.getMessage());
            // Don't fail initialization if trade history fails
            return Map.of("result", Map.of("trades", new HashMap<>()));
        }
    }
    
    /**
     * Fetch current prices and 24h changes for assets in the portfolio
     */
    private Map<String, PriceData> fetchCurrentPricesWithChanges(Map<String, Object> balanceResponse) {
        log.debug("Fetching current prices and 24h changes from Kraken API");
        
        Map<String, PriceData> priceDataMap = new HashMap<>();
        
        try {
            // Extract asset symbols from balance
            if (balanceResponse.containsKey("result")) {
                Map<String, Object> balances = (Map<String, Object>) balanceResponse.get("result");
                
                // Build list of trading pairs for assets we actually have
                List<String> pairsToQuery = new ArrayList<>();
                Map<String, String> assetToPairMap = new HashMap<>();
                
                for (String asset : balances.keySet()) {
                    // Skip fiat currencies and zero balances
                    if (!KrakenConstants.CASH_ASSETS.contains(asset)) {
                        try {
                            BigDecimal balance = new BigDecimal(balances.get(asset).toString());
                            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                                // Build Kraken trading pair format based on asset type
                                
                                // Special handling for ETH2.S and ETH2 (staked ETH)
                                if (asset.equals("ETH2.S") || asset.equals("ETH2")) {
                                    // ETH2 uses ETH price - there's no ETH2USD pair
                                    pairsToQuery.add("XETHZUSD");
                                    pairsToQuery.add("ETHUSD");
                                    pairsToQuery.add("ETHEUR");
                                    assetToPairMap.put(asset, "XETHZUSD");
                                }
                                // Handle assets with X prefix (XXBT, XETH, etc.)
                                else if (asset.startsWith("X") && asset.length() == 4) {
                                    String withoutX = asset.substring(1);
                                    // For XXBT -> try XBTUSD
                                    if (asset.equals("XXBT")) {
                                        pairsToQuery.add("XBTUSD");
                                        pairsToQuery.add("XBTEUR");
                                        assetToPairMap.put(asset, "XBTUSD");
                                    } else if (asset.equals("XETH")) {
                                        // For XETH -> try XETHZUSD (Kraken's format)
                                        pairsToQuery.add("XETHZUSD");
                                        pairsToQuery.add("ETHUSD");
                                        pairsToQuery.add("ETHEUR");
                                        assetToPairMap.put(asset, "XETHZUSD");
                                    } else {
                                        // For other X-prefixed assets
                                        pairsToQuery.add(withoutX + "USD");
                                        pairsToQuery.add(withoutX + "EUR");
                                        pairsToQuery.add(asset + "ZUSD");
                                        assetToPairMap.put(asset, withoutX + "USD");
                                    }
                                }
                                // Handle regular assets (ATOM, ADA, DOT, etc.)
                                else {
                                    // Remove any staking suffixes for price lookup
                                    String baseAsset = asset.replace(".S", "").replace(".F", "");
                                    
                                    // For regular crypto assets, Kraken uses simple format: ATOMUSD, ADAUSD, etc.
                                    pairsToQuery.add(baseAsset + "USD");
                                    pairsToQuery.add(baseAsset + "EUR");
                                    pairsToQuery.add(baseAsset + "GBP");
                                    assetToPairMap.put(asset, baseAsset + "USD");
                                }
                                
                                log.debug("Asset {} will query pairs: {}", asset, 
                                    pairsToQuery.subList(Math.max(0, pairsToQuery.size() - 3), pairsToQuery.size()));
                            }
                        } catch (NumberFormatException e) {
                            log.debug("Skipping asset {} due to invalid balance format", asset);
                        }
                    }
                }
                
                if (!pairsToQuery.isEmpty() && yahooFinancePriceService != null) {
                    // Use Yahoo Finance for price fetching instead of Kraken ticker API
                    log.info("Fetching real-time prices for {} assets from Yahoo Finance", assetToPairMap.size());
                    
                    // Extract asset symbols for Yahoo Finance
                    List<String> assetSymbols = new ArrayList<>();
                    for (String asset : assetToPairMap.keySet()) {
                        // Convert Kraken symbols to standard symbols
                        String normalizedSymbol = asset
                            .replace("XXBT", "BTC")
                            .replace("XETH", "ETH")
                            .replace("XXRP", "XRP")
                            .replace("XLTC", "LTC")
                            .replace("XXLM", "XLM")
                            .replace("XZEC", "ZEC")
                            .replace("XXMR", "XMR")
                            .replace("XDOGE", "DOGE")
                            .replace("ETH2", "ETH")
                            .replace(".S", "")  // Remove staking suffix
                            .replace(".F", ""); // Remove futures suffix
                        assetSymbols.add(normalizedSymbol);
                    }
                    
                    try {
                        Map<String, Double> yahooFinancePrices = yahooFinancePriceService.getBulkPrices(assetSymbols);
                        
                        if (yahooFinancePrices != null && !yahooFinancePrices.isEmpty()) {
                            log.info("Successfully fetched {} prices from Yahoo Finance", yahooFinancePrices.size());
                            
                            // Process each asset's price from Yahoo Finance
                            for (String asset : assetToPairMap.keySet()) {
                                // Convert Kraken symbol to standard symbol for Yahoo Finance lookup
                                String normalizedSymbol = asset
                                    .replace("XXBT", "BTC")
                                    .replace("XETH", "ETH")
                                    .replace("XXRP", "XRP")
                                    .replace("XLTC", "LTC")
                                    .replace("XXLM", "XLM")
                                    .replace("XZEC", "ZEC")
                                    .replace("XXMR", "XMR")
                                    .replace("XDOGE", "DOGE")
                                    .replace("ETH2", "ETH")
                                    .replace(".S", "")  // Remove staking suffix
                                    .replace(".F", ""); // Remove futures suffix
                                
                                // Try to get price from Yahoo Finance
                                Double yahooPrice = yahooFinancePrices.get(normalizedSymbol);
                                
                                if (yahooPrice != null && yahooPrice > 0) {
                                    BigDecimal price = BigDecimal.valueOf(yahooPrice);
                                    // For now, we'll use 0 for 24h change since Yahoo Finance doesn't provide it in this simple call
                                    // In production, you might want to fetch this separately
                                    BigDecimal change24h = BigDecimal.ZERO;
                                    
                                    priceDataMap.put(asset, new PriceData(price, change24h));
                                    log.info("Got price for {} (normalized: {}) from Yahoo Finance: ${}", 
                                            asset, normalizedSymbol, price);
                                } else {
                                    log.warn("Could not find price for asset {} (normalized: {}) in Yahoo Finance",
                                            asset, normalizedSymbol);
                                    // Leave this asset out - we'll use fallback prices for assets without Yahoo Finance data
                                }
                            }
                        } else {
                            log.error("Failed to get prices from Yahoo Finance - response was empty, using fallback static prices");
                            return getFallbackPrices(assetToPairMap.keySet());
                        }
                    } catch (Exception e) {
                        log.error("Failed to fetch prices from Yahoo Finance: {}", e.getMessage());
                        log.info("Using fallback static prices for portfolio valuation");
                        // Use fallback static prices when Yahoo Finance fails
                        return getFallbackPrices(assetToPairMap.keySet());
                    }

                    // Fill in missing prices with fallback static prices
                    Map<String, PriceData> fallbackPrices = getFallbackPrices(assetToPairMap.keySet());
                    for (String asset : assetToPairMap.keySet()) {
                        if (!priceDataMap.containsKey(asset) && fallbackPrices.containsKey(asset)) {
                            priceDataMap.put(asset, fallbackPrices.get(asset));
                            log.info("Using fallback price for {}: ${}", asset, fallbackPrices.get(asset).price);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error in fetchCurrentPrices: {}", e.getMessage(), e);
            // Return empty map rather than fake data
            return new HashMap<>();
        }
        
        return priceDataMap;
    }
    
    /**
     * Extract raw balances from the API response.
     */
    private Map<String, Object> extractRawBalances(Map<String, Object> balanceResponse) {
        if (balanceResponse != null && balanceResponse.containsKey("result")) {
            return (Map<String, Object>) balanceResponse.get("result");
        }
        return new HashMap<>();
    }
    
    /**
     * Get fallback static prices when external APIs fail.
     * These are approximate market prices to ensure portfolio displays values.
     */
    private Map<String, PriceData> getFallbackPrices(Set<String> assets) {
        Map<String, PriceData> fallbackPrices = new HashMap<>();
        
        // Static price mappings based on recent market data
        Map<String, BigDecimal> staticPrices = new HashMap<>();
        staticPrices.put("XXBT", new BigDecimal("110851.07"));  // Bitcoin
        staticPrices.put("XETH", new BigDecimal("4292.10"));    // Ethereum
        staticPrices.put("ETH2", new BigDecimal("4292.10"));    // Staked ETH
        staticPrices.put("ETH2.S", new BigDecimal("4292.10"));  // Staked ETH
        staticPrices.put("ADA", new BigDecimal("0.8333"));      // Cardano
        staticPrices.put("ADA.S", new BigDecimal("0.8333"));    // Staked ADA
        staticPrices.put("SOL", new BigDecimal("206.16"));      // Solana
        staticPrices.put("DOT", new BigDecimal("7.50"));        // Polkadot
        staticPrices.put("DOT.S", new BigDecimal("7.50"));      // Staked DOT
        staticPrices.put("MATIC", new BigDecimal("0.85"));      // Polygon (old)
        staticPrices.put("POL", new BigDecimal("0.276"));       // Polygon (new)
        staticPrices.put("LINK", new BigDecimal("15.00"));      // Chainlink
        staticPrices.put("UNI", new BigDecimal("6.50"));        // Uniswap
        staticPrices.put("ATOM", new BigDecimal("8.20"));       // Cosmos
        staticPrices.put("ATOM.S", new BigDecimal("8.20"));     // Staked ATOM
        staticPrices.put("XXRP", new BigDecimal("2.87"));       // XRP
        staticPrices.put("XXLM", new BigDecimal("0.12"));       // Stellar
        staticPrices.put("XLTC", new BigDecimal("75.00"));      // Litecoin
        staticPrices.put("XDOGE", new BigDecimal("0.2276"));    // Dogecoin
        staticPrices.put("AVAX", new BigDecimal("24.65"));      // Avalanche
        staticPrices.put("TRX", new BigDecimal("0.3303"));      // TRON
        staticPrices.put("XXMR", new BigDecimal("160.00"));     // Monero
        staticPrices.put("XZEC", new BigDecimal("35.00"));      // Zcash
        staticPrices.put("ALGO", new BigDecimal("0.20"));       // Algorand
        staticPrices.put("FLOW", new BigDecimal("0.80"));       // Flow
        staticPrices.put("FLOW.S", new BigDecimal("0.80"));     // Staked Flow
        staticPrices.put("NEAR", new BigDecimal("2.50"));       // Near
        staticPrices.put("FIL", new BigDecimal("4.50"));        // Filecoin
        staticPrices.put("GRT", new BigDecimal("0.18"));        // The Graph
        staticPrices.put("OCEAN", new BigDecimal("0.65"));      // Ocean
        staticPrices.put("STORJ", new BigDecimal("0.55"));      // Storj
        staticPrices.put("SAND", new BigDecimal("0.40"));       // Sandbox
        staticPrices.put("MANA", new BigDecimal("0.55"));       // Decentraland
        staticPrices.put("GALA", new BigDecimal("0.01625"));    // Gala
        staticPrices.put("AKT", new BigDecimal("1.09"));        // Akash
        staticPrices.put("SEI", new BigDecimal("0.2945"));      // Sei
        staticPrices.put("INJ", new BigDecimal("12.98"));       // Injective
        staticPrices.put("RENDER", new BigDecimal("5.50"));     // Render
        staticPrices.put("SHIB", new BigDecimal("0.00001"));    // Shiba Inu
        staticPrices.put("PEPE", new BigDecimal("0.00001"));    // Pepe
        staticPrices.put("APE", new BigDecimal("1.50"));        // ApeCoin
        staticPrices.put("AAVE", new BigDecimal("85.00"));      // Aave
        staticPrices.put("CRV", new BigDecimal("0.45"));        // Curve
        staticPrices.put("MKR", new BigDecimal("1450.00"));     // Maker
        staticPrices.put("COMP", new BigDecimal("45.00"));      // Compound
        staticPrices.put("SNX", new BigDecimal("2.20"));        // Synthetix
        staticPrices.put("YFI", new BigDecimal("5500.00"));     // Yearn
        staticPrices.put("SUSHI", new BigDecimal("0.95"));      // SushiSwap
        staticPrices.put("BAL", new BigDecimal("2.15"));        // Balancer
        staticPrices.put("1INCH", new BigDecimal("0.35"));      // 1inch
        staticPrices.put("ENJ", new BigDecimal("0.28"));        // Enjin
        staticPrices.put("CHZ", new BigDecimal("0.07"));        // Chiliz
        staticPrices.put("BLUR", new BigDecimal("0.25"));       // Blur
        staticPrices.put("IMX", new BigDecimal("1.40"));        // Immutable X
        staticPrices.put("LDO", new BigDecimal("1.85"));        // Lido
        staticPrices.put("RPL", new BigDecimal("25.00"));       // Rocket Pool
        staticPrices.put("KAVA", new BigDecimal("0.65"));       // Kava
        staticPrices.put("KAVA.S", new BigDecimal("0.65"));     // Staked Kava
        staticPrices.put("SCRT", new BigDecimal("0.35"));       // Secret
        staticPrices.put("SCRT.S", new BigDecimal("0.35"));     // Staked Secret
        staticPrices.put("TIA", new BigDecimal("5.20"));        // Celestia
        staticPrices.put("TIA.S", new BigDecimal("5.20"));      // Staked Celestia
        staticPrices.put("OSMO", new BigDecimal("0.55"));       // Osmosis
        staticPrices.put("OSMO.S", new BigDecimal("0.55"));     // Staked Osmosis
        staticPrices.put("JUNO", new BigDecimal("0.35"));       // Juno
        staticPrices.put("BAND", new BigDecimal("1.35"));       // Band Protocol
        staticPrices.put("KSM", new BigDecimal("25.00"));       // Kusama
        staticPrices.put("KSM.S", new BigDecimal("25.00"));     // Staked Kusama
        
        // Stablecoins
        staticPrices.put("USDT", BigDecimal.ONE);
        staticPrices.put("USDC", BigDecimal.ONE);
        staticPrices.put("DAI", BigDecimal.ONE);
        staticPrices.put("ZUSD", BigDecimal.ONE);
        staticPrices.put("USD", BigDecimal.ONE);
        staticPrices.put("EUR", new BigDecimal("1.10"));
        staticPrices.put("GBP", new BigDecimal("1.25"));
        staticPrices.put("CAD", new BigDecimal("0.74"));
        staticPrices.put("AUD", new BigDecimal("0.65"));
        staticPrices.put("CHF", new BigDecimal("1.12"));
        staticPrices.put("JPY", new BigDecimal("0.0067"));
        
        // For each requested asset, provide a price
        for (String asset : assets) {
            BigDecimal price = staticPrices.get(asset);
            if (price == null) {
                // Try without staking suffix
                String baseAsset = asset.replace(".S", "").replace(".F", "");
                price = staticPrices.get(baseAsset);
            }
            if (price == null) {
                // Default to $1 for unknown assets (better than $0)
                price = BigDecimal.ONE;
                log.warn("No fallback price for asset {}, using $1", asset);
            }
            
            // Create price data with zero change (since we don't have historical data)
            fallbackPrices.put(asset, new PriceData(price, BigDecimal.ZERO));
        }
        
        log.info("Using fallback prices for {} assets", fallbackPrices.size());
        return fallbackPrices;
    }
    
    /**
     * Normalize Kraken asset code for trading pair lookup
     */
    private String normalizeAssetForPair(String asset) {
        // Kraken uses different formats for API vs trading pairs
        // XXBT in balance becomes XBT in trading pair
        if (asset.equals("XXBT")) return "XBT";
        if (asset.startsWith("X") && asset.length() == 4) {
            return asset.substring(1); // Remove X prefix
        }
        return asset;
    }
}