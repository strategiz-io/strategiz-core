package io.strategiz.framework.secrets;

/**
 * Error codes for secrets framework operations
 * 
 * Usage: throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED);
 */
public enum SecretsErrors {
    VAULT_CONNECTION_FAILED,
    VAULT_AUTHENTICATION_FAILED,
    SECRET_NOT_FOUND,
    SECRET_ACCESS_DENIED,
    CONFIGURATION_ERROR,
    ENCRYPTION_ERROR,
    DECRYPTION_ERROR,
    INVALID_SECRET_KEY,
    SECRET_EXPIRED
} 