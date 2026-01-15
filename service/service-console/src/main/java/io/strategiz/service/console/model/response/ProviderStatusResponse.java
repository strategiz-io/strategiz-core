package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Response model for provider status information.
 */
public class ProviderStatusResponse {

	@JsonProperty("name")
	private String name;

	@JsonProperty("displayName")
	private String displayName;

	@JsonProperty("type")
	private String type; // CRYPTO, STOCK, OAUTH

	@JsonProperty("status")
	private String status; // UP, DOWN, DEGRADED

	@JsonProperty("activeIntegrations")
	private Integer activeIntegrations;

	@JsonProperty("lastSyncTime")
	private Instant lastSyncTime;

	@JsonProperty("lastSyncStatus")
	private String lastSyncStatus;

	@JsonProperty("errorRate")
	private Double errorRate;

	@JsonProperty("avgLatencyMs")
	private Long avgLatencyMs;

	// Constructors
	public ProviderStatusResponse() {
	}

	public ProviderStatusResponse(String name, String displayName, String type) {
		this.name = name;
		this.displayName = displayName;
		this.type = type;
		this.status = "UP";
	}

	// Getters and Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getActiveIntegrations() {
		return activeIntegrations;
	}

	public void setActiveIntegrations(Integer activeIntegrations) {
		this.activeIntegrations = activeIntegrations;
	}

	public Instant getLastSyncTime() {
		return lastSyncTime;
	}

	public void setLastSyncTime(Instant lastSyncTime) {
		this.lastSyncTime = lastSyncTime;
	}

	public String getLastSyncStatus() {
		return lastSyncStatus;
	}

	public void setLastSyncStatus(String lastSyncStatus) {
		this.lastSyncStatus = lastSyncStatus;
	}

	public Double getErrorRate() {
		return errorRate;
	}

	public void setErrorRate(Double errorRate) {
		this.errorRate = errorRate;
	}

	public Long getAvgLatencyMs() {
		return avgLatencyMs;
	}

	public void setAvgLatencyMs(Long avgLatencyMs) {
		this.avgLatencyMs = avgLatencyMs;
	}

}
