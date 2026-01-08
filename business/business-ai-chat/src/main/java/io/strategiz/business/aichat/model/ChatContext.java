package io.strategiz.business.aichat.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Context information to enhance AI chat responses
 */
public class ChatContext {

	private String userId;

	private String feature; // "learn", "labs", "portfolio", etc.

	private String currentPage; // What page/section the user is viewing

	private String systemPrompt; // Optional override for system prompt

	private Map<String, Object> marketData; // Current market information

	private Map<String, Object> portfolioData; // User's portfolio information

	private Map<String, Object> userPreferences; // User settings and preferences

	private Map<String, Object> additionalContext; // Any other contextual data

	public ChatContext() {
		this.marketData = new HashMap<>();
		this.portfolioData = new HashMap<>();
		this.userPreferences = new HashMap<>();
		this.additionalContext = new HashMap<>();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public Map<String, Object> getMarketData() {
		return marketData;
	}

	public void setMarketData(Map<String, Object> marketData) {
		this.marketData = marketData;
	}

	public Map<String, Object> getPortfolioData() {
		return portfolioData;
	}

	public void setPortfolioData(Map<String, Object> portfolioData) {
		this.portfolioData = portfolioData;
	}

	public Map<String, Object> getUserPreferences() {
		return userPreferences;
	}

	public void setUserPreferences(Map<String, Object> userPreferences) {
		this.userPreferences = userPreferences;
	}

	public Map<String, Object> getAdditionalContext() {
		return additionalContext;
	}

	public void setAdditionalContext(Map<String, Object> additionalContext) {
		this.additionalContext = additionalContext;
	}

}
