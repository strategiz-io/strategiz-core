package io.strategiz.service.auth.model.totp;

/**
 * Response for TOTP authentication (login)
 * Contains authentication tokens upon successful verification
 */
public record TotpAuthenticationResponse(
        boolean success,
        String accessToken,
        String refreshToken,
        String message
) {
    /**
     * Create a successful authentication response
     * 
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @return Successful response object
     */
    public static TotpAuthenticationResponse success(String accessToken, String refreshToken) {
        return new TotpAuthenticationResponse(true, accessToken, refreshToken, "Authentication successful");
    }
    
    /**
     * Create an error response
     * 
     * @param errorMessage Error message
     * @return Error response object
     */
    public static TotpAuthenticationResponse error(String errorMessage) {
        return new TotpAuthenticationResponse(false, null, null, errorMessage);
    }
}
