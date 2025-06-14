package io.strategiz.framework.apidocs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Standard API response wrapper for all API endpoints.
 * Ensures consistent response structure while preserving real API data.
 * 
 * @param <T> The type of data in the response
 */
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
     * Default constructor
     */
    public ApiResponseWrapper() {
    }
    
    /**
     * All-args constructor
     */
    public ApiResponseWrapper(boolean success, int status, String message, ZonedDateTime timestamp, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponseWrapper<?> that = (ApiResponseWrapper<?>) o;
        return success == that.success && 
               status == that.status && 
               Objects.equals(message, that.message) && 
               Objects.equals(timestamp, that.timestamp) && 
               Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, status, message, timestamp, data);
    }
    
    @Override
    public String toString() {
        return "ApiResponseWrapper{" +
                "success=" + success +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
    
    // Builder
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static class Builder<T> {
        private final ApiResponseWrapper<T> instance = new ApiResponseWrapper<>();
        
        public Builder<T> success(boolean success) {
            instance.setSuccess(success);
            return this;
        }
        
        public Builder<T> status(int status) {
            instance.setStatus(status);
            return this;
        }
        
        public Builder<T> message(String message) {
            instance.setMessage(message);
            return this;
        }
        
        public Builder<T> timestamp(ZonedDateTime timestamp) {
            instance.setTimestamp(timestamp);
            return this;
        }
        
        public Builder<T> data(T data) {
            instance.setData(data);
            return this;
        }
        
        public ApiResponseWrapper<T> build() {
            return instance;
        }
    }
    
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
