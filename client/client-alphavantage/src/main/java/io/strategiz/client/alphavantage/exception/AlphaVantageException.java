package io.strategiz.client.alphavantage.exception;

/**
 * Exception thrown when AlphaVantage API operations fail
 */
public class AlphaVantageException extends RuntimeException {
    
    public AlphaVantageException(String message) {
        super(message);
    }
    
    public AlphaVantageException(String message, Throwable cause) {
        super(message, cause);
    }
}