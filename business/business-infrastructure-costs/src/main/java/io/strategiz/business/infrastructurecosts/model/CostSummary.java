package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated cost summary combining GCP, ClickHouse, SendGrid, and subscription costs
 */
public record CostSummary(
        String month,
        BigDecimal totalCost,
        BigDecimal gcpCost,
        BigDecimal clickhouseCost,
        BigDecimal sendgridCost,
        BigDecimal subscriptionCosts,
        String currency,
        int daysSoFar,
        BigDecimal avgDailyCost,
        String vsLastMonth,
        Map<String, BigDecimal> costByService
) {
    public static CostSummary empty(String month) {
        return new CostSummary(
                month,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "USD",
                0,
                BigDecimal.ZERO,
                "N/A",
                Map.of()
        );
    }
}
