package io.strategiz.client.timescalebilling.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Usage metrics for TimescaleDB Cloud
 */
public record TimescaleUsage(
        String serviceId,
        String serviceName,
        BigDecimal storageUsedGb,
        BigDecimal computeHours,
        BigDecimal dataIngestedGb,
        BigDecimal queriesExecuted,
        Instant timestamp
) {}
