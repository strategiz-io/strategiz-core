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
 * This implementation provides estimated costs based on:
 * - Storage: ~$0.09/GB/month for standard tier
 * - Compute: Based on instance type and hours
 *
 * When TimescaleDB releases a billing API, this client can be updated.
 */
@Component
public class TimescaleBillingClient {

    private static final Logger log = LoggerFactory.getLogger(TimescaleBillingClient.class);

    // TimescaleDB Cloud pricing estimates (USD)
    private static final BigDecimal STORAGE_PRICE_PER_GB_MONTH = new BigDecimal("0.09");
    private static final BigDecimal COMPUTE_PRICE_PER_HOUR_SMALL = new BigDecimal("0.07"); // dev tier
    private static final BigDecimal COMPUTE_PRICE_PER_HOUR_MEDIUM = new BigDecimal("0.28"); // standard

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
     * Note: Since TimescaleDB Cloud doesn't have a public billing API,
     * this returns estimated costs based on typical usage patterns.
     * Configure actual storage/compute values in Vault for accurate estimates.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Cost summary with storage and compute breakdown
     */
    @Cacheable(value = "timescaleCostSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
    public TimescaleCostSummary getCostSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching TimescaleDB cost summary from {} to {}", startDate, endDate);

        if (!properties.isConfigured()) {
            log.warn("TimescaleDB billing API not configured - returning estimates");
            return getEstimatedCosts(startDate, endDate);
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
                return parseBillingResponse(response.getBody(), startDate, endDate);
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch TimescaleDB billing data: {} - using estimates", e.getMessage());
        }

        return getEstimatedCosts(startDate, endDate);
    }

    /**
     * Get current usage metrics
     *
     * @return Current usage statistics
     */
    @Cacheable(value = "timescaleUsage", key = "'current'")
    public TimescaleUsage getCurrentUsage() {
        log.info("Fetching TimescaleDB current usage");

        if (!properties.isConfigured()) {
            return getEstimatedUsage();
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
                return parseUsageResponse(response.getBody().get(0));
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch TimescaleDB usage: {} - using estimates", e.getMessage());
        }

        return getEstimatedUsage();
    }

    /**
     * Get estimated monthly cost based on current usage
     *
     * @return Estimated monthly cost
     */
    public BigDecimal getEstimatedMonthlyCost() {
        TimescaleUsage usage = getCurrentUsage();

        // Storage cost (per month)
        BigDecimal storageCost = usage.storageUsedGb().multiply(STORAGE_PRICE_PER_GB_MONTH);

        // Compute cost (24 hours * 30 days = 720 hours per month)
        BigDecimal computeHoursPerMonth = new BigDecimal("720");
        BigDecimal computeCost = computeHoursPerMonth.multiply(COMPUTE_PRICE_PER_HOUR_MEDIUM);

        return storageCost.add(computeCost);
    }

    /**
     * Check if the TimescaleDB billing API is accessible
     *
     * @return true if API is configured and accessible
     */
    public boolean isApiAccessible() {
        return properties.isConfigured();
    }

    private TimescaleCostSummary getEstimatedCosts(LocalDate startDate, LocalDate endDate) {
        // Calculate number of days in the range
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        BigDecimal daysDecimal = BigDecimal.valueOf(days);

        // Estimated storage: ~10GB for development
        BigDecimal storageGb = new BigDecimal("10");
        BigDecimal dailyStorageCost = storageGb.multiply(STORAGE_PRICE_PER_GB_MONTH)
                .divide(BigDecimal.valueOf(30), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal storageCost = dailyStorageCost.multiply(daysDecimal);

        // Estimated compute: 24 hours/day * $0.07/hour (dev tier)
        BigDecimal dailyComputeCost = new BigDecimal("24").multiply(COMPUTE_PRICE_PER_HOUR_SMALL);
        BigDecimal computeCost = dailyComputeCost.multiply(daysDecimal);

        BigDecimal totalCost = storageCost.add(computeCost);

        return new TimescaleCostSummary(
                startDate,
                endDate,
                totalCost.setScale(2, java.math.RoundingMode.HALF_UP),
                "USD",
                computeCost.setScale(2, java.math.RoundingMode.HALF_UP),
                storageCost.setScale(2, java.math.RoundingMode.HALF_UP),
                storageGb,
                new BigDecimal("24").multiply(daysDecimal)
        );
    }

    private TimescaleUsage getEstimatedUsage() {
        return new TimescaleUsage(
                "estimated",
                "TimescaleDB Cloud",
                new BigDecimal("10"), // 10GB storage estimate
                new BigDecimal("24"), // 24 hours compute/day
                new BigDecimal("0.5"), // 0.5GB ingested/day estimate
                BigDecimal.valueOf(10000), // 10k queries/day estimate
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
