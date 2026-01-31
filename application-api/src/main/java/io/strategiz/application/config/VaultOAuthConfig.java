package io.strategiz.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultResponse;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;

/**
 * Early-loading configuration that fetches OAuth credentials from Vault and sets them as
 * system properties before other beans are created.
 */
@Configuration
@Order(1) // Load this configuration first
public class VaultOAuthConfig {

	private static final Logger log = LoggerFactory.getLogger(VaultOAuthConfig.class);

	@Value("${spring.cloud.vault.uri:http://localhost:8200}")
	private String vaultUri;

	@Value("${spring.cloud.vault.token:${VAULT_TOKEN:root}}")
	private String vaultToken;

	@Value("${strategiz.vault.enabled:true}")
	private boolean vaultEnabled;

	@PostConstruct
	public void loadOAuthCredentials() {
		if (!vaultEnabled) {
			log.info("Vault integration disabled, skipping OAuth credential loading");
			return;
		}

		try {
			log.info("Loading OAuth credentials from Vault at: {}", vaultUri);

			// Create Vault client
			VaultEndpoint endpoint = VaultEndpoint.from(new URI(vaultUri));
			VaultTemplate vaultTemplate = new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));

			// Load provider OAuth credentials
			loadProviderCredentials(vaultTemplate);

			// Load authentication OAuth credentials
			loadAuthCredentials(vaultTemplate);

			log.info("Successfully loaded OAuth credentials from Vault");

		}
		catch (Exception e) {
			log.error("Failed to load OAuth credentials from Vault. OAuth features may not work properly.", e);
		}
	}

	private void loadProviderCredentials(VaultTemplate vaultTemplate) {
		// Load Coinbase OAuth
		loadProvider(vaultTemplate, "coinbase");

		// Load Kraken OAuth
		loadProvider(vaultTemplate, "kraken");

		// Load Binance OAuth
		loadProvider(vaultTemplate, "binance");

		// Load Alpaca OAuth
		loadProvider(vaultTemplate, "alpaca");

		// Load Charles Schwab OAuth
		loadProvider(vaultTemplate, "schwab");
	}

	private void loadAuthCredentials(VaultTemplate vaultTemplate) {
		// Load Google OAuth (for authentication)
		try {
			VaultResponse response = vaultTemplate.read("secret/data/strategiz/oauth/google");
			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					setSystemPropertyIfNotNull("oauth.providers.google.client-id", data.get("client-id"));
					setSystemPropertyIfNotNull("oauth.providers.google.client-secret", data.get("client-secret"));
					log.debug("Loaded Google OAuth credentials from Vault");
				}
			}
		}
		catch (Exception e) {
			log.debug("Google OAuth credentials not found in Vault: {}", e.getMessage());
		}

		// Load Facebook OAuth (for authentication)
		try {
			VaultResponse response = vaultTemplate.read("secret/data/strategiz/oauth/facebook");
			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					setSystemPropertyIfNotNull("oauth.providers.facebook.client-id", data.get("client-id"));
					setSystemPropertyIfNotNull("oauth.providers.facebook.client-secret", data.get("client-secret"));
					log.debug("Loaded Facebook OAuth credentials from Vault");
				}
			}
		}
		catch (Exception e) {
			log.debug("Facebook OAuth credentials not found in Vault: {}", e.getMessage());
		}
	}

	private void loadProvider(VaultTemplate vaultTemplate, String provider) {
		try {
			String path = String.format("secret/data/strategiz/oauth/%s", provider);
			VaultResponse response = vaultTemplate.read(path);

			if (response != null && response.getData() != null) {
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					String clientId = (String) data.get("client-id");
					String clientSecret = (String) data.get("client-secret");
					String redirectUriLocal = (String) data.get("redirect-uri-local");
					String redirectUriProd = (String) data.get("redirect-uri-prod");

					if (clientId != null) {
						String propKey = String.format("oauth.providers.%s.client-id", provider);
						System.setProperty(propKey, clientId);
						log.info("Set system property {} from Vault", propKey);
					}

					if (clientSecret != null) {
						String propKey = String.format("oauth.providers.%s.client-secret", provider);
						System.setProperty(propKey, clientSecret);
						log.info("Set system property {} from Vault", propKey);
					}

					// Use redirect-uri-local for local development, redirect-uri-prod for
					// production
					// Check if we're in production mode by looking at active profiles
					// Check both system property and environment variable (Spring uses
					// env var)
					String activeProfiles = System.getProperty("spring.profiles.active",
							System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
					String redirectUri = activeProfiles.contains("prod") ? redirectUriProd : redirectUriLocal;

					if (redirectUri != null) {
						String propKey = String.format("oauth.providers.%s.redirect-uri", provider);
						System.setProperty(propKey, redirectUri);
						log.info("Set system property {} from Vault (using {})", propKey,
								activeProfiles.contains("prod") ? "prod redirect-uri" : "local redirect-uri");
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("{} OAuth credentials not found in Vault: {}", provider, e.getMessage());
		}
	}

	private void setSystemPropertyIfNotNull(String key, Object value) {
		if (value != null) {
			System.setProperty(key, value.toString());
			log.debug("Set system property: {}", key);
		}
	}

}