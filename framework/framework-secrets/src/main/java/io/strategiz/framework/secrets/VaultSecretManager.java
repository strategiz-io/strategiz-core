package io.strategiz.framework.secrets;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashiCorp Vault implementation of the SecretManager interface.
 * Uses direct HTTP calls to Vault API instead of Spring Cloud Vault.
 */
@Service
public class VaultSecretManager implements SecretManager {
    
    private static final Logger log = LoggerFactory.getLogger(VaultSecretManager.class);
    
    private final RestTemplate restTemplate;
    private final VaultProperties properties;
    private final Environment environment;
    
    // Simple cache for secrets
    private final Map<String, CachedSecret> secretCache = new ConcurrentHashMap<>();
    
    @Autowired
    public VaultSecretManager(VaultProperties properties, Environment environment) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
        this.environment = environment;
    }
    
    @Override
    public String readSecret(String key) {
        // Check cache first
        CachedSecret cached = secretCache.get(key);
        if (cached != null && !cached.isExpired()) {
            log.debug("Retrieved secret from cache: {}", key);
            return cached.getValue();
        }
        
        try {
            String vaultPath = buildVaultPath(key);
            String secretField = getSecretField(key);
            
            log.debug("Fetching secret from Vault: path={}, field={}", vaultPath, secretField);
            Map<String, Object> data = readFromVault(vaultPath);
            
            if (data == null || data.isEmpty()) {
                throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, "Secret not found: " + key);
            }
            
            Object value = data.get(secretField);
            if (value == null) {
                throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, "Secret field not found: " + key);
            }
            
            String secretValue = value.toString();
            cacheSecretIfEnabled(key, secretValue);
            
            return secretValue;
            
        } catch (Exception e) {
            log.error("Failed to retrieve secret: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to retrieve secret: " + e.getMessage());
        }
    }
    
    @Override
    public String readSecret(String key, String defaultValue) {
        try {
            return readSecret(key);
        } catch (StrategizException e) {
            log.debug("Secret not found, using default value: {}", key);
            return defaultValue;
        }
    }
    
    @Override
    public Map<String, String> readSecrets(String... keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            try {
                result.put(key, readSecret(key));
            } catch (StrategizException e) {
                log.debug("Secret not found: {}", key);
            }
        }
        return result;
    }
    
    @Override
    public boolean secretExists(String key) {
        try {
            readSecret(key);
            return true;
        } catch (StrategizException e) {
            return false;
        }
    }
    
    @Override
    public void createSecret(String key, String value) {
        try {
            String vaultPath = buildVaultPath(key);
            String secretField = getSecretField(key);
            
            // Read existing data first to preserve other fields
            Map<String, Object> data = new HashMap<>();
            try {
                Map<String, Object> existing = readFromVault(vaultPath);
                if (existing != null) {
                    data.putAll(existing);
                }
            } catch (Exception e) {
                log.debug("No existing data at path: {}", vaultPath);
            }
            
            // Add/update the specific field
            data.put(secretField, value);
            
            writeToVault(vaultPath, data);
            cacheSecretIfEnabled(key, value);
            
            log.info("Successfully created secret: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to create secret: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to create secret: " + e.getMessage());
        }
    }
    
    @Override
    public void createSecret(String key, Map<String, Object> data) {
        try {
            String vaultPath = buildVaultPath(key);
            writeToVault(vaultPath, data);
            log.info("Successfully created secret with complex data: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to create secret with complex data: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to create secret: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> readSecretAsMap(String key) {
        try {
            String vaultPath = buildVaultPath(key);
            Map<String, Object> data = readFromVault(vaultPath);
            
            if (data == null || data.isEmpty()) {
                return null;
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("Failed to retrieve secret as map: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to retrieve secret: " + e.getMessage());
        }
    }
    
    @Override
    public void updateSecret(String key, String value) {
        try {
            String vaultPath = buildVaultPath(key);
            String secretField = getSecretField(key);
            
            // Read existing data first to preserve other fields
            Map<String, Object> data = new HashMap<>();
            try {
                Map<String, Object> existing = readFromVault(vaultPath);
                if (existing != null) {
                    data.putAll(existing);
                }
            } catch (Exception e) {
                log.debug("No existing data at path: {}", vaultPath);
            }
            
            // Update the specific field
            data.put(secretField, value);
            
            writeToVault(vaultPath, data);
            cacheSecretIfEnabled(key, value);
            
            log.info("Successfully updated secret: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to update secret: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to update secret: " + e.getMessage());
        }
    }
    
    @Override
    public void updateSecret(String key, Map<String, Object> data) {
        try {
            String vaultPath = buildVaultPath(key);
            writeToVault(vaultPath, data);
            log.info("Successfully updated secret with complex data: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to update secret with complex data: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to update secret: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteSecret(String key) {
        try {
            String vaultPath = buildVaultPath(key);
            String secretField = getSecretField(key);
            
            // Read existing data first
            Map<String, Object> existing = readFromVault(vaultPath);
            if (existing == null || existing.isEmpty()) {
                log.debug("Secret not found for deletion: {}", key);
                return;
            }
            
            Map<String, Object> data = new HashMap<>(existing);
            data.remove(secretField);
            
            if (data.isEmpty()) {
                // If no fields left, delete the entire path
                deleteFromVault(vaultPath);
            } else {
                // Otherwise, write back the remaining fields
                writeToVault(vaultPath, data);
            }
            
            secretCache.remove(key);
            log.info("Successfully deleted secret: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to delete secret: {}", key, e);
            throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, "Failed to delete secret: " + e.getMessage());
        }
    }
    
    /**
     * Read data from Vault using HTTP API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readFromVault(String path) {
        try {
            String url = properties.getAddress() + "/v1/" + path;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null && data.containsKey("data")) {
                    // KV v2 format
                    return (Map<String, Object>) data.get("data");
                } else {
                    // KV v1 format or direct data
                    return data;
                }
            }
            
            return null;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
    
    /**
     * Write data to Vault using HTTP API
     */
    private void writeToVault(String path, Map<String, Object> data) {
        String url = properties.getAddress() + "/v1/" + path;
        HttpHeaders headers = createHeaders();
        
        // For KV v2, wrap data in "data" field
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    }
    
    /**
     * Delete data from Vault using HTTP API
     */
    private void deleteFromVault(String path) {
        String url = properties.getAddress() + "/v1/" + path;
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
    
    /**
     * Create HTTP headers with Vault token
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Get token from environment or Spring Cloud Vault configuration
        String token = environment.getProperty("VAULT_TOKEN");
        if (token == null) {
            token = environment.getProperty("spring.cloud.vault.token", "root-token");
        }
        headers.set("X-Vault-Token", token);
        
        return headers;
    }
    
    /**
     * Convert a property key to a Vault path
     * Example: "auth.google.client-id" -> "secret/data/strategiz/auth/google"
     */
    private String buildVaultPath(String key) {
        String[] parts = key.split("\\.");
        
        if (parts.length <= 1) {
            return properties.getSecretsPath() + "/data/strategiz/" + key;
        }
        
        // Skip the last part which will be the field name
        StringBuilder path = new StringBuilder(properties.getSecretsPath() + "/data/strategiz");
        for (int i = 0; i < parts.length - 1; i++) {
            path.append("/").append(parts[i]);
        }
        
        return path.toString();
    }
    
    /**
     * Extract the secret field name from the key
     * Example: Get "client-id" from "auth.google.client-id"
     */
    private String getSecretField(String key) {
        String[] parts = key.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : key;
    }
    
    /**
     * Cache a secret if caching is enabled
     */
    private void cacheSecretIfEnabled(String key, String value) {
        if (properties.getCacheTimeoutMs() > 0) {
            secretCache.put(key, new CachedSecret(value, System.currentTimeMillis() + properties.getCacheTimeoutMs()));
            log.debug("Cached secret: {}", key);
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
