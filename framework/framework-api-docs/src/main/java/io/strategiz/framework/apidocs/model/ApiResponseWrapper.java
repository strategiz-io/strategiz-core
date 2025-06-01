package io.strategiz.framework.apidocs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Standard API response wrapper for all API endpoints.
 * Ensures consistent response structure while preserving real API data.
 * 
 * @param <T> The type of data in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response format")
public class ApiResponseWrapper<T> {
    
    @Schema(description = "Whether the operation was successful", example = "true")
    private boolean success;
    
    @Schema(description = "HTTP status code", example = "200")
    private int status;
    
    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Timestamp of the response")
    private ZonedDateTime timestamp;
    
    @Schema(description = "The actual data from the API call. Contains real data from external APIs with minimal transformation.")
    private T data;
    
    /**
     * Static factory method for successful responses
     * 
     * @param data The data to include in the response
     * @param <T> The type of data
     * @return A success response with the provided data
     */
    public static <T> ApiResponseWrapper<T> success(T data) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .timestamp(ZonedDateTime.now())
                .data(data)
                .build();
    }
    
    /**
     * Static factory method for error responses
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param <T> The type of data
     * @return An error response with the provided details
     */
    public static <T> ApiResponseWrapper<T> error(int status, String message) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}
