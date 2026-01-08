package io.strategiz.data.auth.entity;

/**
 * Status of an account recovery request.
 */
public enum RecoveryStatus {

    /**
     * Recovery initiated, waiting for email verification
     */
    PENDING_EMAIL,

    /**
     * Email verified, waiting for SMS verification (if MFA enabled)
     */
    PENDING_SMS,

    /**
     * Recovery completed successfully
     */
    COMPLETED,

    /**
     * Recovery request expired (30 minutes)
     */
    EXPIRED,

    /**
     * Recovery request cancelled by user
     */
    CANCELLED
}
