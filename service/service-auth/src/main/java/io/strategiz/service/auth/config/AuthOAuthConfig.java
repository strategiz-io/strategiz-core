package io.strategiz.service.auth.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for Authentication OAuth providers.
 * Handles OAuth providers used for user authentication (login/signup).
 * 
 * OAuth credentials are loaded from Vault when available, otherwise from environment variables:
 * - AUTH_GOOGLE_CLIENT_ID
 * - AUTH_GOOGLE_CLIENT_SECRET
 * - AUTH_FACEBOOK_CLIENT_ID  
 * - AUTH_FACEBOOK_CLIENT_SECRET
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "oauth")
public class AuthOAuthConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthOAuthConfig.class);

    private Map<String, AuthOAuthSettings> providers;
    private String frontendUrl;
    
    @Autowired(required = false)
    @Qualifier("vaultSecretService")
    private SecretManager secretManager;

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
        AuthOAuthSettings googleConfig = providers != null ? providers.get("google") : null;

        if (googleConfig == null) {
            logger.warn("Google OAuth config not found in providers map. Providers: {}", providers != null ? providers.keySet() : "null");
            return null;
        }

        if (secretManager == null) {
            logger.warn("SecretManager not available for Google OAuth. Using config values. ClientId present: {}",
                googleConfig.getClientId() != null);
            return googleConfig;
        }

        // Try to load secrets from Vault
        try {
            String vaultClientId = secretManager.readSecret("oauth.google.client-id", googleConfig.getClientId());
            String vaultClientSecret = secretManager.readSecret("oauth.google.client-secret", googleConfig.getClientSecret());

            logger.debug("Google OAuth - Vault clientId present: {}, config clientId present: {}",
                vaultClientId != null, googleConfig.getClientId() != null);

            if (vaultClientId != null && !vaultClientId.equals(googleConfig.getClientId())) {
                logger.info("Loading Google OAuth credentials from Vault");
                googleConfig = new AuthOAuthSettings(googleConfig);
                googleConfig.setClientId(vaultClientId);
                googleConfig.setClientSecret(vaultClientSecret);
            }
        } catch (Exception e) {
            logger.error("Failed to load Google OAuth credentials from Vault: {}", e.getMessage());
        }

        if (googleConfig.getClientId() == null || googleConfig.getClientId().isEmpty()) {
            logger.error("Google OAuth client_id is null or empty after loading!");
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
        AuthOAuthSettings facebookConfig = providers != null ? providers.get("facebook") : null;

        if (facebookConfig == null) {
            logger.warn("Facebook OAuth config not found in providers map. Providers: {}", providers != null ? providers.keySet() : "null");
            return null;
        }

        if (secretManager == null) {
            logger.warn("SecretManager not available for Facebook OAuth. Using config values. ClientId present: {}",
                facebookConfig.getClientId() != null);
            return facebookConfig;
        }

        // Try to load secrets from Vault
        try {
            String vaultClientId = secretManager.readSecret("oauth.facebook.client-id", facebookConfig.getClientId());
            String vaultClientSecret = secretManager.readSecret("oauth.facebook.client-secret", facebookConfig.getClientSecret());

            if (vaultClientId != null && !vaultClientId.equals(facebookConfig.getClientId())) {
                logger.info("Loading Facebook OAuth credentials from Vault");
                facebookConfig = new AuthOAuthSettings(facebookConfig);
                facebookConfig.setClientId(vaultClientId);
                facebookConfig.setClientSecret(vaultClientSecret);
            }
        } catch (Exception e) {
            logger.error("Failed to load Facebook OAuth credentials from Vault: {}", e.getMessage());
        }

        if (facebookConfig.getClientId() == null || facebookConfig.getClientId().isEmpty()) {
            logger.error("Facebook OAuth client_id is null or empty after loading!");
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
