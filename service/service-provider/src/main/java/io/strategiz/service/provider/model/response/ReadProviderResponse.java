package io.strategiz.service.provider.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response model for reading provider data. Contains provider information, connection
 * status, and requested data.
 */
public class ReadProviderResponse {

	// Provider identification
	private String providerId;

	private String providerName;

	private String connectionType; // "oauth", "api_key"

	private String status; // "connected", "disconnected", "pending", "error"

	// Connection metadata
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant connectedAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant lastSyncAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant tokenExpiresAt;

	// Account information
	private String accountType; // "paper", "live"

	private String accountId;

	private Map<String, Object> accountInfo;

	// Data payload (varies based on request)
	private Map<String, Object> balanceData;

	private List<Map<String, Object>> transactions;

	private List<Map<String, Object>> orders;

	private List<Map<String, Object>> positions;

	// Pagination info (for lists)
	private Integer page;

	private Integer limit;

	private Integer totalCount;

	private Boolean hasMore;

	// Provider capabilities
	private List<String> supportedFeatures;

	private Map<String, Object> rateLimits;

	// Error information (if status is "error")
	private String errorCode;

	private String errorMessage;

	private String errorDetails;

	// Response metadata
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant responseTimestamp;

	private Long responseTimeMs;

	private Map<String, Object> metadata;

	// Constructors
	public ReadProviderResponse() {
		this.responseTimestamp = Instant.now();
	}

	public ReadProviderResponse(String providerId, String providerName) {
		this.providerId = providerId;
		this.providerName = providerName;
		this.responseTimestamp = Instant.now();
	}

	// Getters and Setters
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

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public void setConnectedAt(Instant connectedAt) {
		this.connectedAt = connectedAt;
	}

	public Instant getLastSyncAt() {
		return lastSyncAt;
	}

	public void setLastSyncAt(Instant lastSyncAt) {
		this.lastSyncAt = lastSyncAt;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(Instant tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public Map<String, Object> getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(Map<String, Object> accountInfo) {
		this.accountInfo = accountInfo;
	}

	public Map<String, Object> getBalanceData() {
		return balanceData;
	}

	public void setBalanceData(Map<String, Object> balanceData) {
		this.balanceData = balanceData;
	}

	public List<Map<String, Object>> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Map<String, Object>> transactions) {
		this.transactions = transactions;
	}

	public List<Map<String, Object>> getOrders() {
		return orders;
	}

	public void setOrders(List<Map<String, Object>> orders) {
		this.orders = orders;
	}

	public List<Map<String, Object>> getPositions() {
		return positions;
	}

	public void setPositions(List<Map<String, Object>> positions) {
		this.positions = positions;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Boolean getHasMore() {
		return hasMore;
	}

	public void setHasMore(Boolean hasMore) {
		this.hasMore = hasMore;
	}

	public List<String> getSupportedFeatures() {
		return supportedFeatures;
	}

	public void setSupportedFeatures(List<String> supportedFeatures) {
		this.supportedFeatures = supportedFeatures;
	}

	public Map<String, Object> getRateLimits() {
		return rateLimits;
	}

	public void setRateLimits(Map<String, Object> rateLimits) {
		this.rateLimits = rateLimits;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}

	public Instant getResponseTimestamp() {
		return responseTimestamp;
	}

	public void setResponseTimestamp(Instant responseTimestamp) {
		this.responseTimestamp = responseTimestamp;
	}

	public Long getResponseTimeMs() {
		return responseTimeMs;
	}

	public void setResponseTimeMs(Long responseTimeMs) {
		this.responseTimeMs = responseTimeMs;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	// Helper methods - removed isConnected() to avoid JSON field duplication
	// Use getStatus().equals("connected") instead

	public boolean hasError() {
		return "error".equals(status);
	}

	public boolean isPaginated() {
		return page != null && limit != null;
	}

	public boolean hasBalance() {
		return balanceData != null && !balanceData.isEmpty();
	}

	public boolean hasTransactions() {
		return transactions != null && !transactions.isEmpty();
	}

	public boolean hasOrders() {
		return orders != null && !orders.isEmpty();
	}

	public boolean hasPositions() {
		return positions != null && !positions.isEmpty();
	}

	public boolean isTokenExpired() {
		return tokenExpiresAt != null && Instant.now().isAfter(tokenExpiresAt);
	}

	@Override
	public String toString() {
		return "ReadProviderResponse{" + "providerId='" + providerId + '\'' + ", providerName='" + providerName + '\''
				+ ", status='" + status + '\'' + ", accountType='" + accountType + '\'' + ", hasBalance=" + hasBalance()
				+ ", hasTransactions=" + hasTransactions() + ", hasOrders=" + hasOrders() + ", hasPositions="
				+ hasPositions() + ", responseTimestamp=" + responseTimestamp + '}';
	}

}