package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.preferences.entity.AlertNotificationPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AlertNotificationPreferences stored at users/{userId}/preferences/alertNotifications
 */
@Repository
public class AlertNotificationPreferencesRepository extends SubcollectionRepository<AlertNotificationPreferences> {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationPreferencesRepository.class);

    public AlertNotificationPreferencesRepository(Firestore firestore) {
        super(firestore, AlertNotificationPreferences.class);
    }

    @Override
    protected String getParentCollectionName() {
        return "users";
    }

    @Override
    protected String getSubcollectionName() {
        return "preferences";
    }

    /**
     * Get alert notification preferences for a user.
     * Creates default preferences if none exist.
     *
     * @param userId The user ID
     * @return The alert notification preferences
     */
    public AlertNotificationPreferences getByUserId(String userId) {
        validateParentId(userId);

        Optional<AlertNotificationPreferences> existing = findByIdInSubcollection(
                userId, AlertNotificationPreferences.PREFERENCE_ID);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Return default preferences (not persisted until updated)
        AlertNotificationPreferences defaults = new AlertNotificationPreferences();
        defaults.setPreferenceId(AlertNotificationPreferences.PREFERENCE_ID);
        return defaults;
    }

    /**
     * Save alert notification preferences for a user.
     *
     * @param userId The user ID
     * @param preferences The preferences to save
     * @return The saved preferences
     */
    public AlertNotificationPreferences save(String userId, AlertNotificationPreferences preferences) {
        validateParentId(userId);

        // Ensure the correct document ID
        preferences.setPreferenceId(AlertNotificationPreferences.PREFERENCE_ID);

        return saveInSubcollection(userId, preferences, userId);
    }

    /**
     * Update phone number and mark as unverified.
     *
     * @param userId The user ID
     * @param phoneNumber The new phone number in E.164 format
     * @return The updated preferences
     */
    public AlertNotificationPreferences updatePhoneNumber(String userId, String phoneNumber) {
        AlertNotificationPreferences prefs = getByUserId(userId);
        prefs.setPhoneNumber(phoneNumber);
        prefs.setPhoneVerified(false); // Require re-verification
        return save(userId, prefs);
    }

    /**
     * Mark phone number as verified.
     *
     * @param userId The user ID
     * @return The updated preferences
     */
    public AlertNotificationPreferences verifyPhoneNumber(String userId) {
        AlertNotificationPreferences prefs = getByUserId(userId);
        if (prefs.getPhoneNumber() != null && !prefs.getPhoneNumber().isEmpty()) {
            prefs.setPhoneVerified(true);
            return save(userId, prefs);
        }
        return prefs;
    }

    /**
     * Update email for alerts.
     *
     * @param userId The user ID
     * @param email The email address
     * @return The updated preferences
     */
    public AlertNotificationPreferences updateEmailForAlerts(String userId, String email) {
        AlertNotificationPreferences prefs = getByUserId(userId);
        prefs.setEmailForAlerts(email);
        prefs.setEmailVerified(false); // Require verification if different
        return save(userId, prefs);
    }

    /**
     * Update enabled notification channels.
     *
     * @param userId The user ID
     * @param channels List of channel names (email, sms, push, in-app)
     * @return The updated preferences
     */
    public AlertNotificationPreferences updateEnabledChannels(String userId, java.util.List<String> channels) {
        AlertNotificationPreferences prefs = getByUserId(userId);
        prefs.setEnabledChannels(channels);
        return save(userId, prefs);
    }

    /**
     * Increment the hourly alert count.
     *
     * @param userId The user ID
     * @return The updated preferences
     */
    public AlertNotificationPreferences incrementAlertCount(String userId) {
        AlertNotificationPreferences prefs = getByUserId(userId);

        // Reset hourly count if needed
        if (shouldResetHourlyCount(prefs)) {
            prefs.setAlertsThisHour(0);
            prefs.setHourlyResetAt(com.google.cloud.Timestamp.now());
        }

        prefs.setAlertsThisHour(prefs.getAlertsThisHour() + 1);
        prefs.setLastAlertSentAt(com.google.cloud.Timestamp.now());

        return save(userId, prefs);
    }

    /**
     * Check if user can receive more alerts this hour.
     *
     * @param userId The user ID
     * @return true if within rate limit
     */
    public boolean canSendAlert(String userId) {
        AlertNotificationPreferences prefs = getByUserId(userId);

        // Reset hourly count if needed
        if (shouldResetHourlyCount(prefs)) {
            return true;
        }

        return prefs.isWithinRateLimit();
    }

    /**
     * Check if the hourly count should be reset.
     */
    private boolean shouldResetHourlyCount(AlertNotificationPreferences prefs) {
        if (prefs.getHourlyResetAt() == null) {
            return true;
        }

        long resetTime = prefs.getHourlyResetAt().toDate().getTime();
        long now = System.currentTimeMillis();
        long hourInMs = 60 * 60 * 1000;

        return (now - resetTime) >= hourInMs;
    }
}
