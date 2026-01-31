package io.strategiz.service.profile.controller;

import io.strategiz.business.preferences.service.AIPreferencesService;
import io.strategiz.data.preferences.entity.AIPreferences;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.AIPreferencesResponse;
import io.strategiz.service.profile.model.UpdateAIPreferencesRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing AI preferences. Provides endpoints for configuring preferred AI
 * model, streaming settings, and other AI-related preferences.
 */
@RestController
@RequestMapping("/v1/users/preferences/ai")
@Validated
public class AIPreferencesController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AIPreferencesController.class);

	private final AIPreferencesService aiPreferencesService;

	public AIPreferencesController(AIPreferencesService aiPreferencesService) {
		this.aiPreferencesService = aiPreferencesService;
	}

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	/**
	 * Get current AI preferences.
	 * @param user The authenticated user
	 * @return The AI preferences
	 */
	@GetMapping
	@RequireAuth(minAcr = "1")
	public ResponseEntity<AIPreferencesResponse> getPreferences(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting AI preferences for user {}", userId);

		AIPreferences prefs = aiPreferencesService.getPreferences(userId);
		AIPreferencesResponse response = mapToResponse(prefs);

		return ResponseEntity.ok(response);
	}

	/**
	 * Update AI preferences.
	 * @param request The update request
	 * @param user The authenticated user
	 * @return The updated preferences
	 */
	@PutMapping
	@RequireAuth(minAcr = "1")
	public ResponseEntity<AIPreferencesResponse> updatePreferences(
			@Valid @RequestBody UpdateAIPreferencesRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Updating AI preferences for user {}", userId);

		AIPreferences prefs = mapFromRequest(request, userId);
		AIPreferences updated = aiPreferencesService.updatePreferences(userId, prefs);
		AIPreferencesResponse response = mapToResponse(updated);

		return ResponseEntity.ok(response);
	}

	/**
	 * Update preferred AI model (convenience endpoint).
	 * @param model The model ID
	 * @param user The authenticated user
	 * @return The updated preferences
	 */
	@PutMapping("/model")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<AIPreferencesResponse> updatePreferredModel(@RequestBody Map<String, String> body,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		String model = body.get("model");

		if (model == null || model.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		logger.info("Updating preferred AI model for user {} to {}", userId, model);

		AIPreferences updated = aiPreferencesService.updatePreferredModel(userId, model);
		AIPreferencesResponse response = mapToResponse(updated);

		return ResponseEntity.ok(response);
	}

	/**
	 * Get preferred AI model (convenience endpoint).
	 * @param user The authenticated user
	 * @return The preferred model ID
	 */
	@GetMapping("/model")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> getPreferredModel(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.debug("Getting preferred AI model for user {}", userId);

		String model = aiPreferencesService.getPreferredModel(userId);
		return ResponseEntity.ok(Map.of("model", model));
	}

	// Helper methods

	private AIPreferencesResponse mapToResponse(AIPreferences prefs) {
		AIPreferencesResponse response = new AIPreferencesResponse();
		response.setPreferredModel(prefs.getPreferredModel());
		response.setPreferredProvider(prefs.getPreferredProvider());
		response.setEnableStreaming(prefs.getEnableStreaming());
		response.setSaveHistory(prefs.getSaveHistory());
		response.setMaxTokens(prefs.getMaxTokens());
		response.setTemperature(prefs.getTemperature());
		return response;
	}

	private AIPreferences mapFromRequest(UpdateAIPreferencesRequest request, String userId) {
		AIPreferences prefs = aiPreferencesService.getPreferences(userId);

		if (request.getPreferredModel() != null) {
			prefs.setPreferredModel(request.getPreferredModel());
		}
		if (request.getEnableStreaming() != null) {
			prefs.setEnableStreaming(request.getEnableStreaming());
		}
		if (request.getSaveHistory() != null) {
			prefs.setSaveHistory(request.getSaveHistory());
		}
		if (request.getMaxTokens() != null) {
			prefs.setMaxTokens(request.getMaxTokens());
		}
		if (request.getTemperature() != null) {
			prefs.setTemperature(request.getTemperature());
		}

		return prefs;
	}

}
