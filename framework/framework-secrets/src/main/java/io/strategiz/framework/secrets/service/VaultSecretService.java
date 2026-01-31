package io.strategiz.framework.secrets.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.client.VaultClient;
import io.strategiz.framework.secrets.config.VaultProperties;
import io.strategiz.framework.secrets.controller.SecretCache;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.framework.secrets.exception.SecretsErrors;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * HashiCorp Vault implementation of the SecretManager interface. Uses direct HTTP calls
 * to Vault API instead of Spring Cloud Vault.
 */
@Service
public class VaultSecretService implements SecretManager {

	private static final Logger log = LoggerFactory.getLogger(VaultSecretService.class);

	private final VaultClient vaultClient;

	private final SecretCache secretCache;

	private final VaultProperties properties;

	/**
	 * Creates a VaultSecretService with the given dependencies.
	 * @param vaultClient Client for Vault HTTP operations
	 * @param secretCache Cache for storing secrets
	 * @param properties Vault configuration properties
	 */
	@Autowired
	public VaultSecretService(VaultClient vaultClient, SecretCache secretCache, VaultProperties properties) {
		this.vaultClient = vaultClient;
		this.secretCache = secretCache;
		this.properties = properties;
	}

	@Override
	public String readSecret(String key) {
		log.info("=== VaultSecretService.readSecret START ===");
		log.info("Input key: {}", key);
		log.info("VaultClient class: {}", vaultClient != null ? vaultClient.getClass().getName() : "NULL");
		log.info("Properties secretsPath: {}", properties.getSecretsPath());

		// Check cache first
		if (properties.getCacheTimeoutMs() > 0) {
			var cached = secretCache.get(key);
			if (cached.isPresent()) {
				log.debug("Retrieved secret from cache: {}", key);
				return cached.get();
			}
		}

		try {
			String vaultPath = buildVaultPath(key);
			String secretField = getSecretField(key);

			log.info("Built Vault path: {}", vaultPath);
			log.info("Extracted secret field: {}", secretField);
			log.info("Full URL will be: {}/v1/{}", properties.getAddress(), vaultPath);

			log.info("Calling vaultClient.read()...");
			Map<String, Object> data = vaultClient.read(vaultPath);
			log.info("vaultClient.read() returned: {}", data != null ? "data with " + data.size() + " keys" : "NULL");

			if (data == null || data.isEmpty()) {
				throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, "Secret not found: " + key);
			}

			Object value = data.get(secretField);
			if (value == null) {
				throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, "Secret field not found: " + key);
			}

			String secretValue = value.toString();

			// Cache if enabled
			if (properties.getCacheTimeoutMs() > 0) {
				secretCache.put(key, secretValue, Duration.ofMillis(properties.getCacheTimeoutMs()));
			}

			return secretValue;

		}
		catch (Exception e) {
			log.error("Failed to retrieve secret: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to retrieve secret: " + e.getMessage());
		}
	}

	@Override
	public String readSecret(String key, String defaultValue) {
		try {
			return readSecret(key);
		}
		catch (StrategizException e) {
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
			}
			catch (StrategizException e) {
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
		}
		catch (StrategizException e) {
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
				Map<String, Object> existing = vaultClient.read(vaultPath);
				if (existing != null) {
					data.putAll(existing);
				}
			}
			catch (Exception e) {
				log.debug("No existing data at path: {}", vaultPath);
			}

			// Add/update the specific field
			data.put(secretField, value);

			vaultClient.write(vaultPath, data);
			if (properties.getCacheTimeoutMs() > 0) {
				secretCache.put(key, value, Duration.ofMillis(properties.getCacheTimeoutMs()));
			}

			log.info("Successfully created secret: {}", key);

		}
		catch (Exception e) {
			log.error("Failed to create secret: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to create secret: " + e.getMessage());
		}
	}

	@Override
	public void createSecret(String key, Map<String, Object> data) {
		try {
			String vaultPath = buildVaultPath(key);
			vaultClient.write(vaultPath, data);
			log.info("Successfully created secret with complex data: {}", key);

		}
		catch (Exception e) {
			log.error("Failed to create secret with complex data: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to create secret: " + e.getMessage());
		}
	}

	@Override
	public Map<String, Object> readSecretAsMap(String key) {
		try {
			String vaultPath = buildVaultPath(key);
			Map<String, Object> data = vaultClient.read(vaultPath);

			if (data == null || data.isEmpty()) {
				return null;
			}

			return data;

		}
		catch (Exception e) {
			log.error("Failed to retrieve secret as map: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to retrieve secret: " + e.getMessage());
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
				Map<String, Object> existing = vaultClient.read(vaultPath);
				if (existing != null) {
					data.putAll(existing);
				}
			}
			catch (Exception e) {
				log.debug("No existing data at path: {}", vaultPath);
			}

			// Update the specific field
			data.put(secretField, value);

			vaultClient.write(vaultPath, data);
			if (properties.getCacheTimeoutMs() > 0) {
				secretCache.put(key, value, Duration.ofMillis(properties.getCacheTimeoutMs()));
			}

			log.info("Successfully updated secret: {}", key);

		}
		catch (Exception e) {
			log.error("Failed to update secret: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to update secret: " + e.getMessage());
		}
	}

	@Override
	public void updateSecret(String key, Map<String, Object> data) {
		try {
			String vaultPath = buildVaultPath(key);
			vaultClient.write(vaultPath, data);
			log.info("Successfully updated secret with complex data: {}", key);

		}
		catch (Exception e) {
			log.error("Failed to update secret with complex data: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to update secret: " + e.getMessage());
		}
	}

	@Override
	public void deleteSecret(String key) {
		try {
			String vaultPath = buildVaultPath(key);
			String secretField = getSecretField(key);

			// Read existing data first
			Map<String, Object> existing = vaultClient.read(vaultPath);
			if (existing == null || existing.isEmpty()) {
				log.debug("Secret not found for deletion: {}", key);
				return;
			}

			Map<String, Object> data = new HashMap<>(existing);
			data.remove(secretField);

			if (data.isEmpty()) {
				// If no fields left, delete the entire path
				vaultClient.delete(vaultPath);
			}
			else {
				// Otherwise, write back the remaining fields
				vaultClient.write(vaultPath, data);
			}

			secretCache.evict(key);
			log.info("Successfully deleted secret: {}", key);

		}
		catch (Exception e) {
			log.error("Failed to delete secret: {}", key, e);
			throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED,
					"Failed to delete secret: " + e.getMessage());
		}
	}

	/**
	 * Convert a property key to a Vault path Example: "auth.google.client-id" ->
	 * "secret/data/strategiz/auth/google" (KV v2 format with /data/).
	 */
	private String buildVaultPath(String key) {
		String[] parts = key.split("\\.");

		if (parts.length <= 1) {
			// KV v2: Include /data/ in path
			return properties.getSecretsPath() + "/data/strategiz/" + key;
		}

		// Skip the last part which will be the field name
		// Build path with /data/ for KV v2
		StringBuilder path = new StringBuilder(properties.getSecretsPath() + "/data/strategiz");
		for (int i = 0; i < parts.length - 1; i++) {
			path.append("/").append(parts[i]);
		}

		return path.toString();
	}

	/**
	 * Extract the secret field name from the key Example: Get "client-id" from
	 * "auth.google.client-id".
	 */
	private String getSecretField(String key) {
		String[] parts = key.split("\\.");
		return parts.length > 0 ? parts[parts.length - 1] : key;
	}

}
