package io.strategiz.service.auth.model.smsotp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for checking SMS OTP status
 *
 * @param phoneNumber The phone number to check OTP status for (E.164 format)
 */
public record SmsOtpStatusRequest(
		@NotBlank(message = "Phone number is required") @Pattern(regexp = "^\\+[1-9]\\d{1,14}$",
				message = "Phone number must be in E.164 format (e.g., +1234567890)") String phoneNumber) {
}