package io.strategiz.framework.secrets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HashiCorp Vault integration.
 * These properties can be set in application.properties/yaml files.
 */
@ConfigurationProperties(prefix = "strategiz.vault")
public class VaultProperties {

    /**
     * Whether vault integration is enabled
     */
    private boolean enabled = true;

    /**
     * Vault server address
     */
    private String address = "http://localhost:8200";

    /**
     * Default path where secrets are stored in vault
     */
    private String secretsPath = "secret";
    
    /**
     * Time in milliseconds for caching secret values
     * Set to 0 to disable caching
     */
    private long cacheTimeoutMs = 60000; // 1 minute by default
    
    /**
     * Whether to fail application startup if vault is not available
     */
    private boolean failFast = false;
    
    /**
     * Whether to fall back to application.properties if a secret is not found in vault
     */
    private boolean fallbackToProperties = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSecretsPath() {
        return secretsPath;
    }

    public void setSecretsPath(String secretsPath) {
        this.secretsPath = secretsPath;
    }
    
    public long getCacheTimeoutMs() {
        return cacheTimeoutMs;
    }
    
    public void setCacheTimeoutMs(long cacheTimeoutMs) {
        this.cacheTimeoutMs = cacheTimeoutMs;
    }
    
    public boolean isFailFast() {
        return failFast;
    }
    
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
    
    public boolean isFallbackToProperties() {
        return fallbackToProperties;
    }
    
    public void setFallbackToProperties(boolean fallbackToProperties) {
        this.fallbackToProperties = fallbackToProperties;
    }
}
