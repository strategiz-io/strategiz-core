package io.strategiz.service.auth.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Loads mail configuration from Vault for sending OTP emails.
 *
 * Vault keys: mail.host, mail.port, mail.username, mail.password
 */
@Configuration
@ConditionalOnBean(MailProperties.class)
public class MailVaultConfig {

	private static final Logger logger = LoggerFactory.getLogger(MailVaultConfig.class);

	private final SecretManager secretManager;

	private final MailProperties mailProperties;

	@Autowired
	public MailVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager,
			MailProperties mailProperties) {
		this.secretManager = secretManager;
		this.mailProperties = mailProperties;
	}

	@PostConstruct
	public void loadFromVault() {
		try {
			logger.info("Loading mail configuration from Vault...");

			// Load host
			String host = secretManager.readSecret("mail.host", null);
			if (host != null && !host.isEmpty()) {
				mailProperties.setHost(host);
				logger.info("Mail host loaded from Vault: {}", host);
			}

			// Load port
			String portStr = secretManager.readSecret("mail.port", null);
			if (portStr != null && !portStr.isEmpty()) {
				mailProperties.setPort(Integer.parseInt(portStr));
				logger.info("Mail port loaded from Vault: {}", portStr);
			}

			// Load username
			String username = secretManager.readSecret("mail.username", null);
			if (username != null && !username.isEmpty()) {
				mailProperties.setUsername(username);
				logger.info("Mail username loaded from Vault: {}", username);
			}

			// Load password
			String password = secretManager.readSecret("mail.password", null);
			if (password != null && !password.isEmpty()) {
				mailProperties.setPassword(password);
				logger.info("Mail password loaded from Vault");
			}

			// Set SMTP properties for Office 365
			mailProperties.getProperties().put("mail.smtp.auth", "true");
			mailProperties.getProperties().put("mail.smtp.starttls.enable", "true");

			if (mailProperties.getHost() != null && mailProperties.getUsername() != null) {
				logger.info("Mail configuration loaded successfully from Vault");
			}
			else {
				logger.warn("Mail not fully configured - some fields missing from Vault");
			}
		}
		catch (Exception e) {
			logger.error("Failed to load mail configuration from Vault: {}", e.getMessage());
		}
	}

}
