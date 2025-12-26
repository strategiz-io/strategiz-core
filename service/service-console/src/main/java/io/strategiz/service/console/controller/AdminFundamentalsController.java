package io.strategiz.service.console.controller;

import io.strategiz.batch.fundamentals.FundamentalsBackfillJob;
import io.strategiz.batch.fundamentals.FundamentalsIncrementalJob;
import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
import io.strategiz.data.fundamentals.timescale.entity.FundamentalsTimescaleEntity;
import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing company fundamentals data collection.
 *
 * <p>
 * Endpoints:
 * - POST /v1/console/fundamentals/jobs/incremental/trigger - Trigger daily update job
 * - POST /v1/console/fundamentals/jobs/backfill/trigger - Trigger full backfill
 * - POST /v1/console/fundamentals/jobs/backfill/custom - Custom backfill with specific symbols
 * - GET /v1/console/fundamentals/jobs/incremental/status - Get incremental job status
 * - GET /v1/console/fundamentals/jobs/backfill/status - Get backfill job status
 * - GET /v1/console/fundamentals/data/{symbol} - Get fundamentals data for symbol
 * - GET /v1/console/fundamentals/status - Get service status
 * </p>
 */
@RestController
@RequestMapping("/v1/console/fundamentals")
@Tag(name = "Admin - Fundamentals", description = "Fundamentals data management endpoints for administrators")
public class AdminFundamentalsController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	private final FundamentalsIncrementalJob incrementalJob;

	private final FundamentalsBackfillJob backfillJob;

	private final FundamentalsQueryService queryService;

	@Autowired
	public AdminFundamentalsController(FundamentalsIncrementalJob incrementalJob, FundamentalsBackfillJob backfillJob,
			FundamentalsQueryService queryService) {
		this.incrementalJob = incrementalJob;
		this.backfillJob = backfillJob;
		this.queryService = queryService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	// ========== Job Trigger Endpoints ==========

	/**
	 * Trigger daily incremental job manually.
	 *
	 * POST /v1/console/fundamentals/jobs/incremental/trigger
	 *
	 * Updates fundamentals for all configured symbols.
	 */
	@PostMapping("/jobs/incremental/trigger")
	@Operation(summary = "Trigger incremental fundamentals update",
			description = "Manually triggers daily fundamentals update for all symbols")
	public ResponseEntity<Map<String, Object>> triggerIncremental(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("triggerIncremental", adminUserId);

		try {
			CollectionResult result = incrementalJob.triggerManualExecution();

			Map<String, Object> response = new HashMap<>();
			response.put("status", result.getErrorCount() == 0 ? "success" : "partial_success");
			response.put("message", "Incremental fundamentals collection completed");
			response.put("totalSymbols", result.getTotalSymbols());
			response.put("successCount", result.getSuccessCount());
			response.put("errorCount", result.getErrorCount());
			response.put("durationSeconds", result.getDurationSeconds());

			log.info("Incremental fundamentals job triggered by admin {}: {} symbols, {} success, {} errors, {}s",
					adminUserId, result.getTotalSymbols(), result.getSuccessCount(), result.getErrorCount(),
					result.getDurationSeconds());

			return ResponseEntity.ok(response);
		}
		catch (Exception ex) {
			log.error("Incremental fundamentals job failed", ex);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Incremental job failed: " + ex.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Trigger full backfill for all symbols.
	 *
	 * POST /v1/console/fundamentals/jobs/backfill/trigger
	 *
	 * Fetches fundamentals for all configured symbols. WARNING: Resource-intensive operation.
	 */
	@PostMapping("/jobs/backfill/trigger")
	@Operation(summary = "Trigger full fundamentals backfill",
			description = "Manually triggers full backfill for all symbols (resource-intensive)")
	public ResponseEntity<Map<String, Object>> triggerBackfill(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("triggerBackfill", adminUserId);

		try {
			CollectionResult result = backfillJob.executeBackfill();

			Map<String, Object> response = new HashMap<>();
			response.put("status", result.getErrorCount() == 0 ? "success" : "partial_success");
			response.put("message", "Full fundamentals backfill completed");
			response.put("totalSymbols", result.getTotalSymbols());
			response.put("successCount", result.getSuccessCount());
			response.put("errorCount", result.getErrorCount());
			response.put("durationSeconds", result.getDurationSeconds());

			log.info("Full fundamentals backfill triggered by admin {}: {} symbols, {} success, {} errors, {}s",
					adminUserId, result.getTotalSymbols(), result.getSuccessCount(), result.getErrorCount(),
					result.getDurationSeconds());

			return ResponseEntity.ok(response);
		}
		catch (Exception ex) {
			log.error("Full fundamentals backfill failed", ex);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Backfill failed: " + ex.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Trigger custom backfill for specific symbols.
	 *
	 * POST /v1/console/fundamentals/jobs/backfill/custom Request body: { "symbols":
	 * ["AAPL", "MSFT", "GOOGL"] }
	 */
	@PostMapping("/jobs/backfill/custom")
	@Operation(summary = "Trigger custom fundamentals backfill",
			description = "Manually triggers backfill for specified symbols")
	public ResponseEntity<Map<String, Object>> triggerCustomBackfill(@RequestBody CustomBackfillRequest request,
			HttpServletRequest httpRequest) {
		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("triggerCustomBackfill", adminUserId, "symbols=" + request.symbols);

		try {
			CollectionResult result = backfillJob.executeBackfill(request.symbols);

			Map<String, Object> response = new HashMap<>();
			response.put("status", result.getErrorCount() == 0 ? "success" : "partial_success");
			response.put("message", "Custom fundamentals backfill completed");
			response.put("symbols", request.symbols);
			response.put("totalSymbols", result.getTotalSymbols());
			response.put("successCount", result.getSuccessCount());
			response.put("errorCount", result.getErrorCount());
			response.put("durationSeconds", result.getDurationSeconds());

			log.info("Custom fundamentals backfill triggered by admin {}: {} symbols, {} success, {} errors, {}s",
					adminUserId, result.getTotalSymbols(), result.getSuccessCount(), result.getErrorCount(),
					result.getDurationSeconds());

			return ResponseEntity.ok(response);
		}
		catch (Exception ex) {
			log.error("Custom fundamentals backfill failed", ex);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Custom backfill failed: " + ex.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	// ========== Job Status Endpoints ==========

	/**
	 * Get incremental job status.
	 *
	 * GET /v1/console/fundamentals/jobs/incremental/status
	 */
	@GetMapping("/jobs/incremental/status")
	@Operation(summary = "Get incremental job status", description = "Returns current status of incremental job")
	public ResponseEntity<Map<String, Object>> getIncrementalStatus(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getIncrementalStatus", adminUserId);

		Map<String, Object> status = new HashMap<>();
		status.put("enabled", incrementalJob.isEnabled());
		status.put("running", incrementalJob.isRunning());

		return ResponseEntity.ok(status);
	}

	/**
	 * Get backfill job status.
	 *
	 * GET /v1/console/fundamentals/jobs/backfill/status
	 */
	@GetMapping("/jobs/backfill/status")
	@Operation(summary = "Get backfill job status", description = "Returns current status of backfill job")
	public ResponseEntity<Map<String, Object>> getBackfillStatus(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getBackfillStatus", adminUserId);

		Map<String, Object> status = new HashMap<>();
		status.put("running", backfillJob.isRunning());

		return ResponseEntity.ok(status);
	}

	// ========== Data Query Endpoints ==========

	/**
	 * Get fundamentals data for a specific symbol.
	 *
	 * GET /v1/console/fundamentals/data/{symbol}
	 */
	@GetMapping("/data/{symbol}")
	@Operation(summary = "Get fundamentals for symbol", description = "Returns latest fundamentals data for a symbol")
	public ResponseEntity<Map<String, Object>> getFundamentals(
			@Parameter(description = "Stock symbol") @PathVariable String symbol, HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getFundamentals", adminUserId, "symbol=" + symbol);

		try {
			FundamentalsTimescaleEntity fundamentals = queryService.getLatestFundamentals(symbol);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("symbol", fundamentals.getSymbol());
			response.put("fiscalPeriod", fundamentals.getFiscalPeriod());
			response.put("periodType", fundamentals.getPeriodType());
			response.put("data", convertToMap(fundamentals));

			return ResponseEntity.ok(response);
		}
		catch (Exception ex) {
			log.error("Failed to get fundamentals for symbol: {}", symbol, ex);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Failed to get fundamentals: " + ex.getMessage());

			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Get service status and configuration.
	 *
	 * GET /v1/console/fundamentals/status
	 */
	@GetMapping("/status")
	@Operation(summary = "Get fundamentals service status", description = "Returns service status and configuration")
	public ResponseEntity<Map<String, Object>> getStatus(HttpServletRequest request) {
		String adminUserId = (String) request.getAttribute("adminUserId");
		logRequest("getStatus", adminUserId);

		Map<String, Object> status = new HashMap<>();
		status.put("status", "operational");
		status.put("service", "FundamentalsCollectionService");
		status.put("dataSource", "YAHOO_FINANCE");
		status.put("incrementalJobEnabled", incrementalJob.isEnabled());
		status.put("incrementalJobRunning", incrementalJob.isRunning());
		status.put("backfillJobRunning", backfillJob.isRunning());

		Map<String, String> endpoints = new HashMap<>();
		endpoints.put("triggerIncremental", "POST /v1/console/fundamentals/jobs/incremental/trigger");
		endpoints.put("triggerBackfill", "POST /v1/console/fundamentals/jobs/backfill/trigger");
		endpoints.put("triggerCustomBackfill", "POST /v1/console/fundamentals/jobs/backfill/custom");
		endpoints.put("incrementalStatus", "GET /v1/console/fundamentals/jobs/incremental/status");
		endpoints.put("backfillStatus", "GET /v1/console/fundamentals/jobs/backfill/status");
		endpoints.put("getData", "GET /v1/console/fundamentals/data/{symbol}");
		status.put("availableEndpoints", endpoints);

		return ResponseEntity.ok(status);
	}

	// ========== Helper Methods ==========

	/**
	 * Convert FundamentalsTimescaleEntity to Map for JSON response.
	 */
	private Map<String, Object> convertToMap(FundamentalsTimescaleEntity entity) {
		Map<String, Object> map = new HashMap<>();

		// Income Statement
		putIfNotNull(map, "revenue", entity.getRevenue());
		putIfNotNull(map, "grossProfit", entity.getGrossProfit());
		putIfNotNull(map, "operatingIncome", entity.getOperatingIncome());
		putIfNotNull(map, "ebitda", entity.getEbitda());
		putIfNotNull(map, "netIncome", entity.getNetIncome());
		putIfNotNull(map, "epsBasic", entity.getEpsBasic());
		putIfNotNull(map, "epsDiluted", entity.getEpsDiluted());

		// Margins & Profitability
		putIfNotNull(map, "grossMargin", entity.getGrossMargin());
		putIfNotNull(map, "operatingMargin", entity.getOperatingMargin());
		putIfNotNull(map, "profitMargin", entity.getProfitMargin());
		putIfNotNull(map, "returnOnEquity", entity.getReturnOnEquity());
		putIfNotNull(map, "returnOnAssets", entity.getReturnOnAssets());

		// Valuation Ratios
		putIfNotNull(map, "priceToEarnings", entity.getPriceToEarnings());
		putIfNotNull(map, "priceToBook", entity.getPriceToBook());
		putIfNotNull(map, "priceToSales", entity.getPriceToSales());
		putIfNotNull(map, "pegRatio", entity.getPegRatio());
		putIfNotNull(map, "enterpriseValue", entity.getEnterpriseValue());
		putIfNotNull(map, "evToEbitda", entity.getEvToEbitda());

		// Balance Sheet
		putIfNotNull(map, "totalAssets", entity.getTotalAssets());
		putIfNotNull(map, "totalLiabilities", entity.getTotalLiabilities());
		putIfNotNull(map, "shareholdersEquity", entity.getShareholdersEquity());
		putIfNotNull(map, "currentAssets", entity.getCurrentAssets());
		putIfNotNull(map, "currentLiabilities", entity.getCurrentLiabilities());
		putIfNotNull(map, "totalDebt", entity.getTotalDebt());
		putIfNotNull(map, "cashAndEquivalents", entity.getCashAndEquivalents());

		// Liquidity & Leverage
		putIfNotNull(map, "currentRatio", entity.getCurrentRatio());
		putIfNotNull(map, "quickRatio", entity.getQuickRatio());
		putIfNotNull(map, "debtToEquity", entity.getDebtToEquity());
		putIfNotNull(map, "debtToAssets", entity.getDebtToAssets());

		// Dividends
		putIfNotNull(map, "dividendPerShare", entity.getDividendPerShare());
		putIfNotNull(map, "dividendYield", entity.getDividendYield());
		putIfNotNull(map, "payoutRatio", entity.getPayoutRatio());

		// Share Info
		putIfNotNull(map, "sharesOutstanding", entity.getSharesOutstanding());
		putIfNotNull(map, "marketCap", entity.getMarketCap());
		putIfNotNull(map, "bookValuePerShare", entity.getBookValuePerShare());

		// Growth Metrics
		putIfNotNull(map, "revenueGrowthYoy", entity.getRevenueGrowthYoy());
		putIfNotNull(map, "epsGrowthYoy", entity.getEpsGrowthYoy());

		return map;
	}

	/**
	 * Helper: Put value in map only if not null.
	 */
	private void putIfNotNull(Map<String, Object> map, String key, Object value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	// ========== Request DTOs ==========

	/**
	 * Request DTO for custom backfill.
	 */
	public static class CustomBackfillRequest {

		public List<String> symbols;

	}

}
