package io.strategiz.business.cryptotoken;

import io.strategiz.framework.exception.ErrorDetails;

/**
 * Error codes for crypto token business operations.
 */
public enum CryptoTokenErrors implements ErrorDetails {

	WALLET_NOT_FOUND("CRYPTO-001", "Crypto wallet not found", 404),
	INSUFFICIENT_BALANCE("CRYPTO-002", "Insufficient token balance", 400),
	WALLET_SUSPENDED("CRYPTO-003", "Wallet is suspended", 403),
	TRANSACTION_NOT_FOUND("CRYPTO-004", "Transaction not found", 404),
	TRANSACTION_FAILED("CRYPTO-005", "Transaction failed", 500),
	INVALID_AMOUNT("CRYPTO-006", "Invalid amount specified", 400),
	TRANSFER_TO_SELF("CRYPTO-007", "Cannot transfer tokens to yourself", 400),
	RECIPIENT_NOT_FOUND("CRYPTO-008", "Recipient user not found", 404),
	PURCHASE_FAILED("CRYPTO-009", "Token purchase failed", 500),
	CONVERSION_FAILED("CRYPTO-010", "Token conversion failed", 500),
	RATE_NOT_FOUND("CRYPTO-011", "Conversion rate not found", 404),
	MINIMUM_PURCHASE("CRYPTO-012", "Minimum purchase amount is $5", 400),
	DAILY_LIMIT_EXCEEDED("CRYPTO-013", "Daily transaction limit exceeded", 400),
	CONVERSION_NOT_ALLOWED("CRYPTO-014",
			"STRAT tokens cannot be converted to AI credits. AI credits come from platform subscription tier only.",
			400),
	PACK_NOT_FOUND("CRYPTO-015", "STRAT pack not found", 404),
	PACK_DISABLED("CRYPTO-016", "This STRAT pack is currently unavailable", 400);

	private final String code;

	private final String message;

	private final int httpStatus;

	CryptoTokenErrors(String code, String message, int httpStatus) {
		this.code = code;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public int getHttpStatus() {
		return httpStatus;
	}

}
