package io.strategiz.framework.apidocs.controller;

import io.strategiz.framework.apidocs.annotation.ApiDocumented;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base controller class for all API endpoints.
 * Provides common OpenAPI documentation and error handling functionality.
 * 
 * All API controllers should extend this class to ensure
 * consistent documentation and behavior across the application.
 */
@SecurityRequirement(name = "bearer-jwt")
public abstract class BaseApiController {

    /**
     * Standard error response handler for all controllers.
     * Handles exceptions and returns a standardized error response.
     * 
     * @param exception The exception that occurred
     * @return Standardized error response with timestamp and details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", ZonedDateTime.now().toString());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.put("message", exception.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Create a standard successful response with data.
     * This ensures all API responses follow the same structure.
     * 
     * @param data The data to include in the response
     * @param <T> The type of data
     * @return ResponseEntity with standardized structure
     */
    protected <T> ResponseEntity<Map<String, Object>> createSuccessResponse(T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", ZonedDateTime.now().toString());
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("data", data);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Create a standard error response.
     * 
     * @param status HTTP status code
     * @param message Error message
     * @return ResponseEntity with standardized error structure
     */
    protected ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", ZonedDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("success", false);
        response.put("message", message);
        
        return new ResponseEntity<>(response, status);
    }
}
