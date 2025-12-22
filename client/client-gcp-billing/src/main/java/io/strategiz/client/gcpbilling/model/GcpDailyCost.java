package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Daily cost breakdown for GCP services
 */
public record GcpDailyCost(
        LocalDate date,
        BigDecimal totalCost,
        String currency,
        Map<String, BigDecimal> costByService
) {
    public static GcpDailyCost empty(LocalDate date) {
        return new GcpDailyCost(date, BigDecimal.ZERO, "USD", Map.of());
    }
}
