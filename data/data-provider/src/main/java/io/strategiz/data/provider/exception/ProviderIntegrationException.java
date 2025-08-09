package io.strategiz.data.provider.exception;

/**
 * Custom exception for provider integration operations
 */
public class ProviderIntegrationException extends RuntimeException {
    
    private final DataProviderErrorDetails errorDetails;
    private final Object[] args;
    
    public ProviderIntegrationException(DataProviderErrorDetails errorDetails) {
        super(errorDetails.getDefaultMessage());
        this.errorDetails = errorDetails;
        this.args = null;
    }
    
    public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Object... args) {
        super(errorDetails.getDefaultMessage());
        this.errorDetails = errorDetails;
        this.args = args;
    }
    
    public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Throwable cause) {
        super(errorDetails.getDefaultMessage(), cause);
        this.errorDetails = errorDetails;
        this.args = null;
    }
    
    public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Throwable cause, Object... args) {
        super(errorDetails.getDefaultMessage(), cause);
        this.errorDetails = errorDetails;
        this.args = args;
    }
    
    public DataProviderErrorDetails getErrorDetails() {
        return errorDetails;
    }
    
    public Object[] getArgs() {
        return args;
    }
    
    public String getErrorCode() {
        return errorDetails.getCode();
    }
}