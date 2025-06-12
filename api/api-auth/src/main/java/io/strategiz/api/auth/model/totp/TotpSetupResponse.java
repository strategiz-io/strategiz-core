package io.strategiz.api.auth.model.totp;

/**
 * Response for TOTP setup containing the secret and QR code
 */
public record TotpSetupResponse(
        String secret,
        String qrCodeImage,
        boolean success,
        String message
) {
    public static TotpSetupResponse success(String secret, String qrCodeImage) {
        return new TotpSetupResponse(secret, qrCodeImage, true, "TOTP setup initiated successfully");
    }
    
    public static TotpSetupResponse error(String message) {
        return new TotpSetupResponse(null, null, false, message);
    }
}
