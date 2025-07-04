package io.strategiz.framework.secrets;

import java.util.Map;

/**
 * Core interface for secret management operations.
 * Provides methods to retrieve and manage secrets from a secure storage.
 */
public interface SecretManager {

    /**
     * Get a secret by key
     *
     * @param key The secret key
     * @return The secret value
     */
    String getSecret(String key);
    
    /**
     * Retrieve a secret value by its key with a default fallback value
     *
     * @param key the identifier for the secret
     * @param defaultValue value to return if the secret is not found
     * @return the secret value or the default if not found
     */
    String getSecret(String key, String defaultValue);
    
    /**
     * Retrieve multiple secrets by their keys
     *
     * @param keys array of keys to retrieve
     * @return Map of key-value pairs with the secret values
     */
    Map<String, String> getSecrets(String... keys);
    
    /**
     * Check if a secret exists
     *
     * @param key the identifier for the secret
     * @return true if the secret exists, false otherwise
     */
    boolean hasSecret(String key);
    
    /**
     * Store a secret (if the implementation supports write operations)
     *
     * @param key the identifier for the secret
     * @param value the secret value to store
     * @return true if stored successfully, false otherwise
     * @throws UnsupportedOperationException if write operations are not supported
     */
    boolean storeSecret(String key, String value);
    
    /**
     * Remove a secret (if the implementation supports delete operations)
     *
     * @param key the identifier for the secret
     * @return true if removed successfully, false otherwise
     * @throws UnsupportedOperationException if delete operations are not supported
     */
    boolean removeSecret(String key);
}
