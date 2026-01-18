package io.strategiz.service.profile.service;

import io.strategiz.data.social.entity.OwnerSubscriptionSettings;
import io.strategiz.data.social.repository.OwnerSubscriptionSettingsRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.EnableSubscriptionsRequest;
import io.strategiz.service.profile.model.OwnerSubscriptionSettingsResponse;
import io.strategiz.service.profile.model.UpdateSubscriptionSettingsRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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

    public OwnerSubscriptionService(
            OwnerSubscriptionSettingsRepository subscriptionSettingsRepository,
            UserRepository userRepository) {
        this.subscriptionSettingsRepository = subscriptionSettingsRepository;
        this.userRepository = userRepository;
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
