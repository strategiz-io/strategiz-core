package io.strategiz.framework.secrets.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.framework.secrets.exception.SecretsErrors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation that just uses Spring Environment properties.
 * Used as a fallback when Vault is disabled.
 */
@Service
public class PropertySecretService implements SecretManager {
    
    private final Environment environment;
    
    public PropertySecretService(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public String readSecret(String key) {
        String secret = System.getenv(key);
        if (secret == null || secret.isEmpty()) {
            throw new StrategizException(SecretsErrors.SECRET_NOT_FOUND, key);
        }
        return secret;
    }
    
    @Override
    public String readSecret(String key, String defaultValue) {
        String value = environment.getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public Map<String, String> readSecrets(String... keys) {
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
    public boolean secretExists(String key) {
        return environment.getProperty(key) != null;
    }
    
    @Override
    public void createSecret(String key, String value) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Cannot store secrets in property-based implementation");
    }

    @Override
    public void createSecret(String key, Map<String, Object> data) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Cannot store secrets in property-based implementation");
    }

    @Override
    public Map<String, Object> readSecretAsMap(String key) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Complex secret data not supported in property-based implementation");
    }

    @Override
    public void updateSecret(String key, String value) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Cannot update secrets in property-based implementation");
    }

    @Override
    public void updateSecret(String key, Map<String, Object> data) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Cannot update secrets in property-based implementation");
    }

    @Override
    public void deleteSecret(String key) {
        throw new StrategizException(SecretsErrors.OPERATION_NOT_SUPPORTED,
            "PropertySecretService", "Cannot delete secrets in property-based implementation");
    }
}