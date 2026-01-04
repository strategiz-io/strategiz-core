package io.strategiz.client.sms.twilio;

import io.strategiz.framework.secrets.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Loads Twilio credentials from Vault and injects them into TwilioSmsConfig.
 *
 * Vault path: secret/strategiz/twilio
 * Required secrets: account-sid, auth-token, phone-number
 */
@Configuration
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
public class TwilioVaultConfig {

    private static final Logger logger = LoggerFactory.getLogger(TwilioVaultConfig.class);
    private static final String VAULT_PATH = "secret/strategiz/twilio";

    private final SecretManager secretManager;
    private final TwilioSmsConfig twilioConfig;

    @Autowired
    public TwilioVaultConfig(
            SecretManager secretManager,
            TwilioSmsConfig twilioConfig) {
        this.secretManager = secretManager;
        this.twilioConfig = twilioConfig;
    }

    @PostConstruct
    public void loadSecretsFromVault() {
        try {
            logger.info("Loading Twilio credentials from Vault: {}", VAULT_PATH);

            Map<String, String> secrets = secretManager.getSecrets(VAULT_PATH);

            // Load and inject secrets into config
            String accountSid = secrets.get("account-sid");
            String authToken = secrets.get("auth-token");
            String phoneNumber = secrets.get("phone-number");

            if (accountSid != null && authToken != null && phoneNumber != null) {
                twilioConfig.setAccountSid(accountSid);
                twilioConfig.setAuthToken(authToken);
                twilioConfig.setPhoneNumber(phoneNumber);
                logger.info("Twilio credentials loaded successfully from Vault");
            } else {
                logger.warn("Twilio credentials incomplete in Vault - missing required fields");
            }

        } catch (Exception e) {
            logger.error("Failed to load Twilio credentials from Vault: {}", e.getMessage());
        }
    }
}
