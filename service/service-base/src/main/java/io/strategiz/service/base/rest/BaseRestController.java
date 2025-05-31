package io.strategiz.service.base.rest;

import io.strategiz.framework.exception.ApplicationClientException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Base REST controller with common functionality for all REST controllers in the Strategiz application.
 * This provides common patterns for REST API implementations.
 * 
 * @param <T> The type of request object
 * @param <R> The type of response object
 */
public abstract class BaseRestController<T, R> {
    
    private static final Logger log = LoggerFactory.getLogger(BaseRestController.class);
    
    /**
     * Process the client request and return the appropriate response.
     * 
     * @param request The request object
     * @return The response object
     */
    protected abstract R processRequest(@Valid T request);
    
    /**
     * Create a successful response entity.
     * 
     * @param body The response body
     * @return A ResponseEntity with HTTP status 200 OK
     */
    protected ResponseEntity<R> createSuccessResponse(R body) {
        return ResponseEntity.ok(body);
    }
    
    /**
     * Create a created response entity.
     * 
     * @param body The response body
     * @return A ResponseEntity with HTTP status 201 Created
     */
    protected ResponseEntity<R> createCreatedResponse(R body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
    
    /**
     * Handles display of raw API data without transformations or mappings.
     * This follows the Strategiz principle of showing exactly what comes from the API.
     * 
     * @param rawData The raw API response data
     * @return A ResponseEntity containing the unmodified API data
     */
    protected ResponseEntity<Object> createRawDataResponse(Object rawData) {
        log.info("Returning raw API data without transformations");
        return ResponseEntity.ok(rawData);
    }
    
    /**
     * Handle client exceptions.
     * 
     * @param ex The ApplicationClientException
     * @return A ResponseEntity with appropriate error status
     */
    @ExceptionHandler(ApplicationClientException.class)
    protected ResponseEntity<String> handleClientException(ApplicationClientException ex) {
        log.error("Client exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
    
    /**
     * Handle general exceptions.
     * 
     * @param ex The Exception
     * @return A ResponseEntity with 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred. Please try again later.");
    }
}
