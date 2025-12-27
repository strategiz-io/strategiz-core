package io.strategiz.client.gcpbilling;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import io.strategiz.client.gcpbilling.config.GcpBillingConfig.GcpBillingProperties;
import io.strategiz.client.gcpbilling.model.GcpServiceUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for retrieving GCP Cloud Monitoring metrics.
 * Provides real-time usage data for Firestore, Cloud Run, and other services.
 *
 * Enable with: gcp.billing.enabled=true and gcp.billing.demo-mode=false
 */
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(name = "gcp.billing.demo-mode", havingValue = "false", matchIfMissing = false)
public class GcpMonitoringClient {

    private static final Logger log = LoggerFactory.getLogger(GcpMonitoringClient.class);

    // Firestore metrics
    private static final String FIRESTORE_READ_COUNT = "firestore.googleapis.com/document/read_count";
    private static final String FIRESTORE_WRITE_COUNT = "firestore.googleapis.com/document/write_count";
    private static final String FIRESTORE_DELETE_COUNT = "firestore.googleapis.com/document/delete_count";

    // Cloud Run metrics
    private static final String CLOUD_RUN_REQUEST_COUNT = "run.googleapis.com/request_count";
    private static final String CLOUD_RUN_CPU_ALLOCATION = "run.googleapis.com/container/cpu/allocation_time";
    private static final String CLOUD_RUN_MEMORY_ALLOCATION = "run.googleapis.com/container/memory/allocation_time";

    private final MetricServiceClient metricServiceClient;
    private final GcpBillingProperties properties;

    public GcpMonitoringClient(MetricServiceClient metricServiceClient, GcpBillingProperties properties) {
        this.metricServiceClient = metricServiceClient;
        this.properties = properties;
        log.info("GcpMonitoringClient initialized for project: {}", properties.projectId());
    }

    /**
     * Get Firestore read/write/delete counts for the last hour
     *
     * @return List of Firestore usage metrics
     */
    @Cacheable(value = "firestoreMetrics", key = "'hourly'")
    public List<GcpServiceUsage> getFirestoreMetrics() {
        log.info("Fetching Firestore metrics for project: {}", properties.projectId());
        List<GcpServiceUsage> metrics = new ArrayList<>();

        try {
            // Get read count
            long readCount = getMetricSum(FIRESTORE_READ_COUNT, Duration.ofHours(1));
            metrics.add(new GcpServiceUsage(
                    "Cloud Firestore",
                    "document_reads",
                    BigDecimal.valueOf(readCount),
                    "count",
                    estimateFirestoreReadCost(readCount),
                    Instant.now()
            ));

            // Get write count
            long writeCount = getMetricSum(FIRESTORE_WRITE_COUNT, Duration.ofHours(1));
            metrics.add(new GcpServiceUsage(
                    "Cloud Firestore",
                    "document_writes",
                    BigDecimal.valueOf(writeCount),
                    "count",
                    estimateFirestoreWriteCost(writeCount),
                    Instant.now()
            ));

            // Get delete count
            long deleteCount = getMetricSum(FIRESTORE_DELETE_COUNT, Duration.ofHours(1));
            metrics.add(new GcpServiceUsage(
                    "Cloud Firestore",
                    "document_deletes",
                    BigDecimal.valueOf(deleteCount),
                    "count",
                    estimateFirestoreDeleteCost(deleteCount),
                    Instant.now()
            ));

        } catch (Exception e) {
            log.warn("Could not fetch Firestore metrics: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * Get Cloud Run metrics for the last hour
     *
     * @return List of Cloud Run usage metrics
     */
    @Cacheable(value = "cloudRunMetrics", key = "'hourly'")
    public List<GcpServiceUsage> getCloudRunMetrics() {
        log.info("Fetching Cloud Run metrics for project: {}", properties.projectId());
        List<GcpServiceUsage> metrics = new ArrayList<>();

        try {
            // Get request count
            long requestCount = getMetricSum(CLOUD_RUN_REQUEST_COUNT, Duration.ofHours(1));
            metrics.add(new GcpServiceUsage(
                    "Cloud Run",
                    "requests",
                    BigDecimal.valueOf(requestCount),
                    "count",
                    BigDecimal.ZERO, // Request cost is bundled with compute
                    Instant.now()
            ));

        } catch (Exception e) {
            log.warn("Could not fetch Cloud Run metrics: {}", e.getMessage());
        }

        return metrics;
    }

    /**
     * Get daily Firestore operation counts for the specified number of days
     *
     * @param days Number of days to retrieve
     * @return List of daily usage records
     */
    public List<GcpServiceUsage> getDailyFirestoreMetrics(int days) {
        log.info("Fetching {} days of Firestore metrics", days);
        List<GcpServiceUsage> metrics = new ArrayList<>();

        // Note: Cloud Monitoring API provides aggregated data
        // For daily breakdowns, we need to query with daily intervals
        // This is a simplified implementation

        try {
            long totalReads = getMetricSum(FIRESTORE_READ_COUNT, Duration.ofDays(days));
            long totalWrites = getMetricSum(FIRESTORE_WRITE_COUNT, Duration.ofDays(days));

            // Average per day
            metrics.add(new GcpServiceUsage(
                    "Cloud Firestore",
                    "avg_daily_reads",
                    BigDecimal.valueOf(totalReads / Math.max(days, 1)),
                    "count/day",
                    estimateFirestoreReadCost(totalReads / Math.max(days, 1)),
                    Instant.now()
            ));

            metrics.add(new GcpServiceUsage(
                    "Cloud Firestore",
                    "avg_daily_writes",
                    BigDecimal.valueOf(totalWrites / Math.max(days, 1)),
                    "count/day",
                    estimateFirestoreWriteCost(totalWrites / Math.max(days, 1)),
                    Instant.now()
            ));
        } catch (Exception e) {
            log.warn("Could not fetch daily Firestore metrics: {}", e.getMessage());
        }

        return metrics;
    }

    private long getMetricSum(String metricType, Duration duration) {
        try {
            String projectName = ProjectName.of(properties.projectId()).toString();
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(duration);

            TimeInterval interval = TimeInterval.newBuilder()
                    .setStartTime(Timestamps.fromMillis(startTime.toEpochMilli()))
                    .setEndTime(Timestamps.fromMillis(endTime.toEpochMilli()))
                    .build();

            ListTimeSeriesRequest request = ListTimeSeriesRequest.newBuilder()
                    .setName(projectName)
                    .setFilter(String.format("metric.type=\"%s\"", metricType))
                    .setInterval(interval)
                    .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
                    .build();

            long sum = 0;
            MetricServiceClient.ListTimeSeriesPagedResponse response = metricServiceClient.listTimeSeries(request);
            for (TimeSeries ts : response.iterateAll()) {
                for (Point point : ts.getPointsList()) {
                    sum += point.getValue().getInt64Value();
                }
            }
            return sum;
        } catch (Exception e) {
            log.debug("Could not fetch metric {}: {}", metricType, e.getMessage());
            return 0;
        }
    }

    /**
     * Estimate Firestore read cost based on pricing
     * Firestore pricing: $0.06 per 100,000 document reads (us-central1)
     */
    private BigDecimal estimateFirestoreReadCost(long readCount) {
        // $0.06 per 100,000 reads = $0.0000006 per read
        return BigDecimal.valueOf(readCount * 0.0000006);
    }

    /**
     * Estimate Firestore write cost based on pricing
     * Firestore pricing: $0.18 per 100,000 document writes (us-central1)
     */
    private BigDecimal estimateFirestoreWriteCost(long writeCount) {
        // $0.18 per 100,000 writes = $0.0000018 per write
        return BigDecimal.valueOf(writeCount * 0.0000018);
    }

    /**
     * Estimate Firestore delete cost based on pricing
     * Firestore pricing: $0.02 per 100,000 document deletes (us-central1)
     */
    private BigDecimal estimateFirestoreDeleteCost(long deleteCount) {
        // $0.02 per 100,000 deletes = $0.0000002 per delete
        return BigDecimal.valueOf(deleteCount * 0.0000002);
    }
}
