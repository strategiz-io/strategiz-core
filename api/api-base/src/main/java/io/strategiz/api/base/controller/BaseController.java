package io.strategiz.api.base.controller;

import io.strategiz.service.base.model.BaseServiceResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Base controller for all REST controllers in the Strategiz application.
 * This is the foundation controller that all feature controllers should extend.
 * 
 * @param <T> The type of request object
 * @param <R> The type of response object extending BaseServiceResponse
 */
public abstract class BaseController<T, R extends BaseServiceResponse> {
    
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);
    
    /**
     * Process the request and return a response.
     * 
     * @param request The request object
     * @return The response object
     */
    public abstract R process(@Valid T request);
    
    /**
     * Default exception handler for all controllers.
     * 
     * @param ex The exception that was thrown
     * @return A response entity with an error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("Error processing request", ex);
        return new ResponseEntity<>("An error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
