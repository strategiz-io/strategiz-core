package io.strategiz.service.exchange.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for exchange service.
 */
public enum ExchangeErrorDetails implements ErrorDetails {

    // Binance US errors
    BINANCE_ACCOUNT_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "exchange-binance-account-fetch-failed"),
    BINANCE_SIGNATURE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "exchange-binance-signature-failed"),
    BINANCE_TICKER_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "exchange-binance-ticker-fetch-failed"),
    BINANCE_EXCHANGE_INFO_FAILED(HttpStatus.BAD_GATEWAY, "exchange-binance-exchange-info-failed"),
    BINANCE_BALANCE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "exchange-binance-balance-fetch-failed"),

    // Coinbase errors
    COINBASE_ACCOUNT_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "exchange-coinbase-account-fetch-failed"),
    COINBASE_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "exchange-coinbase-connection-failed"),
    COINBASE_JWT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "exchange-coinbase-jwt-failed"),
    COINBASE_AUTH_TEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "exchange-coinbase-auth-test-failed"),
    COINBASE_BALANCE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "exchange-coinbase-balance-fetch-failed"),

    // Trading agent errors
    NO_HISTORICAL_DATA(HttpStatus.NOT_FOUND, "exchange-no-historical-data"),
    SIGNAL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "exchange-signal-generation-failed"),
    AI_RESPONSE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "exchange-ai-response-failed");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    ExchangeErrorDetails(HttpStatus httpStatus, String propertyKey) {
        this.httpStatus = httpStatus;
        this.propertyKey = propertyKey;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getPropertyKey() {
        return propertyKey;
    }
}
