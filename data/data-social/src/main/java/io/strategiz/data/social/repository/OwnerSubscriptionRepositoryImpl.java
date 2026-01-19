package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.OwnerSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of OwnerSubscriptionRepository using Firestore.
 *
 * @see io.strategiz.data.social.entity.OwnerSubscription
 * @see io.strategiz.data.social.repository.OwnerSubscriptionRepository
 */
@Repository
public class OwnerSubscriptionRepositoryImpl implements OwnerSubscriptionRepository {

    private final OwnerSubscriptionBaseRepository baseRepository;

    @Autowired
    public OwnerSubscriptionRepositoryImpl(OwnerSubscriptionBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public OwnerSubscription save(OwnerSubscription subscription, String performingUserId) {
        return baseRepository.save(subscription, performingUserId);
    }

    @Override
    public Optional<OwnerSubscription> findById(String subscriptionId) {
        return baseRepository.findById(subscriptionId);
    }

    @Override
    public List<OwnerSubscription> findBySubscriberId(String subscriberId) {
        return baseRepository.findBySubscriberId(subscriberId);
    }

    @Override
    public List<OwnerSubscription> findActiveBySubscriberId(String subscriberId) {
        return baseRepository.findActiveBySubscriberId(subscriberId);
    }

    @Override
    public List<OwnerSubscription> findByOwnerId(String ownerId) {
        return baseRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<OwnerSubscription> findActiveByOwnerId(String ownerId) {
        return baseRepository.findActiveByOwnerId(ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        return baseRepository.findBySubscriberIdAndOwnerId(subscriberId, ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        return baseRepository.findActiveBySubscriberIdAndOwnerId(subscriberId, ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return baseRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    @Override
    public int countActiveByOwnerId(String ownerId) {
        return baseRepository.countActiveByOwnerId(ownerId);
    }

    @Override
    public int countActiveBySubscriberId(String subscriberId) {
        return baseRepository.countActiveBySubscriberId(subscriberId);
    }

    @Override
    public boolean hasActiveSubscription(String subscriberId, String ownerId) {
        return findActiveBySubscriberIdAndOwnerId(subscriberId, ownerId).isPresent();
    }

    @Override
    public void deleteById(String subscriptionId) {
        Optional<OwnerSubscription> subscriptionOpt = findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            baseRepository.delete(subscriptionId, subscriptionOpt.get().getSubscriberId());
        }
    }

}
