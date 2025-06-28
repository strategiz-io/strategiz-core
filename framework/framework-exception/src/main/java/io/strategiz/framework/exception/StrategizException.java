package io.strategiz.framework.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base exception class for all Strategiz application exceptions.
 * Provides standardized error information including domain, error code, 
 * error ID for tracing, and contextual information.
 */
public class StrategizException extends RuntimeException {
    private final ErrorDefinition errorDefinition;
    private final String errorId;
    private final Map<String, Object> context;
    private final Object[] messageArgs;
    
    /**
     * Create a new StrategizException with the specified error definition and message arguments
     * @param errorDefinition the error definition
     * @param messageArgs arguments to format the error message
     */
    public StrategizException(ErrorDefinition errorDefinition, Object... messageArgs) {
        super(errorDefinition.formatMessage(messageArgs));
        this.errorDefinition = errorDefinition;
        this.errorId = UUID.randomUUID().toString();
        this.context = new HashMap<>();
        this.messageArgs = messageArgs;
    }
    
    /**
     * Create a new StrategizException with a custom message
     * @param errorDefinition the error definition
     * @param message custom error message
     * @param messageArgs arguments for the error definition (not used for message, but stored for context)
     */
    public StrategizException(ErrorDefinition errorDefinition, String message, Object... messageArgs) {
        super(message);
        this.errorDefinition = errorDefinition;
        this.errorId = UUID.randomUUID().toString();
        this.context = new HashMap<>();
        this.messageArgs = messageArgs;
    }
    
    /**
     * Create a new StrategizException with an underlying cause
     * @param errorDefinition the error definition
     * @param cause the underlying cause
     * @param messageArgs arguments to format the error message
     */
    public StrategizException(ErrorDefinition errorDefinition, Throwable cause, Object... messageArgs) {
        super(errorDefinition.formatMessage(messageArgs), cause);
        this.errorDefinition = errorDefinition;
        this.errorId = UUID.randomUUID().toString();
        this.context = new HashMap<>();
        this.messageArgs = messageArgs;
    }
    
    /**
     * Add contextual information to the exception
     * @param key the context key
     * @param value the context value
     * @return this exception, for method chaining
     */
    public StrategizException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Add multiple context values at once
     * @param contextMap map of context values
     * @return this exception, for method chaining
     */
    public StrategizException withContext(Map<String, Object> contextMap) {
        this.context.putAll(contextMap);
        return this;
    }
    
    /**
     * Get the error definition for this exception
     * @return the error definition
     */
    public ErrorDefinition getErrorDefinition() {
        return errorDefinition;
    }
    
    /**
     * Get the unique error ID for tracking this exception instance
     * @return the error ID
     */
    public String getErrorId() {
        return errorId;
    }
    
    /**
     * Get the domain this exception belongs to
     * @return the domain
     */
    public String getDomain() {
        return errorDefinition.getDomain();
    }
    
    /**
     * Get the error code for this exception
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorDefinition.getErrorCode();
    }
    
    /**
     * Get the context map for this exception
     * @return the context map
     */
    public Map<String, Object> getContext() {
        return Map.copyOf(context);
    }
    
    /**
     * Get the message arguments used to format the error message
     * @return the message arguments
     */
    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
