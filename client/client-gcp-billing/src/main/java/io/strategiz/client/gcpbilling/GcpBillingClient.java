package io.strategiz.client.gcpbilling;

import com.google.cloud.bigquery.*;
import io.strategiz.client.base.exception.ClientErrorDetails;
import io.strategiz.client.gcpbilling.config.GcpBillingConfig.GcpBillingProperties;
import io.strategiz.client.gcpbilling.model.GcpCostSummary;
import io.strategiz.client.gcpbilling.model.GcpDailyCost;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Client for retrieving GCP billing data from BigQuery billing export.
 * Provides cost summaries, daily breakdowns, and service-level cost analysis.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class GcpBillingClient {

    private static final Logger log = LoggerFactory.getLogger(GcpBillingClient.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BigQuery bigQuery;
    private final GcpBillingProperties properties;

    public GcpBillingClient(BigQuery bigQuery, GcpBillingProperties properties) {
        this.bigQuery = bigQuery;
        this.properties = properties;
        log.info("GcpBillingClient initialized for project: {}", properties.projectId());
    }

    /**
     * Get cost summary for a date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Cost summary with service and project breakdowns
     */
    @Cacheable(value = "gcpCostSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
    public GcpCostSummary getCostSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching GCP cost summary from {} to {}", startDate, endDate);

        try {
            String query = String.format("""
                SELECT
                    SUM(cost) as total_cost,
                    currency,
                    service.description as service_name,
                    project.id as project_id
                FROM `%s`
                WHERE DATE(usage_start_time) >= '%s'
                  AND DATE(usage_start_time) <= '%s'
                GROUP BY currency, service.description, project.id
                ORDER BY total_cost DESC
                """,
                    properties.getBillingTableFullName(),
                    startDate.format(DATE_FORMAT),
                    endDate.format(DATE_FORMAT)
            );

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(queryConfig);

            Map<String, BigDecimal> costByService = new HashMap<>();
            Map<String, BigDecimal> costByProject = new HashMap<>();
            BigDecimal totalCost = BigDecimal.ZERO;
            String currency = "USD";

            for (FieldValueList row : result.iterateAll()) {
                BigDecimal cost = BigDecimal.valueOf(row.get("total_cost").getDoubleValue());
                String serviceName = row.get("service_name").getStringValue();
                String projectId = row.get("project_id").getStringValue();
                currency = row.get("currency").getStringValue();

                totalCost = totalCost.add(cost);
                costByService.merge(serviceName, cost, BigDecimal::add);
                costByProject.merge(projectId, cost, BigDecimal::add);
            }

            return new GcpCostSummary(startDate, endDate, totalCost, currency, costByService, costByProject);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Query interrupted while fetching GCP cost summary", e);
            throw new StrategizException(ClientErrorDetails.DATA_RETRIEVAL_FAILED, "client-gcp-billing", e);
        } catch (Exception e) {
            log.error("Error fetching GCP cost summary: {}", e.getMessage(), e);
            // Return empty summary instead of failing - billing export may not be configured
            return GcpCostSummary.empty(startDate, endDate);
        }
    }

    /**
     * Get daily cost breakdown for the specified number of days
     *
     * @param days Number of days to retrieve
     * @return List of daily cost breakdowns
     */
    @Cacheable(value = "gcpDailyCosts", key = "#days")
    public List<GcpDailyCost> getDailyCosts(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        log.info("Fetching GCP daily costs for last {} days", days);

        try {
            String query = String.format("""
                SELECT
                    DATE(usage_start_time) as usage_date,
                    SUM(cost) as total_cost,
                    currency,
                    service.description as service_name
                FROM `%s`
                WHERE DATE(usage_start_time) >= '%s'
                  AND DATE(usage_start_time) <= '%s'
                GROUP BY usage_date, currency, service.description
                ORDER BY usage_date DESC, total_cost DESC
                """,
                    properties.getBillingTableFullName(),
                    startDate.format(DATE_FORMAT),
                    endDate.format(DATE_FORMAT)
            );

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(queryConfig);

            // Group by date
            Map<LocalDate, Map<String, BigDecimal>> dailyServiceCosts = new LinkedHashMap<>();
            Map<LocalDate, String> dailyCurrency = new HashMap<>();

            for (FieldValueList row : result.iterateAll()) {
                LocalDate date = LocalDate.parse(row.get("usage_date").getStringValue());
                BigDecimal cost = BigDecimal.valueOf(row.get("total_cost").getDoubleValue());
                String serviceName = row.get("service_name").getStringValue();
                String currency = row.get("currency").getStringValue();

                dailyServiceCosts.computeIfAbsent(date, k -> new HashMap<>())
                        .merge(serviceName, cost, BigDecimal::add);
                dailyCurrency.put(date, currency);
            }

            // Convert to list of GcpDailyCost
            List<GcpDailyCost> dailyCosts = new ArrayList<>();
            for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : dailyServiceCosts.entrySet()) {
                LocalDate date = entry.getKey();
                Map<String, BigDecimal> serviceCosts = entry.getValue();
                BigDecimal totalCost = serviceCosts.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String currency = dailyCurrency.getOrDefault(date, "USD");

                dailyCosts.add(new GcpDailyCost(date, totalCost, currency, serviceCosts));
            }

            return dailyCosts;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Query interrupted while fetching GCP daily costs", e);
            throw new StrategizException(ClientErrorDetails.DATA_RETRIEVAL_FAILED, "client-gcp-billing", e);
        } catch (Exception e) {
            log.error("Error fetching GCP daily costs: {}", e.getMessage(), e);
            // Return empty list - billing export may not be configured
            return Collections.emptyList();
        }
    }

    /**
     * Get current month costs with breakdown by service
     *
     * @return Cost summary for the current month
     */
    @Cacheable(value = "gcpCurrentMonthCosts", key = "'current-month'")
    public GcpCostSummary getCurrentMonthCosts() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return getCostSummary(startOfMonth, today);
    }

    /**
     * Get costs for a specific service
     *
     * @param serviceName The GCP service name (e.g., "Cloud Run", "Cloud Firestore")
     * @param startDate Start date
     * @param endDate End date
     * @return Total cost for the service
     */
    public BigDecimal getServiceCost(String serviceName, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching GCP cost for service: {} from {} to {}", serviceName, startDate, endDate);

        try {
            String query = String.format("""
                SELECT SUM(cost) as total_cost
                FROM `%s`
                WHERE DATE(usage_start_time) >= '%s'
                  AND DATE(usage_start_time) <= '%s'
                  AND LOWER(service.description) LIKE '%%%s%%'
                """,
                    properties.getBillingTableFullName(),
                    startDate.format(DATE_FORMAT),
                    endDate.format(DATE_FORMAT),
                    serviceName.toLowerCase()
            );

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(queryConfig);

            for (FieldValueList row : result.iterateAll()) {
                if (!row.get("total_cost").isNull()) {
                    return BigDecimal.valueOf(row.get("total_cost").getDoubleValue());
                }
            }
            return BigDecimal.ZERO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Query interrupted while fetching service cost", e);
            throw new StrategizException(ClientErrorDetails.DATA_RETRIEVAL_FAILED, "client-gcp-billing", e);
        } catch (Exception e) {
            log.error("Error fetching service cost for {}: {}", serviceName, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Check if billing export is configured and accessible
     *
     * @return true if billing data is available
     */
    public boolean isBillingExportConfigured() {
        try {
            String query = String.format("""
                SELECT COUNT(*) as cnt
                FROM `%s`
                LIMIT 1
                """, properties.getBillingTableFullName());

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            bigQuery.query(queryConfig);
            return true;
        } catch (Exception e) {
            log.warn("Billing export not configured or accessible: {}", e.getMessage());
            return false;
        }
    }
}
