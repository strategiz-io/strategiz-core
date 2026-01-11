package io.strategiz.service.mystrategies.model.response;

import io.strategiz.data.strategy.entity.StrategyPerformance;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response model for strategy with embedded deployment information.
 * Used by My Strategies page to display strategy-centric view with nested deployments.
 */
public class StrategyWithDeploymentsResponse {

    // Core strategy info
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    // Ownership
    @JsonProperty("userId")
    private String userId; // Current user's ID (for verification)

    @JsonProperty("ownerId")
    private String ownerId; // Strategy owner ID

    @JsonProperty("creatorId")
    private String creatorId; // Original creator ID

    @JsonProperty("isOwner")
    private Boolean isOwner; // True if current user is the owner

    @JsonProperty("isPurchased")
    private Boolean isPurchased; // True if current user purchased this

    // Publishing info
    @JsonProperty("isPublished")
    private Boolean isPublished;

    @JsonProperty("pricingType")
    private String pricingType; // FREE, ONE_TIME, SUBSCRIPTION

    @JsonProperty("price")
    private Double price;

    // Backtest performance
    @JsonProperty("performance")
    private StrategyPerformance performance;

    // Deployments (only current user's deployments)
    @JsonProperty("deployments")
    private DeploymentsDTO deployments;

    // Aggregated deployment statistics
    @JsonProperty("deploymentStats")
    private DeploymentStatsDTO deploymentStats;

    // Additional metadata
    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("language")
    private String language; // python, java, pinescript

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = isOwner;
    }

    public Boolean getIsPurchased() {
        return isPurchased;
    }

    public void setIsPurchased(Boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public StrategyPerformance getPerformance() {
        return performance;
    }

    public void setPerformance(StrategyPerformance performance) {
        this.performance = performance;
    }

    public DeploymentsDTO getDeployments() {
        return deployments;
    }

    public void setDeployments(DeploymentsDTO deployments) {
        this.deployments = deployments;
    }

    public DeploymentStatsDTO getDeploymentStats() {
        return deploymentStats;
    }

    public void setDeploymentStats(DeploymentStatsDTO deploymentStats) {
        this.deploymentStats = deploymentStats;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
