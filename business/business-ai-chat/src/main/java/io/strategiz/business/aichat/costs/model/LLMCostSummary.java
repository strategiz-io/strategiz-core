package io.strategiz.business.aichat.costs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Summary of LLM costs for the console dashboard.
 */
public class LLMCostSummary {

	private LocalDate startDate;

	private LocalDate endDate;

	private BigDecimal totalCost;

	private BigDecimal previousPeriodCost; // For comparison

	private BigDecimal costChange; // Percentage change

	private long totalTokens;

	private long totalRequests;

	private BigDecimal averageCostPerRequest;

	private BigDecimal averageCostPer1kTokens;

	private String topModel; // Model with highest cost

	private BigDecimal topModelCost;

	private Map<String, BigDecimal> costByProvider;

	private List<ModelCostBreakdown> topModels; // Top N models by cost

	public LLMCostSummary() {
	}

	// Getters and setters

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

	public BigDecimal getPreviousPeriodCost() {
		return previousPeriodCost;
	}

	public void setPreviousPeriodCost(BigDecimal previousPeriodCost) {
		this.previousPeriodCost = previousPeriodCost;
	}

	public BigDecimal getCostChange() {
		return costChange;
	}

	public void setCostChange(BigDecimal costChange) {
		this.costChange = costChange;
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

	public BigDecimal getAverageCostPerRequest() {
		return averageCostPerRequest;
	}

	public void setAverageCostPerRequest(BigDecimal averageCostPerRequest) {
		this.averageCostPerRequest = averageCostPerRequest;
	}

	public BigDecimal getAverageCostPer1kTokens() {
		return averageCostPer1kTokens;
	}

	public void setAverageCostPer1kTokens(BigDecimal averageCostPer1kTokens) {
		this.averageCostPer1kTokens = averageCostPer1kTokens;
	}

	public String getTopModel() {
		return topModel;
	}

	public void setTopModel(String topModel) {
		this.topModel = topModel;
	}

	public BigDecimal getTopModelCost() {
		return topModelCost;
	}

	public void setTopModelCost(BigDecimal topModelCost) {
		this.topModelCost = topModelCost;
	}

	public Map<String, BigDecimal> getCostByProvider() {
		return costByProvider;
	}

	public void setCostByProvider(Map<String, BigDecimal> costByProvider) {
		this.costByProvider = costByProvider;
	}

	public List<ModelCostBreakdown> getTopModels() {
		return topModels;
	}

	public void setTopModels(List<ModelCostBreakdown> topModels) {
		this.topModels = topModels;
	}

}
