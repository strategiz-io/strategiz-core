package io.strategiz.service.auth.model.passkey;

/**
 * Request model for completing passkey registration (sign-up)
 * Contains data from WebAuthn credential creation
 */
public record PasskeyRegistrationCompletionRequest(
    String credentialId,
    String clientDataJSON,
    String attestationObject,
    String email,
    String deviceId,
    String identityToken
) {}
