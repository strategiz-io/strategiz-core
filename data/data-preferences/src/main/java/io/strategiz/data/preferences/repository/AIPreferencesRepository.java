package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.preferences.entity.AIPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AIPreferences stored at users/{userId}/preferences/ai
 */
@Repository
public class AIPreferencesRepository extends SubcollectionRepository<AIPreferences> {

	private static final Logger logger = LoggerFactory.getLogger(AIPreferencesRepository.class);

	private static final String DEFAULT_MODEL = "gemini-3-flash-preview";

	public AIPreferencesRepository(Firestore firestore) {
		super(firestore, AIPreferences.class);
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
	 * Get AI preferences for a user. Creates default preferences if none exist.
	 * @param userId The user ID
	 * @return The AI preferences
	 */
	public AIPreferences getByUserId(String userId) {
		validateParentId(userId);

		Optional<AIPreferences> existing = findByIdInSubcollection(userId, AIPreferences.PREFERENCE_ID);

		if (existing.isPresent()) {
			return existing.get();
		}

		// Return default preferences (not persisted until updated)
		AIPreferences defaults = new AIPreferences();
		defaults.setPreferenceId(AIPreferences.PREFERENCE_ID);
		defaults.setPreferredModel(DEFAULT_MODEL);
		return defaults;
	}

	/**
	 * Save AI preferences for a user.
	 * @param userId The user ID
	 * @param preferences The preferences to save
	 * @return The saved preferences
	 */
	public AIPreferences save(String userId, AIPreferences preferences) {
		validateParentId(userId);

		// Ensure the correct document ID
		preferences.setPreferenceId(AIPreferences.PREFERENCE_ID);

		return saveInSubcollection(userId, preferences, userId);
	}

	/**
	 * Update preferred AI model.
	 * @param userId The user ID
	 * @param model The model ID (e.g., "gemini-3-flash-preview", "claude-opus-4-5")
	 * @return The updated preferences
	 */
	public AIPreferences updatePreferredModel(String userId, String model) {
		AIPreferences prefs = getByUserId(userId);
		prefs.setPreferredModel(model);
		return save(userId, prefs);
	}

	/**
	 * Get the preferred model for a user. Returns default if not set.
	 * @param userId The user ID
	 * @return The preferred model ID
	 */
	public String getPreferredModel(String userId) {
		AIPreferences prefs = getByUserId(userId);
		return prefs.getPreferredModel() != null ? prefs.getPreferredModel() : DEFAULT_MODEL;
	}

}
