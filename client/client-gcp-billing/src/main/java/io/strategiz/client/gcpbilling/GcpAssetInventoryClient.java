package io.strategiz.client.gcpbilling;

import com.google.cloud.asset.v1.AssetServiceClient;
import com.google.cloud.asset.v1.ListAssetsRequest;
import com.google.cloud.asset.v1.ListAssetsResponse;
import com.google.cloud.asset.v1.ProjectName;
import io.strategiz.client.gcpbilling.config.GcpBillingConfig.GcpBillingProperties;
import io.strategiz.client.gcpbilling.model.GcpInfrastructureSummary;
import io.strategiz.client.gcpbilling.model.GcpResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for retrieving GCP infrastructure inventory using Cloud Asset API. Provides
 * comprehensive resource listing and categorization.
 *
 * Cloud Asset Inventory API is FREE to use. Enable with: gcp.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class GcpAssetInventoryClient {

	private static final Logger log = LoggerFactory.getLogger(GcpAssetInventoryClient.class);

	private final GcpBillingProperties properties;

	private final GcpBillingClient billingClient;

	// Resource type mappings
	private static final Map<String, String> RESOURCE_TYPE_NAMES = Map.ofEntries(
			Map.entry("run.googleapis.com/Service", "Cloud Run"),
			Map.entry("firestore.googleapis.com/Database", "Firestore"),
			Map.entry("storage.googleapis.com/Bucket", "Cloud Storage"),
			Map.entry("compute.googleapis.com/Instance", "Compute Engine"),
			Map.entry("container.googleapis.com/Cluster", "GKE Cluster"),
			Map.entry("sqladmin.googleapis.com/Instance", "Cloud SQL"),
			Map.entry("redis.googleapis.com/Instance", "Memorystore"),
			Map.entry("pubsub.googleapis.com/Topic", "Pub/Sub Topic"),
			Map.entry("pubsub.googleapis.com/Subscription", "Pub/Sub Subscription"),
			Map.entry("cloudfunctions.googleapis.com/Function", "Cloud Functions"));

	public GcpAssetInventoryClient(GcpBillingProperties properties, GcpBillingClient billingClient) {
		this.properties = properties;
		this.billingClient = billingClient;
		log.info("GcpAssetInventoryClient initialized for project: {}", properties.projectId());
	}

	/**
	 * Get comprehensive infrastructure summary with all resources
	 * @return Infrastructure summary with resource inventory
	 */
	@Cacheable(value = "infrastructureSummary", key = "'all'")
	public GcpInfrastructureSummary getInfrastructureSummary() {
		log.info("Fetching infrastructure summary for project: {}", properties.projectId());

		try (AssetServiceClient client = AssetServiceClient.create()) {
			String parent = String.format("projects/%s", properties.projectId());

			ListAssetsRequest request = ListAssetsRequest.newBuilder().setParent(parent).build();

			List<GcpResource> resources = new ArrayList<>();
			Map<String, Integer> resourcesByType = new HashMap<>();
			Map<String, Integer> resourcesByRegion = new HashMap<>();
			BigDecimal totalEstimatedCost = BigDecimal.ZERO;

			// Iterate through all assets
			AssetServiceClient.ListAssetsPagedResponse pagedResponse = client.listAssets(request);

			for (com.google.cloud.asset.v1.Asset asset : pagedResponse.iterateAll()) {
				String assetType = asset.getAssetType();
				String resourceName = extractResourceName(asset.getName());
				String region = extractRegion(asset.getName());

				// Map to friendly resource type name
				String resourceType = RESOURCE_TYPE_NAMES.getOrDefault(assetType, assetType);

				// Estimate cost based on historical billing data
				BigDecimal estimatedCost = estimateResourceCost(resourceType, resourceName);
				totalEstimatedCost = totalEstimatedCost.add(estimatedCost);

				// Extract labels if available
				Map<String, String> labels = new HashMap<>();
				// Note: Asset metadata extraction depends on resource type
				// This is a simplified version

				GcpResource resource = new GcpResource(resourceType, resourceName, region, "ACTIVE", Instant.now(), // Would
																													// need
																													// additional
																													// API
																													// call
																													// for
																													// actual
																													// creation
																													// time
						estimatedCost, labels);

				resources.add(resource);
				resourcesByType.merge(resourceType, 1, Integer::sum);
				resourcesByRegion.merge(region, 1, Integer::sum);
			}

			log.info("Found {} resources across {} types", resources.size(), resourcesByType.size());

			return new GcpInfrastructureSummary(resources.size(), totalEstimatedCost, resourcesByType,
					resourcesByRegion, resources);

		}
		catch (Exception e) {
			log.error("Error fetching infrastructure summary: {}", e.getMessage(), e);
			return GcpInfrastructureSummary.empty();
		}
	}

	/**
	 * Get resources filtered by type
	 * @param resourceType Resource type to filter (e.g., "Cloud Run", "Firestore")
	 * @return List of resources of the specified type
	 */
	@Cacheable(value = "resourcesByType", key = "#resourceType")
	public List<GcpResource> getResourcesByType(String resourceType) {
		log.info("Fetching resources of type: {}", resourceType);
		GcpInfrastructureSummary summary = getInfrastructureSummary();

		return summary.resources()
			.stream()
			.filter(r -> r.resourceType().equals(resourceType))
			.collect(Collectors.toList());
	}

	/**
	 * Get resources filtered by region
	 * @param region GCP region (e.g., "us-central1", "us-east1")
	 * @return List of resources in the specified region
	 */
	@Cacheable(value = "resourcesByRegion", key = "#region")
	public List<GcpResource> getResourcesByRegion(String region) {
		log.info("Fetching resources in region: {}", region);
		GcpInfrastructureSummary summary = getInfrastructureSummary();

		return summary.resources().stream().filter(r -> r.region().equals(region)).collect(Collectors.toList());
	}

	private String extractResourceName(String fullName) {
		// Extract resource name from full asset name
		// Format:
		// //service.googleapis.com/projects/{project}/locations/{location}/resources/{name}
		String[] parts = fullName.split("/");
		return parts.length > 0 ? parts[parts.length - 1] : fullName;
	}

	private String extractRegion(String fullName) {
		// Extract region from asset name if present
		if (fullName.contains("/locations/")) {
			String[] parts = fullName.split("/locations/");
			if (parts.length > 1) {
				String locationPart = parts[1].split("/")[0];
				return locationPart;
			}
		}
		return "global";
	}

	private BigDecimal estimateResourceCost(String resourceType, String resourceName) {
		// Estimate monthly cost based on historical billing data
		// This is a simplified estimation - actual costs come from BigQuery billing
		// export

		try {
			// Query last month's cost for this specific service
			BigDecimal serviceCost = billingClient.getServiceCost(resourceType,
					java.time.LocalDate.now().minusMonths(1).withDayOfMonth(1),
					java.time.LocalDate.now()
						.minusMonths(1)
						.withDayOfMonth(java.time.LocalDate.now().minusMonths(1).lengthOfMonth()));

			// Return average cost (could be refined with more specific queries)
			return serviceCost;
		}
		catch (Exception e) {
			log.debug("Could not estimate cost for {}: {}", resourceType, e.getMessage());
			return BigDecimal.ZERO;
		}
	}

}
