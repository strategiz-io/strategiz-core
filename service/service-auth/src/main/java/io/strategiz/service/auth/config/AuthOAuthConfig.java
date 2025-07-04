package io.strategiz.service.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for Authentication OAuth providers.
 * Handles OAuth providers used for user authentication (login/signup).
 * 
 * Environment variables (from .env file):
 * - AUTH_GOOGLE_CLIENT_ID
 * - AUTH_GOOGLE_CLIENT_SECRET
 * - AUTH_FACEBOOK_CLIENT_ID  
 * - AUTH_FACEBOOK_CLIENT_SECRET
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "oauth")
public class AuthOAuthConfig {

    private Map<String, AuthOAuthSettings> providers;

    public Map<String, AuthOAuthSettings> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, AuthOAuthSettings> providers) {
        this.providers = providers;
    }

    public AuthOAuthSettings getGoogle() {
        return providers != null ? providers.get("google") : null;
    }
    
    public void setGoogle(AuthOAuthSettings google) {
        if (providers == null) {
            providers = new java.util.HashMap<>();
        }
        providers.put("google", google);
    }

    public AuthOAuthSettings getFacebook() {
        return providers != null ? providers.get("facebook") : null;
    }
    
    public void setFacebook(AuthOAuthSettings facebook) {
        if (providers == null) {
            providers = new java.util.HashMap<>();
        }
        providers.put("facebook", facebook);
    }

    /**
     * Authentication OAuth provider settings
     */
    public static class AuthOAuthSettings {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope;

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

        public String getAuthUrl() {
            return authUrl;
        }

        public void setAuthUrl(String authUrl) {
            this.authUrl = authUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
