package io.strategiz.service.console.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.SystemHealthResponse;
import io.strategiz.service.console.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for system health and metrics.
 */
@RestController
@RequestMapping("/v1/console/system")
@Tag(name = "Admin - System", description = "System health and metrics endpoints for administrators")
public class AdminSystemController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final SystemMetricsService systemMetricsService;

	@Autowired
	public AdminSystemController(SystemMetricsService systemMetricsService) {
		this.systemMetricsService = systemMetricsService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	@GetMapping("/health")
	@Operation(summary = "Get system health", description = "Returns comprehensive system health information")
	public ResponseEntity<SystemHealthResponse> getSystemHealth(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getSystemHealth", adminUserId);

		SystemHealthResponse health = systemMetricsService.getSystemHealth();
		return ResponseEntity.ok(health);
	}

	@GetMapping("/metrics")
	@Operation(summary = "Get detailed metrics", description = "Returns detailed JVM and system metrics")
	public ResponseEntity<Map<String, Object>> getMetrics(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getMetrics", adminUserId);

		Map<String, Object> metrics = systemMetricsService.getDetailedMetrics();
		return ResponseEntity.ok(metrics);
	}

}
