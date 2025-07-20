package io.strategiz.data.auth.model.provider;

import java.util.List;

/**
 * Result of creating a provider integration
 * Contains metadata about the provider and integration
 */
public class ProviderIntegrationResult {

    /**
     * Provider display name (e.g., "Kraken", "Binance US")
     */
    private String providerName;

    /**
     * Provider type (e.g., "exchange", "brokerage")
     */
    private String providerType;

    /**
     * Whether this provider supports live trading
     */
    private boolean supportsTrading;

    /**
     * Permissions granted for this integration
     */
    private List<String> permissions;

    /**
     * Integration-specific metadata
     */
    private Object metadata;

    /**
     * Default constructor
     */
    public ProviderIntegrationResult() {
    }

    /**
     * All-args constructor
     */
    public ProviderIntegrationResult(String providerName, String providerType, boolean supportsTrading, 
                                    List<String> permissions, Object metadata) {
        this.providerName = providerName;
        this.providerType = providerType;
        this.supportsTrading = supportsTrading;
        this.permissions = permissions;
        this.metadata = metadata;
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    /**
     * Builder pattern implementation
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String providerName;
        private String providerType;
        private boolean supportsTrading;
        private List<String> permissions;
        private Object metadata;

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

        public Builder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder metadata(Object metadata) {
            this.metadata = metadata;
            return this;
        }

        public ProviderIntegrationResult build() {
            return new ProviderIntegrationResult(providerName, providerType, supportsTrading, permissions, metadata);
        }
    }
}