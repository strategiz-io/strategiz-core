package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cost prediction with confidence interval
 */
public record CostPrediction(
        BigDecimal predictedMonthlyTotal,
        ConfidenceInterval confidenceInterval,
        BigDecimal confidenceLevel,
        Map<String, BigDecimal> breakdown
) {
    public record ConfidenceInterval(
            BigDecimal low,
            BigDecimal high
    ) {}

    public static CostPrediction empty() {
        return new CostPrediction(
                BigDecimal.ZERO,
                new ConfidenceInterval(BigDecimal.ZERO, BigDecimal.ZERO),
                BigDecimal.ZERO,
                Map.of()
        );
    }
}
