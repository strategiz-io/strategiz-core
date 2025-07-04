package io.strategiz.framework.secrets;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.SecretsErrors;
import io.strategiz.framework.secrets.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for the Secret Manager framework.
 * Automatically creates and configures a SecretManager bean based on application properties.
 */
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class SecretManagerAutoConfig extends AbstractVaultConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SecretManagerAutoConfig.class);
    
    private final VaultProperties vaultProperties;
    
    public SecretManagerAutoConfig(VaultProperties vaultProperties) {
        this.vaultProperties = vaultProperties;
        log.info("Initializing SecretManager auto-configuration");
    }
    
    @Override
    public VaultEndpoint vaultEndpoint() {
        String vaultUri = getEnvironment().getProperty("spring.cloud.vault.uri", "http://localhost:8200");
        try {
            return VaultEndpoint.from(new URI(vaultUri));
        } catch (URISyntaxException e) {
            log.error("Invalid Vault URI: {}", vaultUri, e);
            throw new IllegalArgumentException("Invalid Vault URI: " + vaultUri, e);
        }
    }
    
    @Override
    public ClientAuthentication clientAuthentication() {
        String token = getEnvironment().getProperty("spring.cloud.vault.token", "00000000-0000-0000-0000-000000000000");
        return new TokenAuthentication(token);
    }
    
    @Bean
    @Primary
    @ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "true", matchIfMissing = true)
    public SecretManager vaultSecretManager(VaultTemplate vaultTemplate) {
        log.info("Creating Vault SecretManager bean");
        return new VaultSecretManager(vaultTemplate, vaultProperties, getEnvironment());
    }
    
    @Bean
    @ConditionalOnMissingBean(SecretManager.class)
    @ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "false")
    public SecretManager propertySecretManager() {
        log.info("Creating Property-based SecretManager fallback bean (Vault disabled)");
        return new PropertySecretManager(getEnvironment());
    }
    
    /**
     * Simple implementation that just uses Spring Environment properties.
     * Used as a fallback when Vault is disabled.
     */
    private static class PropertySecretManager implements SecretManager {
        
        private final org.springframework.core.env.Environment environment;
        
        public PropertySecretManager(org.springframework.core.env.Environment environment) {
            this.environment = environment;
        }
        
        @Override
        public String getSecret(String key) {
            String secret = System.getenv(key);
            if (secret == null || secret.isEmpty()) {
                throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, key);
            }
            return secret;
        }
        
        @Override
        public String getSecret(String key, String defaultValue) {
            String value = environment.getProperty(key);
            return value != null ? value : defaultValue;
        }
        
        @Override
        public Map<String, String> getSecrets(String... keys) {
            Map<String, String> result = new HashMap<>();
            for (String key : keys) {
                String value = environment.getProperty(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        }
        
        @Override
        public boolean hasSecret(String key) {
            return environment.getProperty(key) != null;
        }
        
        @Override
        public boolean storeSecret(String key, String value) {
            throw new UnsupportedOperationException("Cannot store secrets in property-based implementation");
        }
        
        @Override
        public boolean removeSecret(String key) {
            throw new UnsupportedOperationException("Cannot remove secrets in property-based implementation");
        }
    }
}
