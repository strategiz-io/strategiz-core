package io.strategiz.client.schwab.error;

/**
 * Error codes specific to Charles Schwab API client operations.
 *
 * Usage: throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR);
 */
public enum SchwabErrors {
    SCHWAB_API_ERROR,
    SCHWAB_AUTH_FAILED,
    SCHWAB_INVALID_RESPONSE,
    SCHWAB_RATE_LIMIT,
    SCHWAB_TOKEN_EXPIRED,
    SCHWAB_INVALID_TOKEN,
    SCHWAB_OAUTH_ERROR,
    SCHWAB_NETWORK_ERROR,
    SCHWAB_CONFIGURATION_ERROR,
    SCHWAB_ACCOUNT_ERROR,
    SCHWAB_POSITION_ERROR,
    SCHWAB_TRANSACTION_ERROR
}
