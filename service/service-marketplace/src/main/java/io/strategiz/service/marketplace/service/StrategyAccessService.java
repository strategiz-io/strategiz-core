package io.strategiz.service.marketplace.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for checking access permissions to strategies.
 *
 * Implements the owner subscription model access control:
 * - Owner: Full access (view code, edit, deploy, transfer)
 * - Subscriber: Can deploy, view performance (NO code access)
 * - Public: Can view performance only (if strategy is PUBLIC)
 * - Creator: Attribution only (no special access unless also owner)
 */
@Service
public class StrategyAccessService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(StrategyAccessService.class);

    private final ReadStrategyRepository readStrategyRepository;
    private final io.strategiz.data.strategy.repository.StrategySubscriptionRepository subscriptionRepository;

    @Autowired
    public StrategyAccessService(
            ReadStrategyRepository readStrategyRepository,
            io.strategiz.data.strategy.repository.StrategySubscriptionRepository subscriptionRepository) {
        this.readStrategyRepository = readStrategyRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    /**
     * Check if user can view the strategy's source code.
     *
     * Only the OWNER can view code. Subscribers and public cannot.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can view code
     */
    public boolean canViewCode(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Only owner can view code
        return strategy.isOwner(userId);
    }

    /**
     * Check if user can deploy the strategy (as alert or bot).
     *
     * Owner OR Subscriber can deploy, but only if strategy is PUBLISHED.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can deploy
     */
    public boolean canDeploy(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Owner can deploy their own DRAFT or PUBLISHED strategies
        if (strategy.isOwner(userId)) {
            return true;
        }

        // Subscribers can only deploy PUBLISHED strategies
        if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
            return false;
        }

        // Check if user has active subscription to the owner
        return hasActiveSubscription(userId, strategy.getOwnerId());
    }

    /**
     * Check if user can view the strategy at all (name, description, metadata).
     *
     * Access rules based on publishStatus and publicStatus:
     * - DRAFT + PRIVATE: Owner only
     * - DRAFT + PUBLIC: INVALID (prevented by validation)
     * - PUBLISHED + PRIVATE: Owner and subscribers only
     * - PUBLISHED + PUBLIC: Everyone
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access (can be null for public)
     * @return true if user can view the strategy
     */
    public boolean canViewStrategy(String strategyId, String userId) {
        Strategy strategy = getStrategy(strategyId);

        // Owner can always view
        if (userId != null && strategy.isOwner(userId)) {
            return true;
        }

        // DRAFT strategies are owner-only
        if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
            return false;
        }

        // PUBLISHED + PUBLIC: Everyone can view
        if (Boolean.TRUE.equals(strategy.getIsPublic())) {
            return true;
        }

        // PUBLISHED + PRIVATE: Only subscribers can view (besides owner)
        if (userId != null) {
            return hasActiveSubscription(userId, strategy.getOwnerId());
        }

        return false;
    }

    /**
     * Check if user can view the strategy's performance metrics.
     *
     * Owner OR Subscriber OR Public (if strategy publicStatus is PUBLIC).
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access (can be null for public)
     * @return true if user can view performance
     */
    public boolean canViewPerformance(String strategyId, String userId) {
        Strategy strategy = getStrategy(strategyId);

        // Owner can always view
        if (userId != null && strategy.isOwner(userId)) {
            return true;
        }

        // DRAFT strategies: owner only
        if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
            return false;
        }

        // Subscribers can view published strategies
        if (userId != null && hasActiveSubscription(userId, strategy.getOwnerId())) {
            return true;
        }

        // Public can view if strategy is PUBLISHED + PUBLIC
        return Boolean.TRUE.equals(strategy.getIsPublished()) &&
               Boolean.TRUE.equals(strategy.getIsPublic());
    }

    /**
     * Check if user can edit the strategy (code, settings, pricing).
     *
     * Only the OWNER can edit.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can edit
     */
    public boolean canEdit(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Only owner can edit
        return strategy.isOwner(userId);
    }

    /**
     * Check if user can transfer ownership (sell/gift) the strategy.
     *
     * Only the OWNER can transfer ownership.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can transfer ownership
     */
    public boolean canTransferOwnership(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Only owner can transfer
        return strategy.isOwner(userId);
    }

    /**
     * Check if user can delete the strategy.
     *
     * Only the OWNER can delete. Cannot delete if strategy has active subscribers.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can delete
     */
    public boolean canDelete(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Only owner can delete
        if (!strategy.isOwner(userId)) {
            return false;
        }

        // Cannot delete if strategy has active subscribers
        if (strategy.getSubscriberCount() != null && strategy.getSubscriberCount() > 0) {
            log.warn("Cannot delete strategy {} - has {} active subscribers",
                    strategyId, strategy.getSubscriberCount());
            return false;
        }

        return true;
    }

    /**
     * Check if user can publish the strategy to marketplace.
     *
     * Only the OWNER can publish.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can publish
     */
    public boolean canPublish(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Only owner can publish
        return strategy.isOwner(userId);
    }

    /**
     * Check if user can subscribe to the strategy.
     *
     * User can subscribe if:
     * - They are NOT the owner
     * - Strategy is published
     * - Strategy has subscription pricing
     * - They don't already have an active subscription
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can subscribe
     */
    public boolean canSubscribe(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Cannot subscribe to your own strategy
        if (strategy.isOwner(userId)) {
            return false;
        }

        // Strategy must be published
        if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
            return false;
        }

        // Strategy must have subscription pricing
        if (strategy.getPricing() == null || strategy.getPricing().getMonthlyPrice() == null) {
            return false;
        }

        // User must not already have active subscription
        if (hasActiveSubscription(userId, strategyId)) {
            return false;
        }

        return true;
    }

    /**
     * Check if user can purchase the strategy (one-time purchase for full ownership).
     *
     * User can purchase if:
     * - They are NOT the current owner
     * - Strategy is published
     * - Strategy has one-time pricing
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @return true if user can purchase
     */
    public boolean canPurchase(String strategyId, String userId) {
        if (userId == null) {
            return false;
        }

        Strategy strategy = getStrategy(strategyId);

        // Cannot purchase your own strategy
        if (strategy.isOwner(userId)) {
            return false;
        }

        // Strategy must be published
        if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
            return false;
        }

        // Strategy must have one-time pricing
        if (strategy.getPricing() == null || strategy.getPricing().getOneTimePrice() == null) {
            return false;
        }

        return true;
    }

    /**
     * Enforce that user can view strategy, throw exception if not.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @throws StrategizException if access denied
     */
    public void enforceCanView(String strategyId, String userId) {
        if (!canViewStrategy(strategyId, userId)) {
            throw new StrategizException(
                    MarketplaceErrorDetails.VIEW_ACCESS_DENIED,
                    getModuleName(),
                    "You do not have access to view this strategy");
        }
    }

    /**
     * Enforce that user can view code, throw exception if not.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @throws StrategizException if access denied
     */
    public void enforceCanViewCode(String strategyId, String userId) {
        if (!canViewCode(strategyId, userId)) {
            throw new StrategizException(
                    MarketplaceErrorDetails.CODE_ACCESS_DENIED,
                    getModuleName(),
                    "Only the strategy owner can view source code");
        }
    }

    /**
     * Enforce that user can deploy, throw exception if not.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @throws StrategizException if access denied
     */
    public void enforceCanDeploy(String strategyId, String userId) {
        if (!canDeploy(strategyId, userId)) {
            throw new StrategizException(
                    MarketplaceErrorDetails.DEPLOY_ACCESS_DENIED,
                    getModuleName(),
                    "Only the owner or active subscribers can deploy strategies");
        }
    }

    /**
     * Enforce that user can edit, throw exception if not.
     *
     * @param strategyId The strategy ID
     * @param userId The user requesting access
     * @throws StrategizException if access denied
     */
    public void enforceCanEdit(String strategyId, String userId) {
        if (!canEdit(strategyId, userId)) {
            throw new StrategizException(
                    MarketplaceErrorDetails.EDIT_ACCESS_DENIED,
                    getModuleName(),
                    "Only the strategy owner can edit");
        }
    }

    /**
     * Get strategy by ID.
     */
    private Strategy getStrategy(String strategyId) {
        return readStrategyRepository.findById(strategyId)
                .orElseThrow(() -> new StrategizException(
                        MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                        getModuleName(),
                        "Strategy not found: " + strategyId));
    }

    /**
     * Check if user has active subscription to strategy.
     */
    private boolean hasActiveSubscription(String userId, String strategyId) {
        return subscriptionRepository.hasActiveSubscription(userId, strategyId);
    }
}
