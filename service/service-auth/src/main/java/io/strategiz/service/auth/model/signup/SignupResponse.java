package io.strategiz.service.auth.model.signup;

import java.time.Instant;

/**
 * Response model for signup process containing authentication tokens, user details,
 * and any auth method specific data
 */
public class SignupResponse {
    
    private String userId;
    private String name;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private Instant createdAt;
    
    // Auth method specific fields
    private Object authMethodData; // Can contain specific data for different auth methods (e.g., challenge for passkey)
    
    // Default constructor
    public SignupResponse() {
        this.createdAt = Instant.now();
    }
    
    // Basic constructor
    public SignupResponse(String userId, String name, String email, String accessToken, String refreshToken, Long expiresIn) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = "bearer";
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Object getAuthMethodData() {
        return authMethodData;
    }
    
    public void setAuthMethodData(Object authMethodData) {
        this.authMethodData = authMethodData;
    }
}
