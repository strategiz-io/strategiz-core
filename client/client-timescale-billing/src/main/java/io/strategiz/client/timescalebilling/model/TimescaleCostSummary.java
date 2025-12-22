package io.strategiz.client.timescalebilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary of TimescaleDB Cloud costs
 */
public record TimescaleCostSummary(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalCost,
        String currency,
        BigDecimal computeCost,
        BigDecimal storageCost,
        BigDecimal storageUsageGb,
        BigDecimal computeHours
) {
    public static TimescaleCostSummary empty(LocalDate startDate, LocalDate endDate) {
        return new TimescaleCostSummary(
                startDate,
                endDate,
                BigDecimal.ZERO,
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
