package io.strategiz.data.watchlist.repository;

import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for users/{userId}/watchlist subcollection
 */
@Repository
public interface WatchlistRepository extends CrudRepository<WatchlistItemEntity, String> {
    
    // ===============================
    // Spring Data Query Methods
    // ===============================
    
    /**
     * Find items by symbol
     */
    List<WatchlistItemEntity> findBySymbol(String symbol);
    
    /**
     * Find items by symbol (case insensitive)
     */
    List<WatchlistItemEntity> findBySymbolIgnoreCase(String symbol);
    
    /**
     * Find items by type
     */
    List<WatchlistItemEntity> findByType(String type);
    
    /**
     * Find items by exchange
     */
    List<WatchlistItemEntity> findByExchange(String exchange);
    
    /**
     * Find items by symbol and type
     */
    List<WatchlistItemEntity> findBySymbolAndType(String symbol, String type);
    
    /**
     * Check if symbol exists
     */
    boolean existsBySymbol(String symbol);
    
    /**
     * Check if symbol exists (case insensitive)
     */
    boolean existsBySymbolIgnoreCase(String symbol);
    
    /**
     * Count items by type
     */
    long countByType(String type);
    
    /**
     * Count items by exchange
     */
    long countByExchange(String exchange);
    
    /**
     * Find items ordered by priority
     */
    List<WatchlistItemEntity> findAllByOrderByPriorityAsc();
    
    /**
     * Find items ordered by added date
     */
    List<WatchlistItemEntity> findAllByOrderByAddedAtDesc();
}