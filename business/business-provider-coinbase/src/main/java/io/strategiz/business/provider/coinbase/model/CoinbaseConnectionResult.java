package io.strategiz.business.provider.coinbase.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * Result object for Coinbase OAuth connection
 */
public class CoinbaseConnectionResult {
    
    private String userId;
    private String providerId;
    private String providerName;
    private String accessToken;
    private String refreshToken;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant expiresAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant connectedAt;
    
    private Map<String, Object> accountInfo;
    private String status;

    // Constructors
    public CoinbaseConnectionResult() {}

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Map<String, Object> getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(Map<String, Object> accountInfo) {
        this.accountInfo = accountInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 