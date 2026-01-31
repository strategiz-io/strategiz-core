package io.strategiz.service.console.observability.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.observability.model.EndpointMetrics;
import io.strategiz.service.console.observability.model.ModuleMetrics;
import io.strategiz.service.console.observability.model.SystemMetrics;
import io.strategiz.service.console.observability.service.EndpointMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for real-time endpoint observability metrics.
 *
 * <p>
 * Provides availability and performance metrics for EVERY REST endpoint by querying
 * Spring Boot Actuator's Micrometer registry directly (no external dependencies).
 *
 * <p>
 * Metrics include:
 * <ul>
 * <li>Request counts (total, success, error)</li>
 * <li>Availability percentage</li>
 * <li>Latency percentiles (p50, p95, p99, max)</li>
 * <li>Error rates and breakdown by status/exception</li>
 * <li>Module-level aggregations</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/console/endpoints")
@Tag(name = "Admin - Endpoint Metrics", description = "Real-time availability and performance for all REST endpoints")
public class AdminEndpointMetricsController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final EndpointMetricsService endpointMetricsService;

	@Autowired
	public AdminEndpointMetricsController(EndpointMetricsService endpointMetricsService) {
		this.endpointMetricsService = endpointMetricsService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	@GetMapping
	@Operation(summary = "Get all endpoint metrics",
			description = "Returns availability and performance metrics for every discovered REST endpoint. "
					+ "Sorted by request count (most used endpoints first).")
	public ResponseEntity<List<EndpointMetrics>> getAllEndpoints(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getAllEndpoints", adminUserId);

		List<EndpointMetrics> metrics = endpointMetricsService.getAllEndpointMetrics();
		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/endpoint")
	@Operation(summary = "Get metrics for specific endpoint",
			description = "Returns detailed metrics for a single endpoint identified by HTTP method and URI pattern")
	public ResponseEntity<EndpointMetrics> getEndpoint(HttpServletRequest request,
			@Parameter(description = "HTTP method (GET, POST, etc.)") @RequestParam String method,
			@Parameter(description = "URI pattern (e.g., /v1/auth/login)") @RequestParam String uri) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getEndpoint", adminUserId);

		EndpointMetrics metrics = endpointMetricsService.getEndpointMetrics(method, uri);

		if (metrics == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/by-module")
	@Operation(summary = "Get aggregated metrics by service module",
			description = "Returns availability and performance metrics aggregated by service module "
					+ "(auth, console, provider, labs, etc.)")
	public ResponseEntity<Map<String, ModuleMetrics>> getMetricsByModule(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getMetricsByModule", adminUserId);

		Map<String, ModuleMetrics> metrics = endpointMetricsService.getMetricsByModule();
		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/system")
	@Operation(summary = "Get system-wide metrics",
			description = "Returns overall system health including total endpoints, requests, availability, and error rates")
	public ResponseEntity<SystemMetrics> getSystemMetrics(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getSystemMetrics", adminUserId);

		SystemMetrics metrics = endpointMetricsService.getSystemMetrics();
		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/health-check")
	@Operation(summary = "Quick health check for critical endpoints",
			description = "Returns a list of endpoints with availability < 95% or p99 latency > 500ms")
	public ResponseEntity<List<EndpointMetrics>> getUnhealthyEndpoints(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getUnhealthyEndpoints", adminUserId);

		List<EndpointMetrics> allMetrics = endpointMetricsService.getAllEndpointMetrics();

		// Filter for unhealthy endpoints
		List<EndpointMetrics> unhealthy = allMetrics.stream()
			.filter(m -> m.getTotalRequests() > 0) // Only endpoints with traffic
			.filter(m -> m.getAvailability() < 0.95 || m.getLatencyP99Ms() > 500)
			.toList();

		return ResponseEntity.ok(unhealthy);
	}

}
