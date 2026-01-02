package io.strategiz.service.console.observability.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for querying endpoint metrics from Micrometer's MeterRegistry.
 *
 * <p>Provides real-time availability and performance metrics for ALL REST endpoints
 * by querying Spring Boot Actuator's automatic HTTP instrumentation.
 *
 * <p>Metrics tracked per endpoint:
 * <ul>
 *   <li>Request count (total, success, error)</li>
 *   <li>Availability (% successful requests)</li>
 *   <li>Latency (p50, p95, p99, max)</li>
 *   <li>Error rate and error types</li>
 *   <li>Requests per second (calculated from count over time window)</li>
 * </ul>
 */
@Service
public class EndpointMetricsService {

    private static final Logger log = LoggerFactory.getLogger(EndpointMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public EndpointMetricsService(MeterRegistry meterRegistry,
                                   RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.meterRegistry = meterRegistry;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    /**
     * Get metrics for all discovered REST endpoints.
     * Discovers ALL registered endpoints from Spring, then merges with actual metrics data.
     * This ensures endpoints with zero traffic are also visible.
     * @return List of endpoint metrics
     */
    public List<EndpointMetrics> getAllEndpointMetrics() {
        Map<String, EndpointMetrics> metricsMap = new HashMap<>();

        // Step 1: Discover ALL registered endpoints from Spring
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();

            // Get all HTTP methods for this mapping
            Set<String> methods = mappingInfo.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            // Get all patterns (URIs) for this mapping
            Set<String> patterns = mappingInfo.getPathPatternsCondition() != null
                    ? mappingInfo.getPathPatternsCondition().getPatternValues()
                    : (mappingInfo.getPatternsCondition() != null
                        ? mappingInfo.getPatternsCondition().getPatterns()
                        : Set.of());

            // Create metrics entry for each method+uri combination
            for (String method : methods) {
                for (String pattern : patterns) {
                    String endpointKey = method + " " + pattern;

                    // Skip actuator endpoints
                    if (pattern.startsWith("/actuator/")) {
                        continue;
                    }

                    // Create metrics object with zero data
                    EndpointMetrics em = new EndpointMetrics();
                    em.setMethod(method);
                    em.setUri(pattern);
                    em.setEndpoint(endpointKey);
                    em.setTotalRequests(0L);
                    em.setSuccessRequests(0L);
                    em.setErrorRequests(0L);
                    em.setAvailability(0.0);
                    em.setErrorRate(0.0);

                    metricsMap.put(endpointKey, em);
                }
            }
        }

        // Step 2: Query all http.server.requests timers and merge actual metrics
        Collection<io.micrometer.core.instrument.Timer> timers = Search.in(meterRegistry)
                .name("http.server.requests")
                .timers();

        for (io.micrometer.core.instrument.Timer timer : timers) {
            String uri = timer.getId().getTag("uri");
            String method = timer.getId().getTag("method");
            String status = timer.getId().getTag("status");
            String outcome = timer.getId().getTag("outcome");
            String exception = timer.getId().getTag("exception");

            if (uri == null || method == null) {
                continue;
            }

            // Create unique key for endpoint (method + uri)
            String endpointKey = method + " " + uri;

            // Get or create metrics object for this endpoint
            EndpointMetrics metrics = metricsMap.computeIfAbsent(endpointKey, k -> {
                EndpointMetrics em = new EndpointMetrics();
                em.setMethod(method);
                em.setUri(uri);
                em.setEndpoint(endpointKey);
                return em;
            });

            // Aggregate counts
            long count = timer.count();
            metrics.setTotalRequests(metrics.getTotalRequests() + count);

            // Track success/error counts based on status code
            if (status != null) {
                int statusCode = Integer.parseInt(status);
                if (statusCode >= 200 && statusCode < 400) {
                    metrics.setSuccessRequests(metrics.getSuccessRequests() + count);
                } else if (statusCode >= 400) {
                    metrics.setErrorRequests(metrics.getErrorRequests() + count);

                    // Track error by status code
                    metrics.getErrorsByStatus().merge(status, count, Long::sum);
                }
            }

            // Track errors by exception type
            if (exception != null && !"None".equals(exception)) {
                metrics.getErrorsByException().merge(exception, count, Long::sum);
            }

            // Update latency metrics (use the timer with most data - typically the aggregate one)
            if (count > 0) {
                // Get percentile values if available
                double p50 = getPercentile(timer, 0.5);
                double p95 = getPercentile(timer, 0.95);
                double p99 = getPercentile(timer, 0.99);
                double max = timer.max(TimeUnit.MILLISECONDS);
                double mean = timer.mean(TimeUnit.MILLISECONDS);

                // Only update if we have better data (more samples)
                if (metrics.getLatencyP50Ms() == 0 || count > metrics.getTotalRequests() / 2) {
                    metrics.setLatencyP50Ms(p50);
                    metrics.setLatencyP95Ms(p95);
                    metrics.setLatencyP99Ms(p99);
                    metrics.setLatencyMaxMs(max);
                    metrics.setLatencyMeanMs(mean);
                }
            }
        }

        // Calculate derived metrics
        for (EndpointMetrics metrics : metricsMap.values()) {
            // Calculate availability
            if (metrics.getTotalRequests() > 0) {
                double availability = (double) metrics.getSuccessRequests() / metrics.getTotalRequests();
                metrics.setAvailability(availability);
            }

            // Calculate error rate
            if (metrics.getTotalRequests() > 0) {
                double errorRate = (double) metrics.getErrorRequests() / metrics.getTotalRequests();
                metrics.setErrorRate(errorRate);
            }
        }

        // Sort: endpoints with traffic first (by request count desc), then zero-traffic endpoints (alphabetically)
        return metricsMap.values().stream()
                .sorted(Comparator
                        .comparing(EndpointMetrics::getTotalRequests).reversed()
                        .thenComparing(EndpointMetrics::getEndpoint))
                .collect(Collectors.toList());
    }

    /**
     * Get metrics for a specific endpoint.
     * @param method HTTP method (GET, POST, etc.)
     * @param uri URI pattern
     * @return Endpoint metrics or null if not found
     */
    public EndpointMetrics getEndpointMetrics(String method, String uri) {
        String endpointKey = method + " " + uri;

        return getAllEndpointMetrics().stream()
                .filter(em -> em.getEndpoint().equals(endpointKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get metrics grouped by module (first path segment).
     * @return Map of module name to module metrics
     */
    public Map<String, ModuleMetrics> getMetricsByModule() {
        List<EndpointMetrics> allMetrics = getAllEndpointMetrics();
        Map<String, ModuleMetrics> moduleMap = new HashMap<>();

        for (EndpointMetrics em : allMetrics) {
            String moduleName = extractModuleName(em.getUri());

            ModuleMetrics module = moduleMap.computeIfAbsent(moduleName, k -> {
                ModuleMetrics mm = new ModuleMetrics();
                mm.setModuleName(moduleName);
                return mm;
            });

            // Aggregate counts
            module.setEndpointCount(module.getEndpointCount() + 1);
            module.setTotalRequests(module.getTotalRequests() + em.getTotalRequests());
            module.setSuccessRequests(module.getSuccessRequests() + em.getSuccessRequests());
            module.setErrorRequests(module.getErrorRequests() + em.getErrorRequests());

            // Track worst latency
            if (em.getLatencyP99Ms() > module.getWorstP99LatencyMs()) {
                module.setWorstP99LatencyMs(em.getLatencyP99Ms());
            }
        }

        // Calculate derived metrics for modules
        for (ModuleMetrics module : moduleMap.values()) {
            if (module.getTotalRequests() > 0) {
                double availability = (double) module.getSuccessRequests() / module.getTotalRequests();
                module.setAvailability(availability);
            }
        }

        return moduleMap;
    }

    /**
     * Get system-wide metrics summary.
     * @return System metrics
     */
    public SystemMetrics getSystemMetrics() {
        List<EndpointMetrics> allMetrics = getAllEndpointMetrics();

        SystemMetrics system = new SystemMetrics();
        system.setTotalEndpoints(allMetrics.size());

        for (EndpointMetrics em : allMetrics) {
            system.setTotalRequests(system.getTotalRequests() + em.getTotalRequests());
            system.setSuccessRequests(system.getSuccessRequests() + em.getSuccessRequests());
            system.setErrorRequests(system.getErrorRequests() + em.getErrorRequests());

            if (em.getLatencyP99Ms() > system.getWorstP99LatencyMs()) {
                system.setWorstP99LatencyMs(em.getLatencyP99Ms());
            }
        }

        if (system.getTotalRequests() > 0) {
            double availability = (double) system.getSuccessRequests() / system.getTotalRequests();
            system.setOverallAvailability(availability);
        }

        return system;
    }

    /**
     * Get endpoints with availability < 95% or p99 latency > 500ms.
     * @return List of unhealthy endpoints
     */
    public List<EndpointMetrics> getUnhealthyEndpoints() {
        return getAllEndpointMetrics().stream()
                .filter(em -> em.getTotalRequests() > 0) // Only consider endpoints with traffic
                .filter(em -> em.getAvailability() < 0.95 || em.getLatencyP99Ms() > 500)
                .sorted(Comparator.comparing(EndpointMetrics::getAvailability))
                .collect(Collectors.toList());
    }

    // Helper methods

    private String extractModuleName(String uri) {
        if (uri == null || uri.isEmpty() || "/".equals(uri)) {
            return "root";
        }

        // Extract first path segment (e.g., "/v1/auth/session" -> "auth")
        String[] parts = uri.split("/");
        for (String part : parts) {
            if (!part.isEmpty() && !"v1".equalsIgnoreCase(part) && !"v2".equalsIgnoreCase(part)) {
                return part;
            }
        }

        return "other";
    }

    private double getPercentile(io.micrometer.core.instrument.Timer timer, double percentile) {
        try {
            // Try to get from histogram snapshot first (most accurate)
            io.micrometer.core.instrument.distribution.HistogramSnapshot snapshot = timer.takeSnapshot();
            io.micrometer.core.instrument.distribution.ValueAtPercentile[] percentiles = snapshot.percentileValues();
            if (percentiles != null && percentiles.length > 0) {
                for (io.micrometer.core.instrument.distribution.ValueAtPercentile vap : percentiles) {
                    if (Math.abs(vap.percentile() - percentile) < 0.01) {
                        return vap.value(TimeUnit.MILLISECONDS);
                    }
                }
            }

            // Fallback: use max or mean as approximation
            if (percentile >= 0.99) {
                return timer.max(TimeUnit.MILLISECONDS);
            } else if (percentile >= 0.95) {
                return (timer.max(TimeUnit.MILLISECONDS) + timer.mean(TimeUnit.MILLISECONDS)) / 2;
            } else {
                return timer.mean(TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to get percentile {} from timer: {}", percentile, e.getMessage());
            return 0.0;
        }
    }
}
