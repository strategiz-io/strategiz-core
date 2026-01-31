package io.strategiz.business.portfolio.enhancer.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a complete enhanced portfolio with all assets and aggregate metrics.
 */
public class EnhancedPortfolio {

	private String userId;

	private String providerId;

	private String providerName;

	private List<EnhancedAsset> assets;

	private BigDecimal totalValue;

	private BigDecimal totalCostBasis;

	private BigDecimal totalProfitLoss;

	private BigDecimal totalProfitLossPercent;

	private BigDecimal cashBalance;

	private Map<String, BigDecimal> assetAllocation; // Percentage by asset type

	private Long lastUpdated;

	private String syncStatus;

	private String errorMessage;

	public EnhancedPortfolio() {
		this.assets = new ArrayList<>();
		this.assetAllocation = new HashMap<>();
		this.totalValue = BigDecimal.ZERO;
		this.cashBalance = BigDecimal.ZERO;
		this.lastUpdated = System.currentTimeMillis();
		this.syncStatus = "synced";
	}

	/**
	 * Calculate aggregate metrics from individual assets
	 */
	public void calculateMetrics() {
		if (assets == null || assets.isEmpty()) {
			return;
		}

		// Reset totals
		totalValue = BigDecimal.ZERO;
		totalCostBasis = BigDecimal.ZERO;
		totalProfitLoss = BigDecimal.ZERO;
		Map<String, BigDecimal> typeValues = new HashMap<>();

		// Calculate totals
		for (EnhancedAsset asset : assets) {
			if (asset.getValue() != null) {
				totalValue = totalValue.add(asset.getValue());

				// Track by asset type
				String type = asset.getAssetType() != null ? asset.getAssetType() : "unknown";
				typeValues.merge(type, asset.getValue(), BigDecimal::add);
			}

			if (asset.getCostBasis() != null) {
				totalCostBasis = totalCostBasis.add(asset.getCostBasis());
			}

			if (asset.getProfitLoss() != null) {
				totalProfitLoss = totalProfitLoss.add(asset.getProfitLoss());
			}

			// Add to cash balance if it's fiat
			if ("fiat".equals(asset.getAssetType())) {
				cashBalance = cashBalance.add(asset.getValue());
			}
		}

		// Calculate P&L percentage
		if (totalCostBasis != null && totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
			totalProfitLossPercent = totalProfitLoss.divide(totalCostBasis, 4, BigDecimal.ROUND_HALF_UP)
				.multiply(new BigDecimal("100"));
		}

		// Calculate asset allocation percentages
		if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
			assetAllocation.clear();
			for (Map.Entry<String, BigDecimal> entry : typeValues.entrySet()) {
				BigDecimal percentage = entry.getValue()
					.divide(totalValue, 4, BigDecimal.ROUND_HALF_UP)
					.multiply(new BigDecimal("100"));
				assetAllocation.put(entry.getKey(), percentage);
			}
		}
	}

	/**
	 * Add an asset and recalculate metrics
	 */
	public void addAsset(EnhancedAsset asset) {
		if (assets == null) {
			assets = new ArrayList<>();
		}
		assets.add(asset);
		calculateMetrics();
	}

	/**
	 * Get assets sorted by value (highest first)
	 */
	public List<EnhancedAsset> getAssetsSortedByValue() {
		if (assets == null) {
			return new ArrayList<>();
		}

		List<EnhancedAsset> sorted = new ArrayList<>(assets);
		sorted.sort((a, b) -> {
			BigDecimal valueA = a.getValue() != null ? a.getValue() : BigDecimal.ZERO;
			BigDecimal valueB = b.getValue() != null ? b.getValue() : BigDecimal.ZERO;
			return valueB.compareTo(valueA);
		});
		return sorted;
	}

	/**
	 * Get only crypto assets
	 */
	public List<EnhancedAsset> getCryptoAssets() {
		if (assets == null) {
			return new ArrayList<>();
		}

		List<EnhancedAsset> crypto = new ArrayList<>();
		for (EnhancedAsset asset : assets) {
			if ("crypto".equals(asset.getAssetType())) {
				crypto.add(asset);
			}
		}
		return crypto;
	}

	/**
	 * Get only staked assets
	 */
	public List<EnhancedAsset> getStakedAssets() {
		if (assets == null) {
			return new ArrayList<>();
		}

		List<EnhancedAsset> staked = new ArrayList<>();
		for (EnhancedAsset asset : assets) {
			if (asset.isStaked()) {
				staked.add(asset);
			}
		}
		return staked;
	}

	// Getters and Setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public List<EnhancedAsset> getAssets() {
		return assets;
	}

	public void setAssets(List<EnhancedAsset> assets) {
		this.assets = assets;
		calculateMetrics();
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public BigDecimal getTotalCostBasis() {
		return totalCostBasis;
	}

	public void setTotalCostBasis(BigDecimal totalCostBasis) {
		this.totalCostBasis = totalCostBasis;
	}

	public BigDecimal getTotalProfitLoss() {
		return totalProfitLoss;
	}

	public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
		this.totalProfitLoss = totalProfitLoss;
	}

	public BigDecimal getTotalProfitLossPercent() {
		return totalProfitLossPercent;
	}

	public void setTotalProfitLossPercent(BigDecimal totalProfitLossPercent) {
		this.totalProfitLossPercent = totalProfitLossPercent;
	}

	public BigDecimal getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(BigDecimal cashBalance) {
		this.cashBalance = cashBalance;
	}

	public Map<String, BigDecimal> getAssetAllocation() {
		return assetAllocation;
	}

	public void setAssetAllocation(Map<String, BigDecimal> assetAllocation) {
		this.assetAllocation = assetAllocation;
	}

	public Long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public String getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}