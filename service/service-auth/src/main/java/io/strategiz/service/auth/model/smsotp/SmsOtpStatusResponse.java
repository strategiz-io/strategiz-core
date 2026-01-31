package io.strategiz.service.auth.model.smsotp;

/**
 * Response model for SMS OTP status check
 *
 * @param phoneNumber The phone number being checked
 * @param status The current OTP status (PENDING, VERIFIED, EXPIRED, NOT_FOUND)
 * @param expiresAt When the OTP expires (if applicable)
 * @param attemptsRemaining How many verification attempts are left
 */
public record SmsOtpStatusResponse(String phoneNumber, String status, String expiresAt, int attemptsRemaining) {
	/**
	 * OTP Status constants
	 */
	public static class Status {

		public static final String PENDING = "PENDING";

		public static final String VERIFIED = "VERIFIED";

		public static final String EXPIRED = "EXPIRED";

		public static final String NOT_FOUND = "NOT_FOUND";

		public static final String MAX_ATTEMPTS_EXCEEDED = "MAX_ATTEMPTS_EXCEEDED";

	}
}