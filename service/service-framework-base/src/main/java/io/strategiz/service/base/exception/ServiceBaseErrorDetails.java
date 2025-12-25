package io.strategiz.service.base.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Base error details for common service-level errors.
 * These errors can be used across all service modules for common validation and operational errors.
 */
public enum ServiceBaseErrorDetails implements ErrorDetails {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "validation-error"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "too-many-requests"),
    RETRY_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "retry-interrupted"),
    RETRY_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "retry-exhausted"),
    CACHED_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "cached-operation-failed"),
    RESTTEMPLATE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "resttemplate-creation-failed"),
    FIREBASE_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "firebase-initialization-failed");
    
    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    ServiceBaseErrorDetails(HttpStatus httpStatus, String propertyKey) {
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