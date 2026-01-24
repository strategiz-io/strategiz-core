package io.strategiz.data.user.entity;

/**
 * Status of an email reservation during the signup process.
 *
 * PENDING: Email has been reserved but signup is not yet complete.
 *          The reservation will expire after 10 minutes if not confirmed.
 *
 * CONFIRMED: Signup completed successfully. The email is permanently reserved
 *            for the associated user account.
 */
public enum EmailReservationStatus {

    /**
     * Email is reserved but signup not yet complete.
     * Will be automatically cleaned up after expiration.
     */
    PENDING,

    /**
     * Signup completed - email is permanently associated with a user.
     */
    CONFIRMED
}
