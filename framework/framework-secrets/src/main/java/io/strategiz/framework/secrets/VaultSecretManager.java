package io.strategiz.framework.secrets;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.config.VaultProperties;
import io.strategiz.framework.secrets.SecretsErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashiCorp Vault implementation of the SecretManager interface.
 */
@Service
public class VaultSecretManager implements SecretManager {
    
    private static final Logger log = LoggerFactory.getLogger(VaultSecretManager.class);
    
    private final VaultTemplate vaultTemplate;
    private final VaultProperties properties;
    private final Environment environment;
    
    // Simple cache for secrets
    private final Map<String, CachedSecret> secretCache = new ConcurrentHashMap<>();
    
    @Autowired
    public VaultSecretManager(VaultTemplate vaultTemplate, VaultProperties properties, Environment environment) {
        this.vaultTemplate = vaultTemplate;
        this.properties = properties;
        this.environment = environment;
    }
    
    @Override
    public String getSecret(String key) {
        // Implementation details would go here
        // For now, throwing exception as not implemented
        throw new StrategizException(SecretsErrors.CONFIGURATION_ERROR, "Vault integration not implemented");
    }
    
    @Override
    public String getSecret(String key, String defaultValue) {
        try {
            return getSecret(key);
        } catch (StrategizException e) {
            return defaultValue;
        }
    }
    
    @Override
    public Map<String, String> getSecrets(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            try {
                result.put(key, getSecret(key));
            } catch (StrategizException e) {
                log.debug("Secret not found: {}", key);
            }
        }
        return result;
    }
    
    @Override
    public boolean hasSecret(String key) {
        try {
            getSecret(key);
            return true;
        } catch (StrategizException e) {
            return false;
        }
    }
    
    @Override
    public boolean storeSecret(String key, String value) {
        try {
            String vaultPath = buildVaultPath(key);
            String secretField = getSecretField(key);
            
            Map<String, Object> data = new HashMap<>();
            data.put(secretField, value);
            
            vaultTemplate.write(vaultPath, data);
            cacheSecretIfEnabled(key, value);
            return true;
        } catch (Exception e) {
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, e.getMessage());
        }
    }
    
    @Override
    public boolean removeSecret(String key) {
        try {
            String vaultPath = buildVaultPath(key);
            vaultTemplate.delete(vaultPath);
            secretCache.remove(key);
            return true;
        } catch (Exception e) {
            log.error("Failed to remove secret: {}", key, e);
            return false;
        }
    }
    
    /**
     * Convert a property key to a Vault path
     */
    private String buildVaultPath(String key) {
        // Example: Convert "auth.facebook.client-id" to "secret/strategiz/auth/facebook"
        String[] parts = key.split("\\.");
        
        if (parts.length <= 1) {
            return properties.getSecretsPath() + "/" + key;
        }
        
        // Skip the last part which will be the field name
        StringBuilder path = new StringBuilder(properties.getSecretsPath());
        for (int i = 0; i < parts.length - 1; i++) {
            path.append("/").append(parts[i]);
        }
        
        return path.toString();
    }
    
    /**
     * Extract the secret field name from the key
     */
    private String getSecretField(String key) {
        // Example: Get "client-id" from "auth.facebook.client-id"
        String[] parts = key.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : key;
    }
    
    /**
     * Cache a secret if caching is enabled
     */
    private void cacheSecretIfEnabled(String key, String value) {
        if (properties.getCacheTimeoutMs() > 0) {
            secretCache.put(key, new CachedSecret(value, System.currentTimeMillis() + properties.getCacheTimeoutMs()));
        }
    }
    
    /**
     * Helper class to store cached secrets with expiry
     */
    private static class CachedSecret {
        private final String value;
        private final long expiryTime;
        
        public CachedSecret(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
