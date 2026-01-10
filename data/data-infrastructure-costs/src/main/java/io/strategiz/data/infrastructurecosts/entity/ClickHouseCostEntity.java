package io.strategiz.data.infrastructurecosts.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ClickHouseDB cost data stored at infrastructure/costs/clickhouse/{date}
 * Documents monthly ClickHouseDB infrastructure costs for tracking and budgeting.
 */
@Collection("clickhouse")
public class ClickHouseCostEntity extends BaseEntity {

	@DocumentId
	@PropertyName("date")
	@JsonProperty("date")
	private String date; // Format: yyyy-MM-dd

	@PropertyName("totalCost")
	@JsonProperty("totalCost")
	private BigDecimal totalCost;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("computeCost")
	@JsonProperty("computeCost")
	private BigDecimal computeCost;

	@PropertyName("storageCost")
	@JsonProperty("storageCost")
	private BigDecimal storageCost;

	@PropertyName("storageUsageGb")
	@JsonProperty("storageUsageGb")
	private BigDecimal storageUsageGb;

	@PropertyName("computeHours")
	@JsonProperty("computeHours")
	private BigDecimal computeHours;

	@PropertyName("createdAt")
	@JsonProperty("createdAt")
	private Instant createdAt;

	@PropertyName("updatedAt")
	@JsonProperty("updatedAt")
	private Instant updatedAt;

	// Constructors
	public ClickHouseCostEntity() {
		super();
	}

	public ClickHouseCostEntity(String date) {
		super();
		this.date = date;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	// Getters and Setters
	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getComputeCost() {
		return computeCost;
	}

	public void setComputeCost(BigDecimal computeCost) {
		this.computeCost = computeCost;
	}

	public BigDecimal getStorageCost() {
		return storageCost;
	}

	public void setStorageCost(BigDecimal storageCost) {
		this.storageCost = storageCost;
	}

	public BigDecimal getStorageUsageGb() {
		return storageUsageGb;
	}

	public void setStorageUsageGb(BigDecimal storageUsageGb) {
		this.storageUsageGb = storageUsageGb;
	}

	public BigDecimal getComputeHours() {
		return computeHours;
	}

	public void setComputeHours(BigDecimal computeHours) {
		this.computeHours = computeHours;
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
		return date;
	}

	@Override
	public void setId(String id) {
		this.date = id;
	}

}
