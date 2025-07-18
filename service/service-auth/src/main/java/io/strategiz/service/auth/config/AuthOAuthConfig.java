package io.strategiz.service.auth.config;

import io.strategiz.framework.secrets.SecretManager;
import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (googleConfig != null && secretManager != null) {
            // Try to load secrets from Vault
            try {
                String vaultClientId = secretManager.getSecret("oauth.google.client-id", googleConfig.getClientId());
                String vaultClientSecret = secretManager.getSecret("oauth.google.client-secret", googleConfig.getClientSecret());
                
                if (!vaultClientId.equals(googleConfig.getClientId()) || !vaultClientSecret.equals(googleConfig.getClientSecret())) {
                    logger.info("Loading Google OAuth credentials from Vault");
                    googleConfig = new AuthOAuthSettings(googleConfig);
                    googleConfig.setClientId(vaultClientId);
                    googleConfig.setClientSecret(vaultClientSecret);
                }
            } catch (Exception e) {
                logger.debug("Using environment variables for Google OAuth (Vault not available): {}", e.getMessage());
            }
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
        if (facebookConfig != null && secretManager != null) {
            // Try to load secrets from Vault
            try {
                String vaultClientId = secretManager.getSecret("oauth.facebook.client-id", facebookConfig.getClientId());
                String vaultClientSecret = secretManager.getSecret("oauth.facebook.client-secret", facebookConfig.getClientSecret());
                
                if (!vaultClientId.equals(facebookConfig.getClientId()) || !vaultClientSecret.equals(facebookConfig.getClientSecret())) {
                    logger.info("Loading Facebook OAuth credentials from Vault");
                    facebookConfig = new AuthOAuthSettings(facebookConfig);
                    facebookConfig.setClientId(vaultClientId);
                    facebookConfig.setClientSecret(vaultClientSecret);
                }
            } catch (Exception e) {
                logger.debug("Using environment variables for Facebook OAuth (Vault not available): {}", e.getMessage());
            }
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
