package io.strategiz.data.infrastructurecosts.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ClickHouseDB usage metrics stored at infrastructure/usage/clickhouse/{date} Documents
 * usage statistics for capacity planning and monitoring.
 */
@Collection("clickhouse")
public class ClickHouseUsageEntity extends BaseEntity {

	@DocumentId
	@PropertyName("date")
	@JsonProperty("date")
	private String date; // Format: yyyy-MM-dd

	@PropertyName("serviceId")
	@JsonProperty("serviceId")
	private String serviceId;

	@PropertyName("serviceName")
	@JsonProperty("serviceName")
	private String serviceName;

	@PropertyName("storageUsedGb")
	@JsonProperty("storageUsedGb")
	private BigDecimal storageUsedGb;

	@PropertyName("computeHours")
	@JsonProperty("computeHours")
	private BigDecimal computeHours;

	@PropertyName("dataIngestedGb")
	@JsonProperty("dataIngestedGb")
	private BigDecimal dataIngestedGb;

	@PropertyName("queriesExecuted")
	@JsonProperty("queriesExecuted")
	private BigDecimal queriesExecuted;

	@PropertyName("createdAt")
	@JsonProperty("createdAt")
	private Instant createdAt;

	@PropertyName("updatedAt")
	@JsonProperty("updatedAt")
	private Instant updatedAt;

	// Constructors
	public ClickHouseUsageEntity() {
		super();
	}

	public ClickHouseUsageEntity(String date) {
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

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public BigDecimal getStorageUsedGb() {
		return storageUsedGb;
	}

	public void setStorageUsedGb(BigDecimal storageUsedGb) {
		this.storageUsedGb = storageUsedGb;
	}

	public BigDecimal getComputeHours() {
		return computeHours;
	}

	public void setComputeHours(BigDecimal computeHours) {
		this.computeHours = computeHours;
	}

	public BigDecimal getDataIngestedGb() {
		return dataIngestedGb;
	}

	public void setDataIngestedGb(BigDecimal dataIngestedGb) {
		this.dataIngestedGb = dataIngestedGb;
	}

	public BigDecimal getQueriesExecuted() {
		return queriesExecuted;
	}

	public void setQueriesExecuted(BigDecimal queriesExecuted) {
		this.queriesExecuted = queriesExecuted;
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
