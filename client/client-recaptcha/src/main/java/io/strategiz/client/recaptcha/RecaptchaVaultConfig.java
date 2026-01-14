package io.strategiz.client.recaptcha;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Loads reCAPTCHA Enterprise configuration from Vault.
 *
 * Vault keys: recaptcha.project-id, recaptcha.site-key, recaptcha.api-key
 */
@Configuration
@ConditionalOnProperty(name = "recaptcha.enabled", havingValue = "true")
public class RecaptchaVaultConfig {

	private static final Logger logger = LoggerFactory.getLogger(RecaptchaVaultConfig.class);

	private final SecretManager secretManager;

	private final RecaptchaConfig config;

	@Autowired
	public RecaptchaVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, RecaptchaConfig config) {
		this.secretManager = secretManager;
		this.config = config;
	}

	@PostConstruct
	public void loadFromVault() {
		try {
			logger.info("Loading reCAPTCHA configuration from Vault...");

			// Load project ID
			String projectId = secretManager.readSecret("recaptcha.project-id", null);
			if (projectId != null && !projectId.isEmpty()) {
				config.setProjectId(projectId);
				logger.info("reCAPTCHA project ID loaded from Vault");
			}
			else {
				logger.warn("reCAPTCHA project ID not found in Vault");
			}

			// Load site key
			String siteKey = secretManager.readSecret("recaptcha.site-key", null);
			if (siteKey != null && !siteKey.isEmpty()) {
				config.setSiteKey(siteKey);
				logger.info("reCAPTCHA site key loaded from Vault");
			}
			else {
				logger.warn("reCAPTCHA site key not found in Vault");
			}

			// Load API key (optional - can use service account instead)
			String apiKey = secretManager.readSecret("recaptcha.api-key", null);
			if (apiKey != null && !apiKey.isEmpty()) {
				config.setApiKey(apiKey);
				logger.info("reCAPTCHA API key loaded from Vault");
			}

			if (config.isConfigured()) {
				logger.info("reCAPTCHA configuration loaded successfully from Vault");
			}
			else {
				logger.warn("reCAPTCHA not fully configured - some fields missing from Vault");
			}
		}
		catch (Exception e) {
			logger.error("Failed to load reCAPTCHA configuration from Vault: {}", e.getMessage());
		}
	}

}
