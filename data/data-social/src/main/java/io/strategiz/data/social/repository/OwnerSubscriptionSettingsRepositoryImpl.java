package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.OwnerSubscriptionSettings;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Implementation of OwnerSubscriptionSettingsRepository using Firestore.
 */
@Repository
public class OwnerSubscriptionSettingsRepositoryImpl implements OwnerSubscriptionSettingsRepository {

	private final OwnerSubscriptionSettingsBaseRepository baseRepository;

	@Autowired
	public OwnerSubscriptionSettingsRepositoryImpl(OwnerSubscriptionSettingsBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public OwnerSubscriptionSettings save(OwnerSubscriptionSettings settings, String userId) {
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings enableSubscriptions(String userId, BigDecimal monthlyPrice, String profilePitch) {
		OwnerSubscriptionSettings settings = findByUserId(userId).orElse(new OwnerSubscriptionSettings(userId));

		settings.setEnabled(true);
		settings.setMonthlyPrice(monthlyPrice);
		settings.setProfilePitch(profilePitch);

		// Set enabledAt if not already set
		if (settings.getEnabledAt() == null) {
			settings.setEnabledAt(Timestamp.now());
		}

		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings disableSubscriptions(String userId) {
		OwnerSubscriptionSettings settings = findByUserId(userId)
			.orElseThrow(() -> new IllegalStateException("No subscription settings found for user: " + userId));

		settings.setEnabled(false);
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public Optional<OwnerSubscriptionSettings> findByUserId(String userId) {
		return baseRepository.findByUserId(userId);
	}

	@Override
	public boolean isEnabled(String userId) {
		return findByUserId(userId).map(OwnerSubscriptionSettings::isEnabled).orElse(false);
	}

	@Override
	public BigDecimal getMonthlyPrice(String userId) {
		return findByUserId(userId).map(OwnerSubscriptionSettings::getMonthlyPrice).orElse(null);
	}

	@Override
	public OwnerSubscriptionSettings updateMonthlyPrice(String userId, BigDecimal newPrice) {
		OwnerSubscriptionSettings settings = findByUserId(userId)
			.orElseThrow(() -> new IllegalStateException("No subscription settings found for user: " + userId));

		settings.setMonthlyPrice(newPrice);
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings updateProfilePitch(String userId, String pitch) {
		OwnerSubscriptionSettings settings = findByUserId(userId)
			.orElseThrow(() -> new IllegalStateException("No subscription settings found for user: " + userId));

		settings.setProfilePitch(pitch);
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings updateStripeConnectStatus(String userId, String accountId, String status) {
		OwnerSubscriptionSettings settings = findByUserId(userId).orElse(new OwnerSubscriptionSettings(userId));

		settings.setStripeConnectAccountId(accountId);
		settings.setStripeConnectStatus(status);

		// If status is active, payouts are likely enabled
		if ("active".equals(status)) {
			settings.setPayoutsEnabled(true);
		}

		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings incrementSubscriberCount(String userId) {
		OwnerSubscriptionSettings settings = findByUserId(userId)
			.orElseThrow(() -> new IllegalStateException("No subscription settings found for user: " + userId));

		settings.setSubscriberCount(settings.getSubscriberCount() + 1);
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings decrementSubscriberCount(String userId) {
		OwnerSubscriptionSettings settings = findByUserId(userId)
			.orElseThrow(() -> new IllegalStateException("No subscription settings found for user: " + userId));

		int newCount = Math.max(0, settings.getSubscriberCount() - 1);
		settings.setSubscriberCount(newCount);
		return baseRepository.saveForUser(settings, userId);
	}

	@Override
	public OwnerSubscriptionSettings updatePublicStrategyCount(String userId, int count) {
		OwnerSubscriptionSettings settings = findByUserId(userId).orElse(new OwnerSubscriptionSettings(userId));

		settings.setPublicStrategyCount(count);
		return baseRepository.saveForUser(settings, userId);
	}

}
