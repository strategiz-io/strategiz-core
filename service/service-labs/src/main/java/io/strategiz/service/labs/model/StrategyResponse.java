package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.strategy.entity.StrategyPerformance;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class StrategyResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("backtestResults")
    private Map<String, Object> backtestResults;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    @JsonProperty("isPublic")
    private boolean isPublic;

    @JsonProperty("performance")
    private StrategyPerformance performance;

    @JsonProperty("createdAt")
    private Date createdAt;
    
    @JsonProperty("updatedAt")
    private Date updatedAt;

    // Versioning fields
    @JsonProperty("version")
    private Long version;

    @JsonProperty("parentStrategyId")
    private String parentStrategyId;

    // Deployment fields
    @JsonProperty("deploymentType")
    private String deploymentType;

    @JsonProperty("deploymentId")
    private String deploymentId;

    @JsonProperty("deployedAt")
    private Object deployedAt;

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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Map<String, Object> getBacktestResults() {
        return backtestResults;
    }
    
    public void setBacktestResults(Map<String, Object> backtestResults) {
        this.backtestResults = backtestResults;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public StrategyPerformance getPerformance() {
        return performance;
    }

    public void setPerformance(StrategyPerformance performance) {
        this.performance = performance;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getParentStrategyId() {
        return parentStrategyId;
    }

    public void setParentStrategyId(String parentStrategyId) {
        this.parentStrategyId = parentStrategyId;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public void setDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public Object getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Object deployedAt) {
        this.deployedAt = deployedAt;
    }
}