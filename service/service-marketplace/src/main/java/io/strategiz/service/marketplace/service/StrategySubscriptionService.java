package io.strategiz.service.marketplace.service;

import io.strategiz.data.strategy.entity.PricingType;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPricing;
import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import io.strategiz.data.strategy.entity.SubscriptionStatus;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.StrategySubscriptionRepository;
import io.strategiz.data.strategy.repository.UpdateStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import io.strategiz.service.base.BaseService;

/**
 * Service for managing strategy subscriptions.
 *
 * Key rules:
 * - Users cannot subscribe to their own strategies
 * - Only published strategies can be subscribed to
 * - Pricing is determined by the strategy's pricing configuration
 */
@Service
public class StrategySubscriptionService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }    private static final String MODULE_NAME = "service-marketplace";

    private final StrategySubscriptionRepository subscriptionRepository;
    private final ReadStrategyRepository readStrategyRepo;
    private final UpdateStrategyRepository updateStrategyRepo;

    @Autowired
    public StrategySubscriptionService(
            StrategySubscriptionRepository subscriptionRepository,
            ReadStrategyRepository readStrategyRepo,
            UpdateStrategyRepository updateStrategyRepo) {
        this.subscriptionRepository = subscriptionRepository;
        this.readStrategyRepo = readStrategyRepo;
        this.updateStrategyRepo = updateStrategyRepo;
    }

    /**
     * Subscribe to a strategy.
     *
     * @param strategyId The strategy to subscribe to
     * @param userId     The user subscribing
     * @return The created subscription
     */
    public StrategySubscriptionEntity subscribe(String strategyId, String userId) {
        // Validate strategy exists and is published
        Strategy strategy = readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        if (!"PUBLISHED".equals(strategy.getPublishStatus())) {
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_PUBLISHED, MODULE_NAME);
        }

        // Cannot subscribe to own strategy
        if (strategy.getOwnerId().equals(userId)) {
            throw new StrategizException(MarketplaceErrorDetails.CANNOT_SUBSCRIBE_OWN, MODULE_NAME);
        }

        // Check if already subscribed
        Optional<StrategySubscriptionEntity> existing = subscriptionRepository.findByUserAndStrategy(userId, strategyId);
        if (existing.isPresent() && existing.get().isValid()) {
            throw new StrategizException(MarketplaceErrorDetails.ALREADY_SUBSCRIBED, MODULE_NAME);
        }

        // Get pricing
        StrategyPricing pricing = strategy.getPricing();
        PricingType pricingType = (pricing != null) ? pricing.getPricingType() : PricingType.FREE;
        BigDecimal pricePaid = BigDecimal.ZERO;

        if (pricing != null) {
            if (pricingType == PricingType.ONE_TIME) {
                pricePaid = pricing.getOneTimePrice();
            } else if (pricingType == PricingType.SUBSCRIPTION) {
                pricePaid = pricing.getMonthlyPrice();
            }
        }

        // Create subscription
        StrategySubscriptionEntity subscription = new StrategySubscriptionEntity(
                strategyId, userId, strategy.getOwnerId(), pricingType);
        subscription.setPricePaid(pricePaid);
        subscription.setStrategyName(strategy.getName());
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        StrategySubscriptionEntity created = subscriptionRepository.create(subscription, userId);

        // Increment strategy subscriber count
        strategy.incrementSubscribers();
        updateStrategyRepo.update(strategy.getId(), userId, strategy);

        log.info("User {} subscribed to strategy {}", userId, strategyId);
        return created;
    }

    /**
     * Cancel a subscription.
     */
    public StrategySubscriptionEntity cancelSubscription(String subscriptionId, String userId) {
        StrategySubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.SUBSCRIPTION_NOT_FOUND, MODULE_NAME));

        // Verify ownership
        if (!subscription.getOwnerId().equals(userId)) {
            throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_UPDATE, MODULE_NAME);
        }

        StrategySubscriptionEntity cancelled = subscriptionRepository.cancel(subscriptionId, userId);

        // Decrement strategy subscriber count
        readStrategyRepo.findById(subscription.getStrategyId()).ifPresent(strategy -> {
            strategy.decrementSubscribers();
            updateStrategyRepo.update(strategy.getId(), userId, strategy);
        });

        log.info("User {} cancelled subscription {}", userId, subscriptionId);
        return cancelled;
    }

    /**
     * Check if a user has an active subscription to a strategy.
     */
    public boolean hasActiveSubscription(String userId, String strategyId) {
        return subscriptionRepository.hasActiveSubscription(userId, strategyId);
    }

    /**
     * Check if a user can access a strategy (owns it or has valid subscription).
     */
    public boolean canAccessStrategy(String userId, String strategyId) {
        Strategy strategy = readStrategyRepo.findById(strategyId).orElse(null);
        if (strategy == null) {
            return false;
        }

        // Owner always has access
        if (strategy.getOwnerId().equals(userId)) {
            return true;
        }

        // Free strategies are accessible to all
        if (strategy.isFree()) {
            return true;
        }

        // Check subscription
        return hasActiveSubscription(userId, strategyId);
    }

    /**
     * Get a user's subscriptions.
     */
    public List<StrategySubscriptionEntity> getUserSubscriptions(String userId, int limit) {
        return subscriptionRepository.getUserSubscriptions(userId, limit);
    }

    /**
     * Get subscribers for a strategy (creator only).
     */
    public List<StrategySubscriptionEntity> getStrategySubscribers(String strategyId, String requestingUserId, int limit) {
        // Verify requesting user is the strategy creator
        Strategy strategy = readStrategyRepo.findById(strategyId)
                .orElseThrow(() -> new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, MODULE_NAME));

        if (!strategy.getOwnerId().equals(requestingUserId)) {
            throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_UPDATE, MODULE_NAME);
        }

        return subscriptionRepository.getStrategySubscribers(strategyId, limit);
    }

    /**
     * Get a specific subscription.
     */
    public Optional<StrategySubscriptionEntity> getSubscription(String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    /**
     * Get a user's subscription to a specific strategy.
     */
    public Optional<StrategySubscriptionEntity> getSubscriptionByUserAndStrategy(String userId, String strategyId) {
        return subscriptionRepository.findByUserAndStrategy(userId, strategyId);
    }
}
