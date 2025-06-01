package io.strategiz.api.base.controller;

import io.strategiz.api.base.model.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base controller class for all API endpoints in the Strategiz application.
 * Provides consistent OpenAPI documentation and standardized response handling.
 * 
 * All API controllers in the Strategiz application should extend this class to ensure
 * consistent documentation and behavior across the entire application.
 */
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successful operation", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class))),
    @ApiResponse(responseCode = "400", description = "Bad request", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class))),
    @ApiResponse(responseCode = "404", description = "Resource not found", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error", 
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, 
        schema = @Schema(implementation = ApiResponseWrapper.class)))
})
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
        errorResponse.put("success", false);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Creates a successful response with the given data.
     * 
     * @param <T> The type of data
     * @param data The data to include in the response
     * @return A ResponseEntity containing a standardized API response
     */
    protected <T> ResponseEntity<ApiResponseWrapper<T>> success(T data) {
        return ResponseEntity.ok(ApiResponseWrapper.success(data));
    }
    
    /**
     * Creates a successful response with the given data and custom message.
     * 
     * @param <T> The type of data
     * @param data The data to include in the response
     * @param message A custom success message
     * @return A ResponseEntity containing a standardized API response
     */
    protected <T> ResponseEntity<ApiResponseWrapper<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponseWrapper.success(data, message));
    }

    /**
     * Creates an error response with the given status, error code, and message.
     * 
     * @param <T> The type of data (usually Void for error responses)
     * @param status The HTTP status code
     * @param errorCode A specific error code for client reference
     * @param message The error message
     * @return A ResponseEntity containing a standardized API error response
     */
    protected <T> ResponseEntity<ApiResponseWrapper<T>> error(HttpStatus status, String errorCode, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiResponseWrapper.error(errorCode, message));
    }
    
    /**
     * Creates a "not found" error response.
     * 
     * @param <T> The type of data (usually Void for error responses)
     * @param resourceType The type of resource that was not found
     * @param identifier The identifier that was used to look up the resource
     * @return A ResponseEntity containing a standardized API error response
     */
    protected <T> ResponseEntity<ApiResponseWrapper<T>> notFound(String resourceType, String identifier) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", 
                resourceType + " not found with identifier: " + identifier);
    }
    
    /**
     * Creates a "bad request" error response.
     * 
     * @param <T> The type of data (usually Void for error responses)
     * @param message The error message describing the validation issue
     * @return A ResponseEntity containing a standardized API error response
     */
    protected <T> ResponseEntity<ApiResponseWrapper<T>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
