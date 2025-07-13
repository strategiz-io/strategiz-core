package io.strategiz.service.auth.config;

import io.strategiz.framework.secrets.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class that loads OAuth credentials from Vault
 * and makes them available as Spring properties.
 */
@Configuration
public class OAuthVaultConfig {
    
    private static final Logger log = LoggerFactory.getLogger(OAuthVaultConfig.class);
    
    private final SecretManager secretManager;
    private final ConfigurableEnvironment environment;
    
    @Autowired
    public OAuthVaultConfig(SecretManager secretManager, ConfigurableEnvironment environment) {
        this.secretManager = secretManager;
        this.environment = environment;
        loadOAuthPropertiesFromVault();
    }
    
    private void loadOAuthPropertiesFromVault() {
        try {
            log.info("Loading OAuth properties from Vault...");
            
            Map<String, Object> vaultProperties = new HashMap<>();
            
            // Load Google OAuth credentials
            String googleClientId = secretManager.getSecret("oauth/google.client-id");
            String googleClientSecret = secretManager.getSecret("oauth/google.client-secret");
            
            if (googleClientId != null && !googleClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.google.client-id", googleClientId);
                log.info("Loaded Google OAuth client-id from Vault");
            }
            
            if (googleClientSecret != null && !googleClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.google.client-secret", googleClientSecret);
                log.info("Loaded Google OAuth client-secret from Vault");
            }
            
            // Load Facebook OAuth credentials
            String facebookClientId = secretManager.getSecret("oauth/facebook.client-id");
            String facebookClientSecret = secretManager.getSecret("oauth/facebook.client-secret");
            
            if (facebookClientId != null && !facebookClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.facebook.client-id", facebookClientId);
                log.info("Loaded Facebook OAuth client-id from Vault");
            }
            
            if (facebookClientSecret != null && !facebookClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.facebook.client-secret", facebookClientSecret);
                log.info("Loaded Facebook OAuth client-secret from Vault");
            }
            
            // Add the properties to the environment
            if (!vaultProperties.isEmpty()) {
                MapPropertySource vaultPropertySource = new MapPropertySource("vault-oauth", vaultProperties);
                environment.getPropertySources().addFirst(vaultPropertySource);
                log.info("Successfully loaded {} OAuth properties from Vault", vaultProperties.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to load OAuth properties from Vault", e);
        }
    }
}