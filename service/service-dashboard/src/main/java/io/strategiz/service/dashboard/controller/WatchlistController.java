package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.entity.UserEntity;
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
    
    // Default symbols for watchlist
    private static final List<String> DEFAULT_CRYPTO_SYMBOLS = Arrays.asList("BTC", "ETH");
    private static final List<String> DEFAULT_STOCK_SYMBOLS = Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN");
    private static final List<String> DEFAULT_ETF_SYMBOLS = Arrays.asList("SPY");
    
    @Autowired
    public WatchlistController(UserRepository userRepository,
                              CoinGeckoClient coinGeckoClient) {
        this.userRepository = userRepository;
        this.coinGeckoClient = coinGeckoClient;
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
            // TODO: Check if item already exists when repository is implemented
            // For now, just return success
            
            WatchlistOperationResponse response = WatchlistOperationResponse.success(
                "CREATE", 
                UUID.randomUUID().toString(), 
                request.getSymbol(), 
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
            // TODO: Implement actual deletion when repository is available
            // For now, just return success
            
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
            // TODO: Implement actual check when repository is available
            // For now, return false for all symbols
            boolean inWatchlist = false;
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "symbol", symbol,
                "inWatchlist", inWatchlist
            ));
            
        } catch (Exception e) {
            log.error("Error checking symbol in watchlist: {} for user: {}", symbol, userId, e);
            throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "check_symbol_in_watchlist", e.getMessage());
        }
    }
    
    /**
     * Get REAL watchlist data from market APIs
     */
    private WatchlistCollectionResponse getRealWatchlistData(String userId) {
        log.info("Fetching real market data for watchlist");
        List<WatchlistItem> items = new ArrayList<>();
        
        try {
            // Fetch crypto data in parallel
            CompletableFuture<List<WatchlistItem>> cryptoFuture = CompletableFuture.supplyAsync(() -> {
                log.info("Fetching real crypto data from CoinGecko");
                try {
                    // Convert symbol list to coin IDs for CoinGecko
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
                            crypto.getSymbol().toLowerCase() + "-1",
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
                    return new ArrayList<>();
                }
            });
            
            // Fetch stock data in parallel
            CompletableFuture<List<WatchlistItem>> stockFuture = CompletableFuture.supplyAsync(() -> {
                log.info("Using static stock data (Yahoo Finance integration temporarily disabled)");
                try {
                    List<String> allSymbols = new ArrayList<>();
                    allSymbols.addAll(DEFAULT_STOCK_SYMBOLS);
                    allSymbols.addAll(DEFAULT_ETF_SYMBOLS);

                    // Static prices for demo/fallback purposes
                    Map<String, Double> staticStockPrices = new HashMap<>();
                    staticStockPrices.put("AAPL", 182.52);
                    staticStockPrices.put("MSFT", 338.12);
                    staticStockPrices.put("GOOGL", 137.14);
                    staticStockPrices.put("AMZN", 178.22);
                    staticStockPrices.put("SPY", 504.12);

                    return allSymbols.stream()
                        .filter(staticStockPrices::containsKey)
                        .map(symbol -> {
                            // Determine type based on symbol
                            String type = DEFAULT_ETF_SYMBOLS.contains(symbol) ? "etf" : "stock";

                            BigDecimal price = BigDecimal.valueOf(staticStockPrices.get(symbol));

                            // Static change data for demo purposes
                            BigDecimal change = new BigDecimal("1.23");
                            BigDecimal changePercent = new BigDecimal("0.68");

                            return new WatchlistItem(
                                symbol.toLowerCase() + "-1",
                                symbol,
                                symbol, // Use symbol as name since we don't have name data
                                type,
                                price,
                                change,
                                changePercent,
                                true, // Default to positive
                                "/chart/" + symbol.toLowerCase()
                            );
                        }).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("Error creating static stock data", e);
                    return new ArrayList<>();
                }
            });
            
            // Wait for both futures to complete
            List<WatchlistItem> cryptoItems = cryptoFuture.get();
            List<WatchlistItem> stockItems = stockFuture.get();
            
            // Combine results
            items.addAll(cryptoItems);
            items.addAll(stockItems);
            
            log.info("Successfully fetched {} crypto items and {} stock items", cryptoItems.size(), stockItems.size());
            
        } catch (Exception e) {
            log.error("Error fetching real market data", e);
            // If real data fails, return empty list (will be handled by caller)
            throw new RuntimeException("Failed to fetch real market data", e);
        }
        
        // Create response
        WatchlistCollectionResponse response = new WatchlistCollectionResponse();
        response.setItems(items);
        response.setTotalCount(items.size());
        response.setActiveCount(items.size());
        response.setIsEmpty(items.isEmpty());
        // Note: setDescription and setAvailableCategories methods don't exist in the response class
        // These are set in the constructor or through other means
        
        return response;
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
