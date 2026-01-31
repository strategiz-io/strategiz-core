package io.strategiz.service.auth.model.smsotp;

/**
 * Response model for SMS OTP verification
 *
 * @param success Whether the OTP was verified successfully
 * @param message Response message
 * @param phoneNumber The phone number that was verified
 * @param verified Whether the OTP code was valid
 */
public record SmsOtpVerifyResponse(boolean success, String message, String phoneNumber, boolean verified) {
	/**
	 * Create a successful verification response
	 */
	public static SmsOtpVerifyResponse success(String phoneNumber) {
		return new SmsOtpVerifyResponse(true, "SMS OTP verified successfully", phoneNumber, true);
	}

	/**
	 * Create a failed verification response
	 */
	public static SmsOtpVerifyResponse failure(String phoneNumber, String message) {
		return new SmsOtpVerifyResponse(false, message, phoneNumber, false);
	}
}