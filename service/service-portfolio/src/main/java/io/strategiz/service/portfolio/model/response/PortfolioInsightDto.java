package io.strategiz.service.portfolio.model.response;

import java.util.List;

/**
 * Response DTO for portfolio AI insights. Contains a generated insight with metadata for
 * display in the UI.
 */
public class PortfolioInsightDto {

	private String id;

	private String type; // RISK, PERFORMANCE, REBALANCING, OPPORTUNITIES

	private String title;

	private String summary; // 1-2 sentence overview

	private String content; // Full markdown analysis

	private String riskLevel; // LOW, MEDIUM, HIGH (for risk insights)

	private List<ActionItem> actionItems; // Specific recommendations

	private Long generatedAt; // Unix timestamp

	private String model; // Which LLM generated this

	private Boolean success;

	private String error;

	/**
	 * Specific action recommendation within an insight
	 */
	public static class ActionItem {

		private String action; // e.g., "Reduce BTC allocation by 10%"

		private String rationale; // Why this action is recommended

		private String priority; // HIGH, MEDIUM, LOW

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

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Long getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Long generatedAt) {
		this.generatedAt = generatedAt;
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
