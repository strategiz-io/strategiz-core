package io.strategiz.data.user.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.user.model.watchlist.Watchlist;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Watchlist entities using BaseRepository audit support.
 * Provides CRUD operations with automatic audit field management.
 */
@Repository
public class WatchlistRepository extends BaseRepository<Watchlist> {
    
    public WatchlistRepository(Firestore firestore) {
        super(firestore, Watchlist.class);
    }
    
    /**
     * Find all watchlists for a specific user
     * @param userId User ID
     * @return List of user's watchlists
     */
    public List<Watchlist> findByUserId(String userId) {
        return findByField("userId", userId);
    }
    
    /**
     * Find the default watchlist for a user
     * @param userId User ID
     * @return Optional default watchlist
     */
    public Optional<Watchlist> findDefaultByUserId(String userId) {
        List<Watchlist> watchlists = findByUserId(userId);
        return watchlists.stream()
                .filter(Watchlist::isDefault)
                .findFirst();
    }
    
    /**
     * Find a specific watchlist by user and name
     * @param userId User ID
     * @param name Watchlist name
     * @return Optional watchlist
     */
    public Optional<Watchlist> findByUserIdAndName(String userId, String name) {
        List<Watchlist> watchlists = findByUserId(userId);
        return watchlists.stream()
                .filter(w -> name.equals(w.getName()))
                .findFirst();
    }
    
    /**
     * Check if a user has a watchlist with the given name
     * @param userId User ID
     * @param name Watchlist name
     * @return True if exists
     */
    public boolean existsByUserIdAndName(String userId, String name) {
        return findByUserIdAndName(userId, name).isPresent();
    }
    
    /**
     * Count watchlists for a user
     * @param userId User ID
     * @return Number of watchlists
     */
    public long countByUserId(String userId) {
        return findByUserId(userId).size();
    }
    
    /**
     * Find watchlists containing a specific item
     * @param itemId Item ID
     * @return List of watchlists containing the item
     */
    public List<Watchlist> findByItemId(String itemId) {
        return findByField("itemIds", itemId);
    }
    
    /**
     * Create a default watchlist for a new user
     * @param userId User ID
     * @param createdBy Who is creating it
     * @return Created default watchlist
     */
    public Watchlist createDefaultWatchlist(String userId, String createdBy) {
        Watchlist defaultWatchlist = new Watchlist(userId, "My Watchlist", "Default watchlist", true, createdBy);
        return save(defaultWatchlist, createdBy);
    }
}