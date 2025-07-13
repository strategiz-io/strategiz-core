package io.strategiz.data.user.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.user.model.watchlist.WatchlistItem;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WatchlistItem entities using BaseRepository audit support.
 * Provides CRUD operations with automatic audit field management.
 */
@Repository
public class WatchlistItemRepository extends BaseRepository<WatchlistItem> {
    
    public WatchlistItemRepository(Firestore firestore) {
        super(firestore, WatchlistItem.class);
    }
    
    /**
     * Find all items in a specific watchlist
     * @param watchlistId Watchlist ID
     * @return List of watchlist items
     */
    public List<WatchlistItem> findByWatchlistId(String watchlistId) {
        return findByField("watchlistId", watchlistId);
    }
    
    /**
     * Find a specific item by watchlist and symbol
     * @param watchlistId Watchlist ID
     * @param symbol Asset symbol
     * @return Optional watchlist item
     */
    public Optional<WatchlistItem> findByWatchlistIdAndSymbol(String watchlistId, String symbol) {
        List<WatchlistItem> items = findByWatchlistId(watchlistId);
        return items.stream()
                .filter(item -> symbol.equalsIgnoreCase(item.getSymbol()))
                .findFirst();
    }
    
    /**
     * Find all items for a specific symbol across all watchlists
     * @param symbol Asset symbol
     * @return List of watchlist items with that symbol
     */
    public List<WatchlistItem> findBySymbol(String symbol) {
        return findByField("symbol", symbol.toUpperCase());
    }
    
    /**
     * Find items by type (STOCK, CRYPTO, etc.)
     * @param type Asset type
     * @return List of items of that type
     */
    public List<WatchlistItem> findByType(String type) {
        return findByField("type", type.toUpperCase());
    }
    
    /**
     * Find items by exchange
     * @param exchange Exchange name
     * @return List of items from that exchange
     */
    public List<WatchlistItem> findByExchange(String exchange) {
        return findByField("exchange", exchange.toUpperCase());
    }
    
    /**
     * Find items with alerts enabled
     * @param watchlistId Watchlist ID
     * @return List of items with alerts enabled
     */
    public List<WatchlistItem> findByWatchlistIdAndAlertsEnabled(String watchlistId) {
        return findByWatchlistId(watchlistId).stream()
                .filter(WatchlistItem::isAlertsEnabled)
                .toList();
    }
    
    /**
     * Check if a symbol exists in a watchlist
     * @param watchlistId Watchlist ID
     * @param symbol Asset symbol
     * @return True if exists
     */
    public boolean existsByWatchlistIdAndSymbol(String watchlistId, String symbol) {
        return findByWatchlistIdAndSymbol(watchlistId, symbol).isPresent();
    }
    
    /**
     * Count items in a watchlist
     * @param watchlistId Watchlist ID
     * @return Number of items
     */
    public long countByWatchlistId(String watchlistId) {
        return findByWatchlistId(watchlistId).size();
    }
    
    /**
     * Delete all items in a watchlist
     * @param watchlistId Watchlist ID
     * @param userId Who is deleting them
     * @return Number of items deleted
     */
    public int deleteByWatchlistId(String watchlistId, String userId) {
        List<WatchlistItem> items = findByWatchlistId(watchlistId);
        List<String> itemIds = items.stream()
                .map(WatchlistItem::getId)
                .toList();
        return deleteAll(itemIds, userId);
    }
    
    /**
     * Add a new item to a watchlist
     * @param watchlistId Watchlist ID
     * @param symbol Asset symbol
     * @param name Asset name
     * @param type Asset type
     * @param userId Who is adding it
     * @return Created watchlist item
     */
    public WatchlistItem addToWatchlist(String watchlistId, String symbol, String name, String type, String userId) {
        WatchlistItem item = new WatchlistItem(watchlistId, symbol, name, type, userId);
        return save(item, userId);
    }
    
    /**
     * Add a new item to a watchlist with exchange and currency
     * @param watchlistId Watchlist ID
     * @param symbol Asset symbol
     * @param name Asset name
     * @param type Asset type
     * @param exchange Exchange name
     * @param currency Currency code
     * @param userId Who is adding it
     * @return Created watchlist item
     */
    public WatchlistItem addToWatchlist(String watchlistId, String symbol, String name, String type, String exchange, String currency, String userId) {
        WatchlistItem item = new WatchlistItem(watchlistId, symbol, name, type, exchange, currency, userId);
        return save(item, userId);
    }
    
    /**
     * Update the price for all items with a specific symbol
     * @param symbol Asset symbol
     * @param newPrice New price
     * @param userId Who is updating
     * @return Number of items updated
     */
    public int updatePriceBySymbol(String symbol, double newPrice, String userId) {
        List<WatchlistItem> items = findBySymbol(symbol);
        items.forEach(item -> item.updatePrice(newPrice));
        saveAll(items, userId);
        return items.size();
    }
}