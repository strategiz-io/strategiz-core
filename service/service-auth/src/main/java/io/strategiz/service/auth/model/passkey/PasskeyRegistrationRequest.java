package io.strategiz.service.auth.model.passkey;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for beginning passkey registration Can use either verification code or identity
 * token for authorization
 */
public record PasskeyRegistrationRequest(
		@NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

		// Either verification code or identity token must be provided, but not both
		// For direct verification with code
		String verificationCode,

		// For using a previously issued identity token from email verification
		String identityToken) {
}
