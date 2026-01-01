package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.StrategySubscriptionRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for reading strategies with proper access control
 */
@Service
public class ReadStrategyService extends BaseService {

    private final ReadStrategyRepository readStrategyRepository;
    private final StrategySubscriptionRepository subscriptionRepository;

    @Autowired
    public ReadStrategyService(
            ReadStrategyRepository readStrategyRepository,
            StrategySubscriptionRepository subscriptionRepository) {
        this.readStrategyRepository = readStrategyRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    protected String getModuleName() {
        return "service-labs";
    }
    
    /**
     * Get all strategies for a user
     */
    public List<Strategy> getUserStrategies(String userId) {
        log.debug("Fetching strategies for user: {}", userId);
        return readStrategyRepository.findByUserId(userId);
    }
    
    /**
     * Get strategies by status for a user
     */
    public List<Strategy> getUserStrategiesByStatus(String userId, String status) {
        log.debug("Fetching strategies for user: {} with status: {}", userId, status);
        return readStrategyRepository.findByUserIdAndStatus(userId, status);
    }
    
    /**
     * Get strategies by language for a user
     */
    public List<Strategy> getUserStrategiesByLanguage(String userId, String language) {
        log.debug("Fetching strategies for user: {} with language: {}", userId, language);
        return readStrategyRepository.findByUserIdAndLanguage(userId, language);
    }
    
    /**
     * Get a specific strategy by ID with proper access control.
     *
     * Access rules based on publishStatus and publicStatus:
     * - DRAFT + PRIVATE: Owner only
     * - DRAFT + PUBLIC: INVALID (prevented by validation)
     * - PUBLISHED + PRIVATE: Owner and subscribers only
     * - PUBLISHED + PUBLIC: Everyone
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return Optional of strategy if user has access
     */
    public Optional<Strategy> getStrategyById(String strategyId, String userId) {
        log.debug("Fetching strategy: {} for user: {}", strategyId, userId);

        return readStrategyRepository.findById(strategyId)
                .filter(strategy -> canViewStrategy(strategy, userId));
    }

    /**
     * Check if user can view the strategy based on access control rules.
     *
     * @param strategy The strategy to check
     * @param userId The user requesting access (can be null for public access)
     * @return true if user can view the strategy
     */
    private boolean canViewStrategy(Strategy strategy, String userId) {
        // Owner can always view
        if (userId != null && strategy.isOwner(userId)) {
            return true;
        }

        // DRAFT strategies are owner-only
        if ("DRAFT".equals(strategy.getPublishStatus())) {
            return false;
        }

        // PUBLISHED + PUBLIC: Everyone can view
        if ("PUBLIC".equals(strategy.getPublicStatus())) {
            return true;
        }

        // PUBLISHED + PRIVATE: Only subscribers can view (besides owner)
        if (userId != null) {
            return hasActiveSubscription(userId, strategy.getId());
        }

        return false;
    }

    /**
     * Check if user has active subscription to strategy.
     */
    private boolean hasActiveSubscription(String userId, String strategyId) {
        return subscriptionRepository.hasActiveSubscription(userId, strategyId);
    }
    
    /**
     * Get all public strategies
     */
    public List<Strategy> getPublicStrategies() {
        log.debug("Fetching public strategies");
        return readStrategyRepository.findPublicStrategies();
    }
    
    /**
     * Get public strategies by language
     */
    public List<Strategy> getPublicStrategiesByLanguage(String language) {
        log.debug("Fetching public strategies with language: {}", language);
        return readStrategyRepository.findPublicStrategiesByLanguage(language);
    }
    
    /**
     * Get public strategies by tags
     * Note: This requires a custom query since Firestore doesn't support array-contains-any in Spring Data
     */
    public List<Strategy> getPublicStrategiesByTags(List<String> tags) {
        log.debug("Fetching public strategies with tags: {}", tags);
        return readStrategyRepository.findPublicStrategiesByTags(tags);
    }
    
    /**
     * Search strategies by name
     */
    public List<Strategy> searchStrategiesByName(String userId, String searchTerm) {
        log.debug("Searching strategies for user: {} with term: {}", userId, searchTerm);
        return readStrategyRepository.searchByName(userId, searchTerm);
    }
    
    /**
     * Check if a strategy exists
     */
    public boolean strategyExists(String strategyId) {
        return readStrategyRepository.existsById(strategyId);
    }
}