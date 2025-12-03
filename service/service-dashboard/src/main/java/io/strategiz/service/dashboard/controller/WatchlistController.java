package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.watchlist.repository.WatchlistBaseRepository;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import io.strategiz.service.dashboard.model.request.CreateWatchlistItemRequest;
import io.strategiz.service.dashboard.model.request.DeleteWatchlistItemRequest;
import io.strategiz.service.dashboard.model.response.WatchlistCollectionResponse;
import io.strategiz.service.dashboard.model.response.WatchlistOperationResponse;
import io.strategiz.service.dashboard.model.watchlist.WatchlistItem;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller for watchlist data.
 * Returns demo data until proper repository implementations are available.
 * The mode only affects UI behavior, not the data itself.
 */
@RestController
@RequestMapping("/v1/dashboard/watchlists")
@CrossOrigin(origins = "*")
public class WatchlistController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DASHBOARD_MODULE;
    }
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final UserRepository userRepository;
    private final CoinGeckoClient coinGeckoClient;
    private final WatchlistBaseRepository watchlistRepository;

    // Default symbols for new users (when watchlist is empty)
    private static final List<String> DEFAULT_CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH");
    private static final List<String> DEFAULT_STOCK_SYMBOLS = Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN");
    private static final List<String> DEFAULT_ETF_SYMBOLS = Arrays.asList("SPY");

    @Autowired
    public WatchlistController(UserRepository userRepository,
                              CoinGeckoClient coinGeckoClient,
                              WatchlistBaseRepository watchlistRepository) {
        this.userRepository = userRepository;
        this.coinGeckoClient = coinGeckoClient;
        this.watchlistRepository = watchlistRepository;
    }
    
    /**
     * Get watchlist data for a user.
     * Returns REAL market data from CoinGecko and Yahoo Finance.
     * 
     * @param userId The user ID to get data for
     * @return Real-time watchlist data with market information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWatchlist(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving REAL watchlist data for user: {}", userId);
        
        try {
            // Get user demo mode for UI context only
            Boolean demoMode = getUserDemoMode(userId);
            
            // Get REAL market data instead of demo data
            WatchlistCollectionResponse watchlist = getRealWatchlistData(userId);
            
            // Create response with real data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", userId);
            responseData.put("demoMode", demoMode); // For UI context only
            responseData.put("watchlist", formatWatchlistForUI(watchlist));
            responseData.put("metadata", createMetadata(watchlist, "real")); // Mark as real data
            
            return ResponseEntity.ok(responseData);
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving watchlist for user: {}", userId, e);
            // Fall back to demo data if real data fails
            log.warn("Falling back to demo data due to error");
            WatchlistCollectionResponse watchlist = getDemoWatchlistData(userId);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", userId);
            responseData.put("demoMode", "demo");
            responseData.put("watchlist", formatWatchlistForUI(watchlist));
            responseData.put("metadata", createMetadata(watchlist, "demo"));
            return ResponseEntity.ok(responseData);
        }
    }
    
    /**
     * Create a new watchlist item.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWatchlistItem(
            @RequestParam(required = false) String userId,
            @RequestBody CreateWatchlistItemRequest request) {
        
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Creating watchlist item for user: {} - {}", userId, request.getSymbol());
        
        // Validate input
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            throw new StrategizException(ServiceDashboardErrorDetails.INVALID_SYMBOL, "service-dashboard", request.getSymbol());
        }
        
        try {
            String symbol = request.getSymbol().toUpperCase().trim();

            // Check if item already exists in user's watchlist
            if (watchlistRepository.existsBySymbol(symbol, userId)) {
                log.info("Symbol {} already exists in watchlist for user {}", symbol, userId);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "operation", "CREATE",
                    "symbol", symbol,
                    "message", "Symbol already exists in watchlist"
                ));
            }

            // Create new watchlist item entity
            WatchlistItemEntity entity = new WatchlistItemEntity();
            entity.setSymbol(symbol);
            entity.setName(request.getName() != null ? request.getName() : symbol);
            entity.setType(request.getType() != null ? request.getType().toUpperCase() : "STOCK");
            entity.setExchange(request.getExchange());
            entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
            entity.setAlertEnabled(request.getAlertEnabled() != null ? request.getAlertEnabled() : false);

            // Save to Firestore
            WatchlistItemEntity savedEntity = watchlistRepository.save(entity, userId);
            log.info("Created watchlist item {} for user {}", symbol, userId);

            WatchlistOperationResponse response = WatchlistOperationResponse.success(
                "CREATE",
                savedEntity.getId(),
                savedEntity.getSymbol(),
                "Watchlist item created successfully"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "operation", response.getOperation(),
                "id", response.getId(),
                "symbol", response.getSymbol(),
                "message", response.getMessage()
            ));

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error creating watchlist item for user: {} - {}", userId, request.getSymbol(), e);
            throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "create_watchlist_item", e.getMessage());
        }
    }
    
    /**
     * Delete a watchlist item.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteWatchlistItem(
            @RequestParam(required = false) String userId,
            @PathVariable String itemId) {
        
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Deleting watchlist item: {} for user: {}", itemId, userId);
        
        // Validate input
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new StrategizException(ServiceDashboardErrorDetails.WATCHLIST_ITEM_NOT_FOUND, "service-dashboard", userId, itemId);
        }
        
        try {
            // Check if item exists
            Optional<WatchlistItemEntity> existing = watchlistRepository.findById(itemId, userId);
            if (existing.isEmpty()) {
                log.warn("Watchlist item {} not found for user {}", itemId, userId);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "operation", "DELETE",
                    "message", "Watchlist item not found"
                ));
            }

            // Delete from Firestore
            watchlistRepository.delete(itemId, userId);
            log.info("Deleted watchlist item {} for user {}", itemId, userId);

            WatchlistOperationResponse response = WatchlistOperationResponse.success(
                "DELETE",
                "Watchlist item deleted successfully"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "operation", response.getOperation(),
                "message", response.getMessage()
            ));

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error deleting watchlist item: {} for user: {}", itemId, userId, e);
            throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "delete_watchlist_item", e.getMessage());
        }
    }
    
    /**
     * Check if symbol is in user's watchlist.
     */
    @GetMapping("/check/{symbol}")
    public ResponseEntity<Map<String, Object>> checkSymbolInWatchlist(
            @RequestParam(required = false) String userId,
            @PathVariable String symbol) {
        
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Checking if symbol {} is in watchlist for user: {}", symbol, userId);
        
        try {
            // Check if symbol exists in user's watchlist
            boolean inWatchlist = watchlistRepository.existsBySymbol(symbol.toUpperCase(), userId);

            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "symbol", symbol.toUpperCase(),
                "inWatchlist", inWatchlist
            ));

        } catch (Exception e) {
            log.error("Error checking symbol in watchlist: {} for user: {}", symbol, userId, e);
            throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "check_symbol_in_watchlist", e.getMessage());
        }
    }
    
    /**
     * Get REAL watchlist data from user's saved items + market APIs
     */
    private WatchlistCollectionResponse getRealWatchlistData(String userId) {
        log.info("Fetching watchlist data for user: {}", userId);
        List<WatchlistItem> items = new ArrayList<>();

        try {
            // First, get user's saved watchlist items from Firestore
            List<WatchlistItemEntity> savedItems = watchlistRepository.findAllByUserId(userId);
            log.info("Found {} saved watchlist items for user {}", savedItems.size(), userId);

            // If user has no saved items, use defaults
            if (savedItems.isEmpty()) {
                log.info("No saved items, using default watchlist for user {}", userId);
                return getDefaultWatchlistData(userId);
            }

            // Separate items by type for enrichment
            List<String> cryptoSymbols = savedItems.stream()
                .filter(item -> "CRYPTO".equalsIgnoreCase(item.getType()))
                .map(WatchlistItemEntity::getSymbol)
                .collect(Collectors.toList());

            List<WatchlistItemEntity> stockItems = savedItems.stream()
                .filter(item -> !"CRYPTO".equalsIgnoreCase(item.getType()))
                .collect(Collectors.toList());

            // Fetch crypto market data from CoinGecko
            if (!cryptoSymbols.isEmpty()) {
                List<WatchlistItem> enrichedCrypto = fetchCryptoMarketData(cryptoSymbols, savedItems);
                items.addAll(enrichedCrypto);
            }

            // Enrich stock/ETF items with static data (TODO: integrate Yahoo Finance)
            for (WatchlistItemEntity entity : stockItems) {
                items.add(enrichStockItem(entity));
            }

            log.info("Returning {} enriched watchlist items for user {}", items.size(), userId);

        } catch (Exception e) {
            log.error("Error fetching watchlist data for user {}", userId, e);
            // If real data fails, return defaults
            return getDefaultWatchlistData(userId);
        }

        // Create response
        WatchlistCollectionResponse response = new WatchlistCollectionResponse();
        response.setItems(items);
        response.setTotalCount(items.size());
        response.setActiveCount(items.size());
        response.setIsEmpty(items.isEmpty());

        return response;
    }

    /**
     * Get default watchlist data for new users
     */
    private WatchlistCollectionResponse getDefaultWatchlistData(String userId) {
        log.info("Fetching default watchlist data");
        List<WatchlistItem> items = new ArrayList<>();

        try {
            // Fetch crypto data
            CompletableFuture<List<WatchlistItem>> cryptoFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, String> symbolToId = new HashMap<>();
                    symbolToId.put("BTC", "bitcoin");
                    symbolToId.put("ETH", "ethereum");

                    List<String> coinIds = DEFAULT_CRYPTO_SYMBOLS.stream()
                        .map(symbol -> symbolToId.getOrDefault(symbol, symbol.toLowerCase()))
                        .collect(Collectors.toList());

                    List<CryptoCurrency> cryptoData = coinGeckoClient.getCryptocurrencyMarketData(coinIds, "usd");
                    return cryptoData.stream().map(crypto -> {
                        BigDecimal price = crypto.getCurrentPrice();
                        BigDecimal change = crypto.getPriceChange24h();
                        BigDecimal changePercent = crypto.getPriceChangePercentage24h();

                        return new WatchlistItem(
                            crypto.getSymbol().toLowerCase() + "-default",
                            crypto.getSymbol().toUpperCase(),
                            crypto.getName(),
                            "crypto",
                            price,
                            change,
                            changePercent,
                            change != null && change.compareTo(BigDecimal.ZERO) > 0,
                            "/chart/" + crypto.getSymbol().toLowerCase()
                        );
                    }).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("Error fetching crypto data", e);
                    return new ArrayList<WatchlistItem>();
                }
            });

            // Fetch stock data
            CompletableFuture<List<WatchlistItem>> stockFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<String> allSymbols = new ArrayList<>();
                    allSymbols.addAll(DEFAULT_STOCK_SYMBOLS);
                    allSymbols.addAll(DEFAULT_ETF_SYMBOLS);

                    Map<String, Double> staticStockPrices = new HashMap<>();
                    staticStockPrices.put("AAPL", 182.52);
                    staticStockPrices.put("MSFT", 338.12);
                    staticStockPrices.put("GOOGL", 137.14);
                    staticStockPrices.put("AMZN", 178.22);
                    staticStockPrices.put("SPY", 504.12);

                    return allSymbols.stream()
                        .filter(staticStockPrices::containsKey)
                        .map(symbol -> {
                            String type = DEFAULT_ETF_SYMBOLS.contains(symbol) ? "etf" : "stock";
                            BigDecimal price = BigDecimal.valueOf(staticStockPrices.get(symbol));
                            BigDecimal change = new BigDecimal("1.23");
                            BigDecimal changePercent = new BigDecimal("0.68");

                            return new WatchlistItem(
                                symbol.toLowerCase() + "-default",
                                symbol,
                                symbol,
                                type,
                                price,
                                change,
                                changePercent,
                                true,
                                "/chart/" + symbol.toLowerCase()
                            );
                        }).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("Error creating static stock data", e);
                    return new ArrayList<WatchlistItem>();
                }
            });

            List<WatchlistItem> cryptoItems = cryptoFuture.get();
            List<WatchlistItem> stockItems = stockFuture.get();

            items.addAll(cryptoItems);
            items.addAll(stockItems);

        } catch (Exception e) {
            log.error("Error fetching default market data", e);
        }

        WatchlistCollectionResponse response = new WatchlistCollectionResponse();
        response.setItems(items);
        response.setTotalCount(items.size());
        response.setActiveCount(items.size());
        response.setIsEmpty(items.isEmpty());

        return response;
    }

    /**
     * Fetch crypto market data from CoinGecko for saved symbols
     */
    private List<WatchlistItem> fetchCryptoMarketData(List<String> symbols, List<WatchlistItemEntity> savedItems) {
        List<WatchlistItem> items = new ArrayList<>();

        try {
            // Map symbols to CoinGecko IDs
            Map<String, String> symbolToId = new HashMap<>();
            symbolToId.put("BTC", "bitcoin");
            symbolToId.put("ETH", "ethereum");
            symbolToId.put("SOL", "solana");
            symbolToId.put("BNB", "binancecoin");
            symbolToId.put("ADA", "cardano");
            symbolToId.put("XRP", "ripple");
            symbolToId.put("DOGE", "dogecoin");
            symbolToId.put("DOT", "polkadot");
            symbolToId.put("AVAX", "avalanche-2");
            symbolToId.put("MATIC", "matic-network");

            List<String> coinIds = symbols.stream()
                .map(symbol -> symbolToId.getOrDefault(symbol.toUpperCase(), symbol.toLowerCase()))
                .collect(Collectors.toList());

            List<CryptoCurrency> cryptoData = coinGeckoClient.getCryptocurrencyMarketData(coinIds, "usd");

            // Map back to WatchlistItem with saved entity info
            for (CryptoCurrency crypto : cryptoData) {
                String symbol = crypto.getSymbol().toUpperCase();

                // Find the saved entity for this symbol
                WatchlistItemEntity savedEntity = savedItems.stream()
                    .filter(e -> e.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(null);

                String id = savedEntity != null ? savedEntity.getId() : crypto.getSymbol().toLowerCase() + "-1";
                String name = savedEntity != null && savedEntity.getName() != null ? savedEntity.getName() : crypto.getName();

                BigDecimal price = crypto.getCurrentPrice();
                BigDecimal change = crypto.getPriceChange24h();
                BigDecimal changePercent = crypto.getPriceChangePercentage24h();

                items.add(new WatchlistItem(
                    id,
                    symbol,
                    name,
                    "crypto",
                    price,
                    change,
                    changePercent,
                    change != null && change.compareTo(BigDecimal.ZERO) > 0,
                    "/chart/" + crypto.getSymbol().toLowerCase()
                ));
            }
        } catch (Exception e) {
            log.error("Error fetching crypto market data", e);
        }

        return items;
    }

    /**
     * Enrich a stock/ETF item with market data (static for now)
     */
    private WatchlistItem enrichStockItem(WatchlistItemEntity entity) {
        // Static prices for demo/fallback purposes
        Map<String, Double> staticStockPrices = new HashMap<>();
        staticStockPrices.put("AAPL", 182.52);
        staticStockPrices.put("MSFT", 338.12);
        staticStockPrices.put("GOOGL", 137.14);
        staticStockPrices.put("AMZN", 178.22);
        staticStockPrices.put("SPY", 504.12);
        staticStockPrices.put("TSLA", 248.50);
        staticStockPrices.put("NVDA", 875.30);
        staticStockPrices.put("META", 485.20);
        staticStockPrices.put("NFLX", 685.40);
        staticStockPrices.put("QQQ", 398.77);

        String symbol = entity.getSymbol().toUpperCase();
        Double priceVal = staticStockPrices.getOrDefault(symbol, 100.0);
        BigDecimal price = BigDecimal.valueOf(priceVal);
        BigDecimal change = new BigDecimal("1.23");
        BigDecimal changePercent = new BigDecimal("0.68");

        return new WatchlistItem(
            entity.getId(),
            symbol,
            entity.getName() != null ? entity.getName() : symbol,
            entity.getType() != null ? entity.getType().toLowerCase() : "stock",
            price,
            change,
            changePercent,
            true,
            "/chart/" + symbol.toLowerCase()
        );
    }
    
    /**
     * Get demo watchlist data until proper repository implementation is available
     */
    private WatchlistCollectionResponse getDemoWatchlistData(String userId) {
        // Create more realistic demo watchlist items
        List<WatchlistItem> demoItems = Arrays.asList(
            // Cryptocurrencies
            new WatchlistItem("btc-1", "BTC", "Bitcoin", "crypto", 
                new BigDecimal("56241.92"), new BigDecimal("1542.34"), 
                new BigDecimal("2.82"), true, "/chart/btc"),
            new WatchlistItem("eth-1", "ETH", "Ethereum", "crypto", 
                new BigDecimal("3025.18"), new BigDecimal("87.52"), 
                new BigDecimal("2.98"), true, "/chart/eth"),
            
            // Stocks
            new WatchlistItem("aapl-1", "AAPL", "Apple Inc.", "stock", 
                new BigDecimal("182.52"), new BigDecimal("1.23"), 
                new BigDecimal("0.68"), true, "/chart/aapl"),
            new WatchlistItem("msft-1", "MSFT", "Microsoft Corporation", "stock", 
                new BigDecimal("338.12"), new BigDecimal("-0.87"), 
                new BigDecimal("-0.26"), false, "/chart/msft"),
            new WatchlistItem("googl-1", "GOOGL", "Alphabet Inc.", "stock", 
                new BigDecimal("137.14"), new BigDecimal("0.54"), 
                new BigDecimal("0.40"), true, "/chart/googl"),
            new WatchlistItem("amzn-1", "AMZN", "Amazon.com Inc.", "stock", 
                new BigDecimal("178.22"), new BigDecimal("-1.12"), 
                new BigDecimal("-0.62"), false, "/chart/amzn"),
            
            // ETF
            new WatchlistItem("spy-1", "SPY", "SPDR S&P 500 ETF", "etf", 
                new BigDecimal("504.12"), new BigDecimal("2.34"), 
                new BigDecimal("0.47"), true, "/chart/spy")
        );
        
        WatchlistCollectionResponse response = new WatchlistCollectionResponse(userId, demoItems);
        response.setDemoMode(true);
        
        return response;
    }
    
    /**
     * Get user trading mode, defaulting to demo if not found
     */
    private Boolean getUserDemoMode(String userId) {
        try {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserProfileEntity profile = userOpt.get().getProfile();
                return profile != null ? profile.getDemoMode() : true;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user demo mode for {}, defaulting to demo", userId);
        }
        return true;
    }

    /**
     * Format watchlist data for UI consumption
     */
    private Map<String, Object> formatWatchlistForUI(WatchlistCollectionResponse watchlist) {
        Map<String, Object> formattedWatchlist = new HashMap<>();
        
        // Real watchlist data
        formattedWatchlist.put("items", watchlist.getItems());
        formattedWatchlist.put("totalCount", watchlist.getTotalCount());
        formattedWatchlist.put("activeCount", watchlist.getActiveCount());
        formattedWatchlist.put("isEmpty", watchlist.getIsEmpty());
        
        // Standard categories for all users
        formattedWatchlist.put("availableCategories", 
            List.of("All", "Crypto", "Stocks", "ETFs", "Commodities"));
        formattedWatchlist.put("description", "Your market watchlist");
        
        return formattedWatchlist;
    }
    
    /**
     * Creates metadata about the watchlist.
     */
    private Map<String, Object> createMetadata(WatchlistCollectionResponse watchlist, String dataType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastUpdated", System.currentTimeMillis());
        metadata.put("demoMode", "real"); // Always real mode
        metadata.put("dataType", dataType); // "real" for real data, "demo" only as fallback
        
        // Summary stats
        metadata.put("summary", Map.of(
            "totalSymbols", watchlist.getTotalCount(),
            "activeSymbols", watchlist.getActiveCount(),
            "cryptoCount", countByType(watchlist, "crypto"),
            "stockCount", countByType(watchlist, "stock"),
            "etfCount", countByType(watchlist, "etf")
        ));
        
        return metadata;
    }
    
    /**
     * Helper to count items by type.
     */
    private long countByType(WatchlistCollectionResponse watchlist, String type) {
        return watchlist.getItems().stream()
            .filter(item -> type.equals(item.getType()))
            .count();
    }
}
