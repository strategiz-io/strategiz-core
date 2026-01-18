package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.UserSubscription;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserSubscriptionRepository using Firestore.
 */
@Repository
public class UserSubscriptionRepositoryImpl implements UserSubscriptionRepository {

	private final UserSubscriptionBaseRepository baseRepository;

	@Autowired
	public UserSubscriptionRepositoryImpl(UserSubscriptionBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public UserSubscription save(UserSubscription subscription, String performingUserId) {
		return baseRepository.save(subscription, performingUserId);
	}

	@Override
	public Optional<UserSubscription> findById(String subscriptionId) {
		return baseRepository.findById(subscriptionId);
	}

	@Override
	public List<UserSubscription> findBySubscriberId(String subscriberId) {
		return baseRepository.findBySubscriberId(subscriberId);
	}

	@Override
	public List<UserSubscription> findActiveBySubscriberId(String subscriberId) {
		return baseRepository.findActiveBySubscriberId(subscriberId);
	}

	@Override
	public List<UserSubscription> findByOwnerId(String ownerId) {
		return baseRepository.findByOwnerId(ownerId);
	}

	@Override
	public List<UserSubscription> findActiveByOwnerId(String ownerId) {
		return baseRepository.findActiveByOwnerId(ownerId);
	}

	@Override
	public Optional<UserSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
		return baseRepository.findBySubscriberIdAndOwnerId(subscriberId, ownerId);
	}

	@Override
	public Optional<UserSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId) {
		return baseRepository.findActiveBySubscriberIdAndOwnerId(subscriberId, ownerId);
	}

	@Override
	public Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
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
	public UserSubscription updateStatus(String subscriptionId, String status) {
		Optional<UserSubscription> subscriptionOpt = findById(subscriptionId);
		if (subscriptionOpt.isEmpty()) {
			throw new IllegalStateException("Subscription not found: " + subscriptionId);
		}

		UserSubscription subscription = subscriptionOpt.get();
		subscription.setStatus(status);

		return baseRepository.save(subscription, subscription.getSubscriberId());
	}

	@Override
	public UserSubscription cancel(String subscriptionId, String reason) {
		Optional<UserSubscription> subscriptionOpt = findById(subscriptionId);
		if (subscriptionOpt.isEmpty()) {
			throw new IllegalStateException("Subscription not found: " + subscriptionId);
		}

		UserSubscription subscription = subscriptionOpt.get();
		subscription.setStatus("CANCELLED");
		subscription.setCancelledAt(Timestamp.now());
		subscription.setCancellationReason(reason);

		return baseRepository.save(subscription, subscription.getSubscriberId());
	}

	@Override
	public void deleteById(String subscriptionId) {
		Optional<UserSubscription> subscriptionOpt = findById(subscriptionId);
		if (subscriptionOpt.isPresent()) {
			baseRepository.delete(subscriptionId, subscriptionOpt.get().getSubscriberId());
		}
	}

}
