package io.strategiz.service.learn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * DTO for chat API request
 */
public class ChatRequestDto {

	@JsonProperty("message")
	@NotBlank(message = "Message is required")
	private String message;

	@JsonProperty("feature")
	private String feature; // "learn", "labs", etc.

	@JsonProperty("currentPage")
	private String currentPage; // Current page/section user is viewing

	@JsonProperty("conversationHistory")
	private List<ChatMessageDto> conversationHistory;

	@JsonProperty("includeMarketContext")
	private Boolean includeMarketContext = false;

	@JsonProperty("includePortfolioContext")
	private Boolean includePortfolioContext = false;

	@JsonProperty("additionalContext")
	private Map<String, Object> additionalContext;

	@JsonProperty("model")
	private String model; // LLM model to use (e.g., "gemini-1.5-flash", "claude-3-5-sonnet")

	public ChatRequestDto() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	public String getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(String currentPage) {
		this.currentPage = currentPage;
	}

	public List<ChatMessageDto> getConversationHistory() {
		return conversationHistory;
	}

	public void setConversationHistory(List<ChatMessageDto> conversationHistory) {
		this.conversationHistory = conversationHistory;
	}

	public Boolean getIncludeMarketContext() {
		return includeMarketContext;
	}

	public void setIncludeMarketContext(Boolean includeMarketContext) {
		this.includeMarketContext = includeMarketContext;
	}

	public Boolean getIncludePortfolioContext() {
		return includePortfolioContext;
	}

	public void setIncludePortfolioContext(Boolean includePortfolioContext) {
		this.includePortfolioContext = includePortfolioContext;
	}

	public Map<String, Object> getAdditionalContext() {
		return additionalContext;
	}

	public void setAdditionalContext(Map<String, Object> additionalContext) {
		this.additionalContext = additionalContext;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

}
