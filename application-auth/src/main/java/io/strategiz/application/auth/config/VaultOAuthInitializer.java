package io.strategiz.application.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Application context initializer that loads OAuth credentials from Vault very early in
 * the Spring Boot startup process, before beans are created.
 */
public class VaultOAuthInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Logger log = LoggerFactory.getLogger(VaultOAuthInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		String vaultEnabled = environment.getProperty("strategiz.vault.enabled", "true");
		if (!"true".equals(vaultEnabled)) {
			log.info("Vault integration disabled, skipping OAuth credential loading");
			return;
		}

		// Get Vault URI from multiple sources
		String vaultUri = environment.getProperty("spring.cloud.vault.uri");
		if (vaultUri == null || vaultUri.isEmpty()) {
			vaultUri = environment.getProperty("strategiz.vault.address");
		}
		if (vaultUri == null || vaultUri.isEmpty()) {
			vaultUri = System.getenv("VAULT_ADDR");
		}
		if (vaultUri == null || vaultUri.isEmpty()) {
			vaultUri = "http://localhost:8200"; // Default for local development
			log.info("No Vault address configured, using default: {}", vaultUri);
		}

		// Get Vault token - require explicit configuration
		String vaultToken = null;

		// First try Spring property
		vaultToken = environment.getProperty("spring.cloud.vault.token");

		// Then try environment variable (preferred method)
		if (vaultToken == null || vaultToken.isEmpty()) {
			vaultToken = System.getenv("VAULT_TOKEN");
		}

		// Check if token is available
		if (vaultToken == null || vaultToken.isEmpty()) {
			log.error("No Vault token found. Please set VAULT_TOKEN environment variable.");
			log.error("For local development: export VAULT_TOKEN=<your-dev-token>");
			log.error("For production: Configure VAULT_TOKEN in your deployment environment");
			log.warn("OAuth credentials will not be loaded from Vault. Using fallback values from properties.");
			return;
		}

		log.info("Vault token configured (length: {})", vaultToken.length());

		try {
			log.info("Loading OAuth credentials from Vault at: {}", vaultUri);

			// Create Vault client
			VaultEndpoint endpoint = VaultEndpoint.from(new URI(vaultUri));
			VaultTemplate vaultTemplate = new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));

			Map<String, Object> oauthProperties = new HashMap<>();

			// Load provider OAuth credentials
			loadProviderCredentials(vaultTemplate, oauthProperties);

			// Load authentication OAuth credentials
			loadAuthCredentials(vaultTemplate, oauthProperties);

			if (!oauthProperties.isEmpty()) {
				// Add properties with highest priority
				MapPropertySource propertySource = new MapPropertySource("vault-oauth-early", oauthProperties);
				environment.getPropertySources().addFirst(propertySource);
				log.info("Successfully loaded {} OAuth properties from Vault", oauthProperties.size());
			}

		}
		catch (Exception e) {
			log.error("Failed to load OAuth credentials from Vault. OAuth features may not work properly.", e);
		}
	}

	private void loadProviderCredentials(VaultTemplate vaultTemplate, Map<String, Object> properties) {
		// Load Coinbase OAuth
		loadProvider(vaultTemplate, "coinbase", properties);

		// Load Kraken OAuth
		loadProvider(vaultTemplate, "kraken", properties);

		// Load Binance OAuth
		loadProvider(vaultTemplate, "binance", properties);

		// Load Alpaca OAuth
		loadProvider(vaultTemplate, "alpaca", properties);
	}

	private void loadAuthCredentials(VaultTemplate vaultTemplate, Map<String, Object> properties) {
		// Load Google OAuth (for authentication)
		try {
			VaultResponse response = vaultTemplate.read("secret/data/strategiz/oauth/google");
			if (response != null && response.getData() != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					addPropertyIfNotNull(properties, "oauth.providers.google.client-id", data.get("client-id"));
					addPropertyIfNotNull(properties, "oauth.providers.google.client-secret", data.get("client-secret"));
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
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					addPropertyIfNotNull(properties, "oauth.providers.facebook.client-id", data.get("client-id"));
					addPropertyIfNotNull(properties, "oauth.providers.facebook.client-secret",
							data.get("client-secret"));
					log.debug("Loaded Facebook OAuth credentials from Vault");
				}
			}
		}
		catch (Exception e) {
			log.debug("Facebook OAuth credentials not found in Vault: {}", e.getMessage());
		}
	}

	private void loadProvider(VaultTemplate vaultTemplate, String provider, Map<String, Object> properties) {
		try {
			String path = String.format("secret/data/strategiz/oauth/%s", provider);
			VaultResponse response = vaultTemplate.read(path);

			if (response != null && response.getData() != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) response.getData().get("data");
				if (data != null) {
					String clientId = (String) data.get("client-id");
					String clientSecret = (String) data.get("client-secret");
					String redirectUriLocal = (String) data.get("redirect-uri-local");
					String redirectUriProd = (String) data.get("redirect-uri-prod");

					if (clientId != null) {
						String propKey = String.format("oauth.providers.%s.client-id", provider);
						properties.put(propKey, clientId);
						log.info("Loaded {} OAuth client-id from Vault", provider);
					}

					if (clientSecret != null) {
						String propKey = String.format("oauth.providers.%s.client-secret", provider);
						properties.put(propKey, clientSecret);
						log.info("Loaded {} OAuth client-secret from Vault", provider);
					}

					// Load redirect URI based on active profile (dev uses local, prod
					// uses prod)
					// Check both system property and environment variable (Spring uses
					// env var)
					String activeProfiles = System.getProperty("spring.profiles.active",
							System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
					String redirectUri = activeProfiles.contains("prod") ? redirectUriProd : redirectUriLocal;

					if (redirectUri != null) {
						String propKey = String.format("oauth.providers.%s.redirect-uri", provider);
						properties.put(propKey, redirectUri);
						log.info("Loaded {} OAuth redirect-uri from Vault (using {})", provider,
								activeProfiles.contains("prod") ? "prod" : "local");
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("{} OAuth credentials not found in Vault: {}", provider, e.getMessage());
		}
	}

	private void addPropertyIfNotNull(Map<String, Object> properties, String key, Object value) {
		if (value != null) {
			properties.put(key, value.toString());
		}
	}

}
