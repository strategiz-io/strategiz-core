package io.strategiz.service.auth.model.totp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for completing TOTP registration by verifying the first TOTP code
 */
public record TotpRegistrationCompletionRequest(@NotBlank(message = "User ID is required") String userId,

		@NotBlank(message = "TOTP code is required") @Pattern(regexp = "\\d{6}",
				message = "TOTP code must be exactly 6 digits") String totpCode,

		String accessToken, String deviceName) {
}