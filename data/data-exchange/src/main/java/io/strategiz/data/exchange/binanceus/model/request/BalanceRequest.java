package io.strategiz.data.exchange.binanceus.model.request;

import java.util.Objects;

/**
 * Request model for balance information
 */
public class BalanceRequest {
    private String apiKey;
    private String secretKey;
    private String userId;
    
    // Constructors
    public BalanceRequest() {}
    
    public BalanceRequest(String apiKey, String secretKey, String userId) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.userId = userId;
    }
    
    // Getters and setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalanceRequest that = (BalanceRequest) o;
        return Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(secretKey, that.secretKey) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(apiKey, secretKey, userId);
    }
    
    @Override
    public String toString() {
        return "BalanceRequest{" +
               "apiKey='" + apiKey + '\'' +
               ", secretKey='[PROTECTED]'" +
               ", userId='" + userId + '\'' +
               '}';
    }
}
