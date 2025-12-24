package io.strategiz.business.preferences.service;

import io.strategiz.business.preferences.exception.PreferencesErrorDetails;
import io.strategiz.data.preferences.entity.AIPreferences;
import io.strategiz.data.preferences.repository.AIPreferencesRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing AI preferences. Handles model selection and AI settings.
 */
@Service
public class AIPreferencesService {

	private static final Logger logger = LoggerFactory.getLogger(AIPreferencesService.class);

	// Valid models (kept in sync with LLMRouter)
	private static final List<String> VALID_MODELS = List.of("gemini-3-flash-preview", "gemini-3-pro-preview",
			"gemini-2.0-flash", "claude-opus-4-5", "claude-sonnet-4", "claude-haiku-4-5");

	private final AIPreferencesRepository repository;

	public AIPreferencesService(AIPreferencesRepository repository) {
		this.repository = repository;
	}

	/**
	 * Get AI preferences for a user.
	 * @param userId The user ID
	 * @return The AI preferences
	 */
	public AIPreferences getPreferences(String userId) {
		logger.debug("Getting AI preferences for user {}", userId);
		return repository.getByUserId(userId);
	}

	/**
	 * Update AI preferences.
	 * @param userId The user ID
	 * @param preferences The preferences to update
	 * @return The updated preferences
	 */
	public AIPreferences updatePreferences(String userId, AIPreferences preferences) {
		logger.info("Updating AI preferences for user {}", userId);

		// Validate model if provided
		if (preferences.getPreferredModel() != null) {
			validateModel(preferences.getPreferredModel());
		}

		// Validate temperature if provided
		if (preferences.getTemperature() != null) {
			if (preferences.getTemperature() < 0 || preferences.getTemperature() > 2) {
				throw new StrategizException(
						PreferencesErrorDetails.INVALID_TEMPERATURE,
						"business-preferences",
						"Temperature must be between 0 and 2"
				);
			}
		}

		// Validate maxTokens if provided
		if (preferences.getMaxTokens() != null) {
			if (preferences.getMaxTokens() < 1 || preferences.getMaxTokens() > 100000) {
				throw new StrategizException(
						PreferencesErrorDetails.INVALID_MAX_TOKENS,
						"business-preferences",
						"Max tokens must be between 1 and 100000"
				);
			}
		}

		return repository.save(userId, preferences);
	}

	/**
	 * Update preferred AI model.
	 * @param userId The user ID
	 * @param model The model ID
	 * @return The updated preferences
	 */
	public AIPreferences updatePreferredModel(String userId, String model) {
		logger.info("Updating preferred AI model for user {} to {}", userId, model);
		validateModel(model);
		return repository.updatePreferredModel(userId, model);
	}

	/**
	 * Get the preferred model for a user.
	 * @param userId The user ID
	 * @return The preferred model ID
	 */
	public String getPreferredModel(String userId) {
		return repository.getPreferredModel(userId);
	}

	/**
	 * Validate that the model is supported.
	 */
	private void validateModel(String model) {
		if (!VALID_MODELS.contains(model)) {
			throw new StrategizException(
					PreferencesErrorDetails.INVALID_MODEL,
					"business-preferences",
					"Invalid model: " + model + ". Valid models are: " + String.join(", ", VALID_MODELS)
			);
		}
	}

}
