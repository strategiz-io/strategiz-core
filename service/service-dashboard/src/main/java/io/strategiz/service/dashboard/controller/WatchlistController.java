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
import io.strategiz.service.dashboard.service.WatchlistService;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;

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
import java.util.stream.Collectors;

/**
 * Controller for watchlist data.
 * Returns demo data until proper repository implementations are available.
 * The mode only affects UI behavior, not the data itself.
 */
@RestController
@RequestMapping("/v1/dashboard/watchlists")
@CrossOrigin(origins = "*")
@RequireAuth(minAcr = "1")
public class WatchlistController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DASHBOARD_MODULE;
    }
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final UserRepository userRepository;
    private final CoinGeckoClient coinGeckoClient;
    private final WatchlistBaseRepository watchlistRepository;
    private final WatchlistService watchlistService;

    @Autowired
    public WatchlistController(UserRepository userRepository,
                              CoinGeckoClient coinGeckoClient,
                              WatchlistBaseRepository watchlistRepository,
                              WatchlistService watchlistService) {
        this.userRepository = userRepository;
        this.coinGeckoClient = coinGeckoClient;
        this.watchlistRepository = watchlistRepository;
        this.watchlistService = watchlistService;
    }
    
    /**
     * Get watchlist data for authenticated user.
     * Returns REAL market data from CoinGecko and Yahoo Finance.
     *
     * @param user The authenticated user from token
     * @return Real-time watchlist data with market information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWatchlist(@AuthUser AuthenticatedUser user) {
        String userId = user.getUserId();
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
     *
     * @param user The authenticated user from token
     * @param request The watchlist item creation request
     * @return Operation result with created item details
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWatchlistItem(
            @AuthUser AuthenticatedUser user,
            @RequestBody CreateWatchlistItemRequest request) {

        String userId = user.getUserId();
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

            // Enrich with market data before saving
            entity = watchlistService.enrichWatchlistItem(entity);

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
     *
     * @param user The authenticated user from token
     * @param itemId The ID of the watchlist item to delete
     * @return Operation result
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteWatchlistItem(
            @AuthUser AuthenticatedUser user,
            @PathVariable String itemId) {

        String userId = user.getUserId();
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
     *
     * @param user The authenticated user from token
     * @param symbol The symbol to check
     * @return Whether the symbol is in the user's watchlist
     */
    @GetMapping("/check/{symbol}")
    public ResponseEntity<Map<String, Object>> checkSymbolInWatchlist(
            @AuthUser AuthenticatedUser user,
            @PathVariable String symbol) {

        String userId = user.getUserId();
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

            // If user has no saved items, return empty watchlist - NO MOCK DATA
            if (savedItems.isEmpty()) {
                log.info("No saved items, returning empty watchlist for user {}", userId);
                WatchlistCollectionResponse empty = new WatchlistCollectionResponse();
                empty.setItems(new ArrayList<>());
                empty.setTotalCount(0);
                empty.setActiveCount(0);
                empty.setIsEmpty(true);
                return empty;
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

            // Enrich stock/ETF items with REAL Yahoo Finance data
            for (WatchlistItemEntity entity : stockItems) {
                try {
                    WatchlistItemEntity enriched = watchlistService.enrichWatchlistItem(entity);
                    items.add(convertToWatchlistItem(enriched));
                } catch (Exception e) {
                    log.warn("Failed to enrich {}: {}", entity.getSymbol(), e.getMessage());
                    // Skip failed items - NO MOCK DATA
                }
            }

            log.info("Returning {} enriched watchlist items for user {}", items.size(), userId);

        } catch (Exception e) {
            log.error("Error fetching watchlist data for user {}", userId, e);
            // If real data fails, return empty - NO MOCK DATA
            WatchlistCollectionResponse empty = new WatchlistCollectionResponse();
            empty.setItems(new ArrayList<>());
            empty.setTotalCount(0);
            empty.setActiveCount(0);
            empty.setIsEmpty(true);
            return empty;
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
     * Convert WatchlistItemEntity to WatchlistItem DTO
     */
    private WatchlistItem convertToWatchlistItem(WatchlistItemEntity entity) {
        return new WatchlistItem(
            entity.getId(),
            entity.getSymbol(),
            entity.getName(),
            entity.getType() != null ? entity.getType().toLowerCase() : "stock",
            entity.getCurrentPrice(),
            entity.getChange(),
            entity.getChangePercent(),
            entity.getChange() != null && entity.getChange().compareTo(BigDecimal.ZERO) > 0,
            "/chart/" + entity.getSymbol().toLowerCase()
        );
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
