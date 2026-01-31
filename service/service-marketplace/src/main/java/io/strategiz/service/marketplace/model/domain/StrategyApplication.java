package io.strategiz.service.marketplace.model.domain;

import java.util.Map;

/**
 * StrategyApplication model - represents a user's application of a purchased strategy
 * Stored in the 'strategy_applications' subcollection under each user document
 */
public class StrategyApplication {

	private String id;

	private String strategyId;

	private String strategyName;

	private String strategyVersion;

	private String userId;

	private String exchangeId; // e.g., "binanceus", "kraken"

	private boolean isActive;

	private Map<String, Object> configuration;

	private long appliedAt;

	private long lastRunAt;

	private String status; // "active", "paused", "error"

	private String errorMessage;

	private Map<String, Object> results;

	private Map<String, Object> metadata;

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getStrategyVersion() {
		return strategyVersion;
	}

	public void setStrategyVersion(String strategyVersion) {
		this.strategyVersion = strategyVersion;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getExchangeId() {
		return exchangeId;
	}

	public void setExchangeId(String exchangeId) {
		this.exchangeId = exchangeId;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	public long getAppliedAt() {
		return appliedAt;
	}

	public void setAppliedAt(long appliedAt) {
		this.appliedAt = appliedAt;
	}

	public long getLastRunAt() {
		return lastRunAt;
	}

	public void setLastRunAt(long lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Map<String, Object> getResults() {
		return results;
	}

	public void setResults(Map<String, Object> results) {
		this.results = results;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}