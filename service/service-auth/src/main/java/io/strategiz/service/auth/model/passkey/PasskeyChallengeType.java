package io.strategiz.service.auth.model.passkey;

/**
 * Enum representing the different types of passkey challenges.
 * Used to differentiate between challenges for registration and authentication.
 */
public enum PasskeyChallengeType {
    /**
     * Challenge used during the registration of a new passkey
     */
    REGISTRATION,
    
    /**
     * Challenge used during authentication with an existing passkey
     */
    AUTHENTICATION
}
