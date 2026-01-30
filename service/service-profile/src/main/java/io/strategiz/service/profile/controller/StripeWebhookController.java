package io.strategiz.service.profile.controller;

import io.strategiz.business.cryptotoken.CryptoTokenBusiness;
import io.strategiz.business.preferences.service.TokenUsageService;
import io.strategiz.client.stripe.StripeWebhookService;
import io.strategiz.client.stripe.StripeWebhookService.ConnectAccountEventData;
import io.strategiz.client.stripe.StripeWebhookService.OwnerSubscriptionCheckoutData;
import io.strategiz.client.stripe.StripeWebhookService.OwnerSubscriptionEventData;
import io.strategiz.client.stripe.StripeWebhookService.OwnerSubscriptionInvoiceData;
import io.strategiz.client.stripe.StripeWebhookService.PlatformSubscriptionCheckoutData;
import io.strategiz.client.stripe.StripeWebhookService.PlatformSubscriptionInvoiceData;
import io.strategiz.client.stripe.StripeWebhookService.StratPackCheckoutData;
import io.strategiz.client.stripe.event.StratPackPurchaseEvent;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.service.profile.service.OwnerSubscriptionService;
import io.strategiz.service.profile.service.UserSubscriptionService;
import org.springframework.context.ApplicationEventPublisher;
import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Controller for handling Stripe webhook events.
 *
 * Handles subscription lifecycle events:
 * - checkout.session.completed: New subscription created
 * - customer.subscription.created: Subscription activated
 * - customer.subscription.updated: Subscription status changed
 * - customer.subscription.deleted: Subscription cancelled/expired
 * - invoice.paid: Payment successful
 * - invoice.payment_failed: Payment failed
 *
 * Webhook endpoint: POST /v1/webhooks/stripe
 */
@RestController
@RequestMapping("/v1/webhooks")
public class StripeWebhookController {

	private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

	private final StripeWebhookService stripeWebhookService;

	private final UserSubscriptionService userSubscriptionService;

	private final OwnerSubscriptionService ownerSubscriptionService;

	private final ApplicationEventPublisher eventPublisher;

	private final CryptoTokenBusiness cryptoTokenBusiness;

	private final TokenUsageService tokenUsageService;

	public StripeWebhookController(StripeWebhookService stripeWebhookService,
			UserSubscriptionService userSubscriptionService, OwnerSubscriptionService ownerSubscriptionService,
			ApplicationEventPublisher eventPublisher, CryptoTokenBusiness cryptoTokenBusiness,
			TokenUsageService tokenUsageService) {
		this.stripeWebhookService = stripeWebhookService;
		this.userSubscriptionService = userSubscriptionService;
		this.ownerSubscriptionService = ownerSubscriptionService;
		this.eventPublisher = eventPublisher;
		this.cryptoTokenBusiness = cryptoTokenBusiness;
		this.tokenUsageService = tokenUsageService;
	}

	/**
	 * Handle Stripe webhook events.
	 * @param payload The raw request body
	 * @param sigHeader The Stripe-Signature header
	 * @return 200 OK if processed, error status otherwise
	 */
	@PostMapping("/stripe")
	public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

		logger.info("Received Stripe webhook");

		try {
			// Verify and parse the event
			Event event = stripeWebhookService.verifyAndParseEvent(payload, sigHeader);
			String eventType = stripeWebhookService.getEventType(event);

			logger.info("Processing Stripe event: {} ({})", eventType, event.getId());

			// Route to appropriate handler based on event type
			switch (eventType) {
				case "checkout.session.completed" -> handleCheckoutCompleted(event);
				case "customer.subscription.created" -> handleSubscriptionCreated(event);
				case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
				case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
				case "invoice.paid" -> handleInvoicePaid(event);
				case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
				case "account.updated" -> handleAccountUpdated(event);
				default -> logger.debug("Unhandled event type: {}", eventType);
			}

			return ResponseEntity.ok("Webhook processed");
		}
		catch (Exception e) {
			logger.error("Error processing Stripe webhook: {}", e.getMessage(), e);
			// Return 200 to prevent Stripe from retrying (we'll handle the error internally)
			// Only return error for signature verification failures
			if (e.getMessage() != null && e.getMessage().contains("signature")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Signature verification failed");
			}
			return ResponseEntity.ok("Webhook received with error: " + e.getMessage());
		}
	}

	/**
	 * Handle checkout.session.completed event. This is when a user completes the Stripe checkout
	 * and the subscription is created (for owner subscriptions) or a one-time purchase is completed
	 * (for STRAT pack purchases).
	 */
	private void handleCheckoutCompleted(Event event) {
		// First, check if this is a STRAT pack purchase
		Optional<StratPackCheckoutData> stratPackOpt = stripeWebhookService.parseStratPackCheckoutCompleted(event);
		if (stratPackOpt.isPresent()) {
			handleStratPackCheckoutCompleted(stratPackOpt.get());
			return;
		}

		// Check if this is a platform subscription checkout (Strategist/Quant)
		Optional<PlatformSubscriptionCheckoutData> platformOpt = stripeWebhookService
				.parsePlatformCheckoutCompleted(event);
		if (platformOpt.isPresent()) {
			handlePlatformSubscriptionCheckout(platformOpt.get());
			return;
		}

		// Then check if this is an owner subscription checkout
		Optional<OwnerSubscriptionCheckoutData> dataOpt = stripeWebhookService.parseCheckoutCompleted(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Checkout session is not an owner subscription or STRAT pack, skipping");
			return;
		}

		OwnerSubscriptionCheckoutData data = dataOpt.get();
		logger.info("Processing owner subscription checkout: subscriber={}, owner={}, subscriptionId={}",
				data.subscriberId(), data.ownerId(), data.subscriptionId());

		try {
			// Calculate monthly price from total (in cents, convert to dollars)
			BigDecimal monthlyPrice = data.amountTotal() != null
					? new BigDecimal(data.amountTotal()).divide(new BigDecimal("100")) : BigDecimal.ZERO;

			// Create the subscription record
			userSubscriptionService.createSubscription(data.subscriberId(), data.ownerId(), data.subscriptionId(),
					monthlyPrice);

			logger.info("Successfully created subscription for checkout session {}", data.sessionId());
		}
		catch (Exception e) {
			logger.error("Failed to process checkout completion for session {}: {}", data.sessionId(), e.getMessage(),
					e);
		}
	}

	/**
	 * Handle STRAT pack purchase checkout completion.
	 * Publishes a StratPackPurchaseEvent that can be listened to by service-crypto-token
	 * to credit the purchased STRAT tokens to the user's wallet.
	 */
	private void handleStratPackCheckoutCompleted(StratPackCheckoutData data) {
		logger.info("Processing STRAT pack purchase: userId={}, packId={}, stratAmount={}", data.userId(), data.packId(),
				data.stratAmount());

		try {
			// Publish event for listeners (service-crypto-token will handle the credit)
			StratPackPurchaseEvent event = new StratPackPurchaseEvent(this, data.sessionId(), data.userId(),
					data.packId(), data.stratAmount(), data.customerId(), data.amountTotal());
			eventPublisher.publishEvent(event);

			logger.info("Published STRAT pack purchase event for user {} pack {} (session: {})", data.userId(),
					data.packId(), data.sessionId());
		}
		catch (Exception e) {
			logger.error("Failed to publish STRAT pack purchase event for session {}: {}", data.sessionId(),
					e.getMessage(), e);
		}
	}

	/**
	 * Handle platform subscription checkout (Strategist/Quant tier).
	 * Credits initial monthly STRAT allocation to user's wallet.
	 */
	private void handlePlatformSubscriptionCheckout(PlatformSubscriptionCheckoutData data) {
		logger.info("Processing platform subscription checkout: userId={}, tier={}", data.userId(), data.tier());

		try {
			SubscriptionTier tier = SubscriptionTier.fromId(data.tier());

			// Initialize STRAT allocation on subscription
			tokenUsageService.initializeStrat(data.userId(), tier);

			// Credit initial monthly STRAT allocation to wallet
			cryptoTokenBusiness.creditMonthlyAllocation(data.userId(), tier.getMonthlyStrat());

			logger.info("Credited {} STRAT to user {} for new {} subscription", tier.getMonthlyStrat(), data.userId(),
					tier.getDisplayName());
		}
		catch (Exception e) {
			logger.error("Failed to process platform subscription checkout for user {}: {}", data.userId(),
					e.getMessage(), e);
		}
	}

	/**
	 * Handle customer.subscription.created event. This is when Stripe creates the subscription
	 * object.
	 */
	private void handleSubscriptionCreated(Event event) {
		Optional<OwnerSubscriptionEventData> dataOpt = stripeWebhookService.parseSubscriptionEvent(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Subscription is not an owner subscription, skipping");
			return;
		}

		OwnerSubscriptionEventData data = dataOpt.get();
		logger.info("Processing subscription created: subscriber={}, owner={}, status={}", data.subscriberId(),
				data.ownerId(), data.status());

		try {
			userSubscriptionService.handleStripeSubscriptionUpdate(data.subscriberId(), data.ownerId(),
					data.subscriptionId(), data.status());

			logger.info("Successfully processed subscription created: {}", data.subscriptionId());
		}
		catch (Exception e) {
			logger.error("Failed to process subscription created {}: {}", data.subscriptionId(), e.getMessage(), e);
		}
	}

	/**
	 * Handle customer.subscription.updated event. This covers status changes like active ->
	 * past_due -> canceled.
	 */
	private void handleSubscriptionUpdated(Event event) {
		Optional<OwnerSubscriptionEventData> dataOpt = stripeWebhookService.parseSubscriptionEvent(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Subscription is not an owner subscription, skipping");
			return;
		}

		OwnerSubscriptionEventData data = dataOpt.get();
		logger.info("Processing subscription updated: subscriber={}, owner={}, status={}, cancelAtPeriodEnd={}",
				data.subscriberId(), data.ownerId(), data.status(), data.cancelAtPeriodEnd());

		try {
			userSubscriptionService.handleStripeSubscriptionUpdate(data.subscriberId(), data.ownerId(),
					data.subscriptionId(), data.status());

			logger.info("Successfully processed subscription update: {}", data.subscriptionId());
		}
		catch (Exception e) {
			logger.error("Failed to process subscription update {}: {}", data.subscriptionId(), e.getMessage(), e);
		}
	}

	/**
	 * Handle customer.subscription.deleted event. This is when a subscription is fully cancelled
	 * (not just scheduled for cancellation).
	 */
	private void handleSubscriptionDeleted(Event event) {
		Optional<OwnerSubscriptionEventData> dataOpt = stripeWebhookService.parseSubscriptionEvent(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Subscription is not an owner subscription, skipping");
			return;
		}

		OwnerSubscriptionEventData data = dataOpt.get();
		logger.info("Processing subscription deleted: subscriber={}, owner={}", data.subscriberId(), data.ownerId());

		try {
			userSubscriptionService.handleStripeSubscriptionDeleted(data.subscriptionId());

			logger.info("Successfully processed subscription deletion: {}", data.subscriptionId());
		}
		catch (Exception e) {
			logger.error("Failed to process subscription deletion {}: {}", data.subscriptionId(), e.getMessage(), e);
		}
	}

	/**
	 * Handle invoice.paid event. This confirms a subscription payment was successful.
	 * For platform subscriptions, this grants monthly STRAT allocation.
	 */
	private void handleInvoicePaid(Event event) {
		// Check for platform subscription renewal first
		Optional<PlatformSubscriptionInvoiceData> platformOpt = stripeWebhookService
				.parsePlatformInvoiceEvent(event);
		if (platformOpt.isPresent()) {
			handlePlatformInvoicePaid(platformOpt.get());
		}

		// Then check for owner subscription invoice
		Optional<OwnerSubscriptionInvoiceData> dataOpt = stripeWebhookService.parseInvoiceEvent(event);

		if (dataOpt.isEmpty()) {
			if (platformOpt.isEmpty()) {
				logger.debug("Invoice is not for an owner or platform subscription, skipping");
			}
			return;
		}

		OwnerSubscriptionInvoiceData data = dataOpt.get();
		logger.info("Processing invoice paid: subscriber={}, owner={}, amount={}", data.subscriberId(), data.ownerId(),
				data.amountPaid());

		logger.info("Invoice {} paid for subscription {}", data.invoiceId(), data.subscriptionId());
	}

	/**
	 * Handle platform subscription invoice.paid - grant monthly STRAT allocation.
	 */
	private void handlePlatformInvoicePaid(PlatformSubscriptionInvoiceData data) {
		logger.info("Processing platform subscription invoice paid: userId={}, tier={}", data.userId(), data.tier());

		try {
			SubscriptionTier tier = SubscriptionTier.fromId(data.tier());

			// Reset monthly usage counter
			tokenUsageService.resetStratUsage(data.userId());

			// Credit monthly STRAT allocation to wallet
			cryptoTokenBusiness.creditMonthlyAllocation(data.userId(), tier.getMonthlyStrat());

			logger.info("Renewed {} STRAT for user {} ({} tier)", tier.getMonthlyStrat(), data.userId(),
					tier.getDisplayName());
		}
		catch (Exception e) {
			logger.error("Failed to process platform invoice for user {}: {}", data.userId(), e.getMessage(), e);
		}
	}

	/**
	 * Handle invoice.payment_failed event. This means a payment attempt failed.
	 */
	private void handleInvoicePaymentFailed(Event event) {
		Optional<OwnerSubscriptionInvoiceData> dataOpt = stripeWebhookService.parseInvoiceEvent(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Invoice is not for an owner subscription, skipping");
			return;
		}

		OwnerSubscriptionInvoiceData data = dataOpt.get();
		logger.warn("Payment failed for invoice {}: subscriber={}, owner={}, amount={}", data.invoiceId(),
				data.subscriberId(), data.ownerId(), data.amountDue());

		// Payment failures are handled by Stripe's dunning process
		// The subscription status will be updated via subscription.updated events
		// We could send a notification to the subscriber here
	}

	/**
	 * Handle account.updated event. This is when a Stripe Connect account's status changes. Used to
	 * track when owners complete onboarding and become ready to receive payments.
	 */
	private void handleAccountUpdated(Event event) {
		Optional<ConnectAccountEventData> dataOpt = stripeWebhookService.parseConnectAccountEvent(event);

		if (dataOpt.isEmpty()) {
			logger.debug("Account event has no userId in metadata, skipping");
			return;
		}

		ConnectAccountEventData data = dataOpt.get();
		logger.info("Processing account updated: userId={}, accountId={}, chargesEnabled={}, payoutsEnabled={}",
				data.userId(), data.accountId(), data.chargesEnabled(), data.payoutsEnabled());

		try {
			ownerSubscriptionService.handleStripeConnectWebhook(data.accountId(), data.userId(), data.chargesEnabled(),
					data.payoutsEnabled(), data.detailsSubmitted());

			if (data.isFullyOnboarded()) {
				logger.info("Connect account {} is now fully onboarded for user {}", data.accountId(), data.userId());
			}
			else {
				logger.info("Connect account {} updated for user {}, not yet fully onboarded", data.accountId(),
						data.userId());
			}
		}
		catch (Exception e) {
			logger.error("Failed to process account update for {}: {}", data.accountId(), e.getMessage(), e);
		}
	}

	/**
	 * Health check endpoint for webhook configuration.
	 */
	@GetMapping("/stripe/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Stripe webhook endpoint is active");
	}

}
