package io.strategiz.client.grok.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads xAI Grok credentials from Vault and injects them into
 * GrokDirectConfig. This ensures all Grok secrets are loaded from Vault at startup.
 *
 * Vault path: secret/strategiz/grok Required field: - api-key: xAI API key (xai-xxx)
 */
@Configuration
public class GrokVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(GrokVaultConfig.class);

	private final SecretManager secretManager;

	private final GrokDirectConfig grokConfig;

	@Autowired
	public GrokVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, GrokDirectConfig grokConfig) {
		this.secretManager = secretManager;
		this.grokConfig = grokConfig;
	}

	@PostConstruct
	public void loadGrokPropertiesFromVault() {
		try {
			log.info("Loading Grok configuration from Vault...");

			// Load Grok API key
			String apiKey = secretManager.readSecret("grok.api-key", null);
			if (apiKey != null && !apiKey.isEmpty()) {
				grokConfig.setApiKey(apiKey);
				log.info("Loaded Grok API key from Vault");
			}
			else {
				log.warn("Grok API key not found in Vault - xAI models will be unavailable");
			}

		}
		catch (Exception e) {
			log.error("Failed to load Grok configuration from Vault", e);
		}
	}

}
