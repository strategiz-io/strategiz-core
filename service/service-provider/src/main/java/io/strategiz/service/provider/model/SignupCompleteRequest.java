package io.strategiz.service.provider.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for completing signup without provider integration
 * 
 * Used when users choose to skip provider connections and proceed
 * with demo mode or basic platform access.
 */
public class SignupCompleteRequest {
    
    @NotBlank(message = "Mode is required")
    @Pattern(regexp = "^(demo|basic)$", message = "Mode must be either 'demo' or 'basic'")
    private String mode;
    
    private Boolean skipProviders;
    
    // Default constructor
    public SignupCompleteRequest() {
        this.skipProviders = true; // Default to skipping providers
    }
    
    // Constructor
    public SignupCompleteRequest(String mode, Boolean skipProviders) {
        this.mode = mode;
        this.skipProviders = skipProviders;
    }
    
    // Getters and setters
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public Boolean getSkipProviders() {
        return skipProviders;
    }
    
    public void setSkipProviders(Boolean skipProviders) {
        this.skipProviders = skipProviders;
    }
    
    @Override
    public String toString() {
        return "SignupCompleteRequest{" +
                "mode='" + mode + '\'' +
                ", skipProviders=" + skipProviders +
                '}';
    }
} 