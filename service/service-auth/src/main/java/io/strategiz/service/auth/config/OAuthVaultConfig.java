package io.strategiz.service.auth.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public OAuthVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, ConfigurableEnvironment environment) {
        this.secretManager = secretManager;
        this.environment = environment;
        loadOAuthPropertiesFromVault();
    }
    
    private void loadOAuthPropertiesFromVault() {
        try {
            log.info("Loading OAuth properties from Vault...");
            
            Map<String, Object> vaultProperties = new HashMap<>();
            
            // Load Google OAuth credentials
            String googleClientId = secretManager.readSecret("oauth.google.client-id");
            String googleClientSecret = secretManager.readSecret("oauth.google.client-secret");
            
            if (googleClientId != null && !googleClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.google.client-id", googleClientId);
                log.info("Loaded Google OAuth client-id from Vault");
            }
            
            if (googleClientSecret != null && !googleClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.google.client-secret", googleClientSecret);
                log.info("Loaded Google OAuth client-secret from Vault");
            }
            
            // Load Facebook OAuth credentials
            String facebookClientId = secretManager.readSecret("oauth.facebook.client-id");
            String facebookClientSecret = secretManager.readSecret("oauth.facebook.client-secret");

            if (facebookClientId != null && !facebookClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.facebook.client-id", facebookClientId);
                log.info("Loaded Facebook OAuth client-id from Vault");
            }

            if (facebookClientSecret != null && !facebookClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.facebook.client-secret", facebookClientSecret);
                log.info("Loaded Facebook OAuth client-secret from Vault");
            }

            // Load Coinbase OAuth credentials (for provider integrations)
            String coinbaseClientId = secretManager.readSecret("oauth.coinbase.client-id");
            String coinbaseClientSecret = secretManager.readSecret("oauth.coinbase.client-secret");

            if (coinbaseClientId != null && !coinbaseClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.coinbase.client-id", coinbaseClientId);
                log.info("Loaded Coinbase OAuth client-id from Vault");
            }

            if (coinbaseClientSecret != null && !coinbaseClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.coinbase.client-secret", coinbaseClientSecret);
                log.info("Loaded Coinbase OAuth client-secret from Vault");
            }

            // Load Alpaca OAuth credentials
            String alpacaClientId = secretManager.readSecret("oauth.alpaca.client-id");
            String alpacaClientSecret = secretManager.readSecret("oauth.alpaca.client-secret");

            if (alpacaClientId != null && !alpacaClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.alpaca.client-id", alpacaClientId);
                log.info("Loaded Alpaca OAuth client-id from Vault");
            }

            if (alpacaClientSecret != null && !alpacaClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.alpaca.client-secret", alpacaClientSecret);
                log.info("Loaded Alpaca OAuth client-secret from Vault");
            }

            // Load Schwab OAuth credentials
            String schwabClientId = secretManager.readSecret("oauth.schwab.client-id");
            String schwabClientSecret = secretManager.readSecret("oauth.schwab.client-secret");

            if (schwabClientId != null && !schwabClientId.isEmpty()) {
                vaultProperties.put("oauth.providers.schwab.client-id", schwabClientId);
                log.info("Loaded Schwab OAuth client-id from Vault");
            }

            if (schwabClientSecret != null && !schwabClientSecret.isEmpty()) {
                vaultProperties.put("oauth.providers.schwab.client-secret", schwabClientSecret);
                log.info("Loaded Schwab OAuth client-secret from Vault");
            }

            // Load Kraken API credentials
            String krakenApiKey = secretManager.readSecret("oauth.kraken.api-key");
            String krakenPrivateKey = secretManager.readSecret("oauth.kraken.private-key");

            if (krakenApiKey != null && !krakenApiKey.isEmpty()) {
                vaultProperties.put("kraken.api-key", krakenApiKey);
                log.info("Loaded Kraken API key from Vault");
            }

            if (krakenPrivateKey != null && !krakenPrivateKey.isEmpty()) {
                vaultProperties.put("kraken.private-key", krakenPrivateKey);
                log.info("Loaded Kraken private key from Vault");
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