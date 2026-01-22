package io.strategiz.service.console.controller;

import io.strategiz.business.aichat.costs.LLMCostAggregator;
import io.strategiz.business.aichat.costs.LLMCostAlertService;
import io.strategiz.business.aichat.costs.model.CostAlert;
import io.strategiz.business.aichat.costs.model.LLMCostSummary;
import io.strategiz.business.aichat.costs.model.ModelCostBreakdown;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
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
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for LLM billing and cost tracking. Provides endpoints for aggregated LLM costs
 * across all providers (OpenAI, Anthropic, GCP Vertex AI, xAI).
 *
 * This controller uses the new direct billing API integrations: - OpenAI: Usage API
 * (/v1/organization/usage/completions, /v1/organization/costs) - Anthropic: Admin API
 * (/v1/organizations/usage_report/messages, /v1/organizations/cost_report) - GCP: BigQuery
 * billing export for Vertex AI
 *
 * Enable with: llm.billing.enabled=true
 */
@RestController
@RequestMapping("/v1/console/llm-costs")
@Tag(name = "Admin - LLM Costs", description = "LLM cost tracking across all providers (OpenAI, Anthropic, GCP, xAI)")
@ConditionalOnProperty(name = "llm.billing.enabled", havingValue = "true", matchIfMissing = false)
public class AdminLLMCostsController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(AdminLLMCostsController.class);

	private static final String MODULE_NAME = "CONSOLE";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final LLMCostAggregator costAggregator;

	private final LLMCostAlertService alertService;

	public AdminLLMCostsController(LLMCostAggregator costAggregator,
			@org.springframework.lang.Nullable LLMCostAlertService alertService) {
		this.costAggregator = costAggregator;
		this.alertService = alertService;
		log.info("AdminLLMCostsController initialized with {} enabled providers, alerts: {}",
				costAggregator.getEnabledProviders().size(), alertService != null);
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Get comprehensive LLM cost summary for a date range
	 */
	@GetMapping("/summary")
	@Operation(summary = "Get LLM cost summary",
			description = "Returns aggregated LLM costs from all enabled providers with comparison to previous period")
	public ResponseEntity<LLMCostSummary> getCostSummary(HttpServletRequest request,
			@Parameter(description = "Start date (YYYY-MM-DD), defaults to first of current month") @RequestParam(
					required = false) String startDate,
			@Parameter(description = "End date (YYYY-MM-DD), defaults to today") @RequestParam(
					required = false) String endDate) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM cost summary", adminUserId);

		LocalDate start = parseDate(startDate, LocalDate.now().withDayOfMonth(1));
		LocalDate end = parseDate(endDate, LocalDate.now());

		LLMCostSummary summary = costAggregator.getCostSummary(start, end).block();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get current month LLM cost summary
	 */
	@GetMapping("/summary/current-month")
	@Operation(summary = "Get current month LLM costs",
			description = "Returns LLM costs for the current month with comparison to previous month")
	public ResponseEntity<LLMCostSummary> getCurrentMonthCosts(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting current month LLM costs", adminUserId);

		LLMCostSummary summary = costAggregator.getCurrentMonthCosts().block();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get LLM costs for the last N days
	 */
	@GetMapping("/summary/last-{days}-days")
	@Operation(summary = "Get LLM costs for last N days",
			description = "Returns LLM costs for the specified number of days with period-over-period comparison")
	public ResponseEntity<LLMCostSummary> getLastNDaysCosts(HttpServletRequest request,
			@Parameter(description = "Number of days (e.g., 7, 30, 90)") @PathVariable int days) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM costs for last {} days", adminUserId, days);

		if (days < 1 || days > 365) {
			return ResponseEntity.badRequest().build();
		}

		LLMCostSummary summary = costAggregator.getLastNDaysCosts(days).block();
		return ResponseEntity.ok(summary);
	}

	/**
	 * Get LLM costs breakdown by provider
	 */
	@GetMapping("/by-provider")
	@Operation(summary = "Get costs by provider",
			description = "Returns LLM cost breakdown for each provider (OpenAI, Anthropic, Google, xAI)")
	public ResponseEntity<List<ProviderCostReport>> getCostsByProvider(HttpServletRequest request,
			@Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
			@Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM costs by provider", adminUserId);

		LocalDate start = parseDate(startDate, LocalDate.now().withDayOfMonth(1));
		LocalDate end = parseDate(endDate, LocalDate.now());

		List<ProviderCostReport> reports = costAggregator.getCostsByProvider(start, end).block();
		return ResponseEntity.ok(reports);
	}

	/**
	 * Get LLM costs breakdown by model
	 */
	@GetMapping("/by-model")
	@Operation(summary = "Get costs by model",
			description = "Returns LLM cost breakdown for each model (gpt-4o, claude-opus-4-5, grok-4, etc.) sorted by cost")
	public ResponseEntity<List<ModelCostBreakdown>> getCostsByModel(HttpServletRequest request,
			@Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
			@Parameter(description = "End date (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM costs by model", adminUserId);

		LocalDate start = parseDate(startDate, LocalDate.now().withDayOfMonth(1));
		LocalDate end = parseDate(endDate, LocalDate.now());

		List<ModelCostBreakdown> models = costAggregator.getCostsByModel(start, end).block();
		return ResponseEntity.ok(models);
	}

	/**
	 * Get list of enabled billing providers
	 */
	@GetMapping("/providers")
	@Operation(summary = "Get enabled providers",
			description = "Returns list of LLM providers that have billing enabled and configured")
	public ResponseEntity<Map<String, Object>> getEnabledProviders(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting enabled LLM billing providers", adminUserId);

		List<String> providers = costAggregator.getEnabledProviders();
		return ResponseEntity.ok(Map.of("providers", providers, "count", providers.size(), "billingEnabled",
				costAggregator.hasBillingEnabled()));
	}

	/**
	 * Force refresh of LLM cost data
	 */
	@PostMapping("/refresh")
	@Operation(summary = "Refresh LLM cost data",
			description = "Triggers immediate fetch of cost data from all enabled provider billing APIs")
	public ResponseEntity<LLMCostSummary> refreshCosts(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} forcing LLM cost data refresh", adminUserId);

		// Fetch fresh data from all providers
		LLMCostSummary summary = costAggregator.getCurrentMonthCosts().block();

		log.info("LLM cost refresh complete: ${} total from {} providers", summary.getTotalCost(),
				costAggregator.getEnabledProviders().size());

		return ResponseEntity.ok(summary);
	}

	/**
	 * Get cost alerts based on current spending
	 */
	@GetMapping("/alerts")
	@Operation(summary = "Get cost alerts",
			description = "Returns list of active cost alerts based on budget thresholds and spending patterns")
	public ResponseEntity<List<CostAlert>> getAlerts(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM cost alerts", adminUserId);

		if (alertService == null) {
			return ResponseEntity.ok(List.of());
		}

		List<CostAlert> alerts = alertService.generateAlerts().block();
		return ResponseEntity.ok(alerts != null ? alerts : List.of());
	}

	/**
	 * Get budget configuration
	 */
	@GetMapping("/budget")
	@Operation(summary = "Get budget configuration",
			description = "Returns current budget thresholds and alert configuration")
	public ResponseEntity<Map<String, Object>> getBudgetConfig(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		log.info("Admin {} requesting LLM budget configuration", adminUserId);

		if (alertService == null) {
			return ResponseEntity.ok(Map.of("configured", false, "message", "Alert service not enabled"));
		}

		LLMCostAlertService.BudgetInfo budget = alertService.getBudgetInfo();
		return ResponseEntity.ok(Map.of("configured", true, "monthlyBudget", budget.getMonthlyBudget(), "dailyThreshold",
				budget.getDailyThreshold(), "warningPercent", budget.getWarningPercent(), "criticalPercent",
				budget.getCriticalPercent(), "alertsEnabled", budget.isAlertsEnabled()));
	}

	/**
	 * Parse date string with fallback to default
	 */
	private LocalDate parseDate(String dateStr, LocalDate defaultDate) {
		if (dateStr == null || dateStr.isBlank()) {
			return defaultDate;
		}
		try {
			return LocalDate.parse(dateStr, DATE_FORMAT);
		}
		catch (DateTimeParseException e) {
			log.warn("Invalid date format: {}, using default: {}", dateStr, defaultDate);
			return defaultDate;
		}
	}

}
