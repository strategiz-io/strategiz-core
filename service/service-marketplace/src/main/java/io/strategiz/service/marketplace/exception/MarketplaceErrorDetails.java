package io.strategiz.service.marketplace.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for marketplace service.
 */
public enum MarketplaceErrorDetails implements ErrorDetails {

    // Strategy retrieval errors
    STRATEGY_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-strategy-retrieval-failed"),
    STRATEGY_NOT_FOUND(HttpStatus.NOT_FOUND, "marketplace-strategy-not-found"),
    STRATEGY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-strategy-create-failed"),
    STRATEGY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-strategy-update-failed"),
    STRATEGY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-strategy-delete-failed"),

    // Permission errors
    UNAUTHORIZED_UPDATE(HttpStatus.FORBIDDEN, "marketplace-unauthorized-update"),
    UNAUTHORIZED_DELETE(HttpStatus.FORBIDDEN, "marketplace-unauthorized-delete"),

    // Purchase errors
    STRATEGY_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "marketplace-strategy-not-available"),
    PURCHASE_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "marketplace-purchase-required"),
    PURCHASE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-purchase-failed"),
    PURCHASES_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-purchases-retrieval-failed"),

    // Apply errors
    APPLY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-apply-failed"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "marketplace-user-not-found");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    MarketplaceErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
