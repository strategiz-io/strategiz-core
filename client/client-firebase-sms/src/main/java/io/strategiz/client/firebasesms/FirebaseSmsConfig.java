package io.strategiz.client.firebasesms;

/**
 * Configuration interface for Firebase SMS client
 * 
 * This interface defines the configuration contract that implementations
 * must provide to configure the Firebase SMS client behavior.
 */
public interface FirebaseSmsConfig {
    
    /**
     * Whether Firebase SMS functionality is enabled
     * 
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Whether to use mock SMS sending (no actual SMS sent)
     * 
     * @return true if mock mode is enabled, false otherwise
     */
    boolean isMockSmsEnabled();
    
    /**
     * Whether to log OTP codes in development mode
     * 
     * @return true if OTP codes should be logged, false otherwise
     */
    boolean isLogOtpCodes();
    
    /**
     * Firebase project ID for authentication
     * 
     * @return The Firebase project ID
     */
    String getProjectId();
    
    /**
     * Firebase service account key path (optional - can use default credentials)
     * 
     * @return The path to the service account key file, or null for default credentials
     */
    default String getServiceAccountKeyPath() {
        return null;
    }
    
    /**
     * Firebase Phone Auth timeout in seconds
     * 
     * @return The timeout duration in seconds
     */
    default long getPhoneAuthTimeoutSeconds() {
        return 60;
    }
    
    /**
     * Whether to enable auto-retrieval of SMS codes (for testing)
     * 
     * @return true if auto-retrieval is enabled, false otherwise
     */
    default boolean isAutoRetrieveEnabled() {
        return false;
    }
}