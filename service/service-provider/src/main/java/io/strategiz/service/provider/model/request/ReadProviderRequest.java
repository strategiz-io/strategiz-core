package io.strategiz.service.provider.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request model for reading provider data. Used for querying provider information,
 * balances, transactions, etc.
 */
public class ReadProviderRequest {

	@NotBlank(message = "Provider ID is required when reading specific provider")
	private String providerId;

	@NotBlank(message = "User ID is required")
	private String userId;

	// Optional: Status filter
	@Pattern(regexp = "^(connected|disconnected|pending|error)$",
			message = "Status must be one of: connected, disconnected, pending, error")
	private String status;

	// Data type to retrieve
	@Pattern(regexp = "^(info|balance|transactions|orders|positions|all)$",
			message = "Data type must be one of: info, balance, transactions, orders, positions, all")
	private String dataType = "info"; // Default to basic info

	// Pagination for transactions/orders
	@Min(value = 1, message = "Page must be greater than 0")
	private Integer page = 1;

	@Min(value = 1, message = "Limit must be greater than 0")
	@Max(value = 500, message = "Limit must not exceed 500")
	private Integer limit = 50;

	// Date range filtering
	private String startDate; // ISO format: 2024-01-01T00:00:00Z

	private String endDate; // ISO format: 2024-12-31T23:59:59Z

	// Account filtering
	private String accountId; // Optional: specific account ID

	private String accountType; // Optional: paper, live, all

	// Symbol/asset filtering
	private String symbol; // Optional: specific trading symbol

	private List<String> assets; // Optional: specific assets to retrieve

	// Response formatting
	private boolean includeMetadata = false;

	private boolean includeRawData = false;

	// Control what data to include
	private boolean includeBalances = false;

	private boolean includeTransactions = false;

	private boolean includeOrders = false;

	private boolean includePositions = false;

	// Lightweight status check flag
	private boolean statusOnly = false;

	// Constructors
	public ReadProviderRequest() {
	}

	public ReadProviderRequest(String providerId) {
		this.providerId = providerId;
	}

	public ReadProviderRequest(String providerId, String dataType) {
		this.providerId = providerId;
		this.dataType = dataType;
	}

	// Getters and Setters
	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
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

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public List<String> getAssets() {
		return assets;
	}

	public void setAssets(List<String> assets) {
		this.assets = assets;
	}

	public boolean isIncludeMetadata() {
		return includeMetadata;
	}

	public void setIncludeMetadata(boolean includeMetadata) {
		this.includeMetadata = includeMetadata;
	}

	public boolean isIncludeRawData() {
		return includeRawData;
	}

	public void setIncludeRawData(boolean includeRawData) {
		this.includeRawData = includeRawData;
	}

	public boolean isIncludeBalances() {
		return includeBalances;
	}

	public void setIncludeBalances(boolean includeBalances) {
		this.includeBalances = includeBalances;
	}

	public boolean isIncludeTransactions() {
		return includeTransactions;
	}

	public void setIncludeTransactions(boolean includeTransactions) {
		this.includeTransactions = includeTransactions;
	}

	public boolean isIncludeOrders() {
		return includeOrders;
	}

	public void setIncludeOrders(boolean includeOrders) {
		this.includeOrders = includeOrders;
	}

	public boolean isIncludePositions() {
		return includePositions;
	}

	public void setIncludePositions(boolean includePositions) {
		this.includePositions = includePositions;
	}

	public boolean isStatusOnly() {
		return statusOnly;
	}

	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	// Helper methods
	public boolean isSpecificProvider() {
		return providerId != null && !providerId.trim().isEmpty();
	}

	public boolean hasDateRange() {
		return startDate != null && endDate != null;
	}

	public boolean isPaginated() {
		return page != null && limit != null;
	}

	public boolean isAllProviders() {
		return providerId == null || providerId.trim().isEmpty();
	}

	public boolean isInfoOnly() {
		return "info".equals(dataType);
	}

	public boolean isTransactionData() {
		return "transactions".equals(dataType);
	}

	public boolean isBalanceData() {
		return "balance".equals(dataType);
	}

	public boolean isAllData() {
		return "all".equals(dataType);
	}

	@Override
	public String toString() {
		return "ReadProviderRequest{" + "providerId='" + providerId + '\'' + ", status='" + status + '\''
				+ ", dataType='" + dataType + '\'' + ", page=" + page + ", limit=" + limit + ", hasDateRange="
				+ hasDateRange() + ", accountId='" + accountId + '\'' + ", symbol='" + symbol + '\'' + '}';
	}

}