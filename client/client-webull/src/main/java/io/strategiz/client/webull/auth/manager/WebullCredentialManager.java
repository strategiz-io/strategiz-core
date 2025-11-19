package io.strategiz.client.webull.auth.manager;

import io.strategiz.client.webull.auth.model.WebullApiCredentials;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for Webull API credentials in Vault
 */
@Service
public class WebullCredentialManager {

    private static final Logger log = LoggerFactory.getLogger(WebullCredentialManager.class);

    private static final String SECRET_PATH_PREFIX = "secret/strategiz/users/";
    private static final String PROVIDER_PATH = "/providers/webull";

    private final SecretManager secretManager;

    public WebullCredentialManager(@Qualifier("vaultSecretService") SecretManager secretManager) {
        this.secretManager = secretManager;
    }

    /**
     * Store Webull API credentials in Vault
     *
     * @param credentials The credentials to store
     * @return true if stored successfully
     */
    public boolean storeCredentials(WebullApiCredentials credentials) {
        if (credentials == null || credentials.getUserId() == null) {
            log.error("Cannot store credentials: missing userId");
            return false;
        }

        try {
            String secretPath = buildSecretPath(credentials.getUserId());

            Map<String, Object> secretData = new HashMap<>();
            secretData.put("appKey", credentials.getAppKey());
            secretData.put("appSecret", credentials.getAppSecret());
            if (credentials.getAccountId() != null) {
                secretData.put("accountId", credentials.getAccountId());
            }
            secretData.put("provider", "webull");
            secretData.put("createdAt", Instant.now().toString());

            secretManager.createSecret(secretPath, secretData);

            log.info("Stored Webull credentials for user: {}", credentials.getUserId());
            return true;

        } catch (Exception e) {
            log.error("Failed to store Webull credentials for user: {}", credentials.getUserId(), e);
            return false;
        }
    }

    /**
     * Store credentials with separate parameters
     *
     * @param userId User ID
     * @param appKey App key
     * @param appSecret App secret
     * @param accountId Optional account ID
     * @return true if stored successfully
     */
    public boolean storeCredentials(String userId, String appKey, String appSecret, String accountId) {
        WebullApiCredentials credentials = new WebullApiCredentials(appKey, appSecret, accountId, userId);
        return storeCredentials(credentials);
    }

    /**
     * Retrieve Webull API credentials from Vault
     *
     * @param userId User ID
     * @return The credentials, or null if not found
     */
    public WebullApiCredentials getCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

            if (secretData == null || secretData.isEmpty()) {
                log.debug("No Webull credentials found for user: {}", userId);
                return null;
            }

            String appKey = (String) secretData.get("appKey");
            String appSecret = (String) secretData.get("appSecret");
            String accountId = (String) secretData.get("accountId");

            return new WebullApiCredentials(appKey, appSecret, accountId, userId);

        } catch (Exception e) {
            log.error("Failed to retrieve Webull credentials for user: {}", userId, e);
            return null;
        }
    }

    /**
     * Get credentials as a map (for compatibility)
     *
     * @param userId User ID
     * @return Map with appKey, appSecret, and accountId
     */
    public Map<String, String> getCredentialsAsMap(String userId) {
        WebullApiCredentials credentials = getCredentials(userId);
        if (credentials == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        result.put("appKey", credentials.getAppKey());
        result.put("appSecret", credentials.getAppSecret());
        if (credentials.getAccountId() != null) {
            result.put("accountId", credentials.getAccountId());
        }
        return result;
    }

    /**
     * Delete Webull API credentials from Vault
     *
     * @param userId User ID
     * @return true if deleted successfully
     */
    public boolean deleteCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            secretManager.deleteSecret(secretPath);

            log.info("Deleted Webull credentials for user: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete Webull credentials for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Check if credentials exist for a user
     *
     * @param userId User ID
     * @return true if credentials exist
     */
    public boolean hasCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);
            return secretData != null && !secretData.isEmpty();

        } catch (Exception e) {
            log.debug("Error checking credentials for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Update account ID for existing credentials
     *
     * @param userId User ID
     * @param accountId New account ID
     * @return true if updated successfully
     */
    public boolean updateAccountId(String userId, String accountId) {
        try {
            WebullApiCredentials existingCredentials = getCredentials(userId);
            if (existingCredentials == null) {
                log.error("Cannot update account ID: no credentials found for user: {}", userId);
                return false;
            }

            // Update the account ID and store credentials again
            existingCredentials.setAccountId(accountId);
            return storeCredentials(existingCredentials);

        } catch (Exception e) {
            log.error("Failed to update account ID for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Build the Vault secret path for a user
     *
     * @param userId User ID
     * @return The secret path
     */
    private String buildSecretPath(String userId) {
        return SECRET_PATH_PREFIX + userId + PROVIDER_PATH;
    }
}
