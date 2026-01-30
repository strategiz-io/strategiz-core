package io.strategiz.client.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.strategiz.client.stripe.exception.StripeErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Service for handling Stripe webhook events.
 * Verifies signatures and routes events to appropriate handlers.
 */
@Service
public class StripeWebhookService {

	private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

	private final StripeConfig config;

	public StripeWebhookService(StripeConfig config) {
		this.config = config;
	}

	/**
	 * Verify and parse a Stripe webhook event.
	 * @param payload The raw request body
	 * @param sigHeader The Stripe-Signature header
	 * @return The verified Event
	 * @throws StrategizException if signature verification fails
	 */
	public Event verifyAndParseEvent(String payload, String sigHeader) {
		String webhookSecret = config.getWebhookSecret();

		if (webhookSecret == null || webhookSecret.isEmpty()) {
			logger.warn("Webhook secret not configured - skipping signature verification");
			// In development, allow unverified webhooks
			try {
				return Event.GSON.fromJson(payload, Event.class);
			}
			catch (Exception e) {
				throw new StrategizException(StripeErrorDetails.WEBHOOK_EVENT_PROCESSING_FAILED, "client-stripe", e,
						"Failed to parse event");
			}
		}

		try {
			return Webhook.constructEvent(payload, sigHeader, webhookSecret);
		}
		catch (SignatureVerificationException e) {
			logger.error("Webhook signature verification failed: {}", e.getMessage());
			throw new StrategizException(StripeErrorDetails.WEBHOOK_SIGNATURE_VERIFICATION_FAILED, "client-stripe", e);
		}
	}

	/**
	 * Extract the event type from a Stripe event.
	 * @param event The Stripe event
	 * @return The event type string
	 */
	public String getEventType(Event event) {
		return event.getType();
	}

	/**
	 * Parse a checkout.session.completed event for owner subscriptions.
	 * @param event The Stripe event
	 * @return Parsed checkout session data, or empty if not an owner subscription
	 */
	public Optional<OwnerSubscriptionCheckoutData> parseCheckoutCompleted(Event event) {
		if (!"checkout.session.completed".equals(event.getType())) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			logger.warn("Could not deserialize checkout session from event {}", event.getId());
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Session session)) {
			logger.warn("Expected Session but got {} for event {}", stripeObject.getClass().getSimpleName(),
					event.getId());
			return Optional.empty();
		}

		// Check if this is an owner subscription checkout
		Map<String, String> metadata = session.getMetadata();
		if (metadata == null || !"owner_subscription".equals(metadata.get("type"))) {
			logger.debug("Checkout session {} is not an owner subscription", session.getId());
			return Optional.empty();
		}

		String subscriberId = metadata.get("subscriberId");
		String ownerId = metadata.get("ownerId");

		if (subscriberId == null || ownerId == null) {
			logger.warn("Missing metadata in checkout session {}: subscriberId={}, ownerId={}", session.getId(),
					subscriberId, ownerId);
			return Optional.empty();
		}

		return Optional.of(new OwnerSubscriptionCheckoutData(session.getId(), session.getSubscription(), subscriberId,
				ownerId, session.getCustomer(), session.getAmountTotal()));
	}

	/**
	 * Parse a subscription event (created, updated, deleted).
	 * @param event The Stripe event
	 * @return Parsed subscription data, or empty if not an owner subscription
	 */
	public Optional<OwnerSubscriptionEventData> parseSubscriptionEvent(Event event) {
		String eventType = event.getType();
		if (!eventType.startsWith("customer.subscription.")) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			logger.warn("Could not deserialize subscription from event {}", event.getId());
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Subscription subscription)) {
			logger.warn("Expected Subscription but got {} for event {}", stripeObject.getClass().getSimpleName(),
					event.getId());
			return Optional.empty();
		}

		// Check if this is an owner subscription
		Map<String, String> metadata = subscription.getMetadata();
		if (metadata == null || !"owner_subscription".equals(metadata.get("type"))) {
			logger.debug("Subscription {} is not an owner subscription", subscription.getId());
			return Optional.empty();
		}

		String subscriberId = metadata.get("subscriberId");
		String ownerId = metadata.get("ownerId");

		if (subscriberId == null || ownerId == null) {
			logger.warn("Missing metadata in subscription {}: subscriberId={}, ownerId={}", subscription.getId(),
					subscriberId, ownerId);
			return Optional.empty();
		}

		String action = switch (eventType) {
			case "customer.subscription.created" -> "created";
			case "customer.subscription.updated" -> "updated";
			case "customer.subscription.deleted" -> "deleted";
			default -> "unknown";
		};

		return Optional.of(new OwnerSubscriptionEventData(subscription.getId(), subscriberId, ownerId,
				subscription.getStatus(), action, subscription.getCurrentPeriodEnd(),
				subscription.getCancelAtPeriodEnd(), subscription.getCanceledAt()));
	}

	/**
	 * Parse an invoice event for subscription payments.
	 * @param event The Stripe event
	 * @return Parsed invoice data, or empty if not an owner subscription invoice
	 */
	public Optional<OwnerSubscriptionInvoiceData> parseInvoiceEvent(Event event) {
		String eventType = event.getType();
		if (!eventType.startsWith("invoice.")) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			logger.warn("Could not deserialize invoice from event {}", event.getId());
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Invoice invoice)) {
			logger.warn("Expected Invoice but got {} for event {}", stripeObject.getClass().getSimpleName(),
					event.getId());
			return Optional.empty();
		}

		// Check if this invoice is for a subscription
		String subscriptionId = invoice.getSubscription();
		if (subscriptionId == null) {
			return Optional.empty();
		}

		// Check metadata on subscription lines
		Map<String, String> metadata = invoice.getSubscriptionDetails() != null
				? invoice.getSubscriptionDetails().getMetadata() : null;

		if (metadata == null || !"owner_subscription".equals(metadata.get("type"))) {
			// Not an owner subscription invoice
			return Optional.empty();
		}

		String subscriberId = metadata.get("subscriberId");
		String ownerId = metadata.get("ownerId");

		if (subscriberId == null || ownerId == null) {
			logger.warn("Missing metadata in invoice {} subscription", invoice.getId());
			return Optional.empty();
		}

		String action = switch (eventType) {
			case "invoice.paid" -> "paid";
			case "invoice.payment_failed" -> "payment_failed";
			case "invoice.payment_action_required" -> "payment_action_required";
			default -> "unknown";
		};

		return Optional.of(new OwnerSubscriptionInvoiceData(invoice.getId(), subscriptionId, subscriberId, ownerId,
				action, invoice.getAmountPaid(), invoice.getAmountDue(), invoice.getStatus()));
	}

	/**
	 * Parse an account.updated event for Stripe Connect accounts.
	 * @param event The Stripe event
	 * @return Parsed account data, or empty if not relevant
	 */
	public Optional<ConnectAccountEventData> parseConnectAccountEvent(Event event) {
		String eventType = event.getType();
		if (!eventType.startsWith("account.")) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			logger.warn("Could not deserialize account from event {}", event.getId());
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Account account)) {
			logger.warn("Expected Account but got {} for event {}", stripeObject.getClass().getSimpleName(),
					event.getId());
			return Optional.empty();
		}

		// Get the user ID from metadata
		Map<String, String> metadata = account.getMetadata();
		String userId = metadata != null ? metadata.get("userId") : null;

		if (userId == null) {
			logger.debug("Connect account {} has no userId in metadata", account.getId());
			return Optional.empty();
		}

		String action = switch (eventType) {
			case "account.updated" -> "updated";
			case "account.application.authorized" -> "authorized";
			case "account.application.deauthorized" -> "deauthorized";
			default -> "unknown";
		};

		boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();
		boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();
		boolean detailsSubmitted = account.getDetailsSubmitted() != null && account.getDetailsSubmitted();

		return Optional.of(new ConnectAccountEventData(account.getId(), userId, action, chargesEnabled, payoutsEnabled,
				detailsSubmitted));
	}

	/**
	 * Parse a checkout.session.completed event for STRAT pack purchases.
	 * @param event The Stripe event
	 * @return Parsed checkout session data, or empty if not a STRAT pack purchase
	 */
	public Optional<StratPackCheckoutData> parseStratPackCheckoutCompleted(Event event) {
		if (!"checkout.session.completed".equals(event.getType())) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			logger.warn("Could not deserialize checkout session from event {}", event.getId());
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Session session)) {
			logger.warn("Expected Session but got {} for event {}", stripeObject.getClass().getSimpleName(),
					event.getId());
			return Optional.empty();
		}

		// Check if this is a STRAT pack purchase checkout
		Map<String, String> metadata = session.getMetadata();
		if (metadata == null || !"strat_pack_purchase".equals(metadata.get("type"))) {
			logger.debug("Checkout session {} is not a STRAT pack purchase", session.getId());
			return Optional.empty();
		}

		String userId = metadata.get("userId");
		String packId = metadata.get("packId");
		String stratAmountStr = metadata.get("stratAmount");

		if (userId == null || packId == null) {
			logger.warn("Missing metadata in STRAT pack checkout session {}: userId={}, packId={}", session.getId(),
					userId, packId);
			return Optional.empty();
		}

		long stratAmount = 0;
		try {
			if (stratAmountStr != null) {
				stratAmount = Long.parseLong(stratAmountStr);
			}
		}
		catch (NumberFormatException e) {
			logger.warn("Invalid stratAmount in checkout session {}: {}", session.getId(), stratAmountStr);
		}

		return Optional.of(new StratPackCheckoutData(session.getId(), userId, packId, stratAmount,
				session.getCustomer(), session.getAmountTotal()));
	}

	/**
	 * Parse a checkout.session.completed event for platform subscription purchases.
	 * Platform subscriptions have metadata type=platform_subscription and a tier field.
	 * @param event The Stripe event
	 * @return Parsed platform checkout data, or empty if not a platform subscription
	 */
	public Optional<PlatformSubscriptionCheckoutData> parsePlatformCheckoutCompleted(Event event) {
		if (!"checkout.session.completed".equals(event.getType())) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Session session)) {
			return Optional.empty();
		}

		Map<String, String> metadata = session.getMetadata();
		if (metadata == null) {
			return Optional.empty();
		}

		// Platform subscriptions have a "tier" metadata key (and no "type" or type=platform_subscription)
		String tier = metadata.get("tier");
		String userId = metadata.get("userId");
		if (tier == null || userId == null) {
			return Optional.empty();
		}

		// Exclude owner subscriptions and STRAT packs
		String type = metadata.get("type");
		if ("owner_subscription".equals(type) || "strat_pack_purchase".equals(type)) {
			return Optional.empty();
		}

		return Optional.of(new PlatformSubscriptionCheckoutData(session.getId(), userId, tier,
				session.getSubscription(), session.getCustomer(), session.getAmountTotal()));
	}

	/**
	 * Parse an invoice.paid event for platform subscription renewals.
	 * @param event The Stripe event
	 * @return Parsed platform invoice data, or empty if not a platform subscription
	 */
	public Optional<PlatformSubscriptionInvoiceData> parsePlatformInvoiceEvent(Event event) {
		if (!"invoice.paid".equals(event.getType())) {
			return Optional.empty();
		}

		EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
		if (deserializer.getObject().isEmpty()) {
			return Optional.empty();
		}

		StripeObject stripeObject = deserializer.getObject().get();
		if (!(stripeObject instanceof Invoice invoice)) {
			return Optional.empty();
		}

		String subscriptionId = invoice.getSubscription();
		if (subscriptionId == null) {
			return Optional.empty();
		}

		Map<String, String> metadata = invoice.getSubscriptionDetails() != null
				? invoice.getSubscriptionDetails().getMetadata() : null;

		if (metadata == null) {
			return Optional.empty();
		}

		// Exclude owner subscriptions
		if ("owner_subscription".equals(metadata.get("type"))) {
			return Optional.empty();
		}

		String tier = metadata.get("tier");
		String userId = metadata.get("userId");
		if (tier == null || userId == null) {
			return Optional.empty();
		}

		return Optional.of(new PlatformSubscriptionInvoiceData(invoice.getId(), subscriptionId, userId, tier,
				invoice.getAmountPaid()));
	}

	/**
	 * Data from a checkout.session.completed for platform subscription.
	 */
	public record PlatformSubscriptionCheckoutData(String sessionId, String userId, String tier,
			String subscriptionId, String customerId, Long amountTotal) {
	}

	/**
	 * Data from an invoice.paid for platform subscription renewal.
	 */
	public record PlatformSubscriptionInvoiceData(String invoiceId, String subscriptionId, String userId,
			String tier, Long amountPaid) {
	}

	/**
	 * Data from a checkout.session.completed event for STRAT pack purchases.
	 */
	public record StratPackCheckoutData(String sessionId, String userId, String packId, long stratAmount,
			String customerId, Long amountTotal) {
	}

	/**
	 * Data from a checkout.session.completed event for owner subscriptions.
	 */
	public record OwnerSubscriptionCheckoutData(String sessionId, String subscriptionId, String subscriberId,
			String ownerId, String customerId, Long amountTotal) {
	}

	/**
	 * Data from subscription lifecycle events.
	 */
	public record OwnerSubscriptionEventData(String subscriptionId, String subscriberId, String ownerId, String status,
			String action, Long currentPeriodEnd, Boolean cancelAtPeriodEnd, Long canceledAt) {
	}

	/**
	 * Data from invoice events.
	 */
	public record OwnerSubscriptionInvoiceData(String invoiceId, String subscriptionId, String subscriberId,
			String ownerId, String action, Long amountPaid, Long amountDue, String status) {
	}

	/**
	 * Data from Stripe Connect account events.
	 */
	public record ConnectAccountEventData(String accountId, String userId, String action, boolean chargesEnabled,
			boolean payoutsEnabled, boolean detailsSubmitted) {

		/**
		 * Check if the account is fully onboarded and ready to receive payments.
		 */
		public boolean isFullyOnboarded() {
			return chargesEnabled && payoutsEnabled && detailsSubmitted;
		}
	}

}
