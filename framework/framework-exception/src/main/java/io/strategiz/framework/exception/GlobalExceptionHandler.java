package io.strategiz.framework.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Global exception handler for clean API responses.
 * Returns the standard 4-field error format with proper HTTP status codes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Strategiz business exceptions with enum error codes
     */
    @ExceptionHandler(StrategizException.class)
    public ResponseEntity<StandardErrorResponse> handleStrategizException(
            StrategizException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        
        // Log the business exception with context
        log.warn("Business exception [{}]: {} - {}", 
            traceId, ex.getErrorCode(), ex.getMessage());
        
        // Create clean 4-field error response
        StandardErrorResponse errorResponse = new StandardErrorResponse(
            ex.getErrorCode(),
            ex.getMessage() != null ? ex.getMessage() : "An error occurred", 
            ex.getMessage() != null ? ex.getMessage() : ex.getErrorCode(),
            "https://docs.strategiz.io/errors/" + ex.getErrorCode().toLowerCase()
        );
        
        // Add trace ID to MDC for response headers (StandardHeadersInterceptor will add it)
        MDC.put("traceId", traceId);
        
        // Return appropriate HTTP status based on error code
        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle unexpected runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        
        // Log the unexpected exception with full stack trace
        log.error("Unexpected runtime exception [{}]: {}", traceId, ex.getMessage(), ex);
        
        StandardErrorResponse errorResponse = new StandardErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support with the trace ID from response headers.",
            "Exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
            "https://docs.strategiz.io/errors/server/internal-error"
        );
        
        MDC.put("traceId", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        
        // Log the unexpected exception with full stack trace
        log.error("Unexpected exception [{}]: {}", traceId, ex.getMessage(), ex);
        
        StandardErrorResponse errorResponse = new StandardErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support with the trace ID from response headers.",
            "Exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
            "https://docs.strategiz.io/errors/server/internal-error"
        );
        
        MDC.put("traceId", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Map error codes to appropriate HTTP status codes
     */
    private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        // Authentication/Authorization errors
        if (errorCode.contains("INVALID_CREDENTIALS") || 
            errorCode.contains("AUTHENTICATION_FAILED") ||
            errorCode.contains("INVALID_TOKEN") ||
            errorCode.contains("TOKEN_REVOKED")) {
            return HttpStatus.UNAUTHORIZED;
        }
        
        if (errorCode.contains("ACCESS_DENIED") ||
            errorCode.contains("FORBIDDEN")) {
            return HttpStatus.FORBIDDEN;
        }
        
        // Not found errors
        if (errorCode.contains("NOT_FOUND") ||
            errorCode.contains("USER_NOT_FOUND") ||
            errorCode.contains("PROFILE_NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        }
        
        // Validation errors
        if (errorCode.contains("VALIDATION_FAILED") ||
            errorCode.contains("INVALID_REQUEST") ||
            errorCode.contains("INVALID_") ||
            errorCode.contains("ALREADY_EXISTS")) {
            return HttpStatus.BAD_REQUEST;
        }
        
        // Rate limiting
        if (errorCode.contains("RATE_LIMITED") ||
            errorCode.contains("TOO_MANY_REQUESTS")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        
        // External service errors
        if (errorCode.contains("API_") ||
            errorCode.contains("CONNECTION_FAILED") ||
            errorCode.contains("SERVICE_UNAVAILABLE")) {
            return HttpStatus.BAD_GATEWAY;
        }
        
        // Default to 500 for unknown errors
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Generate a unique trace ID for error tracking
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
