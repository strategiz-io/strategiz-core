package io.strategiz.data.strategy.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.Timestamp;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategies")
public class Strategy extends BaseEntity {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("language")
    private String language; // python, java, pinescript
    
    @JsonProperty("type")
    private String type; // technical, fundamental, hybrid
    
    @JsonProperty("status")
    private String status; // draft, active, archived
    
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
    private Map<String, Object> performance;

    // Versioning fields
    @JsonProperty("version")
    private Long version = 1L;

    @JsonProperty("parentStrategyId")
    private String parentStrategyId; // Points to original strategy when versioned

    // Deployment tracking fields
    @JsonProperty("deploymentType")
    private String deploymentType; // ALERT, BOT, or null if not deployed

    @JsonProperty("deployedAt")
    private Timestamp deployedAt;

    @JsonProperty("deploymentId")
    private String deploymentId; // ID of StrategyAlert or StrategyBot

    // Constructors
    public Strategy() {
        super();
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    // Getters and Setters
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
    
    public Map<String, Object> getPerformance() {
        return performance;
    }
    
    public void setPerformance(Map<String, Object> performance) {
        this.performance = performance;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
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

    public Timestamp getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Timestamp deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    // Helper methods
    public boolean isDeployed() {
        return "deployed".equals(this.status) && this.deploymentId != null;
    }

    public boolean isAlertDeployment() {
        return "ALERT".equals(this.deploymentType);
    }

    public boolean isBotDeployment() {
        return "BOT".equals(this.deploymentType);
    }
}