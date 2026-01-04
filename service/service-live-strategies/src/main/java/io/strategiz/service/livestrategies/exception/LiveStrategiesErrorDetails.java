package io.strategiz.service.livestrategies.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for live strategies service.
 */
public enum LiveStrategiesErrorDetails implements ErrorDetails {

    // Alert errors
    ALERT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-fetch-failed"),
    ALERT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-create-failed"),
    ALERT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-update-failed"),
    ALERT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-delete-failed"),
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "alert-not-found"),
    ALERT_HISTORY_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alert-history-fetch-failed"),
    ALERT_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "alert-limit-exceeded"),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "notification-send-failed"),

    // Bot errors
    BOT_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-fetch-failed"),
    BOT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-create-failed"),
    BOT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-update-failed"),
    BOT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-delete-failed"),
    BOT_NOT_FOUND(HttpStatus.NOT_FOUND, "bot-not-found"),
    BOT_PERFORMANCE_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-performance-fetch-failed"),
    BOT_PREREQUISITES_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "bot-prerequisites-check-failed"),
    BOT_PREREQUISITES_NOT_MET(HttpStatus.BAD_REQUEST, "bot-prerequisites-not-met");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    LiveStrategiesErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
