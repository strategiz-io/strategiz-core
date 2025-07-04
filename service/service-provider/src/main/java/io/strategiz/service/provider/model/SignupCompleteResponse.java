package io.strategiz.service.provider.model;

import java.time.Instant;
import java.util.Map;

/**
 * Response model for completing the signup process
 * 
 * Contains information about the completed signup including
 * user status, mode, and next steps for the user.
 */
public class SignupCompleteResponse {
    
    private String userId;
    private String name;
    private String email;
    private String mode;               // "demo" or "basic"
    private Boolean signupComplete;
    private Integer providersConnected;
    private Map<String, Object> userPreferences;
    private String nextStepUrl;        // URL for user's next action
    private Instant completedAt;
    
    // Default constructor
    public SignupCompleteResponse() {
        this.signupComplete = true;
        this.providersConnected = 0;
        this.completedAt = Instant.now();
    }
    
    // Constructor
    public SignupCompleteResponse(String userId, String name, String email, String mode, 
                                 Integer providersConnected, String nextStepUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.mode = mode;
        this.signupComplete = true;
        this.providersConnected = providersConnected;
        this.nextStepUrl = nextStepUrl;
        this.completedAt = Instant.now();
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
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public Boolean getSignupComplete() {
        return signupComplete;
    }
    
    public void setSignupComplete(Boolean signupComplete) {
        this.signupComplete = signupComplete;
    }
    
    public Integer getProvidersConnected() {
        return providersConnected;
    }
    
    public void setProvidersConnected(Integer providersConnected) {
        this.providersConnected = providersConnected;
    }
    
    public Map<String, Object> getUserPreferences() {
        return userPreferences;
    }
    
    public void setUserPreferences(Map<String, Object> userPreferences) {
        this.userPreferences = userPreferences;
    }
    
    public String getNextStepUrl() {
        return nextStepUrl;
    }
    
    public void setNextStepUrl(String nextStepUrl) {
        this.nextStepUrl = nextStepUrl;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "SignupCompleteResponse{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", mode='" + mode + '\'' +
                ", signupComplete=" + signupComplete +
                ", providersConnected=" + providersConnected +
                ", nextStepUrl='" + nextStepUrl + '\'' +
                ", completedAt=" + completedAt +
                '}';
    }
} 