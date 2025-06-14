package io.strategiz.api.auth.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Standard API response format for all authentication endpoints
 * 
 * @param <T> Type of data contained in the response
 */
public class ApiResponse<T> {
    
    private String status;
    private String message;
    private T data;
    private long timestamp;
    
    /**
     * Default constructor
     */
    public ApiResponse() {
    }
    
    /**
     * All-args constructor
     * 
     * @param status status of the response
     * @param message message of the response
     * @param data data payload
     * @param timestamp timestamp of the response
     */
    public ApiResponse(String status, String message, T data, long timestamp) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }
    
    /**
     * @return status of the response
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * @param status status of the response
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * @return message of the response
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @param message message of the response
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * @return data payload
     */
    public T getData() {
        return data;
    }
    
    /**
     * @param data data payload
     */
    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * @return timestamp of the response
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * @param timestamp timestamp of the response
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponse<?> that = (ApiResponse<?>) o;
        return timestamp == that.timestamp && 
               Objects.equals(status, that.status) && 
               Objects.equals(message, that.message) && 
               Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, message, data, timestamp);
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * Creates a new builder for ApiResponse
     * 
     * @param <T> Type of data contained in the response
     * @return a new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    /**
     * Builder class for ApiResponse
     * 
     * @param <T> Type of data contained in the response
     */
    public static class Builder<T> {
        private String status;
        private String message;
        private T data;
        private long timestamp;
        
        /**
         * Set the status
         * 
         * @param status response status
         * @return builder instance
         */
        public Builder<T> status(String status) {
            this.status = status;
            return this;
        }
        
        /**
         * Set the message
         * 
         * @param message response message
         * @return builder instance
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }
        
        /**
         * Set the data
         * 
         * @param data response data
         * @return builder instance
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }
        
        /**
         * Set the timestamp
         * 
         * @param timestamp response timestamp
         * @return builder instance
         */
        public Builder<T> timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        /**
         * Build the ApiResponse
         * 
         * @return new ApiResponse instance
         */
        public ApiResponse<T> build() {
            return new ApiResponse<>(status, message, data, timestamp);
        }
    }
    
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
