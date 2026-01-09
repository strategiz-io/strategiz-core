package io.strategiz.data.auth.entity;

/**
 * Status of a push authentication request.
 */
public enum PushAuthStatus {

    /**
     * Push notification sent, waiting for user response.
     */
    PENDING,

    /**
     * User approved the authentication request.
     */
    APPROVED,

    /**
     * User denied the authentication request.
     */
    DENIED,

    /**
     * Request expired without response.
     */
    EXPIRED,

    /**
     * Request was cancelled (e.g., user signed in another way).
     */
    CANCELLED
}
