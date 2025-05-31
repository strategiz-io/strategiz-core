package io.strategiz.framework.exception;

/**
 * Exception thrown when there is an error communicating with an external service client.
 * This is a replacement for the Synapse framework exception of the same name.
 */
public class ApplicationClientException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new ApplicationClientException with the specified detail message.
     *
     * @param message the detail message
     */
    public ApplicationClientException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ApplicationClientException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ApplicationClientException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new ApplicationClientException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ApplicationClientException(Throwable cause) {
        super(cause);
    }
}
