package io.strategiz.service.auth.model.passkey;

/**
 * Data for completing passkey authentication
 */
public record AuthenticationCompletion(String credentialId, String clientDataJSON, String authenticatorData,
		String signature, String userHandle) {
}
