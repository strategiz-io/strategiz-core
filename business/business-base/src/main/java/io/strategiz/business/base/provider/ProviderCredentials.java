package io.strategiz.business.base.provider;

/**
 * Basic provider credentials interface
 */
public class ProviderCredentials {
    private String apiKey;
    private String apiSecret;
    private String otp;
    private String passphrase;

    public ProviderCredentials() {}

    public ProviderCredentials(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public ProviderCredentials(String apiKey, String apiSecret, String otp) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.otp = otp;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}