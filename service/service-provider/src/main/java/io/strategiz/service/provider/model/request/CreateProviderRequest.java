package io.strategiz.service.provider.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request model for creating a new provider connection.
 * Used for initiating OAuth flows or API key connections.
 */
public class CreateProviderRequest {
    
    @NotBlank(message = "Provider ID is required")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "Provider ID must contain only lowercase letters, numbers, hyphens, and underscores")
    private String providerId;
    
    private String userId;
    
    @NotBlank(message = "Connection type is required")
    @Pattern(regexp = "^(oauth|api_key)$", message = "Connection type must be either 'oauth' or 'api_key'")
    private String connectionType;
    
    // Optional: For API key connections
    private String apiKey;
    private String apiSecret;
    private String passphrase; // Some exchanges like Coinbase Pro require this
    
    // Optional: For OAuth connections
    private String redirectUri; // Custom redirect URI if different from default
    private String scope; // Custom OAuth scope if different from default
    
    // Optional: Account type preference
    @Pattern(regexp = "^(paper|live)$", message = "Account type must be either 'paper' or 'live'")
    private String accountType = "paper"; // Default to paper trading
    
    // Optional: Additional provider-specific configuration
    private Map<String, Object> additionalConfig;
    
    // Optional: API key data from frontend (nested structure)
    @JsonProperty("apiKeyData")
    private Map<String, String> apiKeyData;
    
    // Optional: User preferences
    private boolean enableNotifications = true;
    private boolean enableAutoRefresh = true;

    // Constructors
    public CreateProviderRequest() {
    }

    public CreateProviderRequest(String providerId, String connectionType) {
        this.providerId = providerId;
        this.connectionType = connectionType;
    }

    // Getters and Setters
    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Map<String, Object> getAdditionalConfig() {
        return additionalConfig;
    }

    public void setAdditionalConfig(Map<String, Object> additionalConfig) {
        this.additionalConfig = additionalConfig;
    }

    public boolean isEnableNotifications() {
        return enableNotifications;
    }

    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }

    public boolean isEnableAutoRefresh() {
        return enableAutoRefresh;
    }

    public void setEnableAutoRefresh(boolean enableAutoRefresh) {
        this.enableAutoRefresh = enableAutoRefresh;
    }
    
    public Map<String, String> getApiKeyData() {
        return apiKeyData;
    }
    
    public void setApiKeyData(Map<String, String> apiKeyData) {
        this.apiKeyData = apiKeyData;
        // If apiKeyData is provided, extract values to individual fields
        if (apiKeyData != null) {
            this.apiKey = apiKeyData.get("apiKey");
            this.apiSecret = apiKeyData.get("apiSecret");
            // Handle OTP through additionalConfig
            String otp = apiKeyData.get("otp");
            if (otp != null && !otp.trim().isEmpty()) {
                if (this.additionalConfig == null) {
                    this.additionalConfig = new java.util.HashMap<>();
                }
                this.additionalConfig.put("otp", otp);
            }
        }
    }

    // Helper methods
    public boolean isOAuthConnection() {
        return "oauth".equals(connectionType);
    }

    public boolean isApiKeyConnection() {
        return "api_key".equals(connectionType);
    }

    public boolean hasApiCredentials() {
        return apiKey != null && !apiKey.trim().isEmpty() && 
               apiSecret != null && !apiSecret.trim().isEmpty();
    }

    public Map<String, Object> getCredentials() {
        Map<String, Object> credentials = new java.util.HashMap<>();
        if (apiKey != null) credentials.put("apiKey", apiKey);
        if (apiSecret != null) credentials.put("apiSecret", apiSecret);
        if (passphrase != null) credentials.put("passphrase", passphrase);
        // Support for OTP if provided in additionalConfig
        if (additionalConfig != null && additionalConfig.containsKey("otp")) {
            credentials.put("otp", additionalConfig.get("otp"));
        }
        return credentials;
    }

    @Override
    public String toString() {
        return "CreateProviderRequest{" +
                "providerId='" + providerId + '\'' +
                ", connectionType='" + connectionType + '\'' +
                ", accountType='" + accountType + '\'' +
                ", enableNotifications=" + enableNotifications +
                ", enableAutoRefresh=" + enableAutoRefresh +
                ", hasApiCredentials=" + hasApiCredentials() +
                '}';
    }
} 