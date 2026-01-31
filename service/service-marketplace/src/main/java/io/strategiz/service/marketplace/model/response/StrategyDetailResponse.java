package io.strategiz.service.marketplace.model.response;

import io.strategiz.data.strategy.entity.StrategyPerformance;
import io.strategiz.data.strategy.entity.StrategyPricing;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive strategy detail response for strategy detail page. Includes all strategy
 * information with conditional inclusion based on access control.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyDetailResponse {

	// Basic Info
	@JsonProperty("id")
	private String id;

	@JsonProperty("name")
	private String name;

	@JsonProperty("description")
	private String description;

	@JsonProperty("language")
	private String language;

	@JsonProperty("type")
	private String type;

	@JsonProperty("category")
	private String category;

	@JsonProperty("tags")
	private List<String> tags;

	// Code & Configuration (conditionally included based on access)
	@JsonProperty("code")
	private String code; // Only if user is owner

	@JsonProperty("visualRules")
	private Map<String, Object> visualRules; // Only if user is owner

	@JsonProperty("parameters")
	private Map<String, Object> parameters;

	// Performance
	@JsonProperty("performance")
	private StrategyPerformance performance;

	// Backtest Data (conditionally included via query params)
	@JsonProperty("tradeHistory")
	private List<TradeHistoryItem> tradeHistory;

	@JsonProperty("equityCurve")
	private List<EquityCurvePoint> equityCurve;

	@JsonProperty("drawdownCurve")
	private List<DrawdownPoint> drawdownCurve;

	// Ownership & Creator Info
	@JsonProperty("creatorId")
	private String creatorId;

	@JsonProperty("ownerId")
	private String ownerId;

	@JsonProperty("creator")
	private CreatorInfo creator;

	@JsonProperty("owner")
	private CreatorInfo owner; // Only if different from creator

	// Visibility & Pricing
	@JsonProperty("isPublished")
	private Boolean isPublished;

	@JsonProperty("isPublic")
	private Boolean isPublic;

	@JsonProperty("isListed")
	private Boolean isListed;

	@JsonProperty("pricing")
	private StrategyPricing pricing;

	// Stats
	@JsonProperty("subscriberCount")
	private Integer subscriberCount;

	@JsonProperty("commentCount")
	private Integer commentCount;

	@JsonProperty("averageRating")
	private Double averageRating;

	@JsonProperty("reviewCount")
	private Integer reviewCount;

	@JsonProperty("deploymentCount")
	private Integer deploymentCount;

	// Badges
	@JsonProperty("isBestSeller")
	private Boolean isBestSeller;

	@JsonProperty("isTrending")
	private Boolean isTrending;

	@JsonProperty("isNew")
	private Boolean isNew;

	@JsonProperty("isFeatured")
	private Boolean isFeatured;

	// Access Control Flags (for conditional rendering on frontend)
	@JsonProperty("access")
	private AccessFlags access;

	// Comments (conditionally included via query params)
	@JsonProperty("comments")
	private CommentsResponse comments;

	// Timestamps
	@JsonProperty("createdAt")
	private Date createdAt;

	@JsonProperty("updatedAt")
	private Date updatedAt;

	// Nested Classes

	/**
	 * Creator/Owner information
	 */
	public static class CreatorInfo {

		@JsonProperty("userId")
		private String userId;

		@JsonProperty("name")
		private String name;

		@JsonProperty("email")
		private String email;

		@JsonProperty("photoURL")
		private String photoURL;

		// Constructors
		public CreatorInfo() {
		}

		public CreatorInfo(String userId, String name, String email, String photoURL) {
			this.userId = userId;
			this.name = name;
			this.email = email;
			this.photoURL = photoURL;
		}

		// Getters and Setters
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

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPhotoURL() {
			return photoURL;
		}

		public void setPhotoURL(String photoURL) {
			this.photoURL = photoURL;
		}

	}

	/**
	 * Access control flags for frontend conditional rendering
	 */
	public static class AccessFlags {

		@JsonProperty("isOwner")
		private Boolean isOwner;

		@JsonProperty("isSubscriber")
		private Boolean isSubscriber;

		@JsonProperty("canViewCode")
		private Boolean canViewCode;

		@JsonProperty("canDeploy")
		private Boolean canDeploy;

		@JsonProperty("canEdit")
		private Boolean canEdit;

		// Constructors
		public AccessFlags() {
		}

		public AccessFlags(Boolean isOwner, Boolean isSubscriber, Boolean canViewCode, Boolean canDeploy,
				Boolean canEdit) {
			this.isOwner = isOwner;
			this.isSubscriber = isSubscriber;
			this.canViewCode = canViewCode;
			this.canDeploy = canDeploy;
			this.canEdit = canEdit;
		}

		// Getters and Setters
		public Boolean getIsOwner() {
			return isOwner;
		}

		public void setIsOwner(Boolean isOwner) {
			this.isOwner = isOwner;
		}

		public Boolean getIsSubscriber() {
			return isSubscriber;
		}

		public void setIsSubscriber(Boolean isSubscriber) {
			this.isSubscriber = isSubscriber;
		}

		public Boolean getCanViewCode() {
			return canViewCode;
		}

		public void setCanViewCode(Boolean canViewCode) {
			this.canViewCode = canViewCode;
		}

		public Boolean getCanDeploy() {
			return canDeploy;
		}

		public void setCanDeploy(Boolean canDeploy) {
			this.canDeploy = canDeploy;
		}

		public Boolean getCanEdit() {
			return canEdit;
		}

		public void setCanEdit(Boolean canEdit) {
			this.canEdit = canEdit;
		}

	}

	/**
	 * Individual trade history item from backtest
	 */
	public static class TradeHistoryItem {

		@JsonProperty("entryTime")
		private Date entryTime;

		@JsonProperty("exitTime")
		private Date exitTime;

		@JsonProperty("direction")
		private String direction; // BUY or SELL

		@JsonProperty("entryPrice")
		private Double entryPrice;

		@JsonProperty("exitPrice")
		private Double exitPrice;

		@JsonProperty("quantity")
		private Double quantity;

		@JsonProperty("pnl")
		private Double pnl;

		@JsonProperty("pnlPercent")
		private Double pnlPercent;

		@JsonProperty("signal")
		private String signal; // Signal that triggered the trade

		// Constructors
		public TradeHistoryItem() {
		}

		// Getters and Setters
		public Date getEntryTime() {
			return entryTime;
		}

		public void setEntryTime(Date entryTime) {
			this.entryTime = entryTime;
		}

		public Date getExitTime() {
			return exitTime;
		}

		public void setExitTime(Date exitTime) {
			this.exitTime = exitTime;
		}

		public String getDirection() {
			return direction;
		}

		public void setDirection(String direction) {
			this.direction = direction;
		}

		public Double getEntryPrice() {
			return entryPrice;
		}

		public void setEntryPrice(Double entryPrice) {
			this.entryPrice = entryPrice;
		}

		public Double getExitPrice() {
			return exitPrice;
		}

		public void setExitPrice(Double exitPrice) {
			this.exitPrice = exitPrice;
		}

		public Double getQuantity() {
			return quantity;
		}

		public void setQuantity(Double quantity) {
			this.quantity = quantity;
		}

		public Double getPnl() {
			return pnl;
		}

		public void setPnl(Double pnl) {
			this.pnl = pnl;
		}

		public Double getPnlPercent() {
			return pnlPercent;
		}

		public void setPnlPercent(Double pnlPercent) {
			this.pnlPercent = pnlPercent;
		}

		public String getSignal() {
			return signal;
		}

		public void setSignal(String signal) {
			this.signal = signal;
		}

	}

	/**
	 * Equity curve data point for charting
	 */
	public static class EquityCurvePoint {

		@JsonProperty("timestamp")
		private Date timestamp;

		@JsonProperty("portfolioValue")
		private Double portfolioValue;

		@JsonProperty("returnPercent")
		private Double returnPercent;

		// Constructors
		public EquityCurvePoint() {
		}

		public EquityCurvePoint(Date timestamp, Double portfolioValue, Double returnPercent) {
			this.timestamp = timestamp;
			this.portfolioValue = portfolioValue;
			this.returnPercent = returnPercent;
		}

		// Getters and Setters
		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Double getPortfolioValue() {
			return portfolioValue;
		}

		public void setPortfolioValue(Double portfolioValue) {
			this.portfolioValue = portfolioValue;
		}

		public Double getReturnPercent() {
			return returnPercent;
		}

		public void setReturnPercent(Double returnPercent) {
			this.returnPercent = returnPercent;
		}

	}

	/**
	 * Drawdown data point for charting
	 */
	public static class DrawdownPoint {

		@JsonProperty("timestamp")
		private Date timestamp;

		@JsonProperty("drawdownPercent")
		private Double drawdownPercent;

		// Constructors
		public DrawdownPoint() {
		}

		public DrawdownPoint(Date timestamp, Double drawdownPercent) {
			this.timestamp = timestamp;
			this.drawdownPercent = drawdownPercent;
		}

		// Getters and Setters
		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Double getDrawdownPercent() {
			return drawdownPercent;
		}

		public void setDrawdownPercent(Double drawdownPercent) {
			this.drawdownPercent = drawdownPercent;
		}

	}

	/**
	 * Paginated comments response
	 */
	public static class CommentsResponse {

		@JsonProperty("comments")
		private List<CommentDto> comments;

		@JsonProperty("totalCount")
		private Integer totalCount;

		@JsonProperty("page")
		private Integer page;

		@JsonProperty("pageSize")
		private Integer pageSize;

		// Constructors
		public CommentsResponse() {
		}

		// Getters and Setters
		public List<CommentDto> getComments() {
			return comments;
		}

		public void setComments(List<CommentDto> comments) {
			this.comments = comments;
		}

		public Integer getTotalCount() {
			return totalCount;
		}

		public void setTotalCount(Integer totalCount) {
			this.totalCount = totalCount;
		}

		public Integer getPage() {
			return page;
		}

		public void setPage(Integer page) {
			this.page = page;
		}

		public Integer getPageSize() {
			return pageSize;
		}

		public void setPageSize(Integer pageSize) {
			this.pageSize = pageSize;
		}

	}

	/**
	 * Comment DTO (simplified for now)
	 */
	public static class CommentDto {

		@JsonProperty("id")
		private String id;

		@JsonProperty("userId")
		private String userId;

		@JsonProperty("userName")
		private String userName;

		@JsonProperty("userPhotoURL")
		private String userPhotoURL;

		@JsonProperty("content")
		private String content;

		@JsonProperty("rating")
		private Integer rating;

		@JsonProperty("createdAt")
		private Date createdAt;

		// Constructors
		public CommentDto() {
		}

		// Getters and Setters
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

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getUserPhotoURL() {
			return userPhotoURL;
		}

		public void setUserPhotoURL(String userPhotoURL) {
			this.userPhotoURL = userPhotoURL;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Integer getRating() {
			return rating;
		}

		public void setRating(Integer rating) {
			this.rating = rating;
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Date createdAt) {
			this.createdAt = createdAt;
		}

	}

	// Main Class Getters and Setters

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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Map<String, Object> getVisualRules() {
		return visualRules;
	}

	public void setVisualRules(Map<String, Object> visualRules) {
		this.visualRules = visualRules;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public StrategyPerformance getPerformance() {
		return performance;
	}

	public void setPerformance(StrategyPerformance performance) {
		this.performance = performance;
	}

	public List<TradeHistoryItem> getTradeHistory() {
		return tradeHistory;
	}

	public void setTradeHistory(List<TradeHistoryItem> tradeHistory) {
		this.tradeHistory = tradeHistory;
	}

	public List<EquityCurvePoint> getEquityCurve() {
		return equityCurve;
	}

	public void setEquityCurve(List<EquityCurvePoint> equityCurve) {
		this.equityCurve = equityCurve;
	}

	public List<DrawdownPoint> getDrawdownCurve() {
		return drawdownCurve;
	}

	public void setDrawdownCurve(List<DrawdownPoint> drawdownCurve) {
		this.drawdownCurve = drawdownCurve;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public CreatorInfo getCreator() {
		return creator;
	}

	public void setCreator(CreatorInfo creator) {
		this.creator = creator;
	}

	public CreatorInfo getOwner() {
		return owner;
	}

	public void setOwner(CreatorInfo owner) {
		this.owner = owner;
	}

	public Boolean getIsPublished() {
		return isPublished;
	}

	public void setIsPublished(Boolean isPublished) {
		this.isPublished = isPublished;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getIsListed() {
		return isListed;
	}

	public void setIsListed(Boolean isListed) {
		this.isListed = isListed;
	}

	public StrategyPricing getPricing() {
		return pricing;
	}

	public void setPricing(StrategyPricing pricing) {
		this.pricing = pricing;
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

	public AccessFlags getAccess() {
		return access;
	}

	public void setAccess(AccessFlags access) {
		this.access = access;
	}

	public CommentsResponse getComments() {
		return comments;
	}

	public void setComments(CommentsResponse comments) {
		this.comments = comments;
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

}
