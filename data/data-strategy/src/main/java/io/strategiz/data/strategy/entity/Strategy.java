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

    @JsonProperty("timeframe")
    private String timeframe; // "1Min", "5Min", "15Min", "1H", "4H", "1D", "1W", "1M"

    @JsonProperty("type")
    private String type; // technical, fundamental, hybrid
    
    @JsonProperty("status")
    private String status; // draft, active, archived
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("backtestResults")
    private Map<String, Object> backtestResults; // Deprecated - use performance instead

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("isPublic")
    private boolean isPublic;

    @JsonProperty("performance")
    private StrategyPerformance performance;

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

    // Pricing and Publishing fields
    @JsonProperty("pricing")
    private StrategyPricing pricing;

    @JsonProperty("publishedAt")
    private Timestamp publishedAt; // null = private, non-null = public/published

    // Marketplace stats (denormalized for display)
    @JsonProperty("subscriberCount")
    private Integer subscriberCount = 0;

    @JsonProperty("deploymentCount")
    private Integer deploymentCount = 0;

    @JsonProperty("commentCount")
    private Integer commentCount = 0;

    @JsonProperty("averageRating")
    private Double averageRating;

    @JsonProperty("reviewCount")
    private Integer reviewCount = 0;

    @JsonProperty("category")
    private String category; // momentum, mean-reversion, trend, volatility, hybrid, ai

    // Marketplace badges
    @JsonProperty("isBestSeller")
    private Boolean isBestSeller = false;

    @JsonProperty("isTrending")
    private Boolean isTrending = false;

    @JsonProperty("isNew")
    private Boolean isNew = false;

    @JsonProperty("isFeatured")
    private Boolean isFeatured = false;

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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
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

    // Pricing and Publishing getters/setters
    public StrategyPricing getPricing() {
        return pricing;
    }

    public void setPricing(StrategyPricing pricing) {
        this.pricing = pricing;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(Integer subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getDeploymentCount() {
        return deploymentCount;
    }

    public void setDeploymentCount(Integer deploymentCount) {
        this.deploymentCount = deploymentCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsBestSeller() {
        return isBestSeller;
    }

    public void setIsBestSeller(Boolean isBestSeller) {
        this.isBestSeller = isBestSeller;
    }

    public Boolean getIsTrending() {
        return isTrending;
    }

    public void setIsTrending(Boolean isTrending) {
        this.isTrending = isTrending;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
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

    /**
     * Check if strategy is published (publicly visible in marketplace).
     */
    public boolean isPublished() {
        return publishedAt != null;
    }

    /**
     * Check if strategy is private (not published).
     */
    public boolean isPrivate() {
        return publishedAt == null;
    }

    /**
     * Publish the strategy with the given pricing.
     */
    public void publish(StrategyPricing pricing) {
        this.pricing = pricing;
        this.publishedAt = Timestamp.now();
        this.isPublic = true;
    }

    /**
     * Unpublish the strategy (make private).
     */
    public void unpublish() {
        this.publishedAt = null;
        this.isPublic = false;
    }

    /**
     * Check if strategy is free to use.
     */
    public boolean isFree() {
        return pricing == null || pricing.isFree();
    }

    /**
     * Increment subscriber count.
     */
    public void incrementSubscribers() {
        this.subscriberCount = (this.subscriberCount != null ? this.subscriberCount : 0) + 1;
    }

    /**
     * Decrement subscriber count.
     */
    public void decrementSubscribers() {
        if (this.subscriberCount != null && this.subscriberCount > 0) {
            this.subscriberCount--;
        }
    }

    /**
     * Increment deployment count.
     */
    public void incrementDeployments() {
        this.deploymentCount = (this.deploymentCount != null ? this.deploymentCount : 0) + 1;
    }

    /**
     * Decrement deployment count.
     */
    public void decrementDeployments() {
        if (this.deploymentCount != null && this.deploymentCount > 0) {
            this.deploymentCount--;
        }
    }

    /**
     * Increment comment count.
     */
    public void incrementComments() {
        this.commentCount = (this.commentCount != null ? this.commentCount : 0) + 1;
    }

    /**
     * Decrement comment count.
     */
    public void decrementComments() {
        if (this.commentCount != null && this.commentCount > 0) {
            this.commentCount--;
        }
    }
}