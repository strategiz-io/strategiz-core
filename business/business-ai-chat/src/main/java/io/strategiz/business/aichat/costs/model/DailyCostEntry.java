package io.strategiz.business.aichat.costs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Daily aggregated cost entry across all providers.
 */
public class DailyCostEntry {

	private LocalDate date;

	private BigDecimal totalCost;

	private long totalTokens;

	private long totalRequests;

	private Map<String, BigDecimal> costByProvider; // provider -> cost

	private Map<String, BigDecimal> costByModel; // modelId -> cost

	public DailyCostEntry() {
		this.costByProvider = new HashMap<>();
		this.costByModel = new HashMap<>();
	}

	public DailyCostEntry(LocalDate date) {
		this.date = date;
		this.totalCost = BigDecimal.ZERO;
		this.totalTokens = 0;
		this.totalRequests = 0;
		this.costByProvider = new HashMap<>();
		this.costByModel = new HashMap<>();
	}

	// Getters and setters

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
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

	public long getTotalRequests() {
		return totalRequests;
	}

	public void setTotalRequests(long totalRequests) {
		this.totalRequests = totalRequests;
	}

	public Map<String, BigDecimal> getCostByProvider() {
		return costByProvider;
	}

	public void setCostByProvider(Map<String, BigDecimal> costByProvider) {
		this.costByProvider = costByProvider;
	}

	public Map<String, BigDecimal> getCostByModel() {
		return costByModel;
	}

	public void setCostByModel(Map<String, BigDecimal> costByModel) {
		this.costByModel = costByModel;
	}

	/**
	 * Add cost for a provider
	 */
	public void addProviderCost(String provider, BigDecimal cost) {
		this.costByProvider.merge(provider, cost, BigDecimal::add);
		this.totalCost = this.totalCost.add(cost);
	}

	/**
	 * Add cost for a model
	 */
	public void addModelCost(String modelId, BigDecimal cost) {
		this.costByModel.merge(modelId, cost, BigDecimal::add);
	}

}
