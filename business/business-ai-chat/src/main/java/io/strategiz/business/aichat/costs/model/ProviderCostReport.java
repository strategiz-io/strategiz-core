package io.strategiz.business.aichat.costs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cost report from a single LLM provider for a date range.
 */
public class ProviderCostReport {

	private String provider; // "openai", "anthropic", "google", "xai"

	private LocalDate startDate;

	private LocalDate endDate;

	private BigDecimal totalCost; // USD

	private long totalTokens;

	private long inputTokens;

	private long outputTokens;

	private long cachedTokens; // Anthropic-specific (prompt caching)

	private long requestCount;

	private List<ModelCostBreakdown> byModel;

	public ProviderCostReport() {
	}

	public ProviderCostReport(String provider, LocalDate startDate, LocalDate endDate) {
		this.provider = provider;
		this.startDate = startDate;
		this.endDate = endDate;
		this.totalCost = BigDecimal.ZERO;
		this.totalTokens = 0;
		this.inputTokens = 0;
		this.outputTokens = 0;
		this.cachedTokens = 0;
		this.requestCount = 0;
	}

	// Getters and setters

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost;
	}

	public long getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(long totalTokens) {
		this.totalTokens = totalTokens;
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

	public List<ModelCostBreakdown> getByModel() {
		return byModel;
	}

	public void setByModel(List<ModelCostBreakdown> byModel) {
		this.byModel = byModel;
	}

	/**
	 * Calculate average cost per request
	 */
	public BigDecimal getAverageCostPerRequest() {
		if (requestCount == 0) {
			return BigDecimal.ZERO;
		}
		return totalCost.divide(BigDecimal.valueOf(requestCount), 6, java.math.RoundingMode.HALF_UP);
	}

	/**
	 * Calculate cost per 1K tokens
	 */
	public BigDecimal getCostPer1kTokens() {
		if (totalTokens == 0) {
			return BigDecimal.ZERO;
		}
		return totalCost.multiply(BigDecimal.valueOf(1000))
			.divide(BigDecimal.valueOf(totalTokens), 6, java.math.RoundingMode.HALF_UP);
	}

}
