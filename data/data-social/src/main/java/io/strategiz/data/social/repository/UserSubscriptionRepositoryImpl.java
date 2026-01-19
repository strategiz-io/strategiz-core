package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.OwnerSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated Use {@link OwnerSubscriptionRepositoryImpl} instead.
 * This class is kept for backward compatibility only.
 *
 * <p>UserSubscriptionRepositoryImpl has been replaced by OwnerSubscriptionRepositoryImpl.</p>
 */
@Deprecated(forRemoval = true)
@Repository("deprecatedUserSubscriptionRepository")
public class UserSubscriptionRepositoryImpl implements UserSubscriptionRepository {

    private final OwnerSubscriptionRepositoryImpl delegate;

    @Autowired
    public UserSubscriptionRepositoryImpl(OwnerSubscriptionBaseRepository baseRepository) {
        this.delegate = new OwnerSubscriptionRepositoryImpl(baseRepository);
    }

    @Override
    public OwnerSubscription save(OwnerSubscription subscription, String performingUserId) {
        return delegate.save(subscription, performingUserId);
    }

    @Override
    public Optional<OwnerSubscription> findById(String subscriptionId) {
        return delegate.findById(subscriptionId);
    }

    @Override
    public List<OwnerSubscription> findBySubscriberId(String subscriberId) {
        return delegate.findBySubscriberId(subscriberId);
    }

    @Override
    public List<OwnerSubscription> findActiveBySubscriberId(String subscriberId) {
        return delegate.findActiveBySubscriberId(subscriberId);
    }

    @Override
    public List<OwnerSubscription> findByOwnerId(String ownerId) {
        return delegate.findByOwnerId(ownerId);
    }

    @Override
    public List<OwnerSubscription> findActiveByOwnerId(String ownerId) {
        return delegate.findActiveByOwnerId(ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        return delegate.findBySubscriberIdAndOwnerId(subscriberId, ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
        return delegate.findActiveBySubscriberIdAndOwnerId(subscriberId, ownerId);
    }

    @Override
    public Optional<OwnerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return delegate.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    @Override
    public int countActiveByOwnerId(String ownerId) {
        return delegate.countActiveByOwnerId(ownerId);
    }

    @Override
    public int countActiveBySubscriberId(String subscriberId) {
        return delegate.countActiveBySubscriberId(subscriberId);
    }

    @Override
    public boolean hasActiveSubscription(String subscriberId, String ownerId) {
        return delegate.hasActiveSubscription(subscriberId, ownerId);
    }

    @Override
    public OwnerSubscription updateStatus(String subscriptionId, String status) {
        return delegate.updateStatus(subscriptionId, status);
    }

    @Override
    public OwnerSubscription cancel(String subscriptionId, String reason) {
        return delegate.cancel(subscriptionId, reason);
    }

    @Override
    public void deleteById(String subscriptionId) {
        delegate.deleteById(subscriptionId);
    }

}
