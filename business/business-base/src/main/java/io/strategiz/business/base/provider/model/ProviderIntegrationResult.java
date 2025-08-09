package io.strategiz.business.base.provider.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Result model for provider integration
 */
public class ProviderIntegrationResult {
    private boolean success;
    private String message;
    private String integrationId;
    private Map<String, Object> metadata;
    
    public ProviderIntegrationResult() {
        this.metadata = new HashMap<>();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getIntegrationId() {
        return integrationId;
    }
    
    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}