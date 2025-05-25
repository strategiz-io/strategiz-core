package io.strategiz.api.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard API response format for all authentication endpoints
 * 
 * @param <T> Type of data contained in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private String status;
    private String message;
    private T data;
    private long timestamp;
    
    /**
     * Create a success response
     * 
     * @param message Success message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(null)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Create a success response with data
     * 
     * @param message Success message
     * @param data Response data
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Create an error response
     * 
     * @param message Error message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .data(null)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Create an error response with data
     * 
     * @param message Error message
     * @param data Response data
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Create a new ApiResponse with the same status and message but different data
     * 
     * @param data New data
     * @return New ApiResponse instance with updated data
     */
    public <U> ApiResponse<U> withData(U data) {
        return ApiResponse.<U>builder()
                .status(this.status)
                .message(this.message)
                .data(data)
                .timestamp(this.timestamp)
                .build();
    }
}
