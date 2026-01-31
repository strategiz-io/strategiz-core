package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Summary of GCP costs for a specific period
 */
public record GcpCostSummary(LocalDate startDate, LocalDate endDate, BigDecimal totalCost, String currency,
		Map<String, BigDecimal> costByService, Map<String, BigDecimal> costByProject) {
	public static GcpCostSummary empty(LocalDate startDate, LocalDate endDate) {
		return new GcpCostSummary(startDate, endDate, BigDecimal.ZERO, "USD", Map.of(), Map.of());
	}
}
