package io.strategiz.data.social.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.social.entity.OwnerSubscriptionSettings;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Base repository for OwnerSubscriptionSettings entities using Firestore. Provides
 * Firestore CRUD operations.
 */
@Repository
public class OwnerSubscriptionSettingsBaseRepository extends BaseRepository<OwnerSubscriptionSettings> {

	public OwnerSubscriptionSettingsBaseRepository(Firestore firestore) {
		super(firestore, OwnerSubscriptionSettings.class);
	}

	@Override
	protected String getModuleName() {
		return "data-social";
	}

	/**
	 * Find settings by user ID. Since we use userId as the document ID, this is a simple
	 * findById.
	 */
	public Optional<OwnerSubscriptionSettings> findByUserId(String userId) {
		return findById(userId);
	}

	/**
	 * Check if settings exist for a user.
	 */
	public boolean existsByUserId(String userId) {
		return exists(userId);
	}

	/**
	 * Create or update settings for a user.
	 */
	public OwnerSubscriptionSettings saveForUser(OwnerSubscriptionSettings settings, String userId) {
		// Ensure the settings have the correct ID
		settings.setId(settings.getUserId());
		return save(settings, userId);
	}

	/**
	 * Create new settings for a user with a predefined ID.
	 */
	public OwnerSubscriptionSettings createForUser(OwnerSubscriptionSettings settings, String userId) {
		// Ensure the settings have the correct ID
		settings.setId(settings.getUserId());
		return forceCreate(settings, userId);
	}

}
