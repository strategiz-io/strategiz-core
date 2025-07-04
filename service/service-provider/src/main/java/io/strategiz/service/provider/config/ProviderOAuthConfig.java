package io.strategiz.service.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for Provider OAuth settings.
 * Follows the same clean pattern as auth module but remains separate.
 * 
 * Environment variables (from .env file):
 * - STRATEGIZ_KRAKEN_CLIENT_ID
 * - STRATEGIZ_KRAKEN_CLIENT_SECRET
 */
@Configuration
@ConfigurationProperties(prefix = "oauth.providers")
public class ProviderOAuthConfig {

    private Map<String, ProviderOAuthSettings> providers;

    public Map<String, ProviderOAuthSettings> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderOAuthSettings> providers) {
        this.providers = providers;
    }

    public ProviderOAuthSettings getKraken() {
        return providers != null ? providers.get("kraken") : null;
    }
    
    public void setKraken(ProviderOAuthSettings kraken) {
        if (providers == null) {
            providers = new java.util.HashMap<>();
        }
        providers.put("kraken", kraken);
    }

    /**
     * Provider OAuth settings
     */
    public static class ProviderOAuthSettings {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        
        // Getters and setters
        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
} 