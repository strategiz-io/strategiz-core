package io.strategiz.service.profile.service;

import com.stripe.model.Account;
import io.strategiz.client.stripe.StripeConnectService;
import io.strategiz.client.stripe.StripeConnectService.ConnectAccountStatus;
import io.strategiz.data.social.entity.OwnerSubscriptionSettings;
import io.strategiz.data.social.repository.OwnerSubscriptionSettingsRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.EnableSubscriptionsRequest;
import io.strategiz.service.profile.model.OwnerSubscriptionSettingsResponse;
import io.strategiz.service.profile.model.StripeConnectStatusResponse;
import io.strategiz.service.profile.model.UpdateSubscriptionSettingsRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service for managing owner subscription settings.
 * Handles enabling/disabling subscriptions, updating settings, and retrieving subscription info.
 */
@Service
public class OwnerSubscriptionService extends BaseService {

    private static final String MODULE_NAME = "service-profile";
    private static final BigDecimal MIN_PRICE = new BigDecimal("5.00");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000.00");
    private static final int MIN_PITCH_LENGTH = 20;
    private static final int MAX_PITCH_LENGTH = 500;

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    private final OwnerSubscriptionSettingsRepository subscriptionSettingsRepository;
    private final UserRepository userRepository;
    private final StripeConnectService stripeConnectService;

    public OwnerSubscriptionService(
            OwnerSubscriptionSettingsRepository subscriptionSettingsRepository,
            UserRepository userRepository,
            StripeConnectService stripeConnectService) {
        this.subscriptionSettingsRepository = subscriptionSettingsRepository;
        this.userRepository = userRepository;
        this.stripeConnectService = stripeConnectService;
    }

    /**
     * Get subscription settings for the current user.
     *
     * @param userId The owner's user ID
     * @return Subscription settings response
     */
    public OwnerSubscriptionSettingsResponse getSettings(String userId) {
        log.debug("Getting subscription settings for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);

        if (settingsOpt.isEmpty()) {
            // Return default settings if none exist
            OwnerSubscriptionSettings defaultSettings = new OwnerSubscriptionSettings(userId);
            // Get public strategy count (would normally query strategy repository)
            defaultSettings.setPublicStrategyCount(0);
            return OwnerSubscriptionSettingsResponse.fromEntity(defaultSettings);
        }

        return OwnerSubscriptionSettingsResponse.fromEntity(settingsOpt.get());
    }

    /**
     * Enable subscriptions for an owner.
     *
     * @param userId  The owner's user ID
     * @param request The enable request containing price and pitch
     * @return Updated subscription settings
     */
    public OwnerSubscriptionSettingsResponse enableSubscriptions(String userId, EnableSubscriptionsRequest request) {
        log.info("Enabling subscriptions for user: {} with price: {}", userId, request.getMonthlyPrice());

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, MODULE_NAME, "User not found: " + userId);
        }

        // Check if already enabled
        Optional<OwnerSubscriptionSettings> existingOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (existingOpt.isPresent() && existingOpt.get().isEnabled()) {
            throw new StrategizException(ProfileErrors.SUBSCRIPTION_ALREADY_ENABLED, MODULE_NAME,
                    "Subscriptions already enabled for user: " + userId);
        }

        // Validate price
        validatePrice(request.getMonthlyPrice());

        // Validate pitch if provided
        if (request.getProfilePitch() != null && !request.getProfilePitch().isEmpty()) {
            validatePitch(request.getProfilePitch());
        }

        // Enable subscriptions
        OwnerSubscriptionSettings settings = subscriptionSettingsRepository.enableSubscriptions(
                userId,
                request.getMonthlyPrice(),
                request.getProfilePitch() != null ? request.getProfilePitch() : getDefaultPitch()
        );

        log.info("Subscriptions enabled for user: {} with price: ${}/month", userId, request.getMonthlyPrice());
        return OwnerSubscriptionSettingsResponse.fromEntity(settings);
    }

    /**
     * Disable subscriptions for an owner.
     * Note: Existing subscribers will remain active until they cancel.
     *
     * @param userId The owner's user ID
     * @return Updated subscription settings
     */
    public OwnerSubscriptionSettingsResponse disableSubscriptions(String userId) {
        log.info("Disabling subscriptions for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty() || !settingsOpt.get().isEnabled()) {
            throw new StrategizException(ProfileErrors.SUBSCRIPTION_NOT_ENABLED, MODULE_NAME,
                    "Subscriptions not enabled for user: " + userId);
        }

        OwnerSubscriptionSettings settings = subscriptionSettingsRepository.disableSubscriptions(userId);

        log.info("Subscriptions disabled for user: {}", userId);
        return OwnerSubscriptionSettingsResponse.fromEntity(settings);
    }

    /**
     * Update subscription settings.
     *
     * @param userId  The owner's user ID
     * @param request The update request
     * @return Updated subscription settings
     */
    public OwnerSubscriptionSettingsResponse updateSettings(String userId, UpdateSubscriptionSettingsRequest request) {
        log.debug("Updating subscription settings for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.SUBSCRIPTION_SETTINGS_NOT_FOUND, MODULE_NAME,
                    "No subscription settings found for user: " + userId);
        }

        OwnerSubscriptionSettings settings = settingsOpt.get();

        // Update price if provided
        if (request.getMonthlyPrice() != null) {
            validatePrice(request.getMonthlyPrice());
            settings = subscriptionSettingsRepository.updateMonthlyPrice(userId, request.getMonthlyPrice());
            log.info("Updated subscription price for user: {} to ${}/month", userId, request.getMonthlyPrice());
        }

        // Update pitch if provided
        if (request.getProfilePitch() != null) {
            validatePitch(request.getProfilePitch());
            settings = subscriptionSettingsRepository.updateProfilePitch(userId, request.getProfilePitch());
            log.info("Updated profile pitch for user: {}", userId);
        }

        return OwnerSubscriptionSettingsResponse.fromEntity(settings);
    }

    /**
     * Update Stripe Connect status.
     * Called by Stripe webhook handlers.
     *
     * @param userId    The owner's user ID
     * @param accountId The Stripe Connect account ID
     * @param status    The connection status
     * @return Updated subscription settings
     */
    public OwnerSubscriptionSettingsResponse updateStripeConnectStatus(String userId, String accountId, String status) {
        log.info("Updating Stripe Connect status for user: {} - accountId: {}, status: {}", userId, accountId, status);

        OwnerSubscriptionSettings settings = subscriptionSettingsRepository.updateStripeConnectStatus(
                userId, accountId, status
        );

        return OwnerSubscriptionSettingsResponse.fromEntity(settings);
    }

    /**
     * Check if subscriptions are enabled for a user.
     *
     * @param userId The owner's user ID
     * @return True if subscriptions are enabled
     */
    public boolean isEnabled(String userId) {
        return subscriptionSettingsRepository.isEnabled(userId);
    }

    /**
     * Get the current monthly price for new subscribers.
     *
     * @param userId The owner's user ID
     * @return The monthly price, or null if not set
     */
    public BigDecimal getMonthlyPrice(String userId) {
        return subscriptionSettingsRepository.getMonthlyPrice(userId);
    }

    /**
     * Increment subscriber count when a new subscription is created.
     *
     * @param userId The owner's user ID
     */
    public void incrementSubscriberCount(String userId) {
        subscriptionSettingsRepository.incrementSubscriberCount(userId);
        log.debug("Incremented subscriber count for user: {}", userId);
    }

    /**
     * Decrement subscriber count when a subscription is cancelled.
     *
     * @param userId The owner's user ID
     */
    public void decrementSubscriberCount(String userId) {
        subscriptionSettingsRepository.decrementSubscriberCount(userId);
        log.debug("Decremented subscriber count for user: {}", userId);
    }

    /**
     * Update the public strategy count for an owner.
     *
     * @param userId The owner's user ID
     * @param count  The new count
     */
    public void updatePublicStrategyCount(String userId, int count) {
        subscriptionSettingsRepository.updatePublicStrategyCount(userId, count);
        log.debug("Updated public strategy count for user: {} to {}", userId, count);
    }

    // === Stripe Connect Methods ===

    /**
     * Create a Stripe Connect account for the user.
     * This is the first step in enabling subscriptions.
     *
     * @param userId The owner's user ID
     * @return The onboarding URL to complete Stripe Connect setup
     */
    public String createStripeConnectAccount(String userId) {
        log.info("Creating Stripe Connect account for user: {}", userId);

        // Get user email
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, MODULE_NAME, "User not found: " + userId);
        }

        String userEmail = userOpt.get().getProfile().getEmail();

        // Check if user already has a Connect account
        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isPresent() && settingsOpt.get().getStripeConnectAccountId() != null) {
            // Already has an account - return onboarding link to continue/refresh
            String accountId = settingsOpt.get().getStripeConnectAccountId();
            log.info("User {} already has Connect account {}, returning onboarding link", userId, accountId);
            return stripeConnectService.createOnboardingLink(accountId);
        }

        // Create new Connect account
        Account account = stripeConnectService.createConnectAccount(userId, userEmail);

        // Save the account ID to user settings
        subscriptionSettingsRepository.updateStripeConnectStatus(userId, account.getId(), "pending");

        // Create and return onboarding link
        String onboardingUrl = stripeConnectService.createOnboardingLink(account.getId());

        log.info("Created Stripe Connect account {} for user {}", account.getId(), userId);
        return onboardingUrl;
    }

    /**
     * Get the Stripe Connect onboarding link for a user.
     * Use this when the user needs to continue or refresh onboarding.
     *
     * @param userId The owner's user ID
     * @return The onboarding URL
     */
    public String getStripeConnectOnboardingLink(String userId) {
        log.info("Getting Stripe Connect onboarding link for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty() || settingsOpt.get().getStripeConnectAccountId() == null) {
            throw new StrategizException(ProfileErrors.STRIPE_NOT_CONNECTED, MODULE_NAME,
                    "No Stripe Connect account found for user: " + userId);
        }

        String accountId = settingsOpt.get().getStripeConnectAccountId();
        return stripeConnectService.createOnboardingLink(accountId);
    }

    /**
     * Get the Stripe Express dashboard login link for a connected account.
     *
     * @param userId The owner's user ID
     * @return The dashboard login URL
     */
    public String getStripeConnectDashboardLink(String userId) {
        log.info("Getting Stripe Connect dashboard link for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty() || settingsOpt.get().getStripeConnectAccountId() == null) {
            throw new StrategizException(ProfileErrors.STRIPE_NOT_CONNECTED, MODULE_NAME,
                    "No Stripe Connect account found for user: " + userId);
        }

        String accountId = settingsOpt.get().getStripeConnectAccountId();
        return stripeConnectService.createLoginLink(accountId);
    }

    /**
     * Get the Stripe Connect status for a user.
     *
     * @param userId The owner's user ID
     * @return The Connect account status
     */
    public StripeConnectStatusResponse getStripeConnectStatus(String userId) {
        log.debug("Getting Stripe Connect status for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty() || settingsOpt.get().getStripeConnectAccountId() == null) {
            // No Connect account yet
            return new StripeConnectStatusResponse(
                    null,
                    "not_started",
                    false,
                    false,
                    false,
                    null,
                    null
            );
        }

        String accountId = settingsOpt.get().getStripeConnectAccountId();
        ConnectAccountStatus status = stripeConnectService.getAccountStatus(accountId);

        // Update our stored status if it changed
        if (!status.status().equals(settingsOpt.get().getStripeConnectStatus())) {
            subscriptionSettingsRepository.updateStripeConnectStatus(userId, accountId, status.status());
        }

        return StripeConnectStatusResponse.fromConnectStatus(status);
    }

    /**
     * Refresh Stripe Connect status from Stripe API.
     * Called after user completes onboarding or from webhook.
     *
     * @param userId The owner's user ID
     * @return Updated subscription settings
     */
    public OwnerSubscriptionSettingsResponse refreshStripeConnectStatus(String userId) {
        log.info("Refreshing Stripe Connect status for user: {}", userId);

        Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
        if (settingsOpt.isEmpty() || settingsOpt.get().getStripeConnectAccountId() == null) {
            throw new StrategizException(ProfileErrors.STRIPE_NOT_CONNECTED, MODULE_NAME,
                    "No Stripe Connect account found for user: " + userId);
        }

        String accountId = settingsOpt.get().getStripeConnectAccountId();
        ConnectAccountStatus status = stripeConnectService.getAccountStatus(accountId);

        // Update stored status
        OwnerSubscriptionSettings settings = subscriptionSettingsRepository.updateStripeConnectStatus(
                userId, accountId, status.status()
        );

        // Update payouts enabled flag
        if (status.payoutsEnabled() != settings.isPayoutsEnabled()) {
            settings.setPayoutsEnabled(status.payoutsEnabled());
            subscriptionSettingsRepository.save(settings, userId);
        }

        log.info("Stripe Connect status refreshed for user {}: {}", userId, status.status());
        return OwnerSubscriptionSettingsResponse.fromEntity(settings);
    }

    /**
     * Check if Stripe Connect is configured for the platform.
     *
     * @return True if Stripe Connect is available
     */
    public boolean isStripeConnectConfigured() {
        return stripeConnectService.isConfigured();
    }

    /**
     * Handle Stripe Connect account webhook update.
     * Called when account.updated event is received from Stripe.
     *
     * @param accountId The Stripe Connect account ID
     * @param userId The owner's user ID
     * @param chargesEnabled Whether the account can accept charges
     * @param payoutsEnabled Whether the account can receive payouts
     * @param detailsSubmitted Whether onboarding details are submitted
     */
    public void handleStripeConnectWebhook(String accountId, String userId, boolean chargesEnabled,
            boolean payoutsEnabled, boolean detailsSubmitted) {
        log.info("Handling Stripe Connect webhook for user {}: chargesEnabled={}, payoutsEnabled={}, detailsSubmitted={}",
                userId, chargesEnabled, payoutsEnabled, detailsSubmitted);

        try {
            Optional<OwnerSubscriptionSettings> settingsOpt = subscriptionSettingsRepository.findByUserId(userId);
            if (settingsOpt.isEmpty()) {
                log.warn("No subscription settings found for user {} during Connect webhook", userId);
                return;
            }

            OwnerSubscriptionSettings settings = settingsOpt.get();

            // Verify this is the right account
            if (!accountId.equals(settings.getStripeConnectAccountId())) {
                log.warn("Account ID mismatch for user {}: expected {}, got {}",
                        userId, settings.getStripeConnectAccountId(), accountId);
                return;
            }

            // Determine new status
            String newStatus;
            if (chargesEnabled && payoutsEnabled) {
                newStatus = "active";
            } else if (detailsSubmitted) {
                newStatus = "restricted"; // Details submitted but not yet approved
            } else {
                newStatus = "pending";
            }

            // Update status
            subscriptionSettingsRepository.updateStripeConnectStatus(userId, accountId, newStatus);

            // Update payouts enabled flag
            if (payoutsEnabled != settings.isPayoutsEnabled()) {
                settings.setPayoutsEnabled(payoutsEnabled);
                subscriptionSettingsRepository.save(settings, userId);
            }

            log.info("Updated Stripe Connect status for user {}: {}", userId, newStatus);
        } catch (Exception e) {
            log.error("Error handling Stripe Connect webhook for user {}: {}", userId, e.getMessage(), e);
        }
    }

    // === Private helper methods ===

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new StrategizException(ProfileErrors.INVALID_SUBSCRIPTION_PRICE, MODULE_NAME,
                    "Monthly price is required");
        }
        if (price.compareTo(MIN_PRICE) < 0) {
            throw new StrategizException(ProfileErrors.INVALID_SUBSCRIPTION_PRICE, MODULE_NAME,
                    "Monthly price must be at least $" + MIN_PRICE);
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new StrategizException(ProfileErrors.INVALID_SUBSCRIPTION_PRICE, MODULE_NAME,
                    "Monthly price cannot exceed $" + MAX_PRICE);
        }
    }

    private void validatePitch(String pitch) {
        if (pitch.length() < MIN_PITCH_LENGTH) {
            throw new StrategizException(ProfileErrors.INVALID_PROFILE_PITCH, MODULE_NAME,
                    "Profile pitch must be at least " + MIN_PITCH_LENGTH + " characters");
        }
        if (pitch.length() > MAX_PITCH_LENGTH) {
            throw new StrategizException(ProfileErrors.INVALID_PROFILE_PITCH, MODULE_NAME,
                    "Profile pitch cannot exceed " + MAX_PITCH_LENGTH + " characters");
        }
    }

    private String getDefaultPitch() {
        return "Subscribe to get access to all my trading strategies. Deploy them directly to your portfolio with just a few clicks.";
    }
}
