package io.strategiz.service.monitoring.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.monitoring.service.ExecutionMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for observability metrics.
 * Provides REST APIs for fetching metrics from Grafana Cloud.
 */
@RestController
@RequestMapping("/v1/console/observability")
@Tag(name = "Admin - Observability", description = "Observability metrics endpoints for administrators")
public class AdminObservabilityController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final ExecutionMetricsService executionMetricsService;

    @Autowired
    public AdminObservabilityController(ExecutionMetricsService executionMetricsService) {
        this.executionMetricsService = executionMetricsService;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @GetMapping("/execution/health")
    @Operation(
        summary = "Get execution service health",
        description = "Returns current health metrics for the strategy execution service including success rate, latency, and cache performance"
    )
    public ResponseEntity<Map<String, Object>> getExecutionHealth(HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getExecutionHealth", adminUserId);

        Map<String, Object> health = executionMetricsService.getExecutionHealth();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/execution/latency")
    @Operation(
        summary = "Get execution latency metrics",
        description = "Returns latency percentiles (p50, p95, p99) over the specified time period"
    )
    public ResponseEntity<Map<String, Object>> getExecutionLatency(
            HttpServletRequest request,
            @Parameter(description = "Duration in minutes to look back (default: 60)")
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getExecutionLatency", adminUserId);

        Map<String, Object> latency = executionMetricsService.getExecutionLatency(durationMinutes);
        return ResponseEntity.ok(latency);
    }

    @GetMapping("/execution/throughput")
    @Operation(
        summary = "Get execution throughput metrics",
        description = "Returns request throughput by status (success/failure) over the specified time period"
    )
    public ResponseEntity<Map<String, Object>> getExecutionThroughput(
            HttpServletRequest request,
            @Parameter(description = "Duration in minutes to look back (default: 60)")
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getExecutionThroughput", adminUserId);

        Map<String, Object> throughput = executionMetricsService.getExecutionThroughput(durationMinutes);
        return ResponseEntity.ok(throughput);
    }

    @GetMapping("/execution/cache")
    @Operation(
        summary = "Get cache performance metrics",
        description = "Returns cache hits, misses, and hit rate over the specified time period"
    )
    public ResponseEntity<Map<String, Object>> getCachePerformance(
            HttpServletRequest request,
            @Parameter(description = "Duration in minutes to look back (default: 60)")
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getCachePerformance", adminUserId);

        Map<String, Object> cache = executionMetricsService.getCachePerformance(durationMinutes);
        return ResponseEntity.ok(cache);
    }

    @GetMapping("/execution/errors")
    @Operation(
        summary = "Get execution error metrics",
        description = "Returns error counts grouped by error type over the specified time period"
    )
    public ResponseEntity<Map<String, Object>> getExecutionErrors(
            HttpServletRequest request,
            @Parameter(description = "Duration in minutes to look back (default: 60)")
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getExecutionErrors", adminUserId);

        Map<String, Object> errors = executionMetricsService.getExecutionErrors(durationMinutes);
        return ResponseEntity.ok(errors);
    }
}
