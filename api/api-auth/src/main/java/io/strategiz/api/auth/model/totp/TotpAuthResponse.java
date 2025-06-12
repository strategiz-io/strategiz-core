package io.strategiz.api.auth.model.totp;

/**
 * Response for TOTP authentication
 */
public record TotpAuthResponse(
        boolean authenticated,
        String accessToken,
        String refreshToken,
        String message
) {
    public static TotpAuthResponse success(String accessToken, String refreshToken) {
        return new TotpAuthResponse(true, accessToken, refreshToken, "Authentication successful");
    }
    
    public static TotpAuthResponse error(String message) {
        return new TotpAuthResponse(false, null, null, message);
    }
}
