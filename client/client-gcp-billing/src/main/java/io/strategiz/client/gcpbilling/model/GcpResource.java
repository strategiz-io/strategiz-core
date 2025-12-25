package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a GCP resource with its metadata and estimated cost.
 *
 * @param resourceType Type of resource (Cloud Run, Firestore, Cloud Storage, etc.)
 * @param resourceName Resource identifier/name
 * @param region GCP region where the resource is deployed
 * @param status Resource status (ACTIVE, STOPPED, DELETED)
 * @param createdAt Resource creation timestamp
 * @param estimatedMonthlyCost Estimated monthly cost based on usage patterns
 * @param labels Resource labels for categorization
 */
public record GcpResource(
        String resourceType,
        String resourceName,
        String region,
        String status,
        Instant createdAt,
        BigDecimal estimatedMonthlyCost,
        java.util.Map<String, String> labels
) {
    public static GcpResource empty() {
        return new GcpResource(
                "Unknown",
                "Unknown",
                "Unknown",
                "UNKNOWN",
                Instant.now(),
                BigDecimal.ZERO,
                java.util.Collections.emptyMap()
        );
    }
}
