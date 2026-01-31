package io.strategiz.client.sendgrid;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Loads SendGrid configuration from Vault.
 *
 * Vault keys: sendgrid.api-key, sendgrid.from-email, sendgrid.from-name Optional:
 * sendgrid.alert-template-id, sendgrid.signal-template-id
 */
@Configuration
@ConditionalOnProperty(name = "sendgrid.enabled", havingValue = "true")
public class SendGridVaultConfig {

	private static final Logger logger = LoggerFactory.getLogger(SendGridVaultConfig.class);

	private final SecretManager secretManager;

	private final SendGridConfig config;

	@Autowired
	public SendGridVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, SendGridConfig config) {
		this.secretManager = secretManager;
		this.config = config;
	}

	@PostConstruct
	public void loadFromVault() {
		try {
			logger.info("Loading SendGrid configuration from Vault...");

			// Load API key
			String apiKey = secretManager.readSecret("sendgrid.api-key", null);
			if (apiKey != null && !apiKey.isEmpty()) {
				config.setApiKey(apiKey);
				logger.info("SendGrid API key loaded from Vault");
			}
			else {
				logger.warn("SendGrid API key not found in Vault");
			}

			// Load from email
			String fromEmail = secretManager.readSecret("sendgrid.from-email", null);
			if (fromEmail != null && !fromEmail.isEmpty()) {
				config.setFromEmail(fromEmail);
				logger.info("SendGrid from email loaded from Vault");
			}

			// Load from name
			String fromName = secretManager.readSecret("sendgrid.from-name", null);
			if (fromName != null && !fromName.isEmpty()) {
				config.setFromName(fromName);
			}

			// Load optional template IDs
			String alertTemplateId = secretManager.readSecret("sendgrid.alert-template-id", null);
			if (alertTemplateId != null && !alertTemplateId.isEmpty()) {
				config.setAlertTemplateId(alertTemplateId);
			}

			String signalTemplateId = secretManager.readSecret("sendgrid.signal-template-id", null);
			if (signalTemplateId != null && !signalTemplateId.isEmpty()) {
				config.setSignalTemplateId(signalTemplateId);
			}

			if (config.isConfigured()) {
				logger.info("SendGrid configuration loaded successfully from Vault");
			}
			else {
				logger.warn("SendGrid not fully configured - some fields missing from Vault");
			}
		}
		catch (Exception e) {
			logger.error("Failed to load SendGrid configuration from Vault: {}", e.getMessage());
		}
	}

}
