package io.strategiz.service.base.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * Base controller for all provider-related controllers. Provides common provider patterns
 * following the clean architecture.
 *
 * Feature controllers should extend this and return clean data objects, throwing
 * StrategizException for errors.
 */
public abstract class ProviderBaseController {

	protected final Logger providerLog = LoggerFactory.getLogger("PROVIDER." + getClass().getSimpleName());

	/**
	 * Extract user ID from authentication principal Common utility for all provider
	 * controllers
	 */
	protected String extractUserId(Principal principal) {
		if (principal != null) {
			String userId = principal.getName();
			providerLog.debug("Extracted user ID from principal: {}", userId);
			return userId;
		}

		// For development/testing - should not happen in production
		providerLog.warn("No principal found, using test user ID");
		return "test-user-" + System.currentTimeMillis();
	}

	/**
	 * Generate state parameter for OAuth flows Common utility for provider OAuth
	 */
	protected String generateState(String userId, String context) {
		String state = context + ":" + userId + ":" + System.currentTimeMillis();
		providerLog.debug("Generated state parameter for user: {}, context: {}", userId, context);
		return state;
	}

	/**
	 * Validate state parameter for OAuth callbacks
	 */
	protected boolean validateState(String state, String userId, String expectedContext) {
		if (state == null || !state.startsWith(expectedContext + ":")) {
			providerLog.warn("Invalid state parameter: {}", state);
			return false;
		}

		String[] parts = state.split(":");
		if (parts.length >= 2) {
			String stateUserId = parts[1];
			boolean valid = userId.equals(stateUserId);

			if (!valid) {
				providerLog.warn("State validation failed - expected user: {}, got: {}", userId, stateUserId);
			}

			return valid;
		}

		return false;
	}

	/**
	 * Log provider connection attempt for audit purposes
	 */
	protected void logProviderAttempt(String userId, String action, boolean success) {
		if (success) {
			providerLog.info("PROVIDER_SUCCESS - User: {}, Provider: {}, Action: {}", userId, getProviderId(), action);
		}
		else {
			providerLog.warn("PROVIDER_FAILURE - User: {}, Provider: {}, Action: {}", userId, getProviderId(), action);
		}
	}

	/**
	 * Log OAuth flow steps for debugging
	 */
	protected void logOAuthStep(String userId, String step, String details) {
		providerLog.info("OAUTH_STEP - User: {}, Provider: {}, Step: {}, Details: {}", userId, getProviderId(), step,
				details);
	}

	/**
	 * Get the provider ID for this controller Each provider controller must implement
	 * this
	 */
	protected abstract String getProviderId();

	/**
	 * Get the provider display name Each provider controller must implement this
	 */
	protected abstract String getProviderName();

}