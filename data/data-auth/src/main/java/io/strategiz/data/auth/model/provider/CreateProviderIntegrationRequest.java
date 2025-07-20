package io.strategiz.data.auth.model.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request DTO for creating provider integrations during signup
 */
@Schema(description = "Request to create a provider integration")
public class CreateProviderIntegrationRequest {
    
    @NotBlank(message = "Provider ID is required")
    @Schema(description = "Provider identifier", example = "kraken", required = true)
    private String providerId;
    
    @Schema(description = "Provider-specific credentials", required = true)
    private ProviderCredentials credentials;
    
    // Constructors
    public CreateProviderIntegrationRequest() {
    }
    
    public CreateProviderIntegrationRequest(String providerId, ProviderCredentials credentials) {
        this.providerId = providerId;
        this.credentials = credentials;
    }
    
    // Getters and Setters
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public ProviderCredentials getCredentials() {
        return credentials;
    }
    
    public void setCredentials(ProviderCredentials credentials) {
        this.credentials = credentials;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String providerId;
        private ProviderCredentials credentials;
        
        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }
        
        public Builder credentials(ProviderCredentials credentials) {
            this.credentials = credentials;
            return this;
        }
        
        public CreateProviderIntegrationRequest build() {
            return new CreateProviderIntegrationRequest(providerId, credentials);
        }
    }
    
    @Schema(description = "Provider credentials")
    public static class ProviderCredentials {
        
        @Schema(description = "API key or client ID", example = "your-api-key", required = true)
        private String apiKey;
        
        @Schema(description = "API secret or client secret", example = "your-api-secret", required = true)
        private String apiSecret;
        
        @Schema(description = "One-time password for 2FA (if required)", example = "123456")
        private String otp;
        
        @Schema(description = "Additional provider-specific parameters")
        private Map<String, String> additionalParams;
        
        // Constructors
        public ProviderCredentials() {
        }
        
        public ProviderCredentials(String apiKey, String apiSecret, String otp, Map<String, String> additionalParams) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            this.otp = otp;
            this.additionalParams = additionalParams;
        }
        
        // Getters and Setters
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
        
        public String getOtp() {
            return otp;
        }
        
        public void setOtp(String otp) {
            this.otp = otp;
        }
        
        public Map<String, String> getAdditionalParams() {
            return additionalParams;
        }
        
        public void setAdditionalParams(Map<String, String> additionalParams) {
            this.additionalParams = additionalParams;
        }
        
        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String apiKey;
            private String apiSecret;
            private String otp;
            private Map<String, String> additionalParams;
            
            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }
            
            public Builder apiSecret(String apiSecret) {
                this.apiSecret = apiSecret;
                return this;
            }
            
            public Builder otp(String otp) {
                this.otp = otp;
                return this;
            }
            
            public Builder additionalParams(Map<String, String> additionalParams) {
                this.additionalParams = additionalParams;
                return this;
            }
            
            public ProviderCredentials build() {
                return new ProviderCredentials(apiKey, apiSecret, otp, additionalParams);
            }
        }
    }
}