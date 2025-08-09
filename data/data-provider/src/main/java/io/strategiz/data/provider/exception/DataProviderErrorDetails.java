package io.strategiz.data.provider.exception;

/**
 * Error details enum for data-provider module
 */
public enum DataProviderErrorDetails {
    
    // Provider errors (PROV_xxx)
    PROV_001("PROV_001", "provider.not.found", "Provider integration not found"),
    PROV_002("PROV_002", "provider.already.exists", "Provider integration already exists"),
    PROV_003("PROV_003", "provider.invalid.credentials", "Invalid provider credentials"),
    PROV_004("PROV_004", "provider.connection.failed", "Provider connection failed"),
    PROV_005("PROV_005", "provider.oauth.failed", "OAuth authentication failed"),
    PROV_006("PROV_006", "provider.update.failed", "Failed to update provider integration"),
    PROV_007("PROV_007", "provider.delete.failed", "Failed to delete provider integration"),
    PROV_008("PROV_008", "provider.invalid.type", "Invalid provider type"),
    PROV_009("PROV_009", "provider.invalid.status", "Invalid provider status"),
    PROV_010("PROV_010", "provider.sync.failed", "Provider synchronization failed"),
    
    // Repository errors (REPO_xxx)
    REPO_001("REPO_001", "repository.save.failed", "Failed to save provider integration"),
    REPO_002("REPO_002", "repository.find.failed", "Failed to find provider integration"),
    REPO_003("REPO_003", "repository.delete.failed", "Failed to delete provider integration"),
    REPO_004("REPO_004", "repository.update.failed", "Failed to update provider integration"),
    
    // Validation errors (VAL_xxx)
    VAL_001("VAL_001", "validation.user.required", "User ID is required"),
    VAL_002("VAL_002", "validation.provider.required", "Provider ID is required"),
    VAL_003("VAL_003", "validation.credentials.required", "Provider credentials are required"),
    VAL_004("VAL_004", "validation.invalid.input", "Invalid input provided");
    
    private final String code;
    private final String messageKey;
    private final String defaultMessage;
    
    DataProviderErrorDetails(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}