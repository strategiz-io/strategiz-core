package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model for ClickHouse Cloud API cost response.
 * Represents costs in ClickHouse Credits (CHC) with breakdown by category.
 *
 * @param startDate Start date of the cost period
 * @param endDate End date of the cost period
 * @param totalCostChc Total cost in ClickHouse Credits
 * @param computeCostChc Compute costs (CPU/memory for queries)
 * @param storageCostChc Storage costs (data at rest)
 * @param backupCostChc Backup storage costs
 * @param dataTransferCostChc Data transfer/egress costs
 * @param currency Currency code (typically "CHC" for credits or "USD")
 */
public record ClickHouseCloudCost(
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal totalCostChc,
		BigDecimal computeCostChc,
		BigDecimal storageCostChc,
		BigDecimal backupCostChc,
		BigDecimal dataTransferCostChc,
		String currency
) {

	/**
	 * Create an empty cost record for when no data is available
	 */
	public static ClickHouseCloudCost empty(LocalDate startDate, LocalDate endDate) {
		return new ClickHouseCloudCost(
				startDate,
				endDate,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				"USD"
		);
	}

	/**
	 * Convert ClickHouse Credits (CHC) to USD.
	 * Note: 1 CHC = $1 USD as of 2025
	 */
	public BigDecimal totalCostUsd() {
		return totalCostChc;
	}

}
