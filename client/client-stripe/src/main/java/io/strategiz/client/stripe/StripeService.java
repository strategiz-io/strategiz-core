package io.strategiz.client.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.strategiz.client.stripe.exception.StripeErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Stripe payment operations. Handles checkout sessions, webhooks, and
 * subscription management.
 */
@Service
public class StripeService {

	private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

	private final StripeConfig config;

	public StripeService(StripeConfig config) {
		this.config = config;
	}

	/**
	 * Create a Stripe checkout session for subscription.
	 * @param userId The internal user ID
	 * @param userEmail The user's email
	 * @param tierId The tier to subscribe to (trader, strategist)
	 * @param customerId Existing Stripe customer ID (optional)
	 * @return Checkout session details
	 */
	public CheckoutResult createCheckoutSession(String userId, String userEmail, String tierId, String customerId) {

		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		String priceId = config.getPriceIdForTier(tierId);
		if (priceId == null || priceId.isEmpty()) {
			throw new StrategizException(StripeErrorDetails.INVALID_TIER, "client-stripe", tierId);
		}

		logger.info("Creating checkout session for user {} tier {}", userId, tierId);

		try {
			// Create or get customer
			String stripeCustomerId = customerId;
			if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
				stripeCustomerId = createCustomer(userId, userEmail);
			}

			// Build checkout session
			SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setCustomer(stripeCustomerId)
				.setSuccessUrl(config.getAppBaseUrl() + "/pricing?success=true&session_id={CHECKOUT_SESSION_ID}")
				.setCancelUrl(config.getAppBaseUrl() + "/pricing?canceled=true")
				.addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
				.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
					.putMetadata("userId", userId)
					.putMetadata("tier", tierId)
					.build())
				.putMetadata("userId", userId)
				.putMetadata("tier", tierId);

			// Allow promotion codes
			paramsBuilder.setAllowPromotionCodes(true);

			Session session = Session.create(paramsBuilder.build());

			logger.info("Created checkout session {} for user {}", session.getId(), userId);

			return new CheckoutResult(session.getId(), session.getUrl(), stripeCustomerId);
		}
		catch (StripeException e) {
			logger.error("Failed to create checkout session for user {}: {}", userId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.CHECKOUT_SESSION_CREATION_FAILED, "client-stripe", e,
					userId);
		}
	}

	/**
	 * Create a Stripe customer.
	 * @param userId The internal user ID
	 * @param email The user's email
	 * @return The Stripe customer ID
	 */
	public String createCustomer(String userId, String email) {
		try {
			CustomerCreateParams params = CustomerCreateParams.builder()
				.setEmail(email)
				.putMetadata("userId", userId)
				.build();

			Customer customer = Customer.create(params);
			logger.info("Created Stripe customer {} for user {}", customer.getId(), userId);
			return customer.getId();
		}
		catch (StripeException e) {
			logger.error("Failed to create Stripe customer for user {}: {}", userId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.CUSTOMER_CREATION_FAILED, "client-stripe", e, userId);
		}
	}

	/**
	 * Verify and parse a Stripe webhook event.
	 * @param payload The raw request body
	 * @param signature The Stripe-Signature header
	 * @return The parsed Event, or null if verification fails
	 */
	public Event verifyWebhookSignature(String payload, String signature) {
		try {
			return Webhook.constructEvent(payload, signature, config.getWebhookSecret());
		}
		catch (SignatureVerificationException e) {
			logger.error("Webhook signature verification failed: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Handle a Stripe webhook event.
	 * @param event The verified Stripe event
	 * @return Subscription update data, or null if event should be ignored
	 */
	public SubscriptionUpdate handleWebhookEvent(Event event) {
		String eventType = event.getType();
		logger.info("Handling Stripe webhook event: {}", eventType);

		StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

		if (stripeObject == null) {
			logger.warn("Could not deserialize event data for {}", eventType);
			return null;
		}

		return switch (eventType) {
			case "checkout.session.completed" -> handleCheckoutCompleted((Session) stripeObject);
			case "customer.subscription.created", "customer.subscription.updated" ->
				handleSubscriptionUpdate((Subscription) stripeObject);
			case "customer.subscription.deleted" -> handleSubscriptionCanceled((Subscription) stripeObject);
			case "invoice.payment_succeeded" -> handlePaymentSucceeded((Invoice) stripeObject);
			case "invoice.payment_failed" -> handlePaymentFailed((Invoice) stripeObject);
			default -> {
				logger.debug("Ignoring event type: {}", eventType);
				yield null;
			}
		};
	}

	private SubscriptionUpdate handleCheckoutCompleted(Session session) {
		logger.info("Checkout completed: session={}", session.getId());

		Map<String, String> metadata = session.getMetadata();
		String userId = metadata != null ? metadata.get("userId") : null;
		String tier = metadata != null ? metadata.get("tier") : null;

		if (userId == null) {
			logger.warn("No userId in checkout session metadata");
			return null;
		}

		return new SubscriptionUpdate(userId, tier, "active", session.getCustomer(), session.getSubscription(), null,
				null, false);
	}

	private SubscriptionUpdate handleSubscriptionUpdate(Subscription subscription) {
		logger.info("Subscription updated: id={} status={}", subscription.getId(), subscription.getStatus());

		Map<String, String> metadata = subscription.getMetadata();
		String userId = metadata != null ? metadata.get("userId") : null;
		String tier = metadata != null ? metadata.get("tier") : null;

		if (userId == null) {
			// Try to get from customer metadata
			try {
				Customer customer = Customer.retrieve(subscription.getCustomer());
				if (customer.getMetadata() != null) {
					userId = customer.getMetadata().get("userId");
				}
			}
			catch (StripeException e) {
				logger.warn("Could not retrieve customer for subscription {}", subscription.getId());
			}
		}

		if (userId == null) {
			logger.warn("Could not determine userId for subscription {}", subscription.getId());
			return null;
		}

		Instant periodStart = subscription.getCurrentPeriodStart() != null
				? Instant.ofEpochSecond(subscription.getCurrentPeriodStart()) : null;
		Instant periodEnd = subscription.getCurrentPeriodEnd() != null
				? Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()) : null;

		return new SubscriptionUpdate(userId, tier, subscription.getStatus(), subscription.getCustomer(),
				subscription.getId(), periodStart, periodEnd, subscription.getCancelAtPeriodEnd());
	}

	private SubscriptionUpdate handleSubscriptionCanceled(Subscription subscription) {
		logger.info("Subscription canceled: id={}", subscription.getId());

		// Get userId from metadata or customer
		String userId = null;
		Map<String, String> metadata = subscription.getMetadata();
		if (metadata != null) {
			userId = metadata.get("userId");
		}

		if (userId == null) {
			try {
				Customer customer = Customer.retrieve(subscription.getCustomer());
				if (customer.getMetadata() != null) {
					userId = customer.getMetadata().get("userId");
				}
			}
			catch (StripeException e) {
				logger.warn("Could not retrieve customer for canceled subscription {}", subscription.getId());
			}
		}

		if (userId == null) {
			return null;
		}

		// Downgrade to scout tier
		return new SubscriptionUpdate(userId, "scout", "canceled", subscription.getCustomer(), null, null, null, false);
	}

	private SubscriptionUpdate handlePaymentSucceeded(Invoice invoice) {
		logger.info("Payment succeeded: invoice={}", invoice.getId());
		// Payment success is usually handled via subscription.updated
		return null;
	}

	private SubscriptionUpdate handlePaymentFailed(Invoice invoice) {
		logger.info("Payment failed: invoice={}", invoice.getId());

		String subscriptionId = invoice.getSubscription();
		if (subscriptionId == null) {
			return null;
		}

		try {
			Subscription subscription = Subscription.retrieve(subscriptionId);
			String userId = null;
			if (subscription.getMetadata() != null) {
				userId = subscription.getMetadata().get("userId");
			}

			if (userId == null) {
				Customer customer = Customer.retrieve(subscription.getCustomer());
				if (customer.getMetadata() != null) {
					userId = customer.getMetadata().get("userId");
				}
			}

			if (userId != null) {
				return new SubscriptionUpdate(userId, null, "past_due", subscription.getCustomer(),
						subscription.getId(), null, null, false);
			}
		}
		catch (StripeException e) {
			logger.error("Error handling payment failure: {}", e.getMessage());
		}

		return null;
	}

	/**
	 * Create a Stripe checkout session for one-time marketplace strategy purchase.
	 * @param userId The internal user ID
	 * @param userEmail The user's email
	 * @param strategyId The strategy ID being purchased
	 * @param strategyName The name of the strategy
	 * @param priceInCents The price in cents (e.g., 9900 for $99.00)
	 * @param currency The currency code (e.g., "USD")
	 * @param customerId Existing Stripe customer ID (optional)
	 * @return Checkout session details
	 */
	public CheckoutResult createStrategyCheckoutSession(String userId, String userEmail, String strategyId,
			String strategyName, long priceInCents, String currency, String customerId) {

		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		logger.info("Creating one-time checkout session for user {} strategy {}", userId, strategyId);

		try {
			// Create or get customer
			String stripeCustomerId = customerId;
			if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
				stripeCustomerId = createCustomer(userId, userEmail);
			}

			// Build checkout session for one-time payment
			SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setCustomer(stripeCustomerId)
				.setSuccessUrl(config.getAppBaseUrl() + "/marketplace/purchase-success?session_id={CHECKOUT_SESSION_ID}")
				.setCancelUrl(config.getAppBaseUrl() + "/marketplace?canceled=true")
				.addLineItem(SessionCreateParams.LineItem.builder()
					.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
						.setCurrency(currency.toLowerCase())
						.setUnitAmount(priceInCents)
						.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
							.setName(strategyName)
							.setDescription("Lifetime access to strategy")
							.build())
						.build())
					.setQuantity(1L)
					.build())
				.putMetadata("userId", userId)
				.putMetadata("strategyId", strategyId)
				.putMetadata("type", "strategy_purchase");

			// Allow promotion codes
			paramsBuilder.setAllowPromotionCodes(true);

			Session session = Session.create(paramsBuilder.build());

			logger.info("Created one-time checkout session {} for user {}", session.getId(), userId);

			return new CheckoutResult(session.getId(), session.getUrl(), stripeCustomerId);
		}
		catch (StripeException e) {
			logger.error("Failed to create strategy checkout session for user {}: {}", userId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.CHECKOUT_SESSION_CREATION_FAILED, "client-stripe", e,
					userId);
		}
	}

	/**
	 * Create a Stripe checkout session for strategy subscription (monthly).
	 * Used when a user subscribes to a strategy owner.
	 * @param userId The internal user ID (subscriber)
	 * @param userEmail The user's email
	 * @param strategyId The strategy ID being subscribed to
	 * @param strategyName The name of the strategy
	 * @param ownerId The current owner ID (receives payments)
	 * @param priceInCents The monthly subscription price in cents (e.g., 1900 for $19.00/mo)
	 * @param currency The currency code (e.g., "USD")
	 * @param customerId Existing Stripe customer ID (optional)
	 * @return Checkout session details
	 */
	public CheckoutResult createStrategySubscriptionCheckoutSession(String userId, String userEmail, String strategyId,
			String strategyName, String ownerId, long priceInCents, String currency, String customerId) {

		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		logger.info("Creating strategy subscription checkout for user {} strategy {} owner {}", userId, strategyId,
				ownerId);

		try {
			// Create or get customer
			String stripeCustomerId = customerId;
			if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
				stripeCustomerId = createCustomer(userId, userEmail);
			}

			// Build checkout session for recurring subscription
			SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setCustomer(stripeCustomerId)
				.setSuccessUrl(config.getAppBaseUrl() + "/marketplace/subscribe-success?session_id={CHECKOUT_SESSION_ID}")
				.setCancelUrl(config.getAppBaseUrl() + "/marketplace/strategies/" + strategyId + "?canceled=true")
				.addLineItem(SessionCreateParams.LineItem.builder()
					.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
						.setCurrency(currency.toLowerCase())
						.setUnitAmount(priceInCents)
						.setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
							.setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
							.build())
						.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
							.setName(strategyName + " - Monthly Subscription")
							.setDescription("Deploy alerts and bots with this strategy")
							.build())
						.build())
					.setQuantity(1L)
					.build())
				.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
					.putMetadata("userId", userId)
					.putMetadata("strategyId", strategyId)
					.putMetadata("ownerId", ownerId)
					.putMetadata("type", "strategy_subscription")
					.build())
				.putMetadata("userId", userId)
				.putMetadata("strategyId", strategyId)
				.putMetadata("ownerId", ownerId)
				.putMetadata("type", "strategy_subscription");

			// Allow promotion codes
			paramsBuilder.setAllowPromotionCodes(true);

			Session session = Session.create(paramsBuilder.build());

			logger.info("Created strategy subscription checkout session {} for user {}", session.getId(), userId);

			return new CheckoutResult(session.getId(), session.getUrl(), stripeCustomerId);
		}
		catch (StripeException e) {
			logger.error("Failed to create strategy subscription checkout for user {}: {}", userId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.CHECKOUT_SESSION_CREATION_FAILED, "client-stripe", e,
					userId);
		}
	}

	/**
	 * Update subscription metadata. Used when ownership transfers to route payments
	 * to new owner.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @param metadata Map of metadata to update
	 */
	public void updateSubscriptionMetadata(String stripeSubscriptionId, Map<String, String> metadata) {
		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		try {
			Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

			Map<String, Object> params = new HashMap<>();
			params.put("metadata", metadata);

			subscription.update(params);

			logger.info("Updated subscription {} metadata: {}", stripeSubscriptionId, metadata);
		}
		catch (StripeException e) {
			logger.error("Failed to update subscription {} metadata: {}", stripeSubscriptionId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_UPDATE_FAILED, "client-stripe", e,
					stripeSubscriptionId);
		}
	}

	/**
	 * Retrieve a Stripe subscription by ID.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @return The Stripe Subscription object
	 */
	public Subscription getSubscription(String stripeSubscriptionId) {
		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		try {
			return Subscription.retrieve(stripeSubscriptionId);
		}
		catch (StripeException e) {
			logger.error("Failed to retrieve subscription {}: {}", stripeSubscriptionId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_RETRIEVAL_FAILED, "client-stripe", e,
					stripeSubscriptionId);
		}
	}

	/**
	 * Cancel a Stripe subscription at the end of the billing period.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 */
	public void cancelSubscription(String stripeSubscriptionId) {
		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		try {
			Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

			Map<String, Object> params = new HashMap<>();
			params.put("cancel_at_period_end", true);

			subscription.update(params);

			logger.info("Scheduled subscription {} for cancellation at period end", stripeSubscriptionId);
		}
		catch (StripeException e) {
			logger.error("Failed to cancel subscription {}: {}", stripeSubscriptionId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_CANCELLATION_FAILED, "client-stripe", e,
					stripeSubscriptionId);
		}
	}

	/**
	 * Cancel a Stripe subscription immediately.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 */
	public void cancelSubscriptionImmediately(String stripeSubscriptionId) {
		if (!config.isConfigured()) {
			throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
		}

		try {
			Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
			subscription.cancel();

			logger.info("Canceled subscription {} immediately", stripeSubscriptionId);
		}
		catch (StripeException e) {
			logger.error("Failed to cancel subscription {} immediately: {}", stripeSubscriptionId, e.getMessage());
			throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_CANCELLATION_FAILED, "client-stripe", e,
					stripeSubscriptionId);
		}
	}

	/**
	 * Get the publishable key for frontend use.
	 */
	public String getPublishableKey() {
		return config.getPublishableKey();
	}

	/**
	 * Check if Stripe is configured.
	 */
	public boolean isConfigured() {
		return config.isConfigured();
	}

	/**
	 * Result of creating a checkout session.
	 */
	public record CheckoutResult(String sessionId, String url, String customerId) {
	}

	/**
	 * Subscription update data from webhook.
	 */
	public record SubscriptionUpdate(String userId, String tier, String status, String stripeCustomerId,
			String stripeSubscriptionId, Instant currentPeriodStart, Instant currentPeriodEnd,
			Boolean cancelAtPeriodEnd) {
	}

}
