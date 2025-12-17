package io.strategiz.service.auth.config;

import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * Configuration properties for Authentication OAuth providers. Handles OAuth providers used for
 * user authentication (login/signup).
 *
 * OAuth credentials are loaded from Spring Environment (set by OAuthVaultConfig) at startup
 * and cached for subsequent requests.
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "oauth")
public class AuthOAuthConfig {

	private static final Logger logger = LoggerFactory.getLogger(AuthOAuthConfig.class);

	@Autowired
	private Environment environment;

	private Map<String, AuthOAuthSettings> providers;

	private String frontendUrl;

	// Cached settings with credentials loaded from Spring Environment
	private AuthOAuthSettings cachedGoogleSettings;

	private AuthOAuthSettings cachedFacebookSettings;

	private boolean credentialsLoaded = false;

	@PostConstruct
	public void loadCredentialsFromEnvironment() {
		logger.info("Loading OAuth credentials from Spring Environment (set by OAuthVaultConfig)...");

		// Load Google credentials from Spring Environment
		// OAuthVaultConfig sets these as oauth.providers.google.client-id
		String googleClientId = environment.getProperty("oauth.providers.google.client-id");
		String googleClientSecret = environment.getProperty("oauth.providers.google.client-secret");

		if (googleClientId != null && !googleClientId.isEmpty()) {
			AuthOAuthSettings googleBase = providers != null ? providers.get("google") : null;
			if (googleBase != null) {
				cachedGoogleSettings = new AuthOAuthSettings(googleBase);
				cachedGoogleSettings.setClientId(googleClientId);
				cachedGoogleSettings.setClientSecret(googleClientSecret);
				logger.info("Loaded Google OAuth credentials (clientId: {}...)",
						googleClientId.substring(0, Math.min(10, googleClientId.length())));
			}
			else {
				logger.warn("Google OAuth base config not found in providers map");
			}
		}
		else {
			logger.warn("Google OAuth client_id not found in Spring Environment");
		}

		// Load Facebook credentials from Spring Environment
		String facebookClientId = environment.getProperty("oauth.providers.facebook.client-id");
		String facebookClientSecret = environment.getProperty("oauth.providers.facebook.client-secret");

		if (facebookClientId != null && !facebookClientId.isEmpty()) {
			AuthOAuthSettings facebookBase = providers != null ? providers.get("facebook") : null;
			if (facebookBase != null) {
				cachedFacebookSettings = new AuthOAuthSettings(facebookBase);
				cachedFacebookSettings.setClientId(facebookClientId);
				cachedFacebookSettings.setClientSecret(facebookClientSecret);
				logger.info("Loaded Facebook OAuth credentials (clientId: {}...)",
						facebookClientId.substring(0, Math.min(10, facebookClientId.length())));
			}
			else {
				logger.warn("Facebook OAuth base config not found in providers map");
			}
		}
		else {
			logger.warn("Facebook OAuth client_id not found in Spring Environment");
		}

		credentialsLoaded = true;
	}

	public Map<String, AuthOAuthSettings> getProviders() {
		return providers;
	}

	public void setProviders(Map<String, AuthOAuthSettings> providers) {
		this.providers = providers;
	}

	public String getFrontendUrl() {
		return frontendUrl;
	}

	public void setFrontendUrl(String frontendUrl) {
		this.frontendUrl = frontendUrl;
	}

	public AuthOAuthSettings getGoogle() {
		// Return cached settings if available
		if (cachedGoogleSettings != null) {
			return cachedGoogleSettings;
		}

		// Fallback to properties-based settings
		AuthOAuthSettings googleConfig = providers != null ? providers.get("google") : null;
		if (googleConfig == null) {
			logger.warn("Google OAuth config not found");
		}
		else if (googleConfig.getClientId() == null || googleConfig.getClientId().isEmpty()) {
			logger.error("Google OAuth client_id is null or empty!");
		}

		return googleConfig;
	}

	public void setGoogle(AuthOAuthSettings google) {
		if (providers == null) {
			providers = new java.util.HashMap<>();
		}
		providers.put("google", google);
	}

	public AuthOAuthSettings getFacebook() {
		// Return cached settings if available
		if (cachedFacebookSettings != null) {
			return cachedFacebookSettings;
		}

		// Fallback to properties-based settings
		AuthOAuthSettings facebookConfig = providers != null ? providers.get("facebook") : null;
		if (facebookConfig == null) {
			logger.warn("Facebook OAuth config not found");
		}
		else if (facebookConfig.getClientId() == null || facebookConfig.getClientId().isEmpty()) {
			logger.error("Facebook OAuth client_id is null or empty!");
		}

		return facebookConfig;
	}

	public void setFacebook(AuthOAuthSettings facebook) {
		if (providers == null) {
			providers = new java.util.HashMap<>();
		}
		providers.put("facebook", facebook);
	}

}
