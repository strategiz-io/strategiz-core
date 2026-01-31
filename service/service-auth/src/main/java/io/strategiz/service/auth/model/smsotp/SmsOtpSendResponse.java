package io.strategiz.service.auth.model.smsotp;

/**
 * Response model for SMS OTP send operation
 *
 * @param success Whether the SMS was sent successfully
 * @param message Response message
 * @param phoneNumber The phone number (for confirmation)
 * @param maskedPhoneNumber Masked phone number for security
 * @param otpId Unique identifier for this OTP (for tracking)
 * @param expiresInSeconds How long the OTP is valid for
 */
public record SmsOtpSendResponse(boolean success, String message, String phoneNumber, String maskedPhoneNumber,
		String otpId, int expiresInSeconds) {
	/**
	 * Create a successful SMS send response
	 */
	public static SmsOtpSendResponse success(String phoneNumber, String maskedPhoneNumber, String otpId,
			int expiresInSeconds) {
		return new SmsOtpSendResponse(true, "SMS OTP sent successfully", phoneNumber, maskedPhoneNumber, otpId,
				expiresInSeconds);
	}

	/**
	 * Create a failure SMS send response
	 */
	public static SmsOtpSendResponse failure(String phoneNumber, String message) {
		return new SmsOtpSendResponse(false, message, phoneNumber, null, null, 0);
	}
}