package io.strategiz.client.coinbase.exception;

/**
 * Custom exception for Coinbase API errors
 * This provides more detailed error information from the Coinbase API
 */
public class CoinbaseApiException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String errorDetails;
    
    /**
     * Constructor with message and cause
     * 
     * @param message Error message
     * @param cause Root cause exception
     */
    public CoinbaseApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorDetails = null;
    }
    
    /**
     * Constructor with message, cause, and detailed error information
     * 
     * @param message Error message
     * @param cause Root cause exception
     * @param errorDetails Detailed error information from the Coinbase API
     */
    public CoinbaseApiException(String message, Throwable cause, String errorDetails) {
        super(message, cause);
        this.errorDetails = errorDetails;
    }
    
    /**
     * Get detailed error information from the Coinbase API
     * 
     * @return Detailed error information
     */
    public String getErrorDetails() {
        return errorDetails;
    }
}
