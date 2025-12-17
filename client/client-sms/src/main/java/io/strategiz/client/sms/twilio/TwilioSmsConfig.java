package io.strategiz.client.sms.twilio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Twilio SMS client.
 * Values are loaded from Vault via application properties.
 */
@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioSmsConfig {

    /**
     * Twilio Account SID (from Vault: strategiz/twilio/account_sid)
     */
    private String accountSid;

    /**
     * Twilio Auth Token (from Vault: strategiz/twilio/auth_token)
     */
    private String authToken;

    /**
     * Twilio phone number for sending SMS (from Vault: strategiz/twilio/phone_number)
     * Must be in E.164 format: +14155551234
     */
    private String phoneNumber;

    /**
     * Optional: Twilio Messaging Service SID (for high-volume sending)
     */
    private String messagingServiceSid;

    /**
     * Enable/disable SMS sending (for dev environments)
     */
    private boolean enabled = true;

    /**
     * Enable mock mode for development (logs instead of sending)
     */
    private boolean mockEnabled = false;

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessagingServiceSid() {
        return messagingServiceSid;
    }

    public void setMessagingServiceSid(String messagingServiceSid) {
        this.messagingServiceSid = messagingServiceSid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    /**
     * Check if the Twilio configuration is valid.
     */
    public boolean isConfigured() {
        return accountSid != null && !accountSid.isEmpty()
                && authToken != null && !authToken.isEmpty()
                && phoneNumber != null && !phoneNumber.isEmpty();
    }
}
