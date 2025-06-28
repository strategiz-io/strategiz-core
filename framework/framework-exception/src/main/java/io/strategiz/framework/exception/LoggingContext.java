package io.strategiz.framework.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for storing logging information throughout a request.
 * This allows adding contextual information that will be available to all
 * log statements within the same thread, useful for request tracking.
 */
public class LoggingContext {
    private static final ThreadLocal<Map<String, String>> context = ThreadLocal.withInitial(HashMap::new);
    
    /**
     * Add a key-value pair to the logging context
     * @param key context key
     * @param value context value
     */
    public static void put(String key, String value) {
        context.get().put(key, value);
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
     * Clear the logging context
     * Should be called at the end of request processing
     */
    public static void clear() {
        context.remove();
    }
}
