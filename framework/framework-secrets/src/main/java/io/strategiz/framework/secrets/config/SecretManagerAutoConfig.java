package io.strategiz.framework.secrets.config;

import io.strategiz.framework.secrets.client.VaultClient;
import io.strategiz.framework.secrets.controller.SecretCache;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.framework.secrets.service.PropertySecretService;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for the Secret Manager framework. Automatically creates and
 * configures a SecretManager bean based on application properties.
 */
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class SecretManagerAutoConfig {

	private static final Logger log = LoggerFactory.getLogger(SecretManagerAutoConfig.class);

	private final VaultProperties vaultProperties;

	private final Environment environment;

	/**
	 * Creates a SecretManagerAutoConfig with the given dependencies.
	 * @param vaultProperties Vault configuration properties
	 * @param environment Spring environment for accessing configuration
	 */
	public SecretManagerAutoConfig(VaultProperties vaultProperties, Environment environment) {
		this.vaultProperties = vaultProperties;
		this.environment = environment;
		log.info("Initializing SecretManager auto-configuration");
	}

	/**
	 * Creates a Vault-based SecretManager bean when Vault is enabled.
	 * @param vaultClient Client for Vault HTTP operations
	 * @param secretCache Cache for storing secrets
	 * @return SecretManager implementation using Vault
	 */
	@Bean("vaultSecretService")
	@Primary
	@ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "true", matchIfMissing = true)
	public SecretManager vaultSecretService(VaultClient vaultClient, SecretCache secretCache) {
		log.info("Creating Vault SecretManager bean with HTTP API");
		return new VaultSecretService(vaultClient, secretCache, vaultProperties);
	}

	/**
	 * Creates a property-based SecretManager fallback when Vault is disabled.
	 * @return SecretManager implementation using Spring properties
	 */
	@Bean
	@ConditionalOnMissingBean(SecretManager.class)
	@ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "false")
	public SecretManager propertySecretManager() {
		log.info("Creating Property-based SecretManager fallback bean (Vault disabled)");
		return new PropertySecretService(environment);
	}

}
