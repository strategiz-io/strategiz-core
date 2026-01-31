package io.strategiz.business.infrastructurecosts.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Model for ClickHouseDB usage metrics. Represents current resource usage statistics.
 */
public record ClickHouseUsage(String serviceId, String serviceName, BigDecimal storageUsedGb, BigDecimal computeHours,
		BigDecimal dataIngestedGb, BigDecimal queriesExecuted, Instant timestamp) {
}
