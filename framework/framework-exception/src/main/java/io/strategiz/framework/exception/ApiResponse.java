package io.strategiz.framework.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized API response format for all Strategiz API endpoints.
 * Provides consistent structure for success and error responses.
 *
 * @param <T> the type of data in the response
 */
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
    private final String errorId;
    private final Instant timestamp;
    private Map<String, Object> metadata;

    private ApiResponse(boolean success, T data, String message, String errorId) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorId = errorId;
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }

    /**
     * Create a successful response with data
     *
     * @param data    the data to include in the response
     * @param message optional success message
     * @param <T>     the type of data
     * @return a new ApiResponse
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * Create a successful response with data and no message
     *
     * @param data the data to include in the response
     * @param <T>  the type of data
     * @return a new ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    /**
     * Create an error response
     *
     * @param message error message
     * @param errorId optional error ID for tracking
     * @param <T>     the type of data (null for errors)
     * @return a new ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, String errorId) {
        return new ApiResponse<>(false, null, message, errorId);
    }

    /**
     * Create an error response with no error ID
     *
     * @param message error message
     * @param <T>     the type of data (null for errors)
     * @return a new ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }

    /**
     * Add metadata to the response
     *
     * @param key   metadata key
     * @param value metadata value
     * @return this response, for method chaining
     */
    public ApiResponse<T> addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    /**
     * Set all metadata at once
     *
     * @param metadata map of metadata
     * @return this response, for method chaining
     */
    public ApiResponse<T> setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
        return this;
    }

    // Getters

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorId() {
        return errorId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}
