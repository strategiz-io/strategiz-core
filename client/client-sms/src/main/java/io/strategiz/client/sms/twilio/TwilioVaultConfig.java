package io.strategiz.client.sms.twilio;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Loads Twilio credentials from Vault and injects them into TwilioSmsConfig.
 *
 * Vault path: secret/strategiz/twilio
 * Required secrets: account-sid, auth-token, phone-number
 */
@Configuration
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
public class TwilioVaultConfig {

    private static final Logger log = LoggerFactory.getLogger(TwilioVaultConfig.class);

    private final SecretManager secretManager;
    private final TwilioSmsConfig twilioConfig;

    @Autowired
    public TwilioVaultConfig(
            @Qualifier("vaultSecretService") SecretManager secretManager,
            TwilioSmsConfig twilioConfig) {
        this.secretManager = secretManager;
        this.twilioConfig = twilioConfig;
    }

    @PostConstruct
    public void loadSecretsFromVault() {
        try {
            log.info("Loading Twilio credentials from Vault...");

            // Load Account SID
            String accountSid = secretManager.readSecret("twilio.account-sid", null);
            if (accountSid != null && !accountSid.isEmpty()) {
                twilioConfig.setAccountSid(accountSid);
                log.info("Loaded Twilio Account SID from Vault");
            } else {
                log.warn("Twilio Account SID not found in Vault");
            }

            // Load Auth Token
            String authToken = secretManager.readSecret("twilio.auth-token", null);
            if (authToken != null && !authToken.isEmpty()) {
                twilioConfig.setAuthToken(authToken);
                log.info("Loaded Twilio Auth Token from Vault");
            } else {
                log.warn("Twilio Auth Token not found in Vault");
            }

            // Load Phone Number
            String phoneNumber = secretManager.readSecret("twilio.phone-number", null);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                twilioConfig.setPhoneNumber(phoneNumber);
                log.info("Loaded Twilio Phone Number from Vault");
            } else {
                log.warn("Twilio Phone Number not found in Vault");
            }

            if (twilioConfig.isConfigured()) {
                log.info("Twilio SMS configuration loaded successfully from Vault");
            } else {
                log.warn("Twilio SMS not fully configured - some fields missing from Vault");
            }

        } catch (Exception e) {
            log.error("Failed to load Twilio credentials from Vault", e);
        }
    }
}
