package io.strategiz.service.auth.model.provider;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for provider integration creation
 */
@Schema(description = "Provider integration creation response")
public class CreateProviderIntegrationResponse {
    
    @Schema(description = "Whether the integration was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Provider identifier", example = "kraken")
    private String providerId;
    
    @Schema(description = "Integration status", example = "connected")
    private String status;
    
    @Schema(description = "Response message", example = "Successfully connected to Kraken")
    private String message;
    
    @Schema(description = "When the integration was created")
    private Instant connectedAt;
    
    @Schema(description = "Permissions granted to this integration")
    private List<String> permissions;
    
    @Schema(description = "Integration metadata")
    private ProviderIntegrationMetadata metadata;
    
    // Constructors
    public CreateProviderIntegrationResponse() {
    }
    
    public CreateProviderIntegrationResponse(boolean success, String providerId, String status, String message,
            Instant connectedAt, List<String> permissions, ProviderIntegrationMetadata metadata) {
        this.success = success;
        this.providerId = providerId;
        this.status = status;
        this.message = message;
        this.connectedAt = connectedAt;
        this.permissions = permissions;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Instant getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    public ProviderIntegrationMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ProviderIntegrationMetadata metadata) {
        this.metadata = metadata;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean success;
        private String providerId;
        private String status;
        private String message;
        private Instant connectedAt;
        private List<String> permissions;
        private ProviderIntegrationMetadata metadata;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder connectedAt(Instant connectedAt) {
            this.connectedAt = connectedAt;
            return this;
        }
        
        public Builder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }
        
        public Builder metadata(ProviderIntegrationMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public CreateProviderIntegrationResponse build() {
            return new CreateProviderIntegrationResponse(success, providerId, status, message, 
                    connectedAt, permissions, metadata);
        }
    }
    
    @Schema(description = "Provider integration metadata")
    public static class ProviderIntegrationMetadata {
        
        @Schema(description = "Provider display name", example = "Kraken")
        private String providerName;
        
        @Schema(description = "Provider type", example = "exchange")
        private String providerType;
        
        @Schema(description = "Whether the provider supports live trading", example = "true")
        private boolean supportsTrading;
        
        @Schema(description = "Last successful connection test")
        private Instant lastTestedAt;
        
        // Constructors
        public ProviderIntegrationMetadata() {
        }
        
        public ProviderIntegrationMetadata(String providerName, String providerType, 
                boolean supportsTrading, Instant lastTestedAt) {
            this.providerName = providerName;
            this.providerType = providerType;
            this.supportsTrading = supportsTrading;
            this.lastTestedAt = lastTestedAt;
        }
        
        // Getters and Setters
        public String getProviderName() {
            return providerName;
        }
        
        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }
        
        public String getProviderType() {
            return providerType;
        }
        
        public void setProviderType(String providerType) {
            this.providerType = providerType;
        }
        
        public boolean isSupportsTrading() {
            return supportsTrading;
        }
        
        public void setSupportsTrading(boolean supportsTrading) {
            this.supportsTrading = supportsTrading;
        }
        
        public Instant getLastTestedAt() {
            return lastTestedAt;
        }
        
        public void setLastTestedAt(Instant lastTestedAt) {
            this.lastTestedAt = lastTestedAt;
        }
        
        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String providerName;
            private String providerType;
            private boolean supportsTrading;
            private Instant lastTestedAt;
            
            public Builder providerName(String providerName) {
                this.providerName = providerName;
                return this;
            }
            
            public Builder providerType(String providerType) {
                this.providerType = providerType;
                return this;
            }
            
            public Builder supportsTrading(boolean supportsTrading) {
                this.supportsTrading = supportsTrading;
                return this;
            }
            
            public Builder lastTestedAt(Instant lastTestedAt) {
                this.lastTestedAt = lastTestedAt;
                return this;
            }
            
            public ProviderIntegrationMetadata build() {
                return new ProviderIntegrationMetadata(providerName, providerType, 
                        supportsTrading, lastTestedAt);
            }
        }
    }
}