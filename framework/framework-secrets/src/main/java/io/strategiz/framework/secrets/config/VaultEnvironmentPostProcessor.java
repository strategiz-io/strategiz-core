package io.strategiz.framework.secrets.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * Environment post-processor that loads OAuth credentials from Vault early in the Spring
 * Boot startup process, before beans are created. This ensures @Value annotations work
 * correctly.
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(VaultEnvironmentPostProcessor.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		// Check if Vault is enabled
		String vaultEnabled = environment.getProperty("strategiz.vault.enabled", "true");
		if (!"true".equals(vaultEnabled)) {
			log.info("Vault integration disabled, skipping OAuth credential loading");
			return;
		}

		try {
			String vaultUri = environment.getProperty("spring.cloud.vault.uri", "http://localhost:8200");
			String vaultToken = environment.getProperty("spring.cloud.vault.token",
					environment.getProperty("VAULT_TOKEN", "root"));

			log.info("Loading OAuth credentials from Vault at: {}", vaultUri);

			// Create Vault client
			VaultEndpoint endpoint = VaultEndpoint.from(new URI(vaultUri));
			VaultTemplate vaultTemplate = new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));

			Map<String, Object> oauthProperties = new HashMap<>();

			// Load provider OAuth credentials (for connecting external accounts)
			loadProviderOAuthCredentials(vaultTemplate, oauthProperties);

			// Load authentication OAuth credentials (for user login)
			loadAuthOAuthCredentials(vaultTemplate, oauthProperties);

			if (!oauthProperties.isEmpty()) {
				// Add properties with highest priority
				MapPropertySource propertySource = new MapPropertySource("vault-oauth", oauthProperties);
				environment.getPropertySources().addFirst(propertySource);
				log.info("Successfully loaded {} OAuth properties from Vault", oauthProperties.size());
			}

		}
		catch (Exception e) {
			log.error("Failed to load OAuth credentials from Vault. Application will start but OAuth features"
					+ " may not work.", e);
		}
	}

	@SuppressWarnings("AbbreviationAsWordInName")
	private void loadProviderOAuthCredentials(VaultTemplate vaultTemplate, Map<String, Object> properties) {
		// Coinbase OAuth
		loadOAuthProvider(vaultTemplate, "coinbase", properties);

		// Kraken OAuth
		loadOAuthProvider(vaultTemplate, "kraken", properties);

		// Binance OAuth
		loadOAuthProvider(vaultTemplate, "binance", properties);

		// Alpaca OAuth
		loadOAuthProvider(vaultTemplate, "alpaca", properties);
	}

	@SuppressWarnings("AbbreviationAsWordInName")
	private void loadAuthOAuthCredentials(VaultTemplate vaultTemplate, Map<String, Object> properties) {
		// Google OAuth (for authentication)
		try {
			VaultResponse response = vaultTemplate.read("secret/data/strategiz/oauth/google");
			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					properties.put("oauth.providers.google.client-id", data.get("client-id"));
					properties.put("oauth.providers.google.client-secret", data.get("client-secret"));
					log.debug("Loaded Google OAuth credentials from Vault");
				}
			}
		}
		catch (Exception e) {
			log.debug("Google OAuth credentials not found in Vault: {}", e.getMessage());
		}

		// Facebook OAuth (for authentication)
		try {
			VaultResponse response = vaultTemplate.read("secret/data/strategiz/oauth/facebook");
			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					properties.put("oauth.providers.facebook.client-id", data.get("client-id"));
					properties.put("oauth.providers.facebook.client-secret", data.get("client-secret"));
					log.debug("Loaded Facebook OAuth credentials from Vault");
				}
			}
		}
		catch (Exception e) {
			log.debug("Facebook OAuth credentials not found in Vault: {}", e.getMessage());
		}
	}

	@SuppressWarnings("AbbreviationAsWordInName")
	private void loadOAuthProvider(VaultTemplate vaultTemplate, String provider, Map<String, Object> properties) {
		try {
			String path = String.format("secret/data/strategiz/oauth/%s", provider);
			VaultResponse response = vaultTemplate.read(path);

			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					String clientId = (String) data.get("client-id");
					String clientSecret = (String) data.get("client-secret");

					if (clientId != null) {
						properties.put(String.format("oauth.providers.%s.client-id", provider), clientId);
					}
					if (clientSecret != null) {
						properties.put(String.format("oauth.providers.%s.client-secret", provider), clientSecret);
					}

					log.debug("Loaded {} OAuth credentials from Vault", provider);
				}
			}
		}
		catch (Exception e) {
			log.debug("{} OAuth credentials not found in Vault: {}", provider, e.getMessage());
		}
	}

}
