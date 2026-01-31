package io.strategiz.data.provider.entity;

import io.strategiz.data.base.entity.BaseEntity;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a provider connection in the user's portfolio. This is a
 * lightweight document for fast queries (Connect Providers page).
 *
 * Firestore path: users/{userId}/portfolio/providers/{providerId}
 *
 * Heavy data (holdings, transactions) is stored in subcollection:
 * users/{userId}/portfolio/providers/{providerId}/holdings/current
 */
public class PortfolioProviderEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("providerId")
	@JsonProperty("providerId")
	@NotBlank(message = "Provider ID is required")
	private String providerId; // e.g., "coinbase", "kraken", "alpaca"

	@PropertyName("providerName")
	@JsonProperty("providerName")
	private String providerName; // e.g., "Coinbase", "Kraken", "Alpaca"

	@PropertyName("providerType")
	@JsonProperty("providerType")
	private String providerType; // crypto, equity, forex

	@PropertyName("providerCategory")
	@JsonProperty("providerCategory")
	private String providerCategory; // exchange, brokerage

	@PropertyName("status")
	@JsonProperty("status")
	private String status = "connected"; // connected, disconnected, error

	@PropertyName("connectionType")
	@JsonProperty("connectionType")
	@NotBlank(message = "Connection type is required")
	private String connectionType; // oauth, api_key

	@PropertyName("environment")
	@JsonProperty("environment")
	private String environment; // live, paper (for providers like Alpaca)

	@PropertyName("errorMessage")
	@JsonProperty("errorMessage")
	private String errorMessage;

	// Summary fields for dashboard (denormalized for fast reads)
	@PropertyName("totalValue")
	@JsonProperty("totalValue")
	private BigDecimal totalValue;

	@PropertyName("dayChange")
	@JsonProperty("dayChange")
	private BigDecimal dayChange;

	@PropertyName("dayChangePercent")
	@JsonProperty("dayChangePercent")
	private BigDecimal dayChangePercent;

	@PropertyName("holdingsCount")
	@JsonProperty("holdingsCount")
	private Integer holdingsCount;

	@PropertyName("cashBalance")
	@JsonProperty("cashBalance")
	private BigDecimal cashBalance;

	@PropertyName("lastSyncedAt")
	@JsonProperty("lastSyncedAt")
	private Instant lastSyncedAt;

	@PropertyName("syncStatus")
	@JsonProperty("syncStatus")
	private String syncStatus; // success, error, syncing

	// Constructors
	public PortfolioProviderEntity() {
		super();
	}

	public PortfolioProviderEntity(String providerId, String connectionType, String userId) {
		super(userId);
		this.providerId = providerId;
		this.id = providerId; // Use providerId as document ID
		this.connectionType = connectionType;
		this.status = "connected";
	}

	// Status helper methods
	public void disconnect() {
		this.status = "disconnected";
	}

	public void setError(String errorMessage) {
		this.status = "error";
		this.errorMessage = errorMessage;
	}

	public boolean isConnected() {
		return "connected".equalsIgnoreCase(this.status) && Boolean.TRUE.equals(getIsActive());
	}

	// Getters and Setters
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getProviderType() {
		return providerType;
	}

	public void setProviderType(String providerType) {
		this.providerType = providerType;
	}

	public String getProviderCategory() {
		return providerCategory;
	}

	public void setProviderCategory(String providerCategory) {
		this.providerCategory = providerCategory;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public BigDecimal getDayChange() {
		return dayChange;
	}

	public void setDayChange(BigDecimal dayChange) {
		this.dayChange = dayChange;
	}

	public BigDecimal getDayChangePercent() {
		return dayChangePercent;
	}

	public void setDayChangePercent(BigDecimal dayChangePercent) {
		this.dayChangePercent = dayChangePercent;
	}

	public Integer getHoldingsCount() {
		return holdingsCount;
	}

	public void setHoldingsCount(Integer holdingsCount) {
		this.holdingsCount = holdingsCount;
	}

	public BigDecimal getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(BigDecimal cashBalance) {
		this.cashBalance = cashBalance;
	}

	public Instant getLastSyncedAt() {
		return lastSyncedAt;
	}

	public void setLastSyncedAt(Instant lastSyncedAt) {
		this.lastSyncedAt = lastSyncedAt;
	}

	public String getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}

}
