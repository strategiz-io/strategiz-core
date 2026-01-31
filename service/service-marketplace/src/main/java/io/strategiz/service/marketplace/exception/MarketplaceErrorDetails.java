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
	VIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "marketplace-view-access-denied"),
	CODE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "marketplace-code-access-denied"),
	DEPLOY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "marketplace-deploy-access-denied"),
	EDIT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "marketplace-edit-access-denied"),

	// Purchase errors
	STRATEGY_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "marketplace-strategy-not-available"),
	PURCHASE_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "marketplace-purchase-required"),
	PURCHASE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-purchase-failed"),
	PURCHASES_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-purchases-retrieval-failed"),

	// Apply errors
	APPLY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-apply-failed"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "marketplace-user-not-found"),

	// Comment errors
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "marketplace-comment-not-found"),
	STRATEGY_NOT_PUBLISHED(HttpStatus.FORBIDDEN, "marketplace-strategy-not-published"),
	INVALID_OPERATION(HttpStatus.BAD_REQUEST, "marketplace-invalid-operation"),

	// Subscription errors
	SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "marketplace-subscription-not-found"),
	ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "marketplace-already-subscribed"),
	SUBSCRIPTION_EXPIRED(HttpStatus.PAYMENT_REQUIRED, "marketplace-subscription-expired"),
	CANNOT_SUBSCRIBE_OWN(HttpStatus.BAD_REQUEST, "marketplace-cannot-subscribe-own"),

	// Ownership transfer errors
	ALREADY_OWNS_STRATEGY(HttpStatus.BAD_REQUEST, "marketplace-already-owns-strategy"),
	STRATEGY_NOT_FOR_SALE(HttpStatus.BAD_REQUEST, "marketplace-strategy-not-for-sale"),

	// Pricing errors
	INVALID_PRICING_TYPE(HttpStatus.BAD_REQUEST, "marketplace-invalid-pricing-type"),
	INVALID_ONE_TIME_PRICE(HttpStatus.BAD_REQUEST, "marketplace-invalid-one-time-price"),
	INVALID_MONTHLY_PRICE(HttpStatus.BAD_REQUEST, "marketplace-invalid-monthly-price"),
	INVALID_PRICING(HttpStatus.BAD_REQUEST, "marketplace-invalid-pricing"),

	// Checkout errors
	CHECKOUT_SESSION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketplace-checkout-session-creation-failed");

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
