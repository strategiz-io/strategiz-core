package io.strategiz.client.timescalebilling;

import io.strategiz.client.timescalebilling.config.TimescaleBillingConfig.TimescaleBillingProperties;
import io.strategiz.client.timescalebilling.model.TimescaleCostSummary;
import io.strategiz.client.timescalebilling.model.TimescaleUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for retrieving TimescaleDB Cloud billing and usage data.
 * Uses the TimescaleDB Cloud Console API to get cost and usage metrics.
 *
 * Note: TimescaleDB Cloud doesn't have a public billing API yet.
 * This implementation attempts to fetch real billing data from the API.
 * If the API is unavailable, returns $0.00 (no estimates/fake data).
 *
 * When TimescaleDB releases a public billing API, this client will
 * automatically start returning real cost data.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class TimescaleBillingClient {

    private static final Logger log = LoggerFactory.getLogger(TimescaleBillingClient.class);

    private final RestTemplate restTemplate;
    private final TimescaleBillingProperties properties;

    public TimescaleBillingClient(
            @Qualifier("timescaleBillingRestTemplate") RestTemplate restTemplate,
            TimescaleBillingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        log.info("TimescaleBillingClient initialized, API configured: {}", properties.isConfigured());
    }

    /**
     * Get cost summary for a date range
     *
     * Note: TimescaleDB Cloud doesn't have a public billing API yet.
     * This method attempts to fetch real billing data from the API.
     * If the API is unavailable or returns an error, returns $0.00 (no fake estimates).
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Cost summary with storage and compute breakdown, or all zeros if unavailable
     */
    @Cacheable(value = "timescaleCostSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
    public TimescaleCostSummary getCostSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching TimescaleDB cost summary from {} to {}", startDate, endDate);

        if (!properties.isConfigured()) {
            log.warn("⚠️ TimescaleDB billing API not configured - returning $0.00 (no billing data available)");
            return TimescaleCostSummary.empty(startDate, endDate);
        }

        try {
            // Try to get actual billing data from TimescaleDB Cloud API
            // Note: This endpoint may not exist - TimescaleDB Cloud billing API is limited
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.apiToken());

            String url = String.format("%s/v1/projects/%s/billing?start_date=%s&end_date=%s",
                    properties.apiUrl(),
                    properties.projectId(),
                    startDate,
                    endDate);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() != null) {
                log.info("✅ Successfully fetched real TimescaleDB billing data from API");
                return parseBillingResponse(response.getBody(), startDate, endDate);
            }
        } catch (RestClientException e) {
            log.warn("⚠️ TimescaleDB billing API not available: {} - returning $0.00 (real data not available)", e.getMessage());
        }

        return TimescaleCostSummary.empty(startDate, endDate);
    }

    /**
     * Get current usage metrics
     *
     * Note: Returns real usage from API or empty/zero usage if unavailable.
     * No estimates are provided.
     *
     * @return Current usage statistics, or zeros if unavailable
     */
    @Cacheable(value = "timescaleUsage", key = "'current'")
    public TimescaleUsage getCurrentUsage() {
        log.info("Fetching TimescaleDB current usage");

        if (!properties.isConfigured()) {
            log.warn("⚠️ TimescaleDB billing API not configured - returning empty usage data");
            return getEmptyUsage();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(properties.apiToken());

            String url = String.format("%s/v1/projects/%s/services",
                    properties.apiUrl(),
                    properties.projectId());

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                log.info("✅ Successfully fetched real TimescaleDB usage data from API");
                return parseUsageResponse(response.getBody().get(0));
            }
        } catch (RestClientException e) {
            log.warn("⚠️ TimescaleDB usage API not available: {} - returning empty usage (real data not available)", e.getMessage());
        }

        return getEmptyUsage();
    }

    /**
     * Check if the TimescaleDB billing API is accessible
     *
     * @return true if API is configured and accessible
     */
    public boolean isApiAccessible() {
        return properties.isConfigured();
    }

    /**
     * Returns empty usage data (all zeros) when real API data is unavailable.
     * No estimates are provided per user requirements.
     *
     * @return Empty usage with all values set to zero
     */
    private TimescaleUsage getEmptyUsage() {
        return new TimescaleUsage(
                "unavailable",
                "TimescaleDB Cloud",
                BigDecimal.ZERO, // No storage data
                BigDecimal.ZERO, // No compute data
                BigDecimal.ZERO, // No ingestion data
                BigDecimal.ZERO, // No query data
                Instant.now()
        );
    }

    private TimescaleCostSummary parseBillingResponse(
            Map<String, Object> response,
            LocalDate startDate,
            LocalDate endDate) {

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal computeCost = BigDecimal.ZERO;
        BigDecimal storageCost = BigDecimal.ZERO;
        BigDecimal storageGb = BigDecimal.ZERO;
        BigDecimal computeHours = BigDecimal.ZERO;

        if (response.containsKey("total_cost")) {
            totalCost = new BigDecimal(response.get("total_cost").toString());
        }
        if (response.containsKey("compute_cost")) {
            computeCost = new BigDecimal(response.get("compute_cost").toString());
        }
        if (response.containsKey("storage_cost")) {
            storageCost = new BigDecimal(response.get("storage_cost").toString());
        }
        if (response.containsKey("storage_gb")) {
            storageGb = new BigDecimal(response.get("storage_gb").toString());
        }
        if (response.containsKey("compute_hours")) {
            computeHours = new BigDecimal(response.get("compute_hours").toString());
        }

        return new TimescaleCostSummary(
                startDate,
                endDate,
                totalCost,
                "USD",
                computeCost,
                storageCost,
                storageGb,
                computeHours
        );
    }

    private TimescaleUsage parseUsageResponse(Map<String, Object> service) {
        String serviceId = service.getOrDefault("id", "unknown").toString();
        String serviceName = service.getOrDefault("name", "TimescaleDB").toString();

        BigDecimal storageGb = BigDecimal.ZERO;
        BigDecimal computeHours = BigDecimal.ZERO;

        if (service.containsKey("storage_gb")) {
            storageGb = new BigDecimal(service.get("storage_gb").toString());
        }
        if (service.containsKey("compute_hours")) {
            computeHours = new BigDecimal(service.get("compute_hours").toString());
        }

        return new TimescaleUsage(
                serviceId,
                serviceName,
                storageGb,
                computeHours,
                BigDecimal.ZERO, // data ingested not typically available
                BigDecimal.ZERO, // queries not typically available
                Instant.now()
        );
    }
}
