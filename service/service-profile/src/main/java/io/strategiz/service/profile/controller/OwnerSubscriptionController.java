package io.strategiz.service.profile.controller;

import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.EnableSubscriptionsRequest;
import io.strategiz.service.profile.model.OwnerSubscriptionSettingsResponse;
import io.strategiz.service.profile.model.StripeConnectStatusResponse;
import io.strategiz.service.profile.model.UpdateSubscriptionSettingsRequest;
import io.strategiz.service.profile.service.OwnerSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller for owner subscription management. Allows strategy owners to enable/disable
 * subscriptions and manage their settings.
 *
 * API Endpoints: - GET /v1/owner-subscriptions/settings - Get current settings - POST
 * /v1/owner-subscriptions/enable - Enable subscriptions - POST
 * /v1/owner-subscriptions/disable - Disable subscriptions - PUT
 * /v1/owner-subscriptions/settings - Update settings - GET /v1/owner-subscriptions/status
 * - Check if subscriptions are enabled - GET /v1/owner-subscriptions/public/{ownerId} -
 * Get public info for an owner
 *
 * Stripe Connect Endpoints: - POST /v1/owner-subscriptions/stripe-connect/create - Create
 * Stripe Connect account - GET /v1/owner-subscriptions/stripe-connect/status - Get
 * Connect account status - GET /v1/owner-subscriptions/stripe-connect/onboarding-link -
 * Get onboarding URL - GET /v1/owner-subscriptions/stripe-connect/dashboard-link - Get
 * dashboard URL - POST /v1/owner-subscriptions/stripe-connect/refresh - Refresh Connect
 * status - GET /v1/owner-subscriptions/stripe-connect/configured - Check if Stripe
 * Connect is configured
 */
@RestController
@RequestMapping("/v1/owner-subscriptions")
@Validated
public class OwnerSubscriptionController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(OwnerSubscriptionController.class);

	private final OwnerSubscriptionService ownerSubscriptionService;

	public OwnerSubscriptionController(OwnerSubscriptionService ownerSubscriptionService) {
		this.ownerSubscriptionService = ownerSubscriptionService;
	}

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	/**
	 * Get current owner subscription settings.
	 * @param user The authenticated user
	 * @return The subscription settings
	 */
	@GetMapping("/settings")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<OwnerSubscriptionSettingsResponse> getSettings(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting owner subscription settings for user: {}", userId);

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.getSettings(userId);
		return ResponseEntity.ok(settings);
	}

	/**
	 * Enable owner subscriptions.
	 * @param request The enable request with price and pitch
	 * @param user The authenticated user
	 * @return The updated subscription settings
	 */
	@PostMapping("/enable")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<OwnerSubscriptionSettingsResponse> enableSubscriptions(
			@Valid @RequestBody EnableSubscriptionsRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Enabling subscriptions for user: {} with price: ${}/month", userId, request.getMonthlyPrice());

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.enableSubscriptions(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(settings);
	}

	/**
	 * Disable owner subscriptions. Note: Existing subscribers will remain active until
	 * they cancel.
	 * @param user The authenticated user
	 * @return The updated subscription settings
	 */
	@PostMapping("/disable")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<OwnerSubscriptionSettingsResponse> disableSubscriptions(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Disabling subscriptions for user: {}", userId);

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.disableSubscriptions(userId);
		return ResponseEntity.ok(settings);
	}

	/**
	 * Update owner subscription settings.
	 * @param request The update request
	 * @param user The authenticated user
	 * @return The updated subscription settings
	 */
	@PutMapping("/settings")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<OwnerSubscriptionSettingsResponse> updateSettings(
			@Valid @RequestBody UpdateSubscriptionSettingsRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Updating subscription settings for user: {}", userId);

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.updateSettings(userId, request);
		return ResponseEntity.ok(settings);
	}

	/**
	 * Check if subscriptions are enabled for the current user.
	 * @param user The authenticated user
	 * @return Whether subscriptions are enabled
	 */
	@GetMapping("/status")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> getStatus(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();

		boolean enabled = ownerSubscriptionService.isEnabled(userId);
		return ResponseEntity.ok(Map.of("enabled", enabled, "userId", userId));
	}

	/**
	 * Get the public subscription info for an owner (for potential subscribers). This
	 * endpoint is publicly accessible.
	 * @param ownerId The owner's user ID
	 * @return The public subscription info
	 */
	@GetMapping("/public/{ownerId}")
	public ResponseEntity<Map<String, Object>> getPublicInfo(@PathVariable String ownerId) {
		logger.debug("Getting public subscription info for owner: {}", ownerId);

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.getSettings(ownerId);

		// Only return public info
		return ResponseEntity.ok(Map.of("enabled", settings.isEnabled(), "monthlyPrice", settings.getMonthlyPrice(),
				"currency", settings.getCurrency(), "profilePitch",
				settings.getProfilePitch() != null ? settings.getProfilePitch() : "", "subscriberCount",
				settings.getSubscriberCount(), "publicStrategyCount", settings.getPublicStrategyCount()));
	}

	// === Stripe Connect Endpoints ===

	/**
	 * Create a Stripe Connect account for the current user. This is the first step in
	 * enabling subscriptions - must be done before enabling.
	 * @param user The authenticated user
	 * @return The onboarding URL to complete Stripe Connect setup
	 */
	@PostMapping("/stripe-connect/create")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> createStripeConnectAccount(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Creating Stripe Connect account for user: {}", userId);

		String onboardingUrl = ownerSubscriptionService.createStripeConnectAccount(userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("onboardingUrl", onboardingUrl));
	}

	/**
	 * Get the Stripe Connect account status for the current user.
	 * @param user The authenticated user
	 * @return The Connect account status
	 */
	@GetMapping("/stripe-connect/status")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<StripeConnectStatusResponse> getStripeConnectStatus(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.debug("Getting Stripe Connect status for user: {}", userId);

		StripeConnectStatusResponse status = ownerSubscriptionService.getStripeConnectStatus(userId);
		return ResponseEntity.ok(status);
	}

	/**
	 * Get the Stripe Connect onboarding link. Use this when the user needs to continue or
	 * refresh onboarding.
	 * @param user The authenticated user
	 * @return The onboarding URL
	 */
	@GetMapping("/stripe-connect/onboarding-link")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> getStripeConnectOnboardingLink(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting Stripe Connect onboarding link for user: {}", userId);

		String onboardingUrl = ownerSubscriptionService.getStripeConnectOnboardingLink(userId);
		return ResponseEntity.ok(Map.of("onboardingUrl", onboardingUrl));
	}

	/**
	 * Get the Stripe Express dashboard login link. Allows the owner to view their Stripe
	 * Express dashboard.
	 * @param user The authenticated user
	 * @return The dashboard login URL
	 */
	@GetMapping("/stripe-connect/dashboard-link")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, String>> getStripeConnectDashboardLink(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting Stripe Connect dashboard link for user: {}", userId);

		String dashboardUrl = ownerSubscriptionService.getStripeConnectDashboardLink(userId);
		return ResponseEntity.ok(Map.of("dashboardUrl", dashboardUrl));
	}

	/**
	 * Refresh Stripe Connect status from Stripe API. Call this after user completes
	 * onboarding.
	 * @param user The authenticated user
	 * @return Updated subscription settings
	 */
	@PostMapping("/stripe-connect/refresh")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<OwnerSubscriptionSettingsResponse> refreshStripeConnectStatus(
			@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Refreshing Stripe Connect status for user: {}", userId);

		OwnerSubscriptionSettingsResponse settings = ownerSubscriptionService.refreshStripeConnectStatus(userId);
		return ResponseEntity.ok(settings);
	}

	/**
	 * Check if Stripe Connect is configured for the platform. This is a public endpoint
	 * to check if the subscription feature is available.
	 * @return Whether Stripe Connect is configured
	 */
	@GetMapping("/stripe-connect/configured")
	public ResponseEntity<Map<String, Boolean>> isStripeConnectConfigured() {
		boolean configured = ownerSubscriptionService.isStripeConnectConfigured();
		return ResponseEntity.ok(Map.of("configured", configured));
	}

}
