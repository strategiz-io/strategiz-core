package io.strategiz.service.profile.service;

import com.google.cloud.Timestamp;
import io.strategiz.client.stripe.StripeConnectService;
import io.strategiz.client.stripe.StripeConnectService.OwnerSubscriptionCheckoutResult;
import io.strategiz.data.social.entity.OwnerSubscriptionSettings;
import io.strategiz.data.social.entity.UserSubscription;
import io.strategiz.data.social.repository.OwnerSubscriptionSettingsRepository;
import io.strategiz.data.social.repository.UserSubscriptionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.SubscriberResponse;
import io.strategiz.service.profile.model.StrategyAccessCheckResponse;
import io.strategiz.service.profile.model.UserSubscriptionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing user-to-owner subscriptions.
 * Users subscribe to OWNERS (not individual strategies) to deploy all of the owner's PUBLIC
 * strategies.
 */
@Service
public class UserSubscriptionService extends BaseService {

	private static final String MODULE_NAME = "service-profile";

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	private final UserSubscriptionRepository userSubscriptionRepository;

	private final OwnerSubscriptionSettingsRepository ownerSubscriptionSettingsRepository;

	private final UserRepository userRepository;

	private final StripeConnectService stripeConnectService;

	private final OwnerSubscriptionService ownerSubscriptionService;

	public UserSubscriptionService(UserSubscriptionRepository userSubscriptionRepository,
			OwnerSubscriptionSettingsRepository ownerSubscriptionSettingsRepository, UserRepository userRepository,
			StripeConnectService stripeConnectService, OwnerSubscriptionService ownerSubscriptionService) {
		this.userSubscriptionRepository = userSubscriptionRepository;
		this.ownerSubscriptionSettingsRepository = ownerSubscriptionSettingsRepository;
		this.userRepository = userRepository;
		this.stripeConnectService = stripeConnectService;
		this.ownerSubscriptionService = ownerSubscriptionService;
	}

	/**
	 * Create a checkout session to subscribe to an owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @param stripeCustomerId Existing Stripe customer ID (optional)
	 * @return Checkout URL for Stripe
	 */
	public Map<String, Object> createSubscriptionCheckout(String subscriberId, String ownerId,
			String stripeCustomerId) {
		log.info("Creating subscription checkout: subscriber={}, owner={}", subscriberId, ownerId);

		// Validate subscriber exists
		Optional<UserEntity> subscriberOpt = userRepository.findById(subscriberId);
		if (subscriberOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, MODULE_NAME,
					"Subscriber not found: " + subscriberId);
		}

		// Cannot subscribe to self
		if (subscriberId.equals(ownerId)) {
			throw new StrategizException(ProfileErrors.CANNOT_SUBSCRIBE_TO_SELF, MODULE_NAME,
					"Cannot subscribe to yourself");
		}

		// Check if already subscribed
		if (userSubscriptionRepository.hasActiveSubscription(subscriberId, ownerId)) {
			throw new StrategizException(ProfileErrors.USER_SUBSCRIPTION_ALREADY_EXISTS, MODULE_NAME,
					"Already subscribed to this owner");
		}

		// Get owner settings and validate
		Optional<OwnerSubscriptionSettings> ownerSettingsOpt = ownerSubscriptionSettingsRepository.findByUserId(ownerId);
		if (ownerSettingsOpt.isEmpty() || !ownerSettingsOpt.get().isEnabled()) {
			throw new StrategizException(ProfileErrors.OWNER_NOT_ACCEPTING_SUBSCRIPTIONS, MODULE_NAME,
					"Owner is not accepting subscriptions");
		}

		OwnerSubscriptionSettings ownerSettings = ownerSettingsOpt.get();

		// Validate owner has active Stripe Connect
		if (!ownerSettings.hasActiveStripeConnect()) {
			throw new StrategizException(ProfileErrors.OWNER_STRIPE_NOT_READY, MODULE_NAME,
					"Owner's payment setup is not complete");
		}

		UserEntity subscriber = subscriberOpt.get();
		String subscriberEmail = subscriber.getProfile().getEmail();
		long priceInCents = ownerSettings.getMonthlyPrice().multiply(new BigDecimal("100")).longValue();

		// Create Stripe checkout session
		OwnerSubscriptionCheckoutResult checkout = stripeConnectService.createOwnerSubscriptionCheckout(subscriberId,
				subscriberEmail, ownerId, ownerSettings.getStripeConnectAccountId(), priceInCents, stripeCustomerId);

		log.info("Created subscription checkout session {} for subscriber {}", checkout.sessionId(), subscriberId);

		return Map.of("checkoutUrl", checkout.url(), "sessionId", checkout.sessionId(), "customerId",
				checkout.customerId(), "priceInCents", checkout.priceInCents(), "platformFeeInCents",
				checkout.platformFeeInCents());
	}

	/**
	 * Create a subscription record after successful Stripe payment. Called from Stripe webhook.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @param monthlyPrice The price paid
	 * @return The created subscription
	 */
	public UserSubscription createSubscription(String subscriberId, String ownerId, String stripeSubscriptionId,
			BigDecimal monthlyPrice) {
		log.info("Creating subscription record: subscriber={}, owner={}, stripeSubscriptionId={}", subscriberId,
				ownerId, stripeSubscriptionId);

		// Check if already exists
		Optional<UserSubscription> existingOpt = userSubscriptionRepository.findBySubscriberIdAndOwnerId(subscriberId,
				ownerId);
		if (existingOpt.isPresent()) {
			UserSubscription existing = existingOpt.get();
			if (existing.isActive()) {
				log.warn("Subscription already exists and is active: {}", existing.getId());
				return existing;
			}
			// Reactivate existing subscription
			existing.setStatus("ACTIVE");
			existing.setStripeSubscriptionId(stripeSubscriptionId);
			existing.setMonthlyPrice(monthlyPrice);
			existing.setSubscribedAt(Timestamp.now());
			existing.setCancelledAt(null);
			existing.setCancellationReason(null);
			return userSubscriptionRepository.save(existing, subscriberId);
		}

		// Create new subscription
		UserSubscription subscription = new UserSubscription();
		subscription.setSubscriberId(subscriberId);
		subscription.setOwnerId(ownerId);
		subscription.setTier("BASIC"); // Default tier
		subscription.setMonthlyPrice(monthlyPrice);
		subscription.setStatus("ACTIVE");
		subscription.setSubscribedAt(Timestamp.now());
		subscription.setStripeSubscriptionId(stripeSubscriptionId);

		UserSubscription saved = userSubscriptionRepository.save(subscription, subscriberId);

		// Increment owner's subscriber count
		ownerSubscriptionService.incrementSubscriberCount(ownerId);

		log.info("Created subscription {} for subscriber {} to owner {}", saved.getId(), subscriberId, ownerId);
		return saved;
	}

	/**
	 * Cancel a subscription.
	 * @param subscriptionId The subscription ID
	 * @param userId The user requesting cancellation
	 * @param reason The cancellation reason
	 * @return The cancelled subscription
	 */
	public UserSubscriptionResponse cancelSubscription(String subscriptionId, String userId, String reason) {
		log.info("Cancelling subscription {} by user {}", subscriptionId, userId);

		Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findById(subscriptionId);
		if (subscriptionOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.USER_SUBSCRIPTION_NOT_FOUND, MODULE_NAME,
					"Subscription not found: " + subscriptionId);
		}

		UserSubscription subscription = subscriptionOpt.get();

		// Only subscriber can cancel their subscription
		if (!subscription.getSubscriberId().equals(userId)) {
			throw new StrategizException(ProfileErrors.PROFILE_ACCESS_DENIED, MODULE_NAME,
					"Not authorized to cancel this subscription");
		}

		// Cancel in Stripe (at period end)
		if (subscription.hasStripeSubscription()) {
			stripeConnectService.cancelOwnerSubscription(subscription.getStripeSubscriptionId());
		}

		// Update subscription record
		UserSubscription cancelled = userSubscriptionRepository.cancel(subscriptionId,
				reason != null ? reason : "User cancelled");

		// Decrement owner's subscriber count
		ownerSubscriptionService.decrementSubscriberCount(subscription.getOwnerId());

		log.info("Cancelled subscription {}", subscriptionId);
		return enrichSubscriptionResponse(UserSubscriptionResponse.fromEntity(cancelled));
	}

	/**
	 * Get user's subscriptions (what the user is subscribed to).
	 * @param userId The user ID
	 * @return List of subscriptions with owner info
	 */
	public List<UserSubscriptionResponse> getMySubscriptions(String userId) {
		log.debug("Getting subscriptions for user {}", userId);

		List<UserSubscription> subscriptions = userSubscriptionRepository.findBySubscriberId(userId);

		return subscriptions.stream()
			.map(UserSubscriptionResponse::fromEntity)
			.map(this::enrichSubscriptionResponse)
			.collect(Collectors.toList());
	}

	/**
	 * Get owner's subscribers.
	 * @param ownerId The owner's user ID
	 * @return List of subscribers with user info
	 */
	public List<SubscriberResponse> getMySubscribers(String ownerId) {
		log.debug("Getting subscribers for owner {}", ownerId);

		List<UserSubscription> subscriptions = userSubscriptionRepository.findActiveByOwnerId(ownerId);

		return subscriptions.stream()
			.map(SubscriberResponse::fromEntity)
			.map(this::enrichSubscriberResponse)
			.collect(Collectors.toList());
	}

	/**
	 * Check if a user has access to deploy a strategy.
	 * @param userId The user ID
	 * @param strategyOwnerId The strategy owner's user ID
	 * @return Access check result
	 */
	public StrategyAccessCheckResponse checkAccess(String userId, String strategyOwnerId) {
		log.debug("Checking access for user {} to owner {}'s strategies", userId, strategyOwnerId);

		// User is the owner - always has access
		if (userId.equals(strategyOwnerId)) {
			return StrategyAccessCheckResponse.ownerAccess();
		}

		// Check for active subscription
		if (userSubscriptionRepository.hasActiveSubscription(userId, strategyOwnerId)) {
			return StrategyAccessCheckResponse.subscriptionAccess(strategyOwnerId);
		}

		// No access - check if owner accepts subscriptions
		Optional<OwnerSubscriptionSettings> ownerSettingsOpt = ownerSubscriptionSettingsRepository
			.findByUserId(strategyOwnerId);

		BigDecimal price = BigDecimal.ZERO;
		if (ownerSettingsOpt.isPresent() && ownerSettingsOpt.get().isEnabled()) {
			price = ownerSettingsOpt.get().getMonthlyPrice();
		}

		StrategyAccessCheckResponse response = StrategyAccessCheckResponse.noAccess(strategyOwnerId, price);

		// Enrich with owner info
		Optional<UserEntity> ownerOpt = userRepository.findById(strategyOwnerId);
		if (ownerOpt.isPresent()) {
			response.setOwnerUsername(ownerOpt.get().getProfile().getName());
		}

		return response;
	}

	/**
	 * Check if a user has access to a specific strategy.
	 * @param userId The user ID
	 * @param strategyOwnerId The strategy owner ID
	 * @return True if user can deploy the strategy
	 */
	public boolean hasAccessToStrategy(String userId, String strategyOwnerId) {
		// Owner always has access
		if (userId.equals(strategyOwnerId)) {
			return true;
		}

		// Check subscription
		return userSubscriptionRepository.hasActiveSubscription(userId, strategyOwnerId);
	}

	/**
	 * Handle Stripe webhook for subscription created/updated.
	 * @param subscriberId The subscriber user ID
	 * @param ownerId The owner user ID
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @param status The subscription status
	 */
	public void handleStripeSubscriptionUpdate(String subscriberId, String ownerId, String stripeSubscriptionId,
			String status) {
		log.info("Handling Stripe subscription update: subscriber={}, owner={}, status={}", subscriberId, ownerId,
				status);

		Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository
			.findByStripeSubscriptionId(stripeSubscriptionId);

		if (subscriptionOpt.isEmpty()) {
			// This is a new subscription - create it
			Optional<OwnerSubscriptionSettings> ownerSettingsOpt = ownerSubscriptionSettingsRepository
				.findByUserId(ownerId);
			BigDecimal price = ownerSettingsOpt.map(OwnerSubscriptionSettings::getMonthlyPrice)
				.orElse(BigDecimal.ZERO);

			createSubscription(subscriberId, ownerId, stripeSubscriptionId, price);
			return;
		}

		UserSubscription subscription = subscriptionOpt.get();

		// Update status based on Stripe status
		String newStatus = switch (status) {
			case "active" -> "ACTIVE";
			case "past_due" -> "ACTIVE"; // Keep active but mark as past due
			case "canceled", "unpaid" -> "CANCELLED";
			default -> subscription.getStatus();
		};

		if (!newStatus.equals(subscription.getStatus())) {
			userSubscriptionRepository.updateStatus(subscription.getId(), newStatus);

			// Update subscriber count if status changed to/from active
			if ("ACTIVE".equals(newStatus) && !"ACTIVE".equals(subscription.getStatus())) {
				ownerSubscriptionService.incrementSubscriberCount(ownerId);
			}
			else if (!"ACTIVE".equals(newStatus) && "ACTIVE".equals(subscription.getStatus())) {
				ownerSubscriptionService.decrementSubscriberCount(ownerId);
			}
		}
	}

	/**
	 * Handle Stripe webhook for subscription deleted.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 */
	public void handleStripeSubscriptionDeleted(String stripeSubscriptionId) {
		log.info("Handling Stripe subscription deleted: {}", stripeSubscriptionId);

		Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository
			.findByStripeSubscriptionId(stripeSubscriptionId);

		if (subscriptionOpt.isEmpty()) {
			log.warn("Subscription not found for Stripe ID: {}", stripeSubscriptionId);
			return;
		}

		UserSubscription subscription = subscriptionOpt.get();

		if (subscription.isActive()) {
			userSubscriptionRepository.cancel(subscription.getId(), "Stripe subscription ended");
			ownerSubscriptionService.decrementSubscriberCount(subscription.getOwnerId());
		}
	}

	// === Private helper methods ===

	private UserSubscriptionResponse enrichSubscriptionResponse(UserSubscriptionResponse response) {
		Optional<UserEntity> ownerOpt = userRepository.findById(response.getOwnerId());
		if (ownerOpt.isPresent()) {
			UserEntity owner = ownerOpt.get();
			response.setOwnerDisplayName(owner.getProfile().getName());
			response.setOwnerUsername(owner.getProfile().getName()); // Use name as username for now
			response.setOwnerAvatarUrl(owner.getProfile().getPhotoURL());
		}

		// Get public strategy count
		Optional<OwnerSubscriptionSettings> ownerSettingsOpt = ownerSubscriptionSettingsRepository
			.findByUserId(response.getOwnerId());
		if (ownerSettingsOpt.isPresent()) {
			response.setPublicStrategyCount(ownerSettingsOpt.get().getPublicStrategyCount());
		}

		return response;
	}

	private SubscriberResponse enrichSubscriberResponse(SubscriberResponse response) {
		Optional<UserEntity> subscriberOpt = userRepository.findById(response.getSubscriberId());
		if (subscriberOpt.isPresent()) {
			UserEntity subscriber = subscriberOpt.get();
			response.setSubscriberDisplayName(subscriber.getProfile().getName());
			response.setSubscriberUsername(subscriber.getProfile().getName()); // Use name as username for now
			response.setSubscriberAvatarUrl(subscriber.getProfile().getPhotoURL());
		}
		return response;
	}

}
