package io.strategiz.service.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update user's demo mode
 */
public class UpdateDemoModeRequest {
    
    @NotNull(message = "Demo mode is required")
    @JsonProperty("demoMode")
    private Boolean demoMode;
    
    // Default constructor for Jackson
    public UpdateDemoModeRequest() {
    }
    
    public UpdateDemoModeRequest(Boolean demoMode) {
        this.demoMode = demoMode;
    }
    
    public Boolean isDemoMode() {
        return demoMode;
    }
    
    public void setDemoMode(Boolean demoMode) {
        this.demoMode = demoMode;
    }
}