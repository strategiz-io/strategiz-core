package io.strategiz.business.aichat.costs.model;

import java.math.BigDecimal;

/**
 * Cost breakdown for a specific model within a provider.
 */
public class ModelCostBreakdown {

	private String modelId; // "gpt-4o", "claude-sonnet-4-5", etc.

	private String provider; // "openai", "anthropic", "google", "xai"

	private BigDecimal cost; // USD

	private long inputTokens;

	private long outputTokens;

	private long cachedTokens; // For providers that support caching

	private long requestCount;

	public ModelCostBreakdown() {
	}

	public ModelCostBreakdown(String modelId, String provider) {
		this.modelId = modelId;
		this.provider = provider;
		this.cost = BigDecimal.ZERO;
		this.inputTokens = 0;
		this.outputTokens = 0;
		this.cachedTokens = 0;
		this.requestCount = 0;
	}

	// Getters and setters

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public long getInputTokens() {
		return inputTokens;
	}

	public void setInputTokens(long inputTokens) {
		this.inputTokens = inputTokens;
	}

	public long getOutputTokens() {
		return outputTokens;
	}

	public void setOutputTokens(long outputTokens) {
		this.outputTokens = outputTokens;
	}

	public long getCachedTokens() {
		return cachedTokens;
	}

	public void setCachedTokens(long cachedTokens) {
		this.cachedTokens = cachedTokens;
	}

	public long getRequestCount() {
		return requestCount;
	}

	public void setRequestCount(long requestCount) {
		this.requestCount = requestCount;
	}

	/**
	 * Get total tokens (input + output)
	 */
	public long getTotalTokens() {
		return inputTokens + outputTokens;
	}

	/**
	 * Calculate average cost per request
	 */
	public BigDecimal getCostPerRequest() {
		if (requestCount == 0) {
			return BigDecimal.ZERO;
		}
		return cost.divide(BigDecimal.valueOf(requestCount), 6, java.math.RoundingMode.HALF_UP);
	}

	/**
	 * Calculate cost per 1K tokens
	 */
	public BigDecimal getCostPer1kTokens() {
		long total = getTotalTokens();
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		return cost.multiply(BigDecimal.valueOf(1000))
			.divide(BigDecimal.valueOf(total), 6, java.math.RoundingMode.HALF_UP);
	}

}
