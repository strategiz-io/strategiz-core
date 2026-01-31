package io.strategiz.service.console.observability.controller;

import io.strategiz.service.console.observability.model.StatusResponse;
import io.strategiz.service.base.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints Provides endpoints to check the health of various
 * services
 */
@RestController
@RequestMapping("/api")
public class HealthCheckController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-monitoring";
	}

	private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Basic health check endpoint
	 * @return Status information
	 */
	@GetMapping("/health")
	public Map<String, Object> healthCheck() {
		log.info("Basic health check endpoint called");
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("message", "Strategiz Core API is running");
		response.put("timestamp", System.currentTimeMillis());
		return response;
	}

	/**
	 * Detailed health check for system components
	 * @return Detailed health status of various components
	 */
	@GetMapping("/system/health")
	public ResponseEntity<Map<String, Object>> systemHealthCheck() {
		log.info("System health check endpoint called");

		Map<String, Object> response = new HashMap<>();
		Map<String, Object> components = new HashMap<>();
		boolean allHealthy = true;

		// Check API status
		components.put("api", Map.of("status", "UP", "message", "API is running normally"));

		// Check database connection
		try {
			// This would be replaced with actual DB health check in a real implementation
			components.put("database", Map.of("status", "UP", "message", "Database connection is active"));
		}
		catch (Exception e) {
			log.error("Database health check failed", e);
			components.put("database",
					Map.of("status", "DOWN", "message", "Database connection failed: " + e.getMessage()));
			allHealthy = false;
		}

		// Add overall status
		response.put("status", allHealthy ? "UP" : "DEGRADED");
		response.put("components", components);
		response.put("timestamp", System.currentTimeMillis());

		HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return new ResponseEntity<>(response, httpStatus);
	}

	/**
	 * Version information endpoint
	 * @return Version details
	 */
	@GetMapping("/version")
	public Map<String, Object> versionInfo() {
		log.info("Version info endpoint called");

		Map<String, Object> response = new HashMap<>();
		response.put("application", "Strategiz Core API");
		response.put("version", "1.0.0"); // This would come from a properties file in a
											// real implementation
		response.put("buildDate", "2025-05-24");
		response.put("environment", "development");
		response.put("timestamp", System.currentTimeMillis());

		return response;
	}

}
