package io.strategiz.service.auth.model.passkey;

/**
 * Request model for completing passkey authentication (sign-in)
 * Contains data from WebAuthn assertion
 */
public record PasskeyAuthenticationCompletionRequest(
    String credentialId,
    String clientDataJSON,
    String authenticatorData,
    String signature,
    String userHandle,
    String deviceId
) {}
