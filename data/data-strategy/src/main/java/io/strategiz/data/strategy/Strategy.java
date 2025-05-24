package io.strategiz.data.strategy;

import java.util.List;
import java.util.Map;

/**
 * Model class for user-created trading strategies
 */
public class Strategy {
    private String id;
    private String userId;
    private String name;
    private String description;
    private String code;
    private String language; // python or java
    private String status; // DRAFT, TESTING, LIVE
    private String mode; // EXPERIMENTAL, PAPER_TRADING, LIVE_TRADING
    private String type; // CUSTOM, MEAN_REVERSION, TREND_FOLLOWING, etc.
    private List<String> tags;
    private Map<String, Object> parameters;
    private Map<String, Object> deployment;
    private Map<String, Object> backtestResults;
    private String createdAt;
    private String updatedAt;

    // Default constructor for Firebase
    public Strategy() {}

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getDeployment() {
        return deployment;
    }

    public void setDeployment(Map<String, Object> deployment) {
        this.deployment = deployment;
    }
    
    public Map<String, Object> getBacktestResults() {
        return backtestResults;
    }

    public void setBacktestResults(Map<String, Object> backtestResults) {
        this.backtestResults = backtestResults;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
