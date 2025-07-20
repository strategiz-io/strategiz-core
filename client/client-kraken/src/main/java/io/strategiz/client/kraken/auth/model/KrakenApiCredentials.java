package io.strategiz.client.kraken.auth.model;

/**
 * Kraken API credentials model
 */
public class KrakenApiCredentials {
    
    private String apiKey;
    private String apiSecret;
    private String otp;
    private String userId;
    
    public KrakenApiCredentials() {
    }
    
    public KrakenApiCredentials(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public KrakenApiCredentials(String apiKey, String apiSecret, String otp) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.otp = otp;
    }
    
    public KrakenApiCredentials(String apiKey, String apiSecret, String otp, String userId) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.otp = otp;
        this.userId = userId;
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "KrakenApiCredentials{" +
                "apiKey='" + (apiKey != null ? apiKey.substring(0, Math.min(apiKey.length(), 4)) + "****" : null) + '\'' +
                ", hasOtp=" + (otp != null && !otp.isEmpty()) +
                ", userId='" + userId + '\'' +
                '}';
    }
}