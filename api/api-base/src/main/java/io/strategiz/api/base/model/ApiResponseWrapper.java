package io.strategiz.api.base.model;

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
     * Constructor with all fields
     * 
     * @param success whether the operation was successful
     * @param status HTTP status code
     * @param message response message
     * @param timestamp timestamp when the response was generated
     * @param data the payload data
     */
    public ApiResponseWrapper(boolean success, int status, String message, ZonedDateTime timestamp, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }
    
    /**
     * @return whether the operation was successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @param success whether the operation was successful
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * @return HTTP status code
     */
    public int getStatus() {
        return status;
    }
    
    /**
     * @param status HTTP status code
     */
    public void setStatus(int status) {
        this.status = status;
    }
    
    /**
     * @return response message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @param message response message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * @return timestamp when the response was generated
     */
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * @param timestamp timestamp when the response was generated
     */
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * @return the payload data
     */
    public T getData() {
        return data;
    }
    
    /**
     * @param data the payload data
     */
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
    
    /**
     * Creates a builder for ApiResponseWrapper
     * @param <T> The type of data in the response
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    /**
     * Builder class for ApiResponseWrapper
     * @param <T> The type of data in the response
     */
    public static class Builder<T> {
        private boolean success;
        private int status;
        private String message;
        private ZonedDateTime timestamp;
        private T data;
        
        /**
         * Sets the success flag
         * @param success whether the operation was successful
         * @return this builder
         */
        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }
        
        /**
         * Sets the status
         * @param status HTTP status code
         * @return this builder
         */
        public Builder<T> status(int status) {
            this.status = status;
            return this;
        }
        
        /**
         * Sets the message
         * @param message response message
         * @return this builder
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }
        
        /**
         * Sets the timestamp
         * @param timestamp timestamp when the response was generated
         * @return this builder
         */
        public Builder<T> timestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        /**
         * Sets the data
         * @param data the payload data
         * @return this builder
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }
        
        /**
         * Builds the ApiResponseWrapper
         * @return a new ApiResponseWrapper instance
         */
        public ApiResponseWrapper<T> build() {
            return new ApiResponseWrapper<>(success, status, message, timestamp, data);
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
     * Static factory method for successful responses with a custom message
     * 
     * @param data The data to include in the response
     * @param message A custom success message
     * @param <T> The type of data
     * @return A success response with the provided data and message
     */
    public static <T> ApiResponseWrapper<T> success(T data, String message) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .status(200)
                .message(message)
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
    
    /**
     * Static factory method for error responses with an error code
     * 
     * @param errorCode Specific error code for client reference
     * @param message Error message
     * @param <T> The type of data
     * @return An error response with the provided details
     */
    public static <T> ApiResponseWrapper<T> error(String errorCode, String message) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .status(400) // Default to Bad Request
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}
