package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Repository interface for updating portfolio summary in Firestore
 */
public interface UpdatePortfolioSummaryRepository {

	/**
	 * Update portfolio summary
	 * @param userId User ID
	 * @param summary Updated portfolio summary
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updatePortfolioSummary(String userId, PortfolioSummaryEntity summary);

	/**
	 * Update portfolio total value
	 * @param userId User ID
	 * @param totalValue New total value
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updateTotalValue(String userId, BigDecimal totalValue);

	/**
	 * Update day change
	 * @param userId User ID
	 * @param dayChange Day change amount
	 * @param dayChangePercent Day change percentage
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updateDayChange(String userId, BigDecimal dayChange, BigDecimal dayChangePercent);

	/**
	 * Update account performance
	 * @param userId User ID
	 * @param accountPerformance Map of providerId to value
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updateAccountPerformance(String userId, Map<String, BigDecimal> accountPerformance);

	/**
	 * Update asset allocation
	 * @param userId User ID
	 * @param assetAllocation Asset allocation data
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updateAssetAllocation(String userId, PortfolioSummaryEntity.AssetAllocation assetAllocation);

	/**
	 * Update last sync timestamp
	 * @param userId User ID
	 * @param timestamp Last sync timestamp
	 * @return Updated portfolio summary entity
	 */
	PortfolioSummaryEntity updateLastSyncTime(String userId, Instant timestamp);

}