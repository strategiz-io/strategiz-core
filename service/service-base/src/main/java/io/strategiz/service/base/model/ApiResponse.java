package io.strategiz.service.base.model;

import java.util.Objects;

/**
 * Generic API response wrapper
 * @param <T> The type of data in the response
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    /**
     * Default constructor
     */
    public ApiResponse() {
    }
    
    /**
     * Constructor with all fields
     * 
     * @param success whether the operation was successful
     * @param message message describing the operation result
     * @param data the payload data
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
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
     * @return message describing the operation result
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @param message message describing the operation result
     */
    public void setMessage(String message) {
        this.message = message;
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
        ApiResponse<?> that = (ApiResponse<?>) o;
        return success == that.success && 
               Objects.equals(message, that.message) && 
               Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, message, data);
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
    
    /**
     * Creates a builder for ApiResponse
     * @param <T> The type of data in the response
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    /**
     * Builder class for ApiResponse
     * @param <T> The type of data in the response
     */
    public static class Builder<T> {
        private boolean success;
        private String message;
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
         * Sets the message
         * @param message message describing the operation result
         * @return this builder
         */
        public Builder<T> message(String message) {
            this.message = message;
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
         * Builds the ApiResponse
         * @return a new ApiResponse instance
         */
        public ApiResponse<T> build() {
            return new ApiResponse<>(success, message, data);
        }
    }
    
    /**
     * Static factory method for successful responses
     * 
     * @param message Success message
     * @param data The data to include in the response
     * @param <T> The type of data
     * @return A success response with the provided data and message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Static factory method for error responses
     * 
     * @param message Error message
     * @param <T> The type of data
     * @return An error response with the provided message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
