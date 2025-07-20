package io.strategiz.framework.secrets;

import java.util.Map;

/**
 * Core interface for secret management operations.
 * Provides methods to retrieve and manage secrets from a secure storage.
 */
public interface SecretManager {

    /**
     * Create a secret
     *
     * @param key The secret key
     * @param value The secret value as a string
     * @throws UnsupportedOperationException if write operations are not supported
     */
    void createSecret(String key, String value);
    
    /**
     * Create a secret with complex data
     *
     * @param key The secret key
     * @param data The secret data as a map
     * @throws UnsupportedOperationException if write operations are not supported
     */
    void createSecret(String key, Map<String, Object> data);
    
    /**
     * Read a secret by key
     *
     * @param key The secret key
     * @return The secret value, or null if not found
     */
    String readSecret(String key);
    
    /**
     * Read a secret with a default value
     *
     * @param key The secret key
     * @param defaultValue Value to return if the secret is not found
     * @return The secret value or the default if not found
     */
    String readSecret(String key, String defaultValue);
    
    /**
     * Read a secret as a map
     *
     * @param key The secret key
     * @return The secret data as a map, or null if not found
     */
    Map<String, Object> readSecretAsMap(String key);
    
    /**
     * Read multiple secrets by their keys
     *
     * @param keys Array of keys to retrieve
     * @return Map of key-value pairs with the secret values
     */
    Map<String, String> readSecrets(String... keys);
    
    /**
     * Update an existing secret
     *
     * @param key The secret key
     * @param value The new secret value
     * @throws UnsupportedOperationException if write operations are not supported
     */
    void updateSecret(String key, String value);
    
    /**
     * Update an existing secret with complex data
     *
     * @param key The secret key
     * @param data The new secret data as a map
     * @throws UnsupportedOperationException if write operations are not supported
     */
    void updateSecret(String key, Map<String, Object> data);
    
    /**
     * Delete a secret
     *
     * @param key The secret key
     * @throws UnsupportedOperationException if delete operations are not supported
     */
    void deleteSecret(String key);
    
    /**
     * Check if a secret exists
     *
     * @param key The secret key
     * @return true if the secret exists, false otherwise
     */
    boolean secretExists(String key);
}
