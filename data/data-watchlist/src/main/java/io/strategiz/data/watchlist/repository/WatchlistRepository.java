package io.strategiz.data.watchlist.repository;

import io.strategiz.data.watchlist.entity.WatchlistItem;

import java.util.List;
import java.util.Optional;

/**
 * Repository for users/{userId}/watchlist subcollection
 */
public interface WatchlistRepository {
    
    /**
     * Add item to user's watchlist
     */
    WatchlistItem addToWatchlist(String userId, WatchlistItem item);
    
    /**
     * Get all watchlist items for user
     */
    List<WatchlistItem> findByUserId(String userId);
    
    /**
     * Get watchlist items by type
     */
    List<WatchlistItem> findByUserIdAndType(String userId, String type);
    
    /**
     * Get specific watchlist item
     */
    Optional<WatchlistItem> findByUserIdAndItemId(String userId, String itemId);
    
    /**
     * Find item by symbol
     */
    Optional<WatchlistItem> findByUserIdAndSymbol(String userId, String symbol);
    
    /**
     * Update watchlist item
     */
    WatchlistItem updateItem(String userId, String itemId, WatchlistItem item);
    
    /**
     * Update item price data
     */
    void updatePriceData(String userId, String itemId, WatchlistItem priceData);
    
    /**
     * Remove item from watchlist
     */
    void removeFromWatchlist(String userId, String itemId);
    
    /**
     * Check if user has symbol in watchlist
     */
    boolean hasSymbolInWatchlist(String userId, String symbol);
    
    /**
     * Count watchlist items
     */
    long countByUserId(String userId);
    
    /**
     * Get items with alerts enabled
     */
    List<WatchlistItem> findByUserIdAndAlertsEnabled(String userId);
    
    /**
     * Get items by exchange
     */
    List<WatchlistItem> findByUserIdAndExchange(String userId, String exchange);
    
    /**
     * Reorder watchlist items
     */
    void reorderWatchlist(String userId, List<String> orderedItemIds);
}