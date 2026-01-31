package io.strategiz.client.binanceus.auth.manager;

import io.strategiz.client.binanceus.auth.model.BinanceUSApiCredentials;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for Binance US API credentials in Vault
 */
@Service
public class BinanceUSCredentialManager {

	private static final Logger log = LoggerFactory.getLogger(BinanceUSCredentialManager.class);

	private static final String SECRET_PATH_PREFIX = "secret/strategiz/users/";

	private static final String PROVIDER_PATH = "/providers/binanceus";

	private final SecretManager secretManager;

	public BinanceUSCredentialManager(@Qualifier("vaultSecretService") SecretManager secretManager) {
		this.secretManager = secretManager;
	}

	/**
	 * Store Binance US API credentials in Vault
	 * @param credentials The credentials to store
	 * @return true if stored successfully
	 */
	public boolean storeCredentials(BinanceUSApiCredentials credentials) {
		if (credentials == null || credentials.getUserId() == null) {
			log.error("Cannot store credentials: missing userId");
			return false;
		}

		try {
			String secretPath = buildSecretPath(credentials.getUserId());

			Map<String, Object> secretData = new HashMap<>();
			secretData.put("apiKey", credentials.getApiKey());
			secretData.put("apiSecret", credentials.getApiSecret());
			secretData.put("provider", "binanceus");
			secretData.put("createdAt", Instant.now().toString());

			secretManager.createSecret(secretPath, secretData);

			log.info("Stored Binance US credentials for user: {}", credentials.getUserId());
			return true;

		}
		catch (Exception e) {
			log.error("Failed to store Binance US credentials for user: {}", credentials.getUserId(), e);
			return false;
		}
	}

	/**
	 * Store credentials with separate parameters
	 * @param userId User ID
	 * @param apiKey API key
	 * @param apiSecret API secret
	 * @return true if stored successfully
	 */
	public boolean storeCredentials(String userId, String apiKey, String apiSecret) {
		BinanceUSApiCredentials credentials = new BinanceUSApiCredentials(apiKey, apiSecret, userId);
		return storeCredentials(credentials);
	}

	/**
	 * Retrieve Binance US API credentials from Vault
	 * @param userId User ID
	 * @return The credentials, or null if not found
	 */
	public BinanceUSApiCredentials getCredentials(String userId) {
		try {
			String secretPath = buildSecretPath(userId);
			Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

			if (secretData == null || secretData.isEmpty()) {
				log.debug("No Binance US credentials found for user: {}", userId);
				return null;
			}

			String apiKey = (String) secretData.get("apiKey");
			String apiSecret = (String) secretData.get("apiSecret");

			return new BinanceUSApiCredentials(apiKey, apiSecret, userId);

		}
		catch (Exception e) {
			log.error("Failed to retrieve Binance US credentials for user: {}", userId, e);
			return null;
		}
	}

	/**
	 * Get credentials as a map (for compatibility)
	 * @param userId User ID
	 * @return Map with apiKey and apiSecret
	 */
	public Map<String, String> getCredentialsAsMap(String userId) {
		BinanceUSApiCredentials credentials = getCredentials(userId);
		if (credentials == null) {
			return null;
		}

		Map<String, String> result = new HashMap<>();
		result.put("apiKey", credentials.getApiKey());
		result.put("apiSecret", credentials.getApiSecret());
		return result;
	}

	/**
	 * Delete Binance US API credentials from Vault
	 * @param userId User ID
	 * @return true if deleted successfully
	 */
	public boolean deleteCredentials(String userId) {
		try {
			String secretPath = buildSecretPath(userId);
			secretManager.deleteSecret(secretPath);

			log.info("Deleted Binance US credentials for user: {}", userId);
			return true;

		}
		catch (Exception e) {
			log.error("Failed to delete Binance US credentials for user: {}", userId, e);
			return false;
		}
	}

	/**
	 * Check if credentials exist for a user
	 * @param userId User ID
	 * @return true if credentials exist
	 */
	public boolean hasCredentials(String userId) {
		try {
			String secretPath = buildSecretPath(userId);
			Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);
			return secretData != null && !secretData.isEmpty();

		}
		catch (Exception e) {
			log.debug("Error checking credentials for user: {}", userId, e);
			return false;
		}
	}

	/**
	 * Build the Vault secret path for a user
	 * @param userId User ID
	 * @return The secret path
	 */
	private String buildSecretPath(String userId) {
		return SECRET_PATH_PREFIX + userId + PROVIDER_PATH;
	}

}