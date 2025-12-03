package io.strategiz.client.alpaca.error;

/**
 * Error codes specific to Alpaca API client operations.
 *
 * Usage: throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR);
 */
public enum AlpacaErrors {
    ALPACA_API_ERROR,
    ALPACA_AUTH_FAILED,
    ALPACA_INVALID_RESPONSE,
    ALPACA_RATE_LIMIT,
    ALPACA_TOKEN_EXPIRED,
    ALPACA_INVALID_TOKEN,
    ALPACA_OAUTH_ERROR,
    ALPACA_NETWORK_ERROR,
    ALPACA_CONFIGURATION_ERROR,
    ALPACA_ACCOUNT_ERROR,
    ALPACA_POSITION_ERROR
}
