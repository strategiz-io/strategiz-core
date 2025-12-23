package io.strategiz.client.openai.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads OpenAI credentials from Vault and directly injects them
 * into OpenAIConfig. This ensures the API key is available regardless of bean
 * initialization order.
 *
 * Vault keys:
 * - openai.api-key: OpenAI API key (required)
 * - openai.api-url: OpenAI API base URL (optional)
 * - openai.default-model: Default model to use (optional)
 * - openai.temperature: Default temperature (optional)
 * - openai.max-tokens: Default max tokens (optional)
 */
@Configuration
public class OpenAIVaultConfig {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIVaultConfig.class);

	private final SecretManager secretManager;

	private final OpenAIConfig openAIConfig;

	@Autowired
	public OpenAIVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			OpenAIConfig openAIConfig) {
		this.secretManager = secretManager;
		this.openAIConfig = openAIConfig;
	}

	@PostConstruct
	public void loadFromVault() {
		try {
			logger.info("Loading OpenAI configuration from Vault...");

			// API Key (required)
			String apiKey = secretManager.readSecret("openai.api-key");
			if (apiKey != null && !apiKey.isEmpty()) {
				openAIConfig.setApiKey(apiKey);
				logger.info("Loaded and set OpenAI API key from Vault");
			}
			else {
				logger.warn("OpenAI API key not found in Vault at path: openai.api-key");
			}

			// API URL (optional)
			String apiUrl = secretManager.readSecret("openai.api-url");
			if (apiUrl != null && !apiUrl.isEmpty()) {
				openAIConfig.setApiUrl(apiUrl);
				logger.debug("OpenAI API URL set to: {}", apiUrl);
			}

			// Default model (optional)
			String model = secretManager.readSecret("openai.default-model");
			if (model != null && !model.isEmpty()) {
				openAIConfig.setModel(model);
				logger.debug("OpenAI default model set to: {}", model);
			}

			// Temperature (optional)
			String temperature = secretManager.readSecret("openai.temperature");
			if (temperature != null && !temperature.isEmpty()) {
				try {
					openAIConfig.setTemperature(Double.parseDouble(temperature));
					logger.debug("OpenAI temperature set to: {}", temperature);
				}
				catch (NumberFormatException e) {
					logger.warn("Invalid temperature value in Vault: {}", temperature);
				}
			}

			// Max tokens (optional)
			String maxTokens = secretManager.readSecret("openai.max-tokens");
			if (maxTokens != null && !maxTokens.isEmpty()) {
				try {
					openAIConfig.setMaxTokens(Integer.parseInt(maxTokens));
					logger.debug("OpenAI max tokens set to: {}", maxTokens);
				}
				catch (NumberFormatException e) {
					logger.warn("Invalid max-tokens value in Vault: {}", maxTokens);
				}
			}

			logger.info("OpenAI configuration loaded successfully from Vault");
		}
		catch (Exception e) {
			logger.error("Failed to load OpenAI configuration from Vault: {}", e.getMessage(), e);
			// Don't throw - allow app to start with property file defaults
		}
	}

}
