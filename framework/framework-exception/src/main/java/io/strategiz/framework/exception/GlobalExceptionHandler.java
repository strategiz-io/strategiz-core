package io.strategiz.framework.exception;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all Spring controllers.
 * Converts exceptions into standardized API responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Strategiz exceptions
     */
    @ExceptionHandler(StrategizException.class)
    public ResponseEntity<ApiResponse<Void>> handleStrategizException(StrategizException ex) {
        // Log with domain context
        log.error("Domain: {}, Error: {} ({}), Message: {}, Context: {}", 
                  ex.getDomain(), 
                  ex.getErrorDefinition().name(),
                  ex.getErrorId(),
                  ex.getMessage(), 
                  ex.getContext());
        
        // Create response
        HttpStatus status = HttpStatus.valueOf(ex.getErrorCode().getHttpStatus());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getErrorId());
        
        // Add context as metadata if available
        if (!ex.getContext().isEmpty()) {
            response.setMetadata(ex.getContext());
        }
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle validation exceptions from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation errors: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Validation failed. See 'data' for details.");
        
        return ResponseEntity.badRequest()
                .body(response.setMetadata(Map.of("errors", errors)));
    }
    
    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<List<String>>> handleBindException(BindException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        log.warn("Binding errors: {}", errors);
        
        ApiResponse<List<String>> response = ApiResponse.error("Binding failed");
        return ResponseEntity.badRequest()
                .body(response.setMetadata(Map.of("errors", errors)));
    }
    
    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();
        
        log.warn("Type mismatch: {}", error);
        
        ApiResponse<Void> response = ApiResponse.error(
                "Type mismatch: " + error);
                
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        // Generate an error ID for tracking
        String errorId = UUID.randomUUID().toString();
        
        // Log the exception with stack trace
        log.error("Unhandled exception (ID: {}): {}", errorId, ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred. Please contact support with this error ID: " + errorId,
                errorId);
                
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
