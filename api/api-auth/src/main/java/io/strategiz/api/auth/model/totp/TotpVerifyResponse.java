package io.strategiz.api.auth.model.totp;

/**
 * Response for TOTP verification
 */
public record TotpVerifyResponse(
        boolean success,
        String message
) {
    public static TotpVerifyResponse success() {
        return new TotpVerifyResponse(true, "TOTP setup completed successfully");
    }
    
    public static TotpVerifyResponse error(String message) {
        return new TotpVerifyResponse(false, message);
    }
}
