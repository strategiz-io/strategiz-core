package io.strategiz.business.provider.plaid.exception;

import io.strategiz.framework.exception.ErrorCode;

/**
 * Error details for Plaid provider operations.
 */
public enum PlaidProviderErrorDetails implements ErrorCode {

    PLAID_DISABLED("PLAID_001", "Plaid integration is disabled. Enable it from the admin console."),
    LINK_TOKEN_CREATION_FAILED("PLAID_002", "Failed to create Plaid Link token: {0}"),
    TOKEN_EXCHANGE_FAILED("PLAID_003", "Failed to exchange public token: {0}"),
    HOLDINGS_FETCH_FAILED("PLAID_004", "Failed to fetch investment holdings: {0}"),
    INVALID_ACCESS_TOKEN("PLAID_005", "Invalid or expired Plaid access token for user: {0}"),
    ITEM_NOT_FOUND("PLAID_006", "Plaid item not found for user: {0}"),
    INSTITUTION_NOT_SUPPORTED("PLAID_007", "Institution not supported for investments: {0}"),
    SYNC_FAILED("PLAID_008", "Failed to sync Plaid data for user: {0}"),
    DISCONNECT_FAILED("PLAID_009", "Failed to disconnect Plaid item: {0}"),
    CONNECTION_EXISTS("PLAID_010", "User already has an active Plaid connection");

    private final String code;
    private final String message;

    PlaidProviderErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
