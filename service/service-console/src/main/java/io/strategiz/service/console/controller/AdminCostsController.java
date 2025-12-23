package io.strategiz.service.console.controller;

import io.strategiz.business.infrastructurecosts.model.CostPrediction;
import io.strategiz.business.infrastructurecosts.model.CostSummary;
import io.strategiz.business.infrastructurecosts.model.DailyCost;
import io.strategiz.business.infrastructurecosts.model.FirestoreUsage;
import io.strategiz.business.infrastructurecosts.service.CostAggregationService;
import io.strategiz.business.infrastructurecosts.service.CostPredictionService;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for infrastructure costs tracking and analysis.
 * Provides endpoints for cost summary, daily breakdowns, service costs,
 * Firestore usage, and cost predictions.
 *
 * Enable with: gcp.billing.enabled=true
 */
@RestController
@RequestMapping("/v1/console/costs")
@Tag(name = "Admin - Infrastructure Costs", description = "Infrastructure cost tracking and prediction endpoints")
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class AdminCostsController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AdminCostsController.class);
    private static final String MODULE_NAME = "CONSOLE";

    private final CostAggregationService costAggregationService;
    private final CostPredictionService costPredictionService;

    public AdminCostsController(
            CostAggregationService costAggregationService,
            CostPredictionService costPredictionService) {
        this.costAggregationService = costAggregationService;
        this.costPredictionService = costPredictionService;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Get current month cost summary
     */
    @GetMapping("/summary")
    @Operation(
            summary = "Get current month cost summary",
            description = "Returns aggregated costs from GCP and TimescaleDB for the current month"
    )
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
    @Operation(
            summary = "Get daily cost breakdown",
            description = "Returns daily cost breakdown for the specified number of days"
    )
    public ResponseEntity<List<DailyCost>> getDailyCosts(
            HttpServletRequest request,
            @Parameter(description = "Number of days to retrieve (default: 30)")
            @RequestParam(defaultValue = "30") int days) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        log.info("Admin {} requesting daily costs for {} days", adminUserId, days);

        List<DailyCost> dailyCosts = costAggregationService.getDailyCosts(days);
        return ResponseEntity.ok(dailyCosts);
    }

    /**
     * Get costs grouped by service
     */
    @GetMapping("/by-service")
    @Operation(
            summary = "Get costs by service",
            description = "Returns current month costs grouped by service (Cloud Run, Firestore, TimescaleDB, etc.)"
    )
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
    @Operation(
            summary = "Get Firestore usage metrics",
            description = "Returns Firestore read/write metrics per collection for the specified number of days"
    )
    public ResponseEntity<List<FirestoreUsage>> getFirestoreUsage(
            HttpServletRequest request,
            @Parameter(description = "Number of days to retrieve (default: 7)")
            @RequestParam(defaultValue = "7") int days) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        log.info("Admin {} requesting Firestore usage for {} days", adminUserId, days);

        List<FirestoreUsage> usage = costAggregationService.getFirestoreUsage(days);
        return ResponseEntity.ok(usage);
    }

    /**
     * Get cost prediction for current month
     */
    @GetMapping("/prediction")
    @Operation(
            summary = "Get cost prediction",
            description = "Returns predicted end-of-month costs using weighted linear regression with confidence intervals"
    )
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
    @Operation(
            summary = "Force refresh cost data",
            description = "Triggers immediate aggregation of cost data from GCP and TimescaleDB"
    )
    public ResponseEntity<CostSummary> refreshCosts(HttpServletRequest request) {
        String adminUserId = (String) request.getAttribute("adminUserId");
        log.info("Admin {} forcing cost data refresh", adminUserId);

        // Trigger the aggregation job manually
        costAggregationService.aggregateDailyCosts();

        // Return fresh summary
        CostSummary summary = costAggregationService.getCurrentMonthSummary();
        return ResponseEntity.ok(summary);
    }
}
