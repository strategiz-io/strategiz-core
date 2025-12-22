package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Daily cost breakdown
 */
public record DailyCost(
        String date,
        BigDecimal totalCost,
        Map<String, BigDecimal> breakdown
) {}
