package io.strategiz.business.preferences.service;

import io.strategiz.data.preferences.entity.AlertNotificationPreferences;
import io.strategiz.data.preferences.repository.AlertNotificationPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for managing alert notification preferences.
 * Handles phone verification, channel configuration, and quiet hours.
 */
@Service
public class AlertPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(AlertPreferencesService.class);

    // E.164 phone number pattern: +[country code][number]
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    // Valid notification channels
    private static final List<String> VALID_CHANNELS = List.of("email", "sms", "push", "in-app");

    private final AlertNotificationPreferencesRepository repository;

    public AlertPreferencesService(AlertNotificationPreferencesRepository repository) {
        this.repository = repository;
    }

    /**
     * Get alert notification preferences for a user.
     *
     * @param userId The user ID
     * @return The alert preferences
     */
    public AlertNotificationPreferences getPreferences(String userId) {
        logger.debug("Getting alert preferences for user {}", userId);
        return repository.getByUserId(userId);
    }

    /**
     * Update alert notification preferences.
     *
     * @param userId The user ID
     * @param preferences The preferences to update
     * @return The updated preferences
     */
    public AlertNotificationPreferences updatePreferences(String userId, AlertNotificationPreferences preferences) {
        logger.info("Updating alert preferences for user {}", userId);

        // Validate channels
        if (preferences.getEnabledChannels() != null) {
            for (String channel : preferences.getEnabledChannels()) {
                if (!VALID_CHANNELS.contains(channel.toLowerCase())) {
                    throw new IllegalArgumentException("Invalid notification channel: " + channel);
                }
            }
        }

        // Validate phone number format if provided
        if (preferences.getPhoneNumber() != null && !preferences.getPhoneNumber().isEmpty()) {
            if (!isValidE164(preferences.getPhoneNumber())) {
                throw new IllegalArgumentException(
                        "Phone number must be in E.164 format (e.g., +14155551234)");
            }
        }

        // Validate quiet hours if enabled
        if (Boolean.TRUE.equals(preferences.getQuietHoursEnabled())) {
            validateQuietHours(preferences);
        }

        return repository.save(userId, preferences);
    }

    /**
     * Update phone number for SMS alerts.
     * Marks phone as unverified until verification is completed.
     *
     * @param userId The user ID
     * @param phoneNumber Phone number in E.164 format
     * @return The updated preferences
     */
    public AlertNotificationPreferences updatePhoneNumber(String userId, String phoneNumber) {
        logger.info("Updating phone number for user {}", userId);

        if (!isValidE164(phoneNumber)) {
            throw new IllegalArgumentException(
                    "Phone number must be in E.164 format (e.g., +14155551234)");
        }

        return repository.updatePhoneNumber(userId, phoneNumber);
    }

    /**
     * Mark phone number as verified.
     * Called after successful OTP verification.
     *
     * @param userId The user ID
     * @return The updated preferences
     */
    public AlertNotificationPreferences verifyPhoneNumber(String userId) {
        logger.info("Marking phone as verified for user {}", userId);
        return repository.verifyPhoneNumber(userId);
    }

    /**
     * Update enabled notification channels.
     *
     * @param userId The user ID
     * @param channels List of channel names
     * @return The updated preferences
     */
    public AlertNotificationPreferences updateChannels(String userId, List<String> channels) {
        logger.info("Updating notification channels for user {}: {}", userId, channels);

        // Validate all channels
        for (String channel : channels) {
            if (!VALID_CHANNELS.contains(channel.toLowerCase())) {
                throw new IllegalArgumentException("Invalid notification channel: " + channel);
            }
        }

        // If SMS is enabled, verify phone is configured
        if (channels.contains("sms")) {
            AlertNotificationPreferences current = repository.getByUserId(userId);
            if (!current.isSmsConfigured()) {
                throw new IllegalStateException(
                        "Cannot enable SMS notifications: phone number not verified");
            }
        }

        return repository.updateEnabledChannels(userId, channels);
    }

    /**
     * Check if the user is currently in quiet hours.
     *
     * @param userId The user ID
     * @return true if in quiet hours
     */
    public boolean isInQuietHours(String userId) {
        AlertNotificationPreferences prefs = repository.getByUserId(userId);

        if (!Boolean.TRUE.equals(prefs.getQuietHoursEnabled())) {
            return false;
        }

        String start = prefs.getQuietHoursStart();
        String end = prefs.getQuietHoursEnd();
        String timezone = prefs.getQuietHoursTimezone();

        if (start == null || end == null || timezone == null) {
            return false;
        }

        try {
            ZoneId zone = ZoneId.of(timezone);
            ZonedDateTime now = ZonedDateTime.now(zone);
            LocalTime currentTime = now.toLocalTime();
            LocalTime startTime = LocalTime.parse(start);
            LocalTime endTime = LocalTime.parse(end);

            // Handle overnight quiet hours (e.g., 22:00 to 08:00)
            if (startTime.isAfter(endTime)) {
                return currentTime.isAfter(startTime) || currentTime.isBefore(endTime);
            }
            else {
                return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
            }
        }
        catch (Exception e) {
            logger.warn("Error checking quiet hours for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if user can receive alerts (rate limit check).
     *
     * @param userId The user ID
     * @return true if within rate limit
     */
    public boolean canSendAlert(String userId) {
        return repository.canSendAlert(userId);
    }

    /**
     * Record that an alert was sent (for rate limiting).
     *
     * @param userId The user ID
     */
    public void recordAlertSent(String userId) {
        repository.incrementAlertCount(userId);
    }

    /**
     * Get the phone number for SMS alerts if configured and verified.
     *
     * @param userId The user ID
     * @return The phone number or null if not available
     */
    public String getSmsPhoneNumber(String userId) {
        AlertNotificationPreferences prefs = repository.getByUserId(userId);
        if (prefs.isSmsConfigured()) {
            return prefs.getPhoneNumber();
        }
        return null;
    }

    /**
     * Get the email for alerts (override or null for default).
     *
     * @param userId The user ID
     * @return The alert email or null to use account email
     */
    public String getAlertEmail(String userId) {
        AlertNotificationPreferences prefs = repository.getByUserId(userId);
        if (prefs.isEmailConfigured()) {
            return prefs.getEmailForAlerts();
        }
        return null;
    }

    // Validation helpers

    private boolean isValidE164(String phoneNumber) {
        return phoneNumber != null && E164_PATTERN.matcher(phoneNumber).matches();
    }

    private void validateQuietHours(AlertNotificationPreferences prefs) {
        if (prefs.getQuietHoursStart() == null || prefs.getQuietHoursEnd() == null) {
            throw new IllegalArgumentException(
                    "Quiet hours start and end times are required when quiet hours are enabled");
        }

        try {
            LocalTime.parse(prefs.getQuietHoursStart());
            LocalTime.parse(prefs.getQuietHoursEnd());
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:mm (e.g., 22:00)");
        }

        if (prefs.getQuietHoursTimezone() != null) {
            try {
                ZoneId.of(prefs.getQuietHoursTimezone());
            }
            catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid timezone. Use IANA timezone (e.g., America/New_York)");
            }
        }
    }

}
