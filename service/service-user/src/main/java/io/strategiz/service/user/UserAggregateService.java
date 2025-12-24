package io.strategiz.service.user;

import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.model.watchlist.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import io.strategiz.service.base.BaseService;

/**
 * Service for aggregating user data from multiple data modules.
 * Handles cross-cutting concerns where you need both user identity and watchlist data.
 */
@Service
public class UserAggregateService extends BaseService {

    @Override
    protected String getModuleName() {
        return "unknown";
    }

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets complete user dashboard data (profile + watchlist).
     * 
     * @param userId The user ID
     * @return UserDashboardProjection with all needed data
     */
    public UserDashboardProjection getUserDashboard(String userId) {
        // Single call to get all user data including watchlist
        // This is efficient because it's all in the same data module
        WatchlistCollectionResponse watchlist = userRepository.readUserWatchlist(userId);
        
        // You could also get user profile here if needed
        // UserProfile profile = userRepository.getUserProfile(userId);
        
        return new UserDashboardProjection(userId, watchlist);
    }

    /**
     * Gets user watchlist with user context.
     * 
     * @param userId The user ID
     * @return Watchlist with user context
     */
    public WatchlistWithUserContext getWatchlistWithUserContext(String userId) {
        WatchlistCollectionResponse watchlist = userRepository.readUserWatchlist(userId);
        
        return new WatchlistWithUserContext(userId, watchlist);
    }

    /**
     * Creates a watchlist item with user validation.
     * 
     * @param userId The user ID
     * @param request The create request
     * @return Operation response
     */
    public WatchlistOperationResponse createWatchlistItem(String userId, CreateWatchlistItemRequest request) {
        // Validate user exists and has permission
        // This is where you'd check user status, limits, etc.
        
        return userRepository.createWatchlistItem(userId, request);
    }

    /**
     * Gets user's active watchlist items only.
     * 
     * @param userId The user ID
     * @return Active watchlist items
     */
    public List<ReadWatchlistItemResponse> getActiveWatchlistItems(String userId) {
        WatchlistCollectionResponse watchlist = userRepository.readUserWatchlist(userId);
        
        return watchlist.getItems().stream()
                .filter(item -> item.getIsActive() != null && item.getIsActive())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Checks if user has specific symbol in watchlist.
     * 
     * @param userId The user ID
     * @param symbol The symbol to check
     * @return True if symbol is in user's watchlist
     */
    public boolean hasSymbolInWatchlist(String userId, String symbol) {
        return userRepository.isAssetInWatchlist(userId, symbol);
    }

    // Inner classes for projections
    
    /**
     * Projection for user dashboard data.
     */
    public static class UserDashboardProjection {
        private String userId;
        private WatchlistCollectionResponse watchlist;
        
        public UserDashboardProjection(String userId, WatchlistCollectionResponse watchlist) {
            this.userId = userId;
            this.watchlist = watchlist;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public WatchlistCollectionResponse getWatchlist() { return watchlist; }
    }

    /**
     * Projection for watchlist with user context.
     */
    public static class WatchlistWithUserContext {
        private String userId;
        private WatchlistCollectionResponse watchlist;
        
        public WatchlistWithUserContext(String userId, WatchlistCollectionResponse watchlist) {
            this.userId = userId;
            this.watchlist = watchlist;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public WatchlistCollectionResponse getWatchlist() { return watchlist; }
        
        /**
         * Gets watchlist summary for user.
         */
        public String getSummary() {
            return String.format("User %s has %d active watchlist items", 
                userId, watchlist.getActiveCount());
        }
    }
} 