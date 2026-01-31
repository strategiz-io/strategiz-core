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
 * Daily cost snapshot stored at infrastructure/costs/daily/{date} Documents daily costs
 * by service.
 */
@Collection("daily")
public class DailyCostEntity extends BaseEntity {

	@DocumentId
	@PropertyName("date")
	@JsonProperty("date")
	private String date; // Format: yyyy-MM-dd

	@PropertyName("totalCost")
	@JsonProperty("totalCost")
	private BigDecimal totalCost;

	@PropertyName("gcpCost")
	@JsonProperty("gcpCost")
	private BigDecimal gcpCost;

	@PropertyName("clickhouseCost")
	@JsonProperty("clickhouseCost")
	private BigDecimal clickhouseCost;

	@PropertyName("currency")
	@JsonProperty("currency")
	private String currency = "USD";

	@PropertyName("costByService")
	@JsonProperty("costByService")
	private Map<String, BigDecimal> costByService;

	@PropertyName("firestoreReads")
	@JsonProperty("firestoreReads")
	private Long firestoreReads;

	@PropertyName("firestoreWrites")
	@JsonProperty("firestoreWrites")
	private Long firestoreWrites;

	@PropertyName("firestoreDeletes")
	@JsonProperty("firestoreDeletes")
	private Long firestoreDeletes;

	@PropertyName("createdAt")
	@JsonProperty("createdAt")
	private Instant createdAt;

	@PropertyName("updatedAt")
	@JsonProperty("updatedAt")
	private Instant updatedAt;

	// Constructors
	public DailyCostEntity() {
		super();
	}

	public DailyCostEntity(String date) {
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

	public BigDecimal getGcpCost() {
		return gcpCost;
	}

	public void setGcpCost(BigDecimal gcpCost) {
		this.gcpCost = gcpCost;
	}

	public BigDecimal getClickhouseCost() {
		return clickhouseCost;
	}

	public void setClickhouseCost(BigDecimal clickhouseCost) {
		this.clickhouseCost = clickhouseCost;
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

	public Long getFirestoreReads() {
		return firestoreReads;
	}

	public void setFirestoreReads(Long firestoreReads) {
		this.firestoreReads = firestoreReads;
	}

	public Long getFirestoreWrites() {
		return firestoreWrites;
	}

	public void setFirestoreWrites(Long firestoreWrites) {
		this.firestoreWrites = firestoreWrites;
	}

	public Long getFirestoreDeletes() {
		return firestoreDeletes;
	}

	public void setFirestoreDeletes(Long firestoreDeletes) {
		this.firestoreDeletes = firestoreDeletes;
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
