package io.strategiz.client.sendgridbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary of SendGrid email service costs
 */
public record SendGridCostSummary(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalCost,
        String currency,
        BigDecimal emailsSent,
        BigDecimal apiRequests
) {
    public static SendGridCostSummary empty(LocalDate startDate, LocalDate endDate) {
        return new SendGridCostSummary(
                startDate,
                endDate,
                BigDecimal.ZERO,
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
