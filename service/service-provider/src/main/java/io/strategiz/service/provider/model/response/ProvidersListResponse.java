package io.strategiz.service.provider.model.response;

import java.util.List;
import java.util.ArrayList;

/**
 * Response model for listing multiple providers.
 * Used when fetching all connected providers for a user.
 */
public class ProvidersListResponse {
    
    private List<ReadProviderResponse> providers;
    private Integer totalCount;
    private String status;
    
    public ProvidersListResponse() {
        this.providers = new ArrayList<>();
        this.totalCount = 0;
        this.status = "success";
    }
    
    // Getters and Setters
    public List<ReadProviderResponse> getProviders() {
        return providers;
    }
    
    public void setProviders(List<ReadProviderResponse> providers) {
        this.providers = providers;
        this.totalCount = providers != null ? providers.size() : 0;
    }
    
    public void addProvider(ReadProviderResponse provider) {
        if (this.providers == null) {
            this.providers = new ArrayList<>();
        }
        this.providers.add(provider);
        this.totalCount = this.providers.size();
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}