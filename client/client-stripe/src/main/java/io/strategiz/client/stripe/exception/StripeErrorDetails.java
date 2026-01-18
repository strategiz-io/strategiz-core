package io.strategiz.client.stripe.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for Stripe payment operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(StripeErrorDetails.XXX, "client-stripe");
 */
public enum StripeErrorDetails implements ErrorDetails {

	// === Configuration Errors ===
	NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "stripe-not-configured"),
	INVALID_TIER(HttpStatus.BAD_REQUEST, "stripe-invalid-tier"),
	MISSING_PRICE_ID(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-missing-price-id"),

	// === Checkout Errors ===
	CHECKOUT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-checkout-failed"),
	CHECKOUT_SESSION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-checkout-session-creation-failed"),

	// === Customer Errors ===
	CUSTOMER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-customer-creation-failed"),
	CUSTOMER_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-customer-retrieval-failed"),

	// === Webhook Errors ===
	WEBHOOK_SIGNATURE_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "stripe-webhook-signature-verification-failed"),
	WEBHOOK_EVENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-webhook-event-processing-failed"),

	// === Subscription Errors ===
	SUBSCRIPTION_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-subscription-retrieval-failed"),
	SUBSCRIPTION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-subscription-update-failed"),
	SUBSCRIPTION_CANCELLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-subscription-cancellation-failed"),

	// === Stripe Connect Errors ===
	CONNECT_ACCOUNT_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-connect-account-creation-failed"),
	CONNECT_ACCOUNT_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-connect-account-retrieval-failed"),
	CONNECT_ACCOUNT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-connect-account-update-failed"),
	CONNECT_ONBOARDING_LINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-connect-onboarding-link-failed"),
	CONNECT_LOGIN_LINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "stripe-connect-login-link-failed"),
	CONNECT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "stripe-connect-not-completed"),
	CONNECT_PAYOUTS_DISABLED(HttpStatus.BAD_REQUEST, "stripe-connect-payouts-disabled"),

	// === API Errors ===
	API_ERROR(HttpStatus.BAD_GATEWAY, "stripe-api-error"),
	NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "stripe-network-error"),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "stripe-rate-limited");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	StripeErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
