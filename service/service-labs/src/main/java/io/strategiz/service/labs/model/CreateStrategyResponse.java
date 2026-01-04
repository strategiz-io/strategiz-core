package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal response for strategy create/update operations
 * Returns only essential fields, not the full code or performance data
 */
public class CreateStrategyResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("publicStatus")
    private String publicStatus;
    
    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("modifiedDate")
    private String modifiedDate;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPublicStatus() {
        return publicStatus;
    }
    
    public void setPublicStatus(String publicStatus) {
        this.publicStatus = publicStatus;
    }
    
    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
