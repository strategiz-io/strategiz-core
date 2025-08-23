package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response after updating trading mode
 * Includes new JWT tokens with updated trading mode claim
 */
public class UpdateTradingModeResponse {
    
    @JsonProperty("tradingMode")
    private String tradingMode;
    
    @JsonProperty("accessToken")
    private String accessToken;
    
    @JsonProperty("refreshToken")
    private String refreshToken;
    
    @JsonProperty("message")
    private String message;
    
    // Default constructor for Jackson
    public UpdateTradingModeResponse() {
    }
    
    public UpdateTradingModeResponse(String tradingMode, String accessToken, String refreshToken) {
        this.tradingMode = tradingMode;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.message = "Trading mode updated successfully";
    }
    
    public String getTradingMode() {
        return tradingMode;
    }
    
    public void setTradingMode(String tradingMode) {
        this.tradingMode = tradingMode;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}