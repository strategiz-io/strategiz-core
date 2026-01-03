package io.strategiz.data.strategy.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details enum for data-strategy module.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 */
public enum DataStrategyErrorDetails implements ErrorDetails {

	// Strategy errors
	STRATEGY_NOT_FOUND(HttpStatus.NOT_FOUND, "strategy-not-found"),
	STRATEGY_ALREADY_EXISTS(HttpStatus.CONFLICT, "strategy-already-exists"),
	STRATEGY_INVALID_STATUS(HttpStatus.BAD_REQUEST, "strategy-invalid-status"),
	STRATEGY_MISSING_PERFORMANCE(HttpStatus.BAD_REQUEST, "strategy-missing-performance"),
	STRATEGY_INVALID_CODE(HttpStatus.BAD_REQUEST, "strategy-invalid-code"),
	STRATEGY_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-execution-failed"),

	// Deployment errors
	DEPLOYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "deployment-not-found"),
	DEPLOYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "deployment-already-exists"),
	DEPLOYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "deployment-invalid-status"),
	DEPLOYMENT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "deployment-create-failed"),
	DEPLOYMENT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "deployment-update-failed"),
	DEPLOYMENT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "deployment-delete-failed"),

	// Alert deployment errors
	ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "alert-not-found"),
	ALERT_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-query-failed"),

	// Bot deployment errors
	BOT_NOT_FOUND(HttpStatus.NOT_FOUND, "bot-not-found"),
	BOT_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-query-failed"),

	// Subscription errors
	SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "subscription-not-found"),
	SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "subscription-already-exists"),
	SUBSCRIPTION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "subscription-invalid-status"),
	SUBSCRIPTION_EXPIRED(HttpStatus.FORBIDDEN, "subscription-expired"),

	// Comment errors
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment-not-found"),
	COMMENT_INVALID_PARENT(HttpStatus.BAD_REQUEST, "comment-invalid-parent"),
	COMMENT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "comment-update-failed"),

	// Repository errors
	REPOSITORY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-save-failed"),
	REPOSITORY_FIND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-find-failed"),
	REPOSITORY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-delete-failed"),
	REPOSITORY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-update-failed"),
	QUERY_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "query-execution-failed"),

	// Validation errors
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "validation-invalid-argument"),
	USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "validation-user-required"),
	STRATEGY_ID_REQUIRED(HttpStatus.BAD_REQUEST, "validation-strategy-required"),
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "validation-invalid-input"),
	AUDIT_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "validation-audit-fields-missing"),

	// Ownership errors
	OWNERSHIP_TRANSFER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ownership-transfer-failed"),
	OWNERSHIP_INVALID_TRANSFER(HttpStatus.BAD_REQUEST, "ownership-invalid-transfer"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "access-denied");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	DataStrategyErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
