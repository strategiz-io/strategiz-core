package io.strategiz.service.auth.controller.push;

import io.strategiz.business.tokenauth.PushAuthBusiness;
import io.strategiz.client.webpush.WebPushClient;
import io.strategiz.client.webpush.WebPushResult;
import io.strategiz.data.auth.entity.PushSubscriptionEntity;
import io.strategiz.service.auth.model.push.PushAuthInitiateRequest;
import io.strategiz.service.auth.model.push.PushAuthInitiateResponse;
import io.strategiz.service.auth.model.push.PushAuthPollResponse;
import io.strategiz.service.auth.model.push.PushAuthRespondRequest;
import io.strategiz.service.auth.model.push.PushSubscriptionRequest;
import io.strategiz.service.auth.model.push.PushSubscriptionResponse;
import io.strategiz.service.auth.model.push.VapidKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for push notification authentication.
 *
 * <p>
 * Push auth enables users to approve sign-in attempts from registered browsers.
 * </p>
 *
 * <p>
 * Endpoints:
 * </p>
 * <ul>
 * <li>GET /vapid-key - Get VAPID public key for browser subscription</li>
 * <li>POST /subscriptions - Register a push subscription</li>
 * <li>GET /subscriptions - List user's subscriptions</li>
 * <li>DELETE /subscriptions/{id} - Remove a subscription</li>
 * <li>POST /initiate - Initiate push auth (sends notification)</li>
 * <li>GET /{requestId}/poll - Poll for auth status</li>
 * <li>POST /respond - Approve or deny from notification</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/auth/push")
@Tag(name = "Push Authentication", description = "Push notification authentication operations")
public class PushAuthController {

	private static final Logger log = LoggerFactory.getLogger(PushAuthController.class);

	@Value("${vapid.public.key:}")
	private String vapidPublicKey;

	@Autowired
	private PushAuthBusiness pushAuthBusiness;

	@Autowired
	private WebPushClient webPushClient;

	// ==================== VAPID Key ====================

	/**
	 * Get the VAPID public key for browser push subscription. The frontend uses this to
	 * subscribe to push notifications.
	 */
	@GetMapping("/vapid-key")
	@Operation(summary = "Get VAPID public key",
			description = "Get the VAPID public key needed for browser push subscription")
	@ApiResponse(responseCode = "200", description = "VAPID public key")
	public ResponseEntity<VapidKeyResponse> getVapidPublicKey() {
		if (vapidPublicKey == null || vapidPublicKey.isEmpty()) {
			log.warn("VAPID public key not configured");
			return ResponseEntity.internalServerError().build();
		}
		return ResponseEntity.ok(new VapidKeyResponse(vapidPublicKey));
	}

	// ==================== Subscription Management ====================

	/**
	 * Register a push subscription. Called after the browser successfully subscribes to
	 * push notifications.
	 */
	@PostMapping("/subscriptions")
	@Operation(summary = "Register push subscription",
			description = "Register a browser's push subscription for authentication")
	@ApiResponse(responseCode = "200", description = "Subscription registered")
	public ResponseEntity<PushSubscriptionResponse> registerSubscription(@AuthenticationPrincipal String userId,
			@Valid @RequestBody PushSubscriptionRequest request) {

		log.info("Registering push subscription for user {}", userId);

		PushSubscriptionEntity subscription = pushAuthBusiness.registerSubscription(userId, request.endpoint(),
				request.p256dh(), request.auth(), request.deviceName());

		return ResponseEntity.ok(PushSubscriptionResponse.from(subscription));
	}

	/**
	 * List user's push subscriptions.
	 */
	@GetMapping("/subscriptions")
	@Operation(summary = "List push subscriptions", description = "Get all push subscriptions for the current user")
	@ApiResponse(responseCode = "200", description = "List of subscriptions")
	public ResponseEntity<List<PushSubscriptionResponse>> listSubscriptions(@AuthenticationPrincipal String userId) {

		List<PushSubscriptionEntity> subscriptions = pushAuthBusiness.getUserSubscriptions(userId);

		List<PushSubscriptionResponse> response = subscriptions.stream()
			.map(PushSubscriptionResponse::from)
			.collect(Collectors.toList());

		return ResponseEntity.ok(response);
	}

	/**
	 * Remove a push subscription.
	 */
	@DeleteMapping("/subscriptions/{subscriptionId}")
	@Operation(summary = "Remove push subscription", description = "Remove a push subscription")
	@ApiResponse(responseCode = "200", description = "Subscription removed")
	public ResponseEntity<Void> removeSubscription(@AuthenticationPrincipal String userId,
			@Parameter(description = "Subscription ID") @PathVariable String subscriptionId) {

		pushAuthBusiness.removeSubscription(subscriptionId, userId);
		return ResponseEntity.ok().build();
	}

	/**
	 * Toggle push auth for a subscription.
	 */
	@PutMapping("/subscriptions/{subscriptionId}/push-auth")
	@Operation(summary = "Toggle push auth", description = "Enable or disable push authentication for a subscription")
	@ApiResponse(responseCode = "200", description = "Push auth status updated")
	public ResponseEntity<PushSubscriptionResponse> togglePushAuth(@AuthenticationPrincipal String userId,
			@Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
			@RequestParam boolean enabled) {

		return pushAuthBusiness.togglePushAuth(subscriptionId, userId, enabled)
			.map(PushSubscriptionResponse::from)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	// ==================== Push Auth Flow ====================

	/**
	 * Check if a user has push auth available. Called by sign-in page to determine if
	 * push auth can be used.
	 */
	@GetMapping("/available/{userId}")
	@Operation(summary = "Check push auth availability",
			description = "Check if a user has active push subscriptions for authentication")
	@ApiResponse(responseCode = "200", description = "Availability status")
	public ResponseEntity<Map<String, Boolean>> checkAvailability(
			@Parameter(description = "User ID") @PathVariable String userId) {

		boolean available = pushAuthBusiness.hasActiveSubscriptions(userId);
		return ResponseEntity.ok(Map.of("available", available));
	}

	/**
	 * Initiate push authentication. Sends push notification to user's registered devices.
	 */
	@PostMapping("/initiate")
	@Operation(summary = "Initiate push auth", description = "Send push authentication request to user's devices")
	@ApiResponse(responseCode = "200", description = "Push auth initiated")
	public ResponseEntity<PushAuthInitiateResponse> initiatePushAuth(
			@Valid @RequestBody PushAuthInitiateRequest request, HttpServletRequest httpRequest) {

		log.info("Initiating push auth for user {}", request.userId());

		String ipAddress = getClientIp(httpRequest);
		String userAgent = httpRequest.getHeader("User-Agent");
		String location = null; // TODO: Derive from IP using GeoIP

		PushAuthBusiness.InitiatePushAuthResult result = pushAuthBusiness.initiatePushAuth(request.userId(),
				request.getPurposeOrDefault(), ipAddress, location, userAgent);

		if (!result.success()) {
			return ResponseEntity.ok(PushAuthInitiateResponse.from(result));
		}

		// Send push notifications to all active subscriptions
		int successCount = sendPushNotifications(result.subscriptionsToNotify(), result.request().getChallenge(),
				result.request().getPurpose(), ipAddress, location, userAgent);

		log.info("Sent push auth notifications to {} of {} devices for user {}", successCount,
				result.subscriptionsToNotify().size(), request.userId());

		return ResponseEntity.ok(PushAuthInitiateResponse.from(result));
	}

	/**
	 * Send push notifications to all subscriptions and handle failures.
	 * @return number of successful notifications
	 */
	private int sendPushNotifications(List<PushSubscriptionEntity> subscriptions, String challenge, String purpose,
			String ipAddress, String location, String userAgent) {
		if (!webPushClient.isAvailable()) {
			log.warn("WebPushClient is not available, cannot send push notifications");
			return 0;
		}

		int successCount = 0;
		for (PushSubscriptionEntity subscription : subscriptions) {
			try {
				WebPushResult pushResult = webPushClient.sendPushAuthNotification(subscription, challenge, purpose,
						ipAddress, location, userAgent);

				if (pushResult.succeeded()) {
					successCount++;
				}
				else if (pushResult.isSubscriptionGone()) {
					// Subscription is no longer valid, record the failure
					log.info("Push subscription {} is no longer valid, recording failure", subscription.getId());
					pushAuthBusiness.recordSubscriptionFailure(subscription.getId());
				}
				else {
					log.warn("Failed to send push notification to subscription {}: {}", subscription.getId(),
							pushResult.error());
				}
			}
			catch (Exception e) {
				log.error("Error sending push notification to subscription {}: {}", subscription.getId(),
						e.getMessage());
			}
		}

		return successCount;
	}

	/**
	 * Poll for push auth status. Called by sign-in page to check if user has approved.
	 */
	@GetMapping("/{requestId}/poll")
	@Operation(summary = "Poll push auth status", description = "Check the status of a push auth request")
	@ApiResponse(responseCode = "200", description = "Current status")
	public ResponseEntity<PushAuthPollResponse> pollPushAuth(
			@Parameter(description = "Push auth request ID") @PathVariable String requestId) {

		PushAuthBusiness.PollResult result = pushAuthBusiness.pollPushAuth(requestId);
		return ResponseEntity.ok(PushAuthPollResponse.from(result));
	}

	/**
	 * Respond to push auth request. Called from the device that received the push
	 * notification.
	 */
	@PostMapping("/respond")
	@Operation(summary = "Respond to push auth", description = "Approve or deny a push auth request")
	@ApiResponse(responseCode = "200", description = "Response recorded")
	public ResponseEntity<Map<String, Object>> respondToPushAuth(@AuthenticationPrincipal String userId,
			@Valid @RequestBody PushAuthRespondRequest request) {

		log.info("Processing push auth response for challenge (approved: {})", request.approved());

		PushAuthBusiness.PushAuthResponse result;
		if (request.approved()) {
			result = pushAuthBusiness.approvePushAuth(request.challenge(), userId, request.subscriptionId());
		}
		else {
			result = pushAuthBusiness.denyPushAuth(request.challenge(), userId);
		}

		return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
	}

	/**
	 * Cancel a push auth request.
	 */
	@DeleteMapping("/{requestId}")
	@Operation(summary = "Cancel push auth", description = "Cancel a pending push auth request")
	@ApiResponse(responseCode = "200", description = "Cancellation result")
	public ResponseEntity<Void> cancelPushAuth(@AuthenticationPrincipal String userId,
			@Parameter(description = "Push auth request ID") @PathVariable String requestId) {

		boolean cancelled = pushAuthBusiness.cancelPushAuth(requestId, userId);

		if (cancelled) {
			return ResponseEntity.ok().build();
		}
		else {
			return ResponseEntity.notFound().build();
		}
	}

	// ==================== Helper Methods ====================

	/**
	 * Extract client IP address from request.
	 */
	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
