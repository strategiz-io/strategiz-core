package io.strategiz.service.auth.model.totp;

/**
 * Response for TOTP registration (setup) Contains QR code URL for the user to scan
 */
public record TotpRegistrationResponse(boolean success, String secret, String qrCodeUrl) {
	/**
	 * Create a successful registration response
	 * @param secret Placeholder for the secret (actual secret stored securely)
	 * @param qrCodeUrl URL of the QR code image
	 * @return Successful response object
	 */
	public static TotpRegistrationResponse success(String secret, String qrCodeUrl) {
		return new TotpRegistrationResponse(true, secret, qrCodeUrl);
	}

	/**
	 * Create an error response
	 * @param errorMessage Error message
	 * @return Error response object
	 */
	public static TotpRegistrationResponse error(String errorMessage) {
		return new TotpRegistrationResponse(false, null, errorMessage);
	}
}
