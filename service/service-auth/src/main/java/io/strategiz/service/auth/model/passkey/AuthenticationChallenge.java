package io.strategiz.service.auth.model.passkey;

import java.util.List;

/**
 * Challenge data for passkey authentication
 */
public record AuthenticationChallenge(String challenge, String rpId, int timeout,
		List<AllowedCredential> allowCredentials) {
}
