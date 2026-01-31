package io.strategiz.business.tokenauth;

import io.strategiz.data.auth.entity.PushAuthRequestEntity;
import io.strategiz.data.auth.entity.PushAuthStatus;
import io.strategiz.data.auth.entity.PushSubscriptionEntity;
import io.strategiz.data.auth.repository.PushAuthRequestRepository;
import io.strategiz.data.auth.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for push notification authentication.
 *
 * Push Auth allows users to approve sign-in attempts from registered browsers/devices.
 *
 * Flow: 1. User registers browser for push notifications (creates PushSubscriptionEntity)
 * 2. On sign-in attempt, system creates PushAuthRequestEntity and sends push notification
 * 3. User approves/denies from the push notification 4. System validates the response and
 * completes authentication
 *
 * Use Cases: - Primary authentication (for returning users with registered devices) - MFA
 * step (as second factor) - Account recovery (to verify device ownership)
 */
@Component
public class PushAuthBusiness {

	private static final Logger log = LoggerFactory.getLogger(PushAuthBusiness.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Value("${push.auth.expiry.minutes:5}")
	private int pushAuthExpiryMinutes;

	@Value("${push.auth.max.pending:3}")
	private int maxPendingRequests;

	private final PushSubscriptionRepository subscriptionRepository;

	private final PushAuthRequestRepository authRequestRepository;

	public PushAuthBusiness(PushSubscriptionRepository subscriptionRepository,
			PushAuthRequestRepository authRequestRepository) {
		this.subscriptionRepository = subscriptionRepository;
		this.authRequestRepository = authRequestRepository;
	}

	// ==================== Subscription Management ====================

	/**
	 * Register or update a push subscription for a user. If the endpoint already exists
	 * for this user, update it.
	 */
	public PushSubscriptionEntity registerSubscription(String userId, String endpoint, String p256dh, String auth,
			String deviceName) {
		log.info("Registering push subscription for user {} on device '{}'", userId, deviceName);

		// Check if subscription already exists for this endpoint
		Optional<PushSubscriptionEntity> existing = subscriptionRepository.findByUserIdAndEndpoint(userId, endpoint);

		PushSubscriptionEntity subscription;
		if (existing.isPresent()) {
			// Update existing subscription
			subscription = existing.get();
			subscription.setP256dh(p256dh);
			subscription.setAuth(auth);
			subscription.setDeviceName(deviceName);
			subscription.setPushAuthEnabled(true);
			subscription.setFailedAttempts(0); // Reset failed attempts on re-registration
			log.info("Updating existing push subscription {} for user {}", subscription.getId(), userId);
		}
		else {
			// Create new subscription using full constructor
			subscription = new PushSubscriptionEntity(userId, endpoint, p256dh, auth);
			subscription.setDeviceName(deviceName);
			log.info("Creating new push subscription for user {}", userId);
		}

		return subscriptionRepository.save(subscription, userId);
	}

	/**
	 * Get all subscriptions for a user.
	 */
	public List<PushSubscriptionEntity> getUserSubscriptions(String userId) {
		return subscriptionRepository.findByUserId(userId);
	}

	/**
	 * Get active (push auth enabled) subscriptions for a user.
	 */
	public List<PushSubscriptionEntity> getActiveSubscriptions(String userId) {
		return subscriptionRepository.findActiveByUserId(userId);
	}

	/**
	 * Check if user has any active push subscriptions.
	 */
	public boolean hasActiveSubscriptions(String userId) {
		return subscriptionRepository.hasActiveSubscriptions(userId);
	}

	/**
	 * Remove a push subscription.
	 */
	public void removeSubscription(String subscriptionId, String userId) {
		Optional<PushSubscriptionEntity> subscription = subscriptionRepository.findById(subscriptionId);
		if (subscription.isPresent() && subscription.get().getUserId().equals(userId)) {
			subscriptionRepository.delete(subscriptionId);
			log.info("Removed push subscription {} for user {}", subscriptionId, userId);
		}
	}

	/**
	 * Remove all subscriptions for a user.
	 */
	public int removeAllSubscriptions(String userId) {
		int count = subscriptionRepository.deleteAllForUser(userId);
		log.info("Removed {} push subscriptions for user {}", count, userId);
		return count;
	}

	/**
	 * Enable/disable push auth for a specific subscription.
	 */
	public Optional<PushSubscriptionEntity> togglePushAuth(String subscriptionId, String userId, boolean enabled) {
		Optional<PushSubscriptionEntity> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
		if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().getUserId().equals(userId)) {
			return Optional.empty();
		}

		PushSubscriptionEntity subscription = subscriptionOpt.get();
		subscription.setPushAuthEnabled(enabled);
		return Optional.of(subscriptionRepository.update(subscription, userId));
	}

	// ==================== Push Auth Request Management ====================

	/**
	 * Initiate a push auth request for a user. Creates a pending request and returns it.
	 * The caller is responsible for sending the push notification.
	 * @param userId The user ID
	 * @param purpose The purpose ("signin", "mfa", "recovery")
	 * @param ipAddress The IP address of the request
	 * @param location The derived location (optional)
	 * @param userAgent The user agent of the request (optional)
	 * @return InitiatePushAuthResult with the request and subscriptions to notify
	 */
	public InitiatePushAuthResult initiatePushAuth(String userId, String purpose, String ipAddress, String location,
			String userAgent) {
		log.info("Initiating push auth for user {} with purpose '{}'", userId, purpose);

		// Check if user has active subscriptions
		List<PushSubscriptionEntity> activeSubscriptions = getActiveSubscriptions(userId);
		if (activeSubscriptions.isEmpty()) {
			log.warn("No active push subscriptions for user {}", userId);
			return new InitiatePushAuthResult(false, "No active push subscriptions", null, List.of());
		}

		// Cancel any existing pending requests (optional: could allow multiple)
		List<PushAuthRequestEntity> existingPending = authRequestRepository.findPendingByUserId(userId);
		if (existingPending.size() >= maxPendingRequests) {
			log.warn("User {} already has {} pending push auth requests, cancelling oldest", userId,
					existingPending.size());
			// Cancel oldest pending requests
			for (int i = 0; i < existingPending.size() - maxPendingRequests + 1; i++) {
				PushAuthRequestEntity oldRequest = existingPending.get(i);
				oldRequest.setStatus(PushAuthStatus.CANCELLED);
				authRequestRepository.update(oldRequest, "system");
			}
		}

		// Generate secure challenge token
		String challenge = generateChallenge();

		// Create push auth request
		PushAuthRequestEntity request = new PushAuthRequestEntity(userId);
		request.setChallenge(challenge);
		request.setStatus(PushAuthStatus.PENDING);
		request.setPurpose(purpose);
		request.setExpiresAt(Instant.now().plusSeconds(pushAuthExpiryMinutes * 60L));
		request.setIpAddress(ipAddress);
		request.setLocation(location);
		request.setUserAgent(userAgent);

		PushAuthRequestEntity savedRequest = authRequestRepository.save(request, userId);

		log.info("Created push auth request {} for user {} with challenge {}", savedRequest.getId(), userId,
				maskChallenge(challenge));

		return new InitiatePushAuthResult(true, null, savedRequest, activeSubscriptions);
	}

	/**
	 * Get the status of a push auth request.
	 */
	public Optional<PushAuthRequestEntity> getPushAuthStatus(String requestId) {
		return authRequestRepository.findById(requestId);
	}

	/**
	 * Get the status of a push auth request by challenge.
	 */
	public Optional<PushAuthRequestEntity> getPushAuthStatusByChallenge(String challenge) {
		return authRequestRepository.findByChallenge(challenge);
	}

	/**
	 * Approve a push auth request. Called when user taps "Approve" on the push
	 * notification.
	 * @param challenge The challenge token from the push notification
	 * @param userId The user ID (for verification)
	 * @param subscriptionId The subscription ID that approved (optional)
	 * @return PushAuthResponse with success status
	 */
	public PushAuthResponse approvePushAuth(String challenge, String userId, String subscriptionId) {
		log.info("Processing push auth approval for challenge {}", maskChallenge(challenge));

		Optional<PushAuthRequestEntity> requestOpt = authRequestRepository.findByChallenge(challenge);
		if (requestOpt.isEmpty()) {
			log.warn("Push auth request not found for challenge {}", maskChallenge(challenge));
			return new PushAuthResponse(false, "Request not found", null);
		}

		PushAuthRequestEntity request = requestOpt.get();

		// Verify user ID matches
		if (!request.getUserId().equals(userId)) {
			log.warn("User ID mismatch for push auth request {} - expected {}, got {}", request.getId(),
					request.getUserId(), userId);
			return new PushAuthResponse(false, "Unauthorized", null);
		}

		// Check if request is still pending
		if (request.getStatus() != PushAuthStatus.PENDING) {
			log.warn("Push auth request {} is not pending (status: {})", request.getId(), request.getStatus());
			return new PushAuthResponse(false, "Request is no longer pending", null);
		}

		// Check if request is expired
		if (request.isExpired()) {
			request.markExpired();
			authRequestRepository.update(request, "system");
			log.warn("Push auth request {} has expired", request.getId());
			return new PushAuthResponse(false, "Request has expired", null);
		}

		// Approve the request
		request.approve(subscriptionId);
		authRequestRepository.update(request, userId);

		log.info("Push auth request {} approved by user {}", request.getId(), userId);
		return new PushAuthResponse(true, "Approved", request);
	}

	/**
	 * Deny a push auth request. Called when user taps "Deny" on the push notification.
	 */
	public PushAuthResponse denyPushAuth(String challenge, String userId) {
		log.info("Processing push auth denial for challenge {}", maskChallenge(challenge));

		Optional<PushAuthRequestEntity> requestOpt = authRequestRepository.findByChallenge(challenge);
		if (requestOpt.isEmpty()) {
			log.warn("Push auth request not found for challenge {}", maskChallenge(challenge));
			return new PushAuthResponse(false, "Request not found", null);
		}

		PushAuthRequestEntity request = requestOpt.get();

		// Verify user ID matches
		if (!request.getUserId().equals(userId)) {
			log.warn("User ID mismatch for push auth denial {} - expected {}, got {}", request.getId(),
					request.getUserId(), userId);
			return new PushAuthResponse(false, "Unauthorized", null);
		}

		// Check if request is still pending
		if (request.getStatus() != PushAuthStatus.PENDING) {
			log.warn("Push auth request {} is not pending (status: {})", request.getId(), request.getStatus());
			return new PushAuthResponse(false, "Request is no longer pending", null);
		}

		// Deny the request
		request.deny();
		authRequestRepository.update(request, userId);

		log.info("Push auth request {} denied by user {}", request.getId(), userId);
		return new PushAuthResponse(true, "Denied", request);
	}

	/**
	 * Cancel a push auth request (by the initiating party).
	 */
	public boolean cancelPushAuth(String requestId, String userId) {
		Optional<PushAuthRequestEntity> requestOpt = authRequestRepository.findById(requestId);
		if (requestOpt.isEmpty()) {
			return false;
		}

		PushAuthRequestEntity request = requestOpt.get();
		if (request.getStatus() != PushAuthStatus.PENDING) {
			return false;
		}

		request.setStatus(PushAuthStatus.CANCELLED);
		authRequestRepository.update(request, userId);

		log.info("Push auth request {} cancelled", requestId);
		return true;
	}

	/**
	 * Poll for push auth completion. Used by the sign-in page to check if the user has
	 * approved/denied.
	 * @param requestId The push auth request ID
	 * @return PollResult with the current status
	 */
	public PollResult pollPushAuth(String requestId) {
		Optional<PushAuthRequestEntity> requestOpt = authRequestRepository.findById(requestId);
		if (requestOpt.isEmpty()) {
			return new PollResult(PushAuthStatus.CANCELLED, "Request not found", null);
		}

		PushAuthRequestEntity request = requestOpt.get();

		// Check for expiration
		if (request.getStatus() == PushAuthStatus.PENDING && request.isExpired()) {
			request.markExpired();
			authRequestRepository.update(request, "system");
		}

		return new PollResult(request.getStatus(), null, request);
	}

	// ==================== Maintenance ====================

	/**
	 * Mark expired push auth requests. Should be called periodically by a scheduled job.
	 */
	public int markExpiredRequests() {
		int count = authRequestRepository.markExpired();
		if (count > 0) {
			log.info("Marked {} push auth requests as expired", count);
		}
		return count;
	}

	/**
	 * Delete old push auth requests (for cleanup).
	 */
	public int deleteOldRequests(int olderThanHours) {
		int count = authRequestRepository.deleteOldRequests(olderThanHours);
		if (count > 0) {
			log.info("Deleted {} old push auth requests (older than {} hours)", count, olderThanHours);
		}
		return count;
	}

	/**
	 * Increment failed attempts for a subscription. After too many failures, the
	 * subscription may be deactivated.
	 */
	public void recordSubscriptionFailure(String subscriptionId) {
		Optional<PushSubscriptionEntity> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
		if (subscriptionOpt.isEmpty()) {
			return;
		}

		PushSubscriptionEntity subscription = subscriptionOpt.get();
		subscription.recordFailure();

		// Check if subscription should be deactivated (after 5 failures, isValid returns
		// false)
		if (!subscription.isValid()) {
			subscription.setPushAuthEnabled(false);
			log.warn("Deactivated push subscription {} due to {} failed attempts", subscriptionId,
					subscription.getFailedAttempts());
		}

		subscriptionRepository.update(subscription, "system");
	}

	// ==================== Helper Methods ====================

	/**
	 * Generate a secure random challenge token.
	 */
	private String generateChallenge() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Mask challenge for logging.
	 */
	private String maskChallenge(String challenge) {
		if (challenge == null || challenge.length() < 8) {
			return "****";
		}
		return challenge.substring(0, 4) + "***" + challenge.substring(challenge.length() - 4);
	}

	// ==================== Result Records ====================

	/**
	 * Result of initiating a push auth request.
	 */
	public record InitiatePushAuthResult(boolean success, String error, PushAuthRequestEntity request,
			List<PushSubscriptionEntity> subscriptionsToNotify) {
	}

	/**
	 * Result of approving/denying a push auth request.
	 */
	public record PushAuthResponse(boolean success, String message, PushAuthRequestEntity request) {
	}

	/**
	 * Result of polling for push auth status.
	 */
	public record PollResult(PushAuthStatus status, String error, PushAuthRequestEntity request) {
	}

}
