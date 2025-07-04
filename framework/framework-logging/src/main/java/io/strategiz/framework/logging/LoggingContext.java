package io.strategiz.framework.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for storing logging information throughout a request.
 * This allows adding contextual information that will be available to all
 * log statements within the same thread, useful for request tracking.
 * 
 * Enhanced version with structured logging support and performance optimizations.
 */
public class LoggingContext {
    private static final ThreadLocal<Map<String, String>> context = ThreadLocal.withInitial(HashMap::new);
    
    // Standard context keys for consistency
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_PATH = "requestPath";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_IP = "requestIp";
    public static final String USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String OPERATION = "operation";
    public static final String COMPONENT = "component";
    public static final String CORRELATION_ID = "correlationId";
    
    /**
     * Add a key-value pair to the logging context
     * @param key context key
     * @param value context value
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            context.get().put(key, value);
        }
    }
    
    /**
     * Add multiple key-value pairs to the logging context
     * @param contextMap map of context values to add
     */
    public static void putAll(Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            context.get().putAll(contextMap);
        }
    }
    
    /**
     * Get a value from the logging context
     * @param key context key
     * @return the value, or null if not found
     */
    public static String get(String key) {
        return context.get().get(key);
    }
    
    /**
     * Get all values from the logging context
     * @return a copy of all context values
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(context.get());
    }
    
    /**
     * Remove a specific key from the context
     * @param key the key to remove
     * @return the previous value, or null if not found
     */
    public static String remove(String key) {
        return context.get().remove(key);
    }
    
    /**
     * Check if a key exists in the context
     * @param key the key to check
     * @return true if the key exists
     */
    public static boolean contains(String key) {
        return context.get().containsKey(key);
    }
    
    /**
     * Get the current request ID
     * @return the request ID, or null if not set
     */
    public static String getRequestId() {
        return get(REQUEST_ID);
    }
    
    /**
     * Set the request ID
     * @param requestId the request ID to set
     */
    public static void setRequestId(String requestId) {
        put(REQUEST_ID, requestId);
    }
    
    /**
     * Get the current user ID
     * @return the user ID, or null if not set
     */
    public static String getUserId() {
        return get(USER_ID);
    }
    
    /**
     * Set the user ID
     * @param userId the user ID to set
     */
    public static void setUserId(String userId) {
        put(USER_ID, userId);
    }
    
    /**
     * Get the current operation
     * @return the operation, or null if not set
     */
    public static String getOperation() {
        return get(OPERATION);
    }
    
    /**
     * Set the current operation
     * @param operation the operation to set
     */
    public static void setOperation(String operation) {
        put(OPERATION, operation);
    }
    
    /**
     * Clear the logging context
     * Should be called at the end of request processing
     */
    public static void clear() {
        context.remove();
    }
    
    /**
     * Get the size of the current context
     * @return number of context entries
     */
    public static int size() {
        return context.get().size();
    }
    
    /**
     * Check if the context is empty
     * @return true if the context is empty
     */
    public static boolean isEmpty() {
        return context.get().isEmpty();
    }
} 