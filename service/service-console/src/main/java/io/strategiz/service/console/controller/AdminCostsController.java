package io.strategiz.service.console.controller;

import io.strategiz.business.infrastructurecosts.model.CostPrediction;
import io.strategiz.business.infrastructurecosts.model.CostSummary;
import io.strategiz.business.infrastructurecosts.model.DailyCost;
import io.strategiz.business.infrastructurecosts.model.FirestoreUsage;
import io.strategiz.business.infrastructurecosts.service.CostAggregationService;
import io.strategiz.business.infrastructurecosts.service.CostPredictionService;
import io.strategiz.client.gcpbilling.GcpAssetInventoryClient;
import io.strategiz.client.gcpbilling.AiCostsAggregationService;
import io.strategiz.client.gcpbilling.model.GcpInfrastructureSummary;
import io.strategiz.client.gcpbilling.model.GcpResource;
import io.strategiz.client.gcpbilling.model.AiCostsSummary;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin controller for operating costs tracking and analysis. Provides endpoints for cost
 * summary, daily breakdowns, service costs, Firestore usage, and cost predictions.
 *
 * Enable with: gcp.billing.enabled=true and gcp.billing.demo-mode=false
 */
@RestController
@RequestMapping("/v1/console/costs")
@Tag(name = "Admin - Operating Costs", description = "Operating cost tracking and prediction endpoints")
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(name = "gcp.billing.demo-mode", havingValue = "false", matchIfMissing = false)
public class AdminCostsController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(AdminCostsController.class);

	private static final String MODULE_NAME = "CONSOLE";

	private final CostAggregationService costAggregationService;

	private final CostPredictionService costPredictionService;

	private final GcpAssetInventoryClient assetInventoryClient;

	private final Optional<AiCostsAggregationService> aiCostsService;

	// Primary constructor with all dependencies
	public AdminCostsController(CostAggregationService costAggregationService,
			CostPredictionService costPredictionService, GcpAssetInventoryClient assetInventoryClient,
			AiCostsAggregationService aiCostsService) {
		this.costAggregationService = costAggregationService;
		this.costPredictionService = costPredictionService;
		this.assetInventoryClient = assetInventoryClient;
		this.aiCostsService = Optional.ofNullable(aiCostsService);
	}

	// Fallback constructor when AI costs service is not available
	@Autowired
	public AdminCostsController(CostAggregationService costAggregationService,
			CostPredictionService costPredictionService, GcpAssetInventoryClient assetInventoryClient) {
		this(costAggregationService, costPredictionService, assetInventoryClient, null);
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Get current month cost summary
	 */
	@GetMapping("/summary")
	@Operation(summary = "Get current month cost summary",
			description = "Returns aggregated costs from GCP and ClickHouse for the current month")
	public ResponseEntity<CostSummary> getCostSummary(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting cost summary", adminUserId);

		CostSummary summary = costAggregationService.getCurrentMonthSummary();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get daily cost breakdown
	 */
	@GetMapping("/daily")
	@Operation(summary = "Get daily cost breakdown",
			description = "Returns daily cost breakdown for the specified number of days")
	public ResponseEntity<List<DailyCost>> getDailyCosts(HttpServletRequest request, @Parameter(
			description = "Number of days to retrieve (default: 30)") @RequestParam(defaultValue = "30") int days) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting daily costs for {} days", adminUserId, days);

		List<DailyCost> dailyCosts = costAggregationService.getDailyCosts(days);
		return ResponseEntity.ok(dailyCosts);
	}

	/**
	 * Get costs grouped by service
	 */
	@GetMapping("/by-service")
	@Operation(summary = "Get costs by service",
			description = "Returns current month costs grouped by service (Cloud Run, Firestore, ClickHouse, etc.)")
	public ResponseEntity<Map<String, BigDecimal>> getCostsByService(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting costs by service", adminUserId);

		Map<String, BigDecimal> costsByService = costAggregationService.getCostsByService();
		return ResponseEntity.ok(costsByService);
	}

	/**
	 * Get Firestore usage metrics
	 */
	@GetMapping("/firestore-usage")
	@Operation(summary = "Get Firestore usage metrics",
			description = "Returns Firestore read/write metrics per collection for the specified number of days")
	public ResponseEntity<List<FirestoreUsage>> getFirestoreUsage(HttpServletRequest request, @Parameter(
			description = "Number of days to retrieve (default: 7)") @RequestParam(defaultValue = "7") int days) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting Firestore usage for {} days", adminUserId, days);

		List<FirestoreUsage> usage = costAggregationService.getFirestoreUsage(days);
		return ResponseEntity.ok(usage);
	}

	/**
	 * Get cost prediction for current month
	 */
	@GetMapping("/prediction")
	@Operation(summary = "Get cost prediction",
			description = "Returns predicted end-of-month costs using weighted linear regression with confidence intervals")
	public ResponseEntity<CostPrediction> getCostPrediction(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting cost prediction", adminUserId);

		CostPrediction prediction = costPredictionService.predictMonthlyCosts();
		return ResponseEntity.ok(prediction);
	}

	/**
	 * Force refresh of cost data
	 */
	@PostMapping("/refresh")
	@Operation(summary = "Force refresh cost data",
			description = "Triggers immediate aggregation of cost data from GCP and ClickHouse")
	public ResponseEntity<CostSummary> refreshCosts(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} forcing cost data refresh", adminUserId);

		// Trigger the aggregation job manually
		costAggregationService.aggregateDailyCosts();

		// Return fresh summary
		CostSummary summary = costAggregationService.getCurrentMonthSummary();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get comprehensive infrastructure inventory
	 */
	@GetMapping("/infrastructure")
	@Operation(summary = "Get infrastructure inventory",
			description = "Returns comprehensive inventory of all GCP resources with estimated costs (uses FREE Cloud Asset API)")
	public ResponseEntity<GcpInfrastructureSummary> getInfrastructure(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting infrastructure inventory", adminUserId);

		GcpInfrastructureSummary summary = assetInventoryClient.getInfrastructureSummary();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get resources filtered by type
	 */
	@GetMapping("/infrastructure/by-type/{resourceType}")
	@Operation(summary = "Get resources by type",
			description = "Returns all resources of a specific type (e.g., 'Cloud Run', 'Firestore', 'Cloud Storage')")
	public ResponseEntity<List<GcpResource>> getResourcesByType(HttpServletRequest request, @Parameter(
			description = "Resource type (e.g., 'Cloud Run', 'Firestore')") @PathVariable String resourceType) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting resources of type: {}", adminUserId, resourceType);

		List<GcpResource> resources = assetInventoryClient.getResourcesByType(resourceType);
		return ResponseEntity.ok(resources);
	}

	/**
	 * Get resources filtered by region
	 */
	@GetMapping("/infrastructure/by-region/{region}")
	@Operation(summary = "Get resources by region",
			description = "Returns all resources in a specific GCP region (e.g., 'us-central1', 'us-east1')")
	public ResponseEntity<List<GcpResource>> getResourcesByRegion(HttpServletRequest request,
			@Parameter(description = "GCP region (e.g., 'us-central1', 'us-east1')") @PathVariable String region) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting resources in region: {}", adminUserId, region);

		List<GcpResource> resources = assetInventoryClient.getResourcesByRegion(region);
		return ResponseEntity.ok(resources);
	}

	/**
	 * Get AI/LLM costs summary
	 */
	@GetMapping("/ai-costs/summary")
	@Operation(summary = "Get AI/LLM costs summary",
			description = "Returns aggregated AI costs from Anthropic Claude, OpenAI, xAI Grok, and personal dev accounts")
	public ResponseEntity<AiCostsSummary> getAiCostsSummary(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting AI costs summary", adminUserId);

		if (aiCostsService.isEmpty()) {
			log.warn("AI costs tracking is disabled (ai.billing.enabled=false)");
			java.time.LocalDate today = java.time.LocalDate.now();
			java.time.LocalDate startOfMonth = today.withDayOfMonth(1);
			return ResponseEntity.ok(AiCostsSummary.empty(startOfMonth, today));
		}

		AiCostsSummary summary = aiCostsService.get().getCurrentMonthAiCosts();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get AI costs breakdown by model
	 */
	@GetMapping("/ai-costs/by-model")
	@Operation(summary = "Get AI costs by model",
			description = "Returns current month AI costs grouped by model (gpt-4, claude-opus-4, grok-4, etc.)")
	public ResponseEntity<Map<String, BigDecimal>> getAiCostsByModel(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting AI costs by model", adminUserId);

		if (aiCostsService.isEmpty()) {
			log.warn("AI costs tracking is disabled (ai.billing.enabled=false)");
			return ResponseEntity.ok(Map.of());
		}

		java.time.LocalDate today = java.time.LocalDate.now();
		java.time.LocalDate startOfMonth = today.withDayOfMonth(1);
		Map<String, BigDecimal> costsByModel = aiCostsService.get().getCostsByModel(startOfMonth, today);
		return ResponseEntity.ok(costsByModel);
	}

	/**
	 * Get daily AI costs for trend analysis
	 */
	@GetMapping("/ai-costs/daily")
	@Operation(summary = "Get daily AI costs",
			description = "Returns daily AI cost breakdown for the specified number of days")
	public ResponseEntity<List<Map<String, Object>>> getDailyAiCosts(HttpServletRequest request, @Parameter(
			description = "Number of days to retrieve (default: 30)") @RequestParam(defaultValue = "30") int days) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting daily AI costs for {} days", adminUserId, days);

		if (aiCostsService.isEmpty()) {
			log.warn("AI costs tracking is disabled (ai.billing.enabled=false)");
			return ResponseEntity.ok(List.of());
		}

		List<Map<String, Object>> dailyCosts = aiCostsService.get().getDailyAiCosts(days);
		return ResponseEntity.ok(dailyCosts);
	}

}
