package io.strategiz.service.auth.model.passkey;

/**
 * Data for completing passkey registration
 */
public record RegistrationCompletion(String credentialId, String clientDataJSON, String attestationObject,
		String email) {
}
