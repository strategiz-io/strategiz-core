package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.preferences.entity.SecurityPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SecurityPreferences stored at users/{userId}/preferences/security
 *
 * <p>
 * This repository manages user security preferences including MFA enforcement settings.
 * </p>
 */
@Repository
public class SecurityPreferencesRepository extends SubcollectionRepository<SecurityPreferences> {

	private static final Logger logger = LoggerFactory.getLogger(SecurityPreferencesRepository.class);

	public SecurityPreferencesRepository(Firestore firestore) {
		super(firestore, SecurityPreferences.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	@Override
	protected String getParentCollectionName() {
		return "users";
	}

	@Override
	protected String getSubcollectionName() {
		return "preferences";
	}

	/**
	 * Get security preferences for a user. Returns default preferences if none exist.
	 * @param userId The user ID
	 * @return The security preferences (never null)
	 */
	public SecurityPreferences getByUserId(String userId) {
		validateParentId(userId);

		Optional<SecurityPreferences> existing = findByIdInSubcollection(userId, SecurityPreferences.PREFERENCE_ID);

		if (existing.isPresent()) {
			logger.debug("Found existing security preferences for user: {}", userId);
			return existing.get();
		}

		// Return default preferences (not persisted until updated)
		logger.debug("Returning default security preferences for user: {}", userId);
		SecurityPreferences defaults = new SecurityPreferences();
		defaults.setPreferenceId(SecurityPreferences.PREFERENCE_ID);
		return defaults;
	}

	/**
	 * Find security preferences for a user as Optional.
	 * @param userId The user ID
	 * @return Optional containing preferences if they exist
	 */
	public Optional<SecurityPreferences> findByUserId(String userId) {
		validateParentId(userId);
		return findByIdInSubcollection(userId, SecurityPreferences.PREFERENCE_ID);
	}

	/**
	 * Save security preferences for a user.
	 * @param userId The user ID
	 * @param preferences The preferences to save
	 * @return The saved preferences
	 */
	public SecurityPreferences save(String userId, SecurityPreferences preferences) {
		validateParentId(userId);

		// Ensure the correct document ID
		preferences.setPreferenceId(SecurityPreferences.PREFERENCE_ID);

		logger.info("Saving security preferences for user: {} (mfaEnforced: {})", userId, preferences.getMfaEnforced());
		return saveInSubcollection(userId, preferences, userId);
	}

	/**
	 * Update MFA enforcement setting.
	 * @param userId The user ID
	 * @param enforced Whether MFA should be enforced
	 * @return The updated preferences
	 */
	public SecurityPreferences updateMfaEnforced(String userId, boolean enforced) {
		SecurityPreferences prefs = getByUserId(userId);
		prefs.setMfaEnforced(enforced);
		logger.info("Updating MFA enforcement for user: {} to: {}", userId, enforced);
		return save(userId, prefs);
	}

	/**
	 * Update minimum ACR level required.
	 * @param userId The user ID
	 * @param acrLevel The minimum ACR level (2 or 3)
	 * @return The updated preferences
	 */
	public SecurityPreferences updateMinimumAcrLevel(String userId, int acrLevel) {
		SecurityPreferences prefs = getByUserId(userId);
		prefs.setMinimumAcrLevel(acrLevel);
		logger.info("Updating minimum ACR level for user: {} to: {}", userId, acrLevel);
		return save(userId, prefs);
	}

	/**
	 * Check if MFA is enforced for a user.
	 * @param userId The user ID
	 * @return true if MFA is enforced
	 */
	public boolean isMfaEnforced(String userId) {
		SecurityPreferences prefs = getByUserId(userId);
		return Boolean.TRUE.equals(prefs.getMfaEnforced());
	}

	/**
	 * Disable MFA enforcement for a user. Used when user removes their last MFA method.
	 * @param userId The user ID
	 */
	public void disableMfaEnforcement(String userId) {
		SecurityPreferences prefs = getByUserId(userId);
		if (Boolean.TRUE.equals(prefs.getMfaEnforced())) {
			logger.warn("Auto-disabling MFA enforcement for user: {} (last MFA method removed)", userId);
			prefs.setMfaEnforced(false);
			save(userId, prefs);
		}
	}

}
