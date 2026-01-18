package io.strategiz.client.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.LoginLink;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.strategiz.client.stripe.exception.StripeErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Stripe Connect operations.
 * Handles connected account creation, onboarding, and management for owner subscriptions.
 *
 * Stripe Connect allows platform owners (Strategiz) to collect payments on behalf of
 * strategy owners and split the revenue (85% to owner, 15% platform fee).
 */
@Service
public class StripeConnectService {

    private static final Logger logger = LoggerFactory.getLogger(StripeConnectService.class);

    /**
     * Platform fee percentage (15%). Owner keeps 85%.
     */
    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.15");

    private final StripeConfig config;

    public StripeConnectService(StripeConfig config) {
        this.config = config;
    }

    /**
     * Create a new Stripe Connect Express account for a strategy owner.
     *
     * @param userId    The internal user ID
     * @param userEmail The user's email
     * @return The created Account
     */
    public Account createConnectAccount(String userId, String userEmail) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        logger.info("Creating Stripe Connect account for user: {}", userId);

        try {
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setEmail(userEmail)
                    .setCapabilities(AccountCreateParams.Capabilities.builder()
                            .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                    .setRequested(true)
                                    .build())
                            .build())
                    .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                    .putMetadata("userId", userId)
                    .putMetadata("platform", "strategiz")
                    .build();

            Account account = Account.create(params);

            logger.info("Created Stripe Connect account {} for user {}", account.getId(), userId);
            return account;

        } catch (StripeException e) {
            logger.error("Failed to create Stripe Connect account for user {}: {}", userId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CONNECT_ACCOUNT_CREATION_FAILED, "client-stripe", e, userId);
        }
    }

    /**
     * Create an onboarding link for a Connect account.
     * This link allows the user to complete Stripe's identity verification and bank setup.
     *
     * @param accountId The Stripe Connect account ID
     * @return The onboarding URL
     */
    public String createOnboardingLink(String accountId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        logger.info("Creating onboarding link for Connect account: {}", accountId);

        try {
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(config.getAppBaseUrl() + "/profile?tab=subscriptions&stripe=refresh")
                    .setReturnUrl(config.getAppBaseUrl() + "/profile?tab=subscriptions&stripe=complete")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink link = AccountLink.create(params);

            logger.info("Created onboarding link for account {}", accountId);
            return link.getUrl();

        } catch (StripeException e) {
            logger.error("Failed to create onboarding link for account {}: {}", accountId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CONNECT_ONBOARDING_LINK_FAILED, "client-stripe", e, accountId);
        }
    }

    /**
     * Create a login link for an Express Connect account dashboard.
     * Allows the owner to view their Stripe Express dashboard.
     *
     * @param accountId The Stripe Connect account ID
     * @return The dashboard login URL
     */
    public String createLoginLink(String accountId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        logger.info("Creating login link for Connect account: {}", accountId);

        try {
            LoginLink link = LoginLink.createOnAccount(accountId);

            logger.info("Created login link for account {}", accountId);
            return link.getUrl();

        } catch (StripeException e) {
            logger.error("Failed to create login link for account {}: {}", accountId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CONNECT_LOGIN_LINK_FAILED, "client-stripe", e, accountId);
        }
    }

    /**
     * Retrieve a Connect account.
     *
     * @param accountId The Stripe Connect account ID
     * @return The Account object
     */
    public Account getAccount(String accountId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        try {
            return Account.retrieve(accountId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve Connect account {}: {}", accountId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CONNECT_ACCOUNT_RETRIEVAL_FAILED, "client-stripe", e, accountId);
        }
    }

    /**
     * Get the status of a Connect account.
     *
     * @param accountId The Stripe Connect account ID
     * @return The account status
     */
    public ConnectAccountStatus getAccountStatus(String accountId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        try {
            Account account = Account.retrieve(accountId);
            return ConnectAccountStatus.fromAccount(account);
        } catch (StripeException e) {
            logger.error("Failed to get Connect account status {}: {}", accountId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CONNECT_ACCOUNT_RETRIEVAL_FAILED, "client-stripe", e, accountId);
        }
    }

    /**
     * Check if a Connect account is fully onboarded and can receive payouts.
     *
     * @param accountId The Stripe Connect account ID
     * @return True if the account is ready for payouts
     */
    public boolean isAccountReady(String accountId) {
        ConnectAccountStatus status = getAccountStatus(accountId);
        return status.isActive() && status.payoutsEnabled();
    }

    /**
     * Check if Stripe Connect is configured.
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }

    // === Owner Subscription Checkout Methods ===

    /**
     * Create a checkout session for subscribing to a strategy owner.
     * Payments are routed to the owner's Connect account with platform fee deducted.
     *
     * @param subscriberId         The subscriber's user ID
     * @param subscriberEmail      The subscriber's email
     * @param ownerId              The owner's user ID
     * @param ownerConnectAccountId The owner's Stripe Connect account ID
     * @param monthlyPriceInCents  The monthly price in cents
     * @param customerId           Existing Stripe customer ID (optional)
     * @return Checkout session result with URL
     */
    public OwnerSubscriptionCheckoutResult createOwnerSubscriptionCheckout(
            String subscriberId,
            String subscriberEmail,
            String ownerId,
            String ownerConnectAccountId,
            long monthlyPriceInCents,
            String customerId) {

        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        logger.info("Creating owner subscription checkout: subscriber={}, owner={}, price={}",
                subscriberId, ownerId, monthlyPriceInCents);

        try {
            // Create or get customer
            String stripeCustomerId = customerId;
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                stripeCustomerId = createCustomer(subscriberId, subscriberEmail);
            }

            // Calculate platform fee (15%)
            long platformFee = Math.round(monthlyPriceInCents * PLATFORM_FEE_PERCENT.doubleValue());

            // Build checkout session with destination charge (payment to connected account)
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(stripeCustomerId)
                    .setSuccessUrl(config.getAppBaseUrl() + "/profile/" + ownerId + "?subscribe=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(config.getAppBaseUrl() + "/profile/" + ownerId + "?subscribe=canceled")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(monthlyPriceInCents)
                                    .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                            .build())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Owner Subscription")
                                            .setDescription("Monthly subscription to deploy strategies")
                                            .build())
                                    .build())
                            .setQuantity(1L)
                            .build())
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .setApplicationFeePercent(PLATFORM_FEE_PERCENT.multiply(new BigDecimal("100")))
                            .setTransferData(SessionCreateParams.SubscriptionData.TransferData.builder()
                                    .setDestination(ownerConnectAccountId)
                                    .build())
                            .putMetadata("subscriberId", subscriberId)
                            .putMetadata("ownerId", ownerId)
                            .putMetadata("type", "owner_subscription")
                            .build())
                    .putMetadata("subscriberId", subscriberId)
                    .putMetadata("ownerId", ownerId)
                    .putMetadata("type", "owner_subscription");

            Session session = Session.create(paramsBuilder.build());

            logger.info("Created owner subscription checkout session {} for subscriber {}",
                    session.getId(), subscriberId);

            return new OwnerSubscriptionCheckoutResult(
                    session.getId(),
                    session.getUrl(),
                    stripeCustomerId,
                    monthlyPriceInCents,
                    platformFee
            );
        }
        catch (StripeException e) {
            logger.error("Failed to create owner subscription checkout for subscriber {}: {}",
                    subscriberId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.CHECKOUT_SESSION_CREATION_FAILED,
                    "client-stripe", e, subscriberId);
        }
    }

    /**
     * Create a Stripe customer.
     * @param userId The internal user ID
     * @param email The user's email
     * @return The Stripe customer ID
     */
    private String createCustomer(String userId, String email) {
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
     * Cancel an owner subscription in Stripe.
     * Cancels at the end of the current billing period.
     *
     * @param stripeSubscriptionId The Stripe subscription ID
     */
    public void cancelOwnerSubscription(String stripeSubscriptionId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();

            subscription.update(params);

            logger.info("Scheduled owner subscription {} for cancellation at period end", stripeSubscriptionId);
        }
        catch (StripeException e) {
            logger.error("Failed to cancel owner subscription {}: {}", stripeSubscriptionId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_CANCELLATION_FAILED,
                    "client-stripe", e, stripeSubscriptionId);
        }
    }

    /**
     * Cancel an owner subscription immediately.
     *
     * @param stripeSubscriptionId The Stripe subscription ID
     */
    public void cancelOwnerSubscriptionImmediately(String stripeSubscriptionId) {
        if (!config.isConfigured()) {
            throw new StrategizException(StripeErrorDetails.NOT_CONFIGURED, "client-stripe");
        }

        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            subscription.cancel();

            logger.info("Canceled owner subscription {} immediately", stripeSubscriptionId);
        }
        catch (StripeException e) {
            logger.error("Failed to cancel owner subscription {} immediately: {}", stripeSubscriptionId, e.getMessage());
            throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_CANCELLATION_FAILED,
                    "client-stripe", e, stripeSubscriptionId);
        }
    }

    /**
     * Retrieve a Stripe subscription.
     *
     * @param stripeSubscriptionId The Stripe subscription ID
     * @return The Subscription object
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
            throw new StrategizException(StripeErrorDetails.SUBSCRIPTION_RETRIEVAL_FAILED,
                    "client-stripe", e, stripeSubscriptionId);
        }
    }

    /**
     * Result of creating an owner subscription checkout session.
     */
    public record OwnerSubscriptionCheckoutResult(
            String sessionId,
            String url,
            String customerId,
            long priceInCents,
            long platformFeeInCents
    ) {
    }

    /**
     * Status of a Stripe Connect account.
     */
    public record ConnectAccountStatus(
            String accountId,
            String status,  // not_started, pending, active, restricted
            boolean detailsSubmitted,
            boolean chargesEnabled,
            boolean payoutsEnabled,
            List<String> pendingRequirements,
            List<String> currentlyDue
    ) {
        /**
         * Create status from a Stripe Account object.
         */
        public static ConnectAccountStatus fromAccount(Account account) {
            String status;
            if (!account.getDetailsSubmitted()) {
                status = "not_started";
            } else if (account.getRequirements() != null &&
                    account.getRequirements().getCurrentlyDue() != null &&
                    !account.getRequirements().getCurrentlyDue().isEmpty()) {
                status = "pending";
            } else if (account.getChargesEnabled() && account.getPayoutsEnabled()) {
                status = "active";
            } else {
                status = "restricted";
            }

            List<String> pendingRequirements = account.getRequirements() != null
                    ? account.getRequirements().getPendingVerification()
                    : List.of();

            List<String> currentlyDue = account.getRequirements() != null
                    ? account.getRequirements().getCurrentlyDue()
                    : List.of();

            return new ConnectAccountStatus(
                    account.getId(),
                    status,
                    account.getDetailsSubmitted(),
                    account.getChargesEnabled(),
                    account.getPayoutsEnabled(),
                    pendingRequirements != null ? pendingRequirements : List.of(),
                    currentlyDue != null ? currentlyDue : List.of()
            );
        }

        /**
         * Check if account is fully active.
         */
        public boolean isActive() {
            return "active".equals(status);
        }

        /**
         * Check if onboarding is needed.
         */
        public boolean needsOnboarding() {
            return "not_started".equals(status) || "pending".equals(status) ||
                    (currentlyDue != null && !currentlyDue.isEmpty());
        }
    }
}
