package io.strategiz.service.monitoring.service;

import io.strategiz.service.monitoring.client.GrafanaMetricsClient;
import io.strategiz.service.monitoring.client.GrafanaMetricsClient.PrometheusQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for fetching strategy execution metrics from Grafana Cloud.
 * Queries Python execution service metrics exported via OpenTelemetry.
 */
@Service
public class ExecutionMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionMetricsService.class);

    private final GrafanaMetricsClient grafanaClient;

    public ExecutionMetricsService(GrafanaMetricsClient grafanaClient) {
        this.grafanaClient = grafanaClient;
    }

    /**
     * Get execution service health metrics
     * @return Health metrics including success rate, latency, cache performance
     */
    public Map<String, Object> getExecutionHealth() {
        Map<String, Object> health = new HashMap<>();

        // Success rate (1 - error rate)
        PrometheusQueryResult successRate = grafanaClient.query(
            "1 - (sum(rate(strategy_requests_total{status=\"failure\"}[5m])) / sum(rate(strategy_requests_total[5m])))"
        );
        health.put("successRate", successRate.getFirstValue().orElse(0.0));

        // Request rate (requests/sec)
        PrometheusQueryResult requestRate = grafanaClient.query(
            "sum(rate(strategy_requests_total[5m]))"
        );
        health.put("requestsPerSecond", requestRate.getFirstValue().orElse(0.0));

        // P99 latency (milliseconds)
        PrometheusQueryResult p99Latency = grafanaClient.query(
            "histogram_quantile(0.99, sum(rate(strategy_execution_time_bucket[5m])) by (le))"
        );
        health.put("p99LatencyMs", p99Latency.getFirstValue().orElse(0.0));

        // Cache hit rate
        PrometheusQueryResult cacheHitRate = grafanaClient.query(
            "sum(rate(strategy_cache_hits[5m])) / (sum(rate(strategy_cache_hits[5m])) + sum(rate(strategy_cache_misses[5m])))"
        );
        health.put("cacheHitRate", cacheHitRate.getFirstValue().orElse(0.0));

        // Active executions
        PrometheusQueryResult activeExecutions = grafanaClient.query(
            "strategy_executions_active"
        );
        health.put("activeExecutions", activeExecutions.getFirstValue().orElse(0.0).intValue());

        // Error rate (errors/sec)
        PrometheusQueryResult errorRate = grafanaClient.query(
            "sum(rate(strategy_errors_total[5m]))"
        );
        health.put("errorsPerSecond", errorRate.getFirstValue().orElse(0.0));

        log.debug("Execution health metrics: {}", health);
        return health;
    }

    /**
     * Get execution latency percentiles over time
     * @param durationMinutes Duration to look back (default: 60 minutes)
     * @return Latency metrics (p50, p95, p99)
     */
    public Map<String, Object> getExecutionLatency(int durationMinutes) {
        long end = System.currentTimeMillis() / 1000; // Unix timestamp
        long start = end - (durationMinutes * 60);

        Map<String, Object> latency = new HashMap<>();

        // P50 latency
        PrometheusQueryResult p50 = grafanaClient.queryRange(
            "histogram_quantile(0.50, sum(rate(strategy_execution_time_bucket[5m])) by (le))",
            start, end, "1m"
        );
        latency.put("p50", convertToTimeSeries(p50));

        // P95 latency
        PrometheusQueryResult p95 = grafanaClient.queryRange(
            "histogram_quantile(0.95, sum(rate(strategy_execution_time_bucket[5m])) by (le))",
            start, end, "1m"
        );
        latency.put("p95", convertToTimeSeries(p95));

        // P99 latency
        PrometheusQueryResult p99 = grafanaClient.queryRange(
            "histogram_quantile(0.99, sum(rate(strategy_execution_time_bucket[5m])) by (le))",
            start, end, "1m"
        );
        latency.put("p99", convertToTimeSeries(p99));

        return latency;
    }

    /**
     * Get throughput metrics over time
     * @param durationMinutes Duration to look back
     * @return Throughput by status (success, failure)
     */
    public Map<String, Object> getExecutionThroughput(int durationMinutes) {
        long end = System.currentTimeMillis() / 1000;
        long start = end - (durationMinutes * 60);

        Map<String, Object> throughput = new HashMap<>();

        // Success rate
        PrometheusQueryResult successRate = grafanaClient.queryRange(
            "sum(rate(strategy_requests_total{status=\"success\"}[5m]))",
            start, end, "1m"
        );
        throughput.put("success", convertToTimeSeries(successRate));

        // Failure rate
        PrometheusQueryResult failureRate = grafanaClient.queryRange(
            "sum(rate(strategy_requests_total{status=\"failure\"}[5m]))",
            start, end, "1m"
        );
        throughput.put("failure", convertToTimeSeries(failureRate));

        return throughput;
    }

    /**
     * Get cache performance metrics
     * @param durationMinutes Duration to look back
     * @return Cache hits and misses over time
     */
    public Map<String, Object> getCachePerformance(int durationMinutes) {
        long end = System.currentTimeMillis() / 1000;
        long start = end - (durationMinutes * 60);

        Map<String, Object> cache = new HashMap<>();

        // Cache hits
        PrometheusQueryResult hits = grafanaClient.queryRange(
            "sum(rate(strategy_cache_hits[5m]))",
            start, end, "1m"
        );
        cache.put("hits", convertToTimeSeries(hits));

        // Cache misses
        PrometheusQueryResult misses = grafanaClient.queryRange(
            "sum(rate(strategy_cache_misses[5m]))",
            start, end, "1m"
        );
        cache.put("misses", convertToTimeSeries(misses));

        // Cache hit rate
        PrometheusQueryResult hitRate = grafanaClient.queryRange(
            "sum(rate(strategy_cache_hits[5m])) / (sum(rate(strategy_cache_hits[5m])) + sum(rate(strategy_cache_misses[5m])))",
            start, end, "1m"
        );
        cache.put("hitRate", convertToTimeSeries(hitRate));

        return cache;
    }

    /**
     * Get error metrics by type
     * @param durationMinutes Duration to look back
     * @return Errors grouped by error type
     */
    public Map<String, Object> getExecutionErrors(int durationMinutes) {
        long end = System.currentTimeMillis() / 1000;
        long start = end - (durationMinutes * 60);

        Map<String, Object> errors = new HashMap<>();

        // Errors by type
        PrometheusQueryResult errorsByType = grafanaClient.queryRange(
            "sum(rate(strategy_errors_total[5m])) by (error_type)",
            start, end, "1m"
        );

        // Group by error type
        errorsByType.getResults().forEach(result -> {
            String errorType = result.getLabels().getOrDefault("error_type", "unknown");
            errors.put(errorType, result.getValues().stream()
                .map(v -> Map.of("timestamp", v.getTimestamp(), "value", v.getValue()))
                .toList());
        });

        return errors;
    }

    /**
     * Convert Prometheus result to time series format for frontend
     */
    private Object convertToTimeSeries(PrometheusQueryResult result) {
        if (result.isEmpty()) {
            return Map.of("data", new Object[0]);
        }

        return result.getResults().get(0).getValues().stream()
            .map(v -> Map.of(
                "timestamp", v.getTimestamp() * 1000, // Convert to milliseconds
                "value", v.getValue()
            ))
            .toList();
    }
}
