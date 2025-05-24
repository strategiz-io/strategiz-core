package io.strategiz.api.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard API response format for all authentication endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    private String status;
    private String message;
    private Map<String, Object> data;
    private long timestamp;
    
    /**
     * Create a success response
     * 
     * @param message Success message
     * @return ApiResponse
     */
    public static ApiResponse success(String message) {
        return ApiResponse.builder()
                .status("success")
                .message(message)
                .data(new HashMap<>())
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
    public static ApiResponse success(String message, Map<String, Object> data) {
        return ApiResponse.builder()
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
    public static ApiResponse error(String message) {
        return ApiResponse.builder()
                .status("error")
                .message(message)
                .data(new HashMap<>())
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
    public static ApiResponse error(String message, Map<String, Object> data) {
        return ApiResponse.builder()
                .status("error")
                .message(message)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Add data to the response
     * 
     * @param key Data key
     * @param value Data value
     * @return this ApiResponse
     */
    public ApiResponse addData(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
        return this;
    }
}
