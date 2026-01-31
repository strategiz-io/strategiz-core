package io.strategiz.data.infrastructurecosts.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Monthly cost aggregate stored at infrastructure/costs/monthly/{month} Documents monthly
 * costs with daily breakdown and totals.
 */
@Collection("monthly")
public class MonthlyCostEntity extends BaseEntity {

	@DocumentId
	@PropertyName("month")
	@JsonProperty("month")
	private String month; // Format: yyyy-MM

	@PropertyName("totalCost")
	@JsonProperty("totalCost")
	private BigDecimal totalCost;

	@PropertyName("gcpCost")
	@JsonProperty("gcpCost")
	private BigDecimal gcpCost;

	@PropertyName("timescaleCost")
	@JsonProperty("timescaleCost")
	private BigDecimal timescaleCost;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("costByService")
	@JsonProperty("costByService")
	private Map<String, BigDecimal> costByService;

	@PropertyName("totalFirestoreReads")
	@JsonProperty("totalFirestoreReads")
	private Long totalFirestoreReads;

	@PropertyName("totalFirestoreWrites")
	@JsonProperty("totalFirestoreWrites")
	private Long totalFirestoreWrites;

	@PropertyName("totalFirestoreDeletes")
	@JsonProperty("totalFirestoreDeletes")
	private Long totalFirestoreDeletes;

	@PropertyName("avgDailyCost")
	@JsonProperty("avgDailyCost")
	private BigDecimal avgDailyCost;

	@PropertyName("daysRecorded")
	@JsonProperty("daysRecorded")
	private Integer daysRecorded;

	@PropertyName("predictedMonthTotal")
	@JsonProperty("predictedMonthTotal")
	private BigDecimal predictedMonthTotal;

	@PropertyName("createdAt")
	@JsonProperty("createdAt")
	private Instant createdAt;

	@PropertyName("updatedAt")
	@JsonProperty("updatedAt")
	private Instant updatedAt;

	// Constructors
	public MonthlyCostEntity() {
		super();
	}

	public MonthlyCostEntity(String month) {
		super();
		this.month = month;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	// Getters and Setters
	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost;
	}

	public BigDecimal getGcpCost() {
		return gcpCost;
	}

	public void setGcpCost(BigDecimal gcpCost) {
		this.gcpCost = gcpCost;
	}

	public BigDecimal getTimescaleCost() {
		return timescaleCost;
	}

	public void setTimescaleCost(BigDecimal timescaleCost) {
		this.timescaleCost = timescaleCost;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Map<String, BigDecimal> getCostByService() {
		return costByService;
	}

	public void setCostByService(Map<String, BigDecimal> costByService) {
		this.costByService = costByService;
	}

	public Long getTotalFirestoreReads() {
		return totalFirestoreReads;
	}

	public void setTotalFirestoreReads(Long totalFirestoreReads) {
		this.totalFirestoreReads = totalFirestoreReads;
	}

	public Long getTotalFirestoreWrites() {
		return totalFirestoreWrites;
	}

	public void setTotalFirestoreWrites(Long totalFirestoreWrites) {
		this.totalFirestoreWrites = totalFirestoreWrites;
	}

	public Long getTotalFirestoreDeletes() {
		return totalFirestoreDeletes;
	}

	public void setTotalFirestoreDeletes(Long totalFirestoreDeletes) {
		this.totalFirestoreDeletes = totalFirestoreDeletes;
	}

	public BigDecimal getAvgDailyCost() {
		return avgDailyCost;
	}

	public void setAvgDailyCost(BigDecimal avgDailyCost) {
		this.avgDailyCost = avgDailyCost;
	}

	public Integer getDaysRecorded() {
		return daysRecorded;
	}

	public void setDaysRecorded(Integer daysRecorded) {
		this.daysRecorded = daysRecorded;
	}

	public BigDecimal getPredictedMonthTotal() {
		return predictedMonthTotal;
	}

	public void setPredictedMonthTotal(BigDecimal predictedMonthTotal) {
		this.predictedMonthTotal = predictedMonthTotal;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String getId() {
		return month;
	}

	@Override
	public void setId(String id) {
		this.month = id;
	}

}
