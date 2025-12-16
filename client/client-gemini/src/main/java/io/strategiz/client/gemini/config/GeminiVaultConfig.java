package io.strategiz.client.gemini.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class that loads Gemini API key from Vault
 * and makes it available as a Spring property.
 */
@Configuration
public class GeminiVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(GeminiVaultConfig.class);

	private final SecretManager secretManager;

	private final ConfigurableEnvironment environment;

	@Autowired
	public GeminiVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			ConfigurableEnvironment environment) {
		this.secretManager = secretManager;
		this.environment = environment;
		loadGeminiPropertiesFromVault();
	}

	private void loadGeminiPropertiesFromVault() {
		try {
			log.info("Loading Gemini API properties from Vault...");

			Map<String, Object> vaultProperties = new HashMap<>();

			// Load Gemini API key from Vault
			// Vault path: secret/strategiz/gemini with field "api-key"
			String geminiApiKey = secretManager.readSecret("gemini.api-key");

			if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
				vaultProperties.put("gemini.api.key", geminiApiKey);
				log.info("Loaded Gemini API key from Vault");
			}
			else {
				log.warn("Gemini API key not found in Vault at path: gemini.api-key");
			}

			// Add the properties to the environment
			if (!vaultProperties.isEmpty()) {
				MapPropertySource vaultPropertySource = new MapPropertySource("vault-gemini", vaultProperties);
				environment.getPropertySources().addFirst(vaultPropertySource);
				log.info("Successfully loaded {} Gemini properties from Vault", vaultProperties.size());
			}

		}
		catch (Exception e) {
			log.error("Failed to load Gemini properties from Vault", e);
		}
	}

}
