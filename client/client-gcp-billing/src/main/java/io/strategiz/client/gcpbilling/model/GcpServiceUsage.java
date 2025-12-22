package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Usage metrics for a specific GCP service
 */
public record GcpServiceUsage(
        String serviceName,
        String metricName,
        BigDecimal usage,
        String unit,
        BigDecimal estimatedCost,
        Instant timestamp
) {}
