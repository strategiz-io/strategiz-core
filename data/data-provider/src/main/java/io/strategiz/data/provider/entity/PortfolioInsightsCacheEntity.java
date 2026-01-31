package io.strategiz.data.provider.entity;

import io.strategiz.data.base.entity.BaseEntity;
import com.google.cloud.Timestamp;

import java.util.List;

/**
 * Entity to cache AI-generated portfolio insights. Stored at:
 * users/{userId}/portfolio/insights/cached
 *
 * Cache is invalidated when: - Provider sync occurs (new data fetched) - Provider is
 * connected - Provider is disconnected - TTL expires (24 hours as fallback)
 */
public class PortfolioInsightsCacheEntity extends BaseEntity {

	/**
	 * Document ID (typically "cached" since this is a singleton per user)
	 */
	private String id;

	/**
	 * List of cached insight objects
	 */
	private List<CachedInsight> insights;

	/**
	 * Timestamp when cache was generated
	 */
	private Timestamp generatedAt;

	/**
	 * Timestamp when cache expires (24 hours after generation)
	 */
	private Timestamp expiresAt;

	/**
	 * Model used to generate insights
	 */
	private String model;

	/**
	 * Hash of portfolio data at time of generation (for validation)
	 */
	private String portfolioHash;

	/**
	 * Whether cache is valid (not expired and portfolio hasn't changed)
	 */
	private Boolean isValid;

	// Default constructor
	public PortfolioInsightsCacheEntity() {
	}

	// Abstract method implementations from BaseEntity
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	// Getters and setters
	public List<CachedInsight> getInsights() {
		return insights;
	}

	public void setInsights(List<CachedInsight> insights) {
		this.insights = insights;
	}

	public Timestamp getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Timestamp generatedAt) {
		this.generatedAt = generatedAt;
	}

	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Timestamp expiresAt) {
		this.expiresAt = expiresAt;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getPortfolioHash() {
		return portfolioHash;
	}

	public void setPortfolioHash(String portfolioHash) {
		this.portfolioHash = portfolioHash;
	}

	public Boolean getIsValid() {
		return isValid;
	}

	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}

	/**
	 * Check if cache is still valid (not expired)
	 */
	public boolean isCacheValid() {
		if (Boolean.FALSE.equals(isValid)) {
			return false;
		}
		if (expiresAt == null) {
			return false;
		}
		return Timestamp.now().compareTo(expiresAt) < 0;
	}

	/**
	 * Represents a single cached insight
	 */
	public static class CachedInsight {

		private String type;

		private String title;

		private String summary;

		private String content;

		private String riskLevel;

		private List<ActionItem> actionItems;

		private Long generatedAtEpoch;

		private String model;

		private Boolean success;

		private String error;

		public CachedInsight() {
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getRiskLevel() {
			return riskLevel;
		}

		public void setRiskLevel(String riskLevel) {
			this.riskLevel = riskLevel;
		}

		public List<ActionItem> getActionItems() {
			return actionItems;
		}

		public void setActionItems(List<ActionItem> actionItems) {
			this.actionItems = actionItems;
		}

		public Long getGeneratedAtEpoch() {
			return generatedAtEpoch;
		}

		public void setGeneratedAtEpoch(Long generatedAtEpoch) {
			this.generatedAtEpoch = generatedAtEpoch;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public Boolean getSuccess() {
			return success;
		}

		public void setSuccess(Boolean success) {
			this.success = success;
		}

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}

	}

	/**
	 * Represents an action item within an insight
	 */
	public static class ActionItem {

		private String action;

		private String rationale;

		private String priority;

		public ActionItem() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getRationale() {
			return rationale;
		}

		public void setRationale(String rationale) {
			this.rationale = rationale;
		}

		public String getPriority() {
			return priority;
		}

		public void setPriority(String priority) {
			this.priority = priority;
		}

	}

}
