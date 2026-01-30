package io.strategiz.business.cryptotoken;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes for crypto token business operations.
 */
public enum CryptoTokenErrors implements ErrorDetails {

	WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "wallet-not-found"),
	INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "insufficient-balance"),
	WALLET_SUSPENDED(HttpStatus.FORBIDDEN, "wallet-suspended"),
	TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "transaction-not-found"),
	TRANSACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "transaction-failed"),
	INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "invalid-amount"),
	TRANSFER_TO_SELF(HttpStatus.BAD_REQUEST, "transfer-to-self"),
	RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "recipient-not-found"),
	PURCHASE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "purchase-failed"),
	CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "conversion-failed"),
	RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "rate-not-found"),
	MINIMUM_PURCHASE(HttpStatus.BAD_REQUEST, "minimum-purchase"),
	DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "daily-limit-exceeded"),
	CONVERSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "conversion-not-allowed"),
	PACK_NOT_FOUND(HttpStatus.NOT_FOUND, "pack-not-found"),
	PACK_DISABLED(HttpStatus.BAD_REQUEST, "pack-disabled");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	CryptoTokenErrors(HttpStatus httpStatus, String propertyKey) {
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
