package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of StrategySubscriptionRepository using Firestore.
 */
@Repository
public class StrategySubscriptionRepositoryImpl implements StrategySubscriptionRepository {

    private final StrategySubscriptionBaseRepository baseRepository;

    @Autowired
    public StrategySubscriptionRepositoryImpl(StrategySubscriptionBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategySubscriptionEntity create(StrategySubscriptionEntity subscription, String userId) {
        return baseRepository.save(subscription, userId);
    }

    @Override
    public Optional<StrategySubscriptionEntity> findById(String subscriptionId) {
        return baseRepository.findById(subscriptionId);
    }

    @Override
    public Optional<StrategySubscriptionEntity> findByUserAndStrategy(String userId, String strategyId) {
        return baseRepository.findByUserAndStrategy(userId, strategyId);
    }

    @Override
    public boolean hasActiveSubscription(String userId, String strategyId) {
        return baseRepository.hasActiveSubscription(userId, strategyId);
    }

    @Override
    public List<StrategySubscriptionEntity> getUserSubscriptions(String userId, int limit) {
        return baseRepository.findActiveByUserId(userId, limit);
    }

    @Override
    public List<StrategySubscriptionEntity> getStrategySubscribers(String strategyId, int limit) {
        return baseRepository.findActiveByStrategyId(strategyId, limit);
    }

    @Override
    public int countActiveSubscribers(String strategyId) {
        return baseRepository.countActiveSubscribers(strategyId);
    }

    @Override
    public StrategySubscriptionEntity update(StrategySubscriptionEntity subscription, String userId) {
        return baseRepository.save(subscription, userId);
    }

    @Override
    public StrategySubscriptionEntity cancel(String subscriptionId, String userId) {
        Optional<StrategySubscriptionEntity> optional = baseRepository.findById(subscriptionId);
        if (optional.isEmpty()) {
            throw new RuntimeException("Subscription not found: " + subscriptionId);
        }

        StrategySubscriptionEntity subscription = optional.get();
        subscription.cancel();
        return baseRepository.save(subscription, userId);
    }

    @Override
    public boolean delete(String subscriptionId, String userId) {
        return baseRepository.delete(subscriptionId, userId);
    }
}
