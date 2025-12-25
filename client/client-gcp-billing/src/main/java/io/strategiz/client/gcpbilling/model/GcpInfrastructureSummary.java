package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Summary of all GCP infrastructure resources and their costs.
 *
 * @param totalResources Total number of resources across all types
 * @param totalEstimatedMonthlyCost Total estimated monthly cost for all resources
 * @param resourcesByType Breakdown of resources by type (Cloud Run, Firestore, etc.)
 * @param resourcesByRegion Breakdown of resources by GCP region
 * @param resources List of individual resources with details
 */
public record GcpInfrastructureSummary(
        int totalResources,
        BigDecimal totalEstimatedMonthlyCost,
        Map<String, Integer> resourcesByType,
        Map<String, Integer> resourcesByRegion,
        List<GcpResource> resources
) {
    public static GcpInfrastructureSummary empty() {
        return new GcpInfrastructureSummary(
                0,
                BigDecimal.ZERO,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyList()
        );
    }
}
