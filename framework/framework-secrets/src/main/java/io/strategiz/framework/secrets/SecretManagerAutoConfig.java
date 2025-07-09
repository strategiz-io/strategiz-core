package io.strategiz.framework.secrets;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for the Secret Manager framework.
 * Automatically creates and configures a SecretManager bean based on application properties.
 */
@Configuration
@EnableConfigurationProperties(VaultProperties.class)
public class SecretManagerAutoConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SecretManagerAutoConfig.class);
    
    private final VaultProperties vaultProperties;
    private final Environment environment;
    
    public SecretManagerAutoConfig(VaultProperties vaultProperties, Environment environment) {
        this.vaultProperties = vaultProperties;
        this.environment = environment;
        log.info("Initializing SecretManager auto-configuration");
    }
    
    @Bean
    @Primary
    @ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "true", matchIfMissing = true)
    public SecretManager vaultSecretManager() {
        log.info("Creating Vault SecretManager bean with HTTP API");
        return new VaultSecretManager(vaultProperties, environment);
    }
    
    @Bean
    @ConditionalOnMissingBean(SecretManager.class)
    @ConditionalOnProperty(name = "strategiz.vault.enabled", havingValue = "false")
    public SecretManager propertySecretManager() {
        log.info("Creating Property-based SecretManager fallback bean (Vault disabled)");
        return new PropertySecretManager(environment);
    }
    
    /**
     * Simple implementation that just uses Spring Environment properties.
     * Used as a fallback when Vault is disabled.
     */
    private static class PropertySecretManager implements SecretManager {
        
        private final Environment environment;
        
        public PropertySecretManager(Environment environment) {
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
