package io.strategiz.client.plaid.config;

import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Loads Plaid credentials from Vault at startup.
 * Vault path: secret/strategiz/plaid
 * Required secrets:
 * - client-id: Plaid client ID
 * - secret: Plaid secret key
 */
@Configuration
@ConditionalOnProperty(name = "plaid.enabled", havingValue = "true", matchIfMissing = false)
public class PlaidVaultConfig {

    private static final Logger log = LoggerFactory.getLogger(PlaidVaultConfig.class);
    private static final String VAULT_PATH = "plaid";

    private final SecretManager secretManager;
    private final PlaidConfig plaidConfig;

    @Autowired
    public PlaidVaultConfig(SecretManager secretManager, PlaidConfig plaidConfig) {
        this.secretManager = secretManager;
        this.plaidConfig = plaidConfig;
    }

    @PostConstruct
    public void loadSecretsFromVault() {
        log.info("Loading Plaid credentials from Vault...");

        try {
            Map<String, Object> secrets = secretManager.readSecretAsMap(VAULT_PATH);

            if (secrets == null || secrets.isEmpty()) {
                log.warn("No Plaid secrets found in Vault at path: {}", VAULT_PATH);
                return;
            }

            Object clientIdObj = secrets.get("client-id");
            Object secretObj = secrets.get("secret");

            if (clientIdObj != null) {
                plaidConfig.setClientId(clientIdObj.toString());
                log.info("Plaid client ID loaded from Vault");
            } else {
                log.warn("Plaid client-id not found in Vault");
            }

            if (secretObj != null) {
                plaidConfig.setSecret(secretObj.toString());
                log.info("Plaid secret loaded from Vault");
            } else {
                log.warn("Plaid secret not found in Vault");
            }

        } catch (Exception e) {
            log.error("Failed to load Plaid credentials from Vault: {}", e.getMessage(), e);
        }
    }
}
