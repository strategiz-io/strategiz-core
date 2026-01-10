package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model for ClickHouseDB cost summary data.
 * Represents aggregated billing costs for a date range.
 */
public record ClickHouseCostSummary(LocalDate startDate, LocalDate endDate, BigDecimal totalCost, String currency,
		BigDecimal computeCost, BigDecimal storageCost, BigDecimal storageUsageGb, BigDecimal computeHours) {

	/**
	 * Create an empty cost summary for a date range.
	 */
	public static ClickHouseCostSummary empty(LocalDate startDate, LocalDate endDate) {
		return new ClickHouseCostSummary(startDate, endDate, BigDecimal.ZERO, "USD", BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO);
	}

}
