package io.strategiz.client.coinbase;

/**
 * Error codes for Coinbase client operations
 * 
 * Usage: throw new StrategizException(CoinbaseErrors.API_CONNECTION_FAILED);
 */
public enum CoinbaseErrors {
    API_CONNECTION_FAILED,
    API_AUTHENTICATION_FAILED,
    API_RATE_LIMITED,
    API_ERROR,
    INVALID_RESPONSE,
    ACCOUNT_NOT_FOUND,
    INSUFFICIENT_FUNDS,
    CONFIGURATION_ERROR,
    INVALID_CREDENTIALS,
    TRANSACTION_FAILED,
    NETWORK_ERROR,
    SERVICE_UNAVAILABLE
} 