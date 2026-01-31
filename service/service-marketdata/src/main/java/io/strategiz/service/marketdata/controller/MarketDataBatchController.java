package io.strategiz.service.marketdata.controller;

import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.business.marketdata.MarketDataCollectionService;
import io.strategiz.data.marketdata.constants.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin REST controller for managing market data collection.
 *
 * Endpoints: - POST /v1/marketdata/admin/backfill/full - Full backfill (configurable
 * months, all symbols) - POST /v1/marketdata/admin/backfill/test - Test backfill (3
 * symbols x 1 week) - POST /v1/marketdata/admin/backfill/custom - Custom backfill
 * (specific symbols/dates) - POST /v1/marketdata/admin/incremental - Force incremental
 * collection - POST /v1/marketdata/admin/incremental/all-timeframes - Incremental for all
 * timeframes - GET /v1/marketdata/admin/status - Get collection status/stats
 *
 * Security: Should be restricted to admin users only (add security annotations)
 *
 * Architecture: Controllers call services directly (business logic layer)
 *
 * Requires ClickHouse to be enabled.
 */
@RestController
@RequestMapping("/v1/marketdata/admin")
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class MarketDataBatchController {

	private static final Logger log = LoggerFactory.getLogger(MarketDataBatchController.class);

	private final MarketDataCollectionService collectionService;

	private final JobExecutionHistoryBusiness jobExecutionHistoryBusiness;

	private final io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository marketDataRepository;

	public MarketDataBatchController(MarketDataCollectionService collectionService,
			JobExecutionHistoryBusiness jobExecutionHistoryBusiness,
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository marketDataRepository) {
		this.collectionService = collectionService;
		this.jobExecutionHistoryBusiness = jobExecutionHistoryBusiness;
		this.marketDataRepository = marketDataRepository;
	}

	/**
	 * Execute full backfill - configurable years of data for all symbols
	 *
	 * POST /v1/marketdata/admin/backfill/full Request body: { "timeframes": ["1D", "1h",
	 * "1W", "1M"], "years": 7 } (optional)
	 *
	 * Processes all timeframes synchronously and returns results when complete.
	 *
	 * WARNING: This is resource-intensive and may take several hours
	 */
	@PostMapping("/backfill/full")
	public ResponseEntity<Map<String, Object>> executeFullBackfill(
			@RequestBody(required = false) BackfillRequest request) {

		List<String> timeframes = request != null && request.timeframes != null && !request.timeframes.isEmpty()
				? request.timeframes : Arrays.asList("1D", "1h", "1W", "1M");

		int years = request != null && request.years > 0 ? request.years : 7;

		log.info("=== Admin API: Full Backfill Request ===");
		log.info("Timeframes: {}", timeframes);
		log.info("Years: {}", years);

		LocalDateTime endDate = LocalDateTime.now(java.time.ZoneOffset.UTC);
		LocalDateTime startDate = endDate.minusYears(years);

		// Validate timeframes - only accept canonical short format (1H, 1D, 1W, 1M)
		List<String> validTimeframes = new ArrayList<>();
		for (String timeframe : timeframes) {
			String tf = timeframe.trim();
			if (!Timeframe.isValid(tf)) {
				log.warn("Skipping invalid timeframe: {} - must use short format (1H, 1D, 1W, 1M)", timeframe);
			}
			else {
				validTimeframes.add(tf);
			}
		}

		if (validTimeframes.isEmpty()) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "No valid timeframes provided");
			return ResponseEntity.badRequest().body(errorResponse);
		}

		// Record job execution start - include timeframes in display name
		String timeframeDisplay = validTimeframes.size() == 4 ? "All" : String.join(", ", validTimeframes);
		String context = years + "yr, S&P 500 symbols";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_BACKFILL",
				"Market Data Backfill (" + timeframeDisplay + ")", context);

		// Process all timeframes synchronously
		long overallStartTime = System.currentTimeMillis();
		int totalSymbolsProcessed = 0;
		int totalDataPointsStored = 0;
		int totalErrors = 0;
		List<Map<String, Object>> timeframeResults = new ArrayList<>();

		try {
			for (String timeframe : validTimeframes) {
				try {
					log.info("--- Processing timeframe: {} ---", timeframe);
					long tfStartTime = System.currentTimeMillis();

					MarketDataCollectionService.CollectionResult result = collectionService
						.backfillIntradayData(startDate, endDate, timeframe);

					long tfDuration = (System.currentTimeMillis() - tfStartTime) / 1000;

					totalSymbolsProcessed += result.totalSymbolsProcessed;
					totalDataPointsStored += result.totalDataPointsStored;
					totalErrors += result.errorCount;

					Map<String, Object> tfResult = new HashMap<>();
					tfResult.put("timeframe", timeframe);
					tfResult.put("symbolsProcessed", result.totalSymbolsProcessed);
					tfResult.put("dataPointsStored", result.totalDataPointsStored);
					tfResult.put("errors", result.errorCount);
					tfResult.put("durationSeconds", tfDuration);
					timeframeResults.add(tfResult);

					log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---", timeframe,
							tfDuration, result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount);

				}
				catch (Exception e) {
					log.error("Failed to process timeframe {}: {}", timeframe, e.getMessage(), e);
					totalErrors++;

					Map<String, Object> tfResult = new HashMap<>();
					tfResult.put("timeframe", timeframe);
					tfResult.put("error", e.getMessage());
					timeframeResults.add(tfResult);
				}
			}

			long overallDuration = (System.currentTimeMillis() - overallStartTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", totalErrors == 0 ? "success" : "partial_success");
			response.put("message", String.format("Backfill completed for %d year(s)", years));
			response.put("years", years);
			response.put("timeframesProcessed", validTimeframes.size());
			response.put("startDate", startDate.toString());
			response.put("endDate", endDate.toString());
			response.put("totalSymbolsProcessed", totalSymbolsProcessed);
			response.put("totalDataPointsStored", totalDataPointsStored);
			response.put("totalErrors", totalErrors);
			response.put("totalDurationSeconds", overallDuration);
			response.put("timeframeResults", timeframeResults);

			log.info("=== Full backfill completed in {}s: {} timeframes, {} symbols, {} bars, {} errors ===",
					overallDuration, validTimeframes.size(), totalSymbolsProcessed, totalDataPointsStored, totalErrors);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", totalSymbolsProcessed,
					totalDataPointsStored, totalErrors, null);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Full backfill failed with exception: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", totalSymbolsProcessed,
					totalDataPointsStored, totalErrors + 1, e.getMessage());

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Full backfill failed: " + e.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute test backfill - 3 symbols x 1 week for testing
	 *
	 * POST /v1/marketdata/admin/backfill/test
	 *
	 * Symbols: AAPL, MSFT, GOOGL Duration: 1 week of 1D bars
	 */
	@PostMapping("/backfill/test")
	public ResponseEntity<Map<String, Object>> executeTestBackfill() {
		log.info("=== Admin API: Test Backfill Request ===");

		List<String> testSymbols = Arrays.asList("AAPL", "MSFT", "GOOGL");

		// Record job execution start
		String context = "1wk, " + testSymbols.size() + " test symbols (" + String.join(", ", testSymbols) + ")";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_BACKFILL_TEST",
				"Market Data Backfill (Test)", context);

		try {
			LocalDateTime endDate = LocalDateTime.now(java.time.ZoneOffset.UTC);
			LocalDateTime startDate = endDate.minusWeeks(1);

			long startTime = System.currentTimeMillis();
			MarketDataCollectionService.CollectionResult result = collectionService.backfillIntradayData(testSymbols,
					startDate, endDate, "1D");
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Test backfill completed");
			response.put("testSymbols", testSymbols);
			response.put("duration", "1 week");
			response.put("timeframe", "1D");
			response.put("symbolsProcessed", result.totalSymbolsProcessed);
			response.put("dataPointsStored", result.totalDataPointsStored);
			response.put("errors", result.errorCount);
			response.put("durationSeconds", duration);

			log.info("Test backfill completed: {} symbols, {} bars, {} errors, {}s", result.totalSymbolsProcessed,
					result.totalDataPointsStored, result.errorCount, duration);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", result.totalSymbolsProcessed,
					result.totalDataPointsStored, result.errorCount, null);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Test backfill failed: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", 0, 0, 1, e.getMessage());

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Test backfill failed: " + e.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute custom backfill with specific parameters
	 *
	 * POST /v1/marketdata/admin/backfill/custom Request body: { "symbols": ["AAPL",
	 * "MSFT", "GOOGL"], "startDate": "2024-11-01T09:30:00", "endDate":
	 * "2024-11-23T16:00:00", "timeframe": "1D" }
	 */
	@PostMapping("/backfill/custom")
	public ResponseEntity<Map<String, Object>> executeCustomBackfill(@RequestBody CustomBackfillRequest request) {

		log.info("=== Admin API: Custom Backfill Request ===");
		log.info("Symbols: {}", request.symbols);
		log.info("Date range: {} to {}", request.startDate, request.endDate);
		log.info("Timeframe: {}", request.timeframe);

		// Record job execution start - include timeframe in display name
		String context = request.symbols.size() + " custom symbols";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_BACKFILL_CUSTOM",
				"Market Data Backfill (" + request.timeframe + ")", context);

		try {
			long startTime = System.currentTimeMillis();
			MarketDataCollectionService.CollectionResult result = collectionService
				.backfillIntradayData(request.symbols, request.startDate, request.endDate, request.timeframe);
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Custom backfill completed");
			response.put("symbols", request.symbols);
			response.put("startDate", request.startDate);
			response.put("endDate", request.endDate);
			response.put("timeframe", request.timeframe);
			response.put("symbolsProcessed", result.totalSymbolsProcessed);
			response.put("dataPointsStored", result.totalDataPointsStored);
			response.put("errors", result.errorCount);
			response.put("durationSeconds", duration);

			log.info("Custom backfill completed: {} symbols, {} bars, {} errors, {}s", result.totalSymbolsProcessed,
					result.totalDataPointsStored, result.errorCount, duration);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", result.totalSymbolsProcessed,
					result.totalDataPointsStored, result.errorCount, null);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Custom backfill failed: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", 0, 0, 1, e.getMessage());

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Custom backfill failed: " + e.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Force incremental collection (last 2 hours of data)
	 *
	 * POST /v1/marketdata/admin/incremental Request body: { "timeframe": "1D" }
	 * (optional)
	 *
	 * Useful for: - Manual data refresh - Testing incremental logic - Recovering from
	 * missed scheduled runs
	 */
	@PostMapping("/incremental")
	public ResponseEntity<Map<String, Object>> executeIncrementalCollection(
			@RequestBody(required = false) Map<String, String> request) {

		String timeframe = request != null && request.containsKey("timeframe") ? request.get("timeframe") : "1D";

		int lookbackHours = 2;

		log.info("=== Admin API: Incremental Collection Request (timeframe: {}, lookback: {}h) ===", timeframe,
				lookbackHours);

		// Record job execution start - include timeframe in display name
		String context = lookbackHours + "hr lookback, S&P 500 symbols";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_INCREMENTAL",
				"Market Data Incremental (" + timeframe + ")", context);

		try {
			LocalDateTime endDate = LocalDateTime.now(java.time.ZoneOffset.UTC);
			LocalDateTime startDate = endDate.minusHours(lookbackHours);

			long startTime = System.currentTimeMillis();
			MarketDataCollectionService.CollectionResult result = collectionService.backfillIntradayData(startDate,
					endDate, timeframe);
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Incremental collection completed");
			response.put("timeframe", timeframe);
			response.put("lookbackHours", lookbackHours);
			response.put("symbolsProcessed", result.totalSymbolsProcessed);
			response.put("dataPointsStored", result.totalDataPointsStored);
			response.put("errors", result.errorCount);
			response.put("durationSeconds", duration);

			log.info("Incremental collection completed: {} symbols, {} bars, {} errors, {}s",
					result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount, duration);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", result.totalSymbolsProcessed,
					result.totalDataPointsStored, result.errorCount, null);

			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Incremental collection failed: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", 0, 0, 1, e.getMessage());

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Incremental collection failed: " + e.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Execute incremental collection for ALL timeframes (1Min to 1M)
	 *
	 * POST /v1/marketdata/admin/incremental/all-timeframes Request body: {
	 * "lookbackHours": 2 } (optional, defaults to 2 hours)
	 *
	 * This pulls the latest delta data across all 9 timeframes: 1Min, 5Min, 15Min, 30Min,
	 * 1H, 4H, 1D, 1W, 1M
	 */
	@PostMapping("/incremental/all-timeframes")
	public ResponseEntity<Map<String, Object>> executeAllTimeframesIncremental(
			@RequestBody(required = false) Map<String, Object> request) {

		int lookbackHours = request != null && request.containsKey("lookbackHours")
				? ((Number) request.get("lookbackHours")).intValue() : 2;

		List<String> allTimeframes = new ArrayList<>(Timeframe.VALID_TIMEFRAMES);

		log.info("=== Admin API: All Timeframes Incremental Request ({} timeframes, lookback: {}h) ===",
				allTimeframes.size(), lookbackHours);

		// Record job execution start
		String context = lookbackHours + "hr lookback, S&P 500 symbols";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_INCREMENTAL",
				"Market Data Incremental (All)", context);

		long startTime = System.currentTimeMillis();

		int totalSymbolsProcessed = 0;
		int totalDataPointsStored = 0;
		int totalErrors = 0;
		int timeframesProcessed = 0;
		List<Map<String, Object>> timeframeResults = new ArrayList<>();

		LocalDateTime endDate = LocalDateTime.now(java.time.ZoneOffset.UTC);
		LocalDateTime startDate = endDate.minusHours(lookbackHours);

		try {
			for (String timeframe : allTimeframes) {
				try {
					log.info("--- Collecting timeframe: {} ---", timeframe);
					long tfStartTime = System.currentTimeMillis();

					MarketDataCollectionService.CollectionResult result = collectionService
						.backfillIntradayData(startDate, endDate, timeframe);

					long tfDuration = (System.currentTimeMillis() - tfStartTime) / 1000;

					totalSymbolsProcessed += result.totalSymbolsProcessed;
					totalDataPointsStored += result.totalDataPointsStored;
					totalErrors += result.errorCount;
					timeframesProcessed++;

					Map<String, Object> tfResult = new HashMap<>();
					tfResult.put("timeframe", timeframe);
					tfResult.put("symbolsProcessed", result.totalSymbolsProcessed);
					tfResult.put("dataPointsStored", result.totalDataPointsStored);
					tfResult.put("errors", result.errorCount);
					tfResult.put("durationSeconds", tfDuration);
					timeframeResults.add(tfResult);

					log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---", timeframe,
							tfDuration, result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount);

				}
				catch (Exception e) {
					log.error("Failed to collect timeframe {}: {}", timeframe, e.getMessage(), e);
					totalErrors++;

					Map<String, Object> tfResult = new HashMap<>();
					tfResult.put("timeframe", timeframe);
					tfResult.put("error", e.getMessage());
					timeframeResults.add(tfResult);
				}
			}

			long totalDuration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", totalErrors == 0 ? "success" : "partial_success");
			response.put("message", "All timeframes incremental collection completed");
			response.put("timeframesRequested", allTimeframes.size());
			response.put("timeframesProcessed", timeframesProcessed);
			response.put("lookbackHours", lookbackHours);
			response.put("totalSymbolsProcessed", totalSymbolsProcessed);
			response.put("totalDataPointsStored", totalDataPointsStored);
			response.put("totalErrors", totalErrors);
			response.put("totalDurationSeconds", totalDuration);
			response.put("timeframeResults", timeframeResults);

			log.info("=== All Timeframes Incremental Completed in {}s ===", totalDuration);
			log.info("Timeframes: {}/{}, Symbols: {}, Bars: {}, Errors: {}", timeframesProcessed, allTimeframes.size(),
					totalSymbolsProcessed, totalDataPointsStored, totalErrors);

			// Record successful completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "SUCCESS", totalSymbolsProcessed,
					totalDataPointsStored, totalErrors, null);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("All timeframes incremental failed with exception: {}", e.getMessage(), e);

			// Record failed completion
			jobExecutionHistoryBusiness.recordJobCompletion(executionId, "FAILED", totalSymbolsProcessed,
					totalDataPointsStored, totalErrors + 1, e.getMessage());

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "All timeframes incremental failed: " + e.getMessage());

			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Get current backfill job status with real-time progress tracking
	 *
	 * GET /v1/marketdata/admin/backfill/status
	 *
	 * Returns: - status: IDLE, RUNNING, COMPLETED, FAILED - timeframe: Current timeframe
	 * being processed - symbolsProcessed: Number of symbols completed - totalSymbols:
	 * Total symbols in job - startTime: Job start time - currentSymbol: Symbol currently
	 * being processed - errorMessage: Error details if failed
	 */
	@GetMapping("/backfill/status")
	public ResponseEntity<Map<String, Object>> getBackfillStatus() {
		log.debug("Admin API: Backfill status request");

		io.strategiz.business.marketdata.MarketDataCollectionService.JobStatus jobStatus = collectionService
			.getCurrentJobStatus();

		Map<String, Object> response = new HashMap<>();
		response.put("status", jobStatus.getStatus().toString());
		response.put("timeframe", jobStatus.getTimeframe());
		response.put("symbolsProcessed", jobStatus.getSymbolsProcessed());
		response.put("totalSymbols", jobStatus.getTotalSymbols());
		response.put("startTime", jobStatus.getStartTime() != null ? jobStatus.getStartTime().toString() : null);
		response.put("currentSymbol", jobStatus.getCurrentSymbol());
		response.put("errorMessage", jobStatus.getErrorMessage());

		return ResponseEntity.ok(response);
	}

	/**
	 * Cancel currently running backfill job
	 *
	 * POST /v1/marketdata/admin/backfill/cancel
	 *
	 * Requests cancellation of the running backfill. The job will stop processing new
	 * symbols but will finish the current symbol being processed.
	 */
	@PostMapping("/backfill/cancel")
	public ResponseEntity<Map<String, Object>> cancelBackfill() {
		log.warn("=== Admin API: Cancel Backfill Request ===");

		boolean cancelled = collectionService.cancelCurrentJob();

		Map<String, Object> response = new HashMap<>();
		if (cancelled) {
			response.put("status", "success");
			response.put("message", "Backfill cancellation requested. Job will stop after current symbol completes.");
			log.info("Backfill cancellation requested successfully");
		}
		else {
			response.put("status", "info");
			response.put("message", "No active backfill job to cancel");
			log.info("No active backfill job running");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Get status and configuration information
	 *
	 * GET /v1/marketdata/admin/status
	 *
	 * Returns configuration and basic stats
	 */
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getStatus() {
		log.debug("Admin API: Status request");

		Map<String, Object> status = new HashMap<>();
		status.put("status", "operational");
		status.put("service", "MarketDataCollectionService");
		status.put("dataSource", "ALPACA");
		status.put("dataFeed", "IEX (free tier)");
		status.put("defaultTimeframes", Arrays.asList("1D", "1h", "1W", "1M"));
		status.put("defaultBackfillYears", 7);
		status.put("threadPoolSize", collectionService.getThreadPoolSize());
		status.put("batchSize", collectionService.getBatchSize());
		status.put("backfillTimeoutMinutes", collectionService.getBackfillTimeoutMinutes());

		Map<String, String> endpoints = new HashMap<>();
		endpoints.put("fullBackfill", "POST /v1/marketdata/admin/backfill/full");
		endpoints.put("testBackfill", "POST /v1/marketdata/admin/backfill/test");
		endpoints.put("customBackfill", "POST /v1/marketdata/admin/backfill/custom");
		endpoints.put("incremental", "POST /v1/marketdata/admin/incremental");
		endpoints.put("allTimeframesIncremental", "POST /v1/marketdata/admin/incremental/all-timeframes");
		endpoints.put("cancelBackfill", "POST /v1/marketdata/admin/backfill/cancel");
		endpoints.put("analyzeCorruption", "GET /v1/marketdata/admin/cleanup/analyze");
		endpoints.put("deleteCorrupted1D", "POST /v1/marketdata/admin/cleanup/1day");
		endpoints.put("optimizeTable", "POST /v1/marketdata/admin/cleanup/optimize");
		status.put("availableEndpoints", endpoints);

		return ResponseEntity.ok(status);
	}

	// ========================= DATA CLEANUP ENDPOINTS =========================

	/**
	 * Analyze timestamp corruption patterns before cleanup
	 *
	 * GET /v1/marketdata/admin/cleanup/analyze?timeframe=1D
	 *
	 * Returns: - Corrupted row counts - Timestamp distribution by hour - Helps decide if
	 * cleanup is needed
	 */
	@GetMapping("/cleanup/analyze")
	public ResponseEntity<Map<String, Object>> analyzeCorruption(
			@RequestParam(required = false, defaultValue = "1D") String timeframe) {
		log.info("=== Admin API: Analyze Corruption (timeframe: {}) ===", timeframe);

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();

			// Get timestamp distribution
			List<Map<String, Object>> distribution = repo.analyzeTimestampsByTimeframe(timeframe);

			// Count corrupted records based on timeframe
			long corruptedCount = 0;
			String corruptionLogic = "";

			if ("1D".equals(timeframe)) {
				corruptedCount = repo.countCorrupted1DBars();
				corruptionLogic = "Daily bars not at midnight UTC (hour != 0)";
			}
			else if ("1h".equals(timeframe)) {
				corruptedCount = repo.countCorrupted1HBars();
				corruptionLogic = "Hourly bars not on-the-hour (minute != 0)";
			}

			// Get total count for timeframe
			long totalCount = distribution.stream().mapToLong(m -> ((Number) m.get("count")).longValue()).sum();

			Map<String, Object> response = new HashMap<>();
			response.put("timeframe", timeframe);
			response.put("totalRecords", totalCount);
			response.put("corruptedRecords", corruptedCount);
			response.put("corruptionPercentage",
					totalCount > 0 ? String.format("%.2f%%", (corruptedCount * 100.0 / totalCount)) : "0%");
			response.put("corruptionLogic", corruptionLogic);
			response.put("timestampDistribution", distribution);

			log.info("Analysis complete: {} total, {} corrupted ({} %)", totalCount, corruptedCount,
					totalCount > 0 ? String.format("%.2f", corruptedCount * 100.0 / totalCount) : "0");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to analyze corruption: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Analysis failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Delete corrupted 1D bars (timestamps not at midnight UTC)
	 *
	 * POST /v1/marketdata/admin/cleanup/1day
	 *
	 * Deletes daily bars where hour != 0 (e.g., 05:00:00Z EST offset timestamps)
	 */
	@PostMapping("/cleanup/1day")
	public ResponseEntity<Map<String, Object>> deleteCorrupted1DBars() {
		log.warn("=== Admin API: Delete Corrupted 1D Bars ===");

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();

			// Count before deletion
			long countBefore = repo.countCorrupted1DBars();

			if (countBefore == 0) {
				Map<String, Object> response = new HashMap<>();
				response.put("status", "info");
				response.put("message", "No corrupted 1D bars found");
				response.put("corruptedCount", 0);
				return ResponseEntity.ok(response);
			}

			// Execute deletion
			repo.deleteCorrupted1DBars();

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", String.format("Submitted DELETE for %d corrupted 1D bars", countBefore));
			response.put("corruptedCount", countBefore);
			response.put("note", "Deletion is async in ClickHouse. Run OPTIMIZE TABLE to apply immediately.");

			log.info("Deleted {} corrupted 1D bars", countBefore);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to delete corrupted 1D bars: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Deletion failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Delete ALL corrupted bars across all timeframes at once
	 *
	 * POST /v1/marketdata/admin/cleanup/all
	 *
	 * Deletes corrupted data from 1D, 1W, 1M (non-midnight UTC) and 1H (non-hour)
	 * timeframes.
	 */
	@PostMapping("/cleanup/all")
	public ResponseEntity<Map<String, Object>> deleteAllCorruptedBars() {
		log.warn("=== Admin API: Delete ALL Corrupted Bars (ALL TIMEFRAMES) ===");

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();

			// Execute deletion across all timeframes
			repo.deleteAllCorruptedBars();

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Submitted DELETE for ALL corrupted bars across all timeframes");
			response.put("timeframes", java.util.List.of("1D", "1W", "1M", "1h"));
			response.put("note", "Deletion is async in ClickHouse. Run OPTIMIZE TABLE to apply immediately.");

			log.info("Deleted all corrupted bars across all timeframes");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to delete all corrupted bars: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Deletion failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Optimize table to apply pending deletions immediately
	 *
	 * POST /v1/marketdata/admin/cleanup/optimize
	 *
	 * Forces ClickHouse to merge data parts and apply ALTER TABLE DELETE mutations. This
	 * can be resource-intensive on large tables.
	 */
	@PostMapping("/cleanup/optimize")
	public ResponseEntity<Map<String, Object>> optimizeTable() {
		log.warn("=== Admin API: Optimize Table (FINAL) ===");

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();

			long startTime = System.currentTimeMillis();
			repo.optimizeTableFinal();
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "OPTIMIZE TABLE FINAL completed");
			response.put("durationSeconds", duration);
			response.put("note", "Pending deletions have been applied. Verify with analyze endpoint.");

			log.info("OPTIMIZE TABLE completed in {}s", duration);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to optimize table: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Optimization failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Migrate timeframe format to canonical format.
	 *
	 * POST /v1/marketdata/admin/migrate/timeframe-format
	 *
	 * Target format: 1m, 30m, 1h, 4h, 1D, 1W, 1M - Minutes/hours: lowercase (1m, 30m, 1h,
	 * 4h) - Day/week/month: uppercase (1D, 1W, 1M)
	 */
	@PostMapping("/migrate/timeframe-format")
	public ResponseEntity<Map<String, Object>> migrateTimeframeFormat() {
		log.warn("=== Admin API: Migrate Timeframe Format to Canonical Format ===");

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();

			long startTime = System.currentTimeMillis();
			repo.migrateTimeframeToShortFormat();
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Timeframe format migration submitted");
			response.put("targetFormat", "1m, 30m, 1h, 4h, 1D, 1W, 1M");
			response.put("conversions", java.util.List.of("1Hour->1h", "1H->1h", "4Hour->4h", "4H->4h", "1Day->1D",
					"1Week->1W", "1Month->1M", "1Min->1m", "30Min->30m"));
			response.put("durationSeconds", duration);
			response.put("note",
					"Migration is async in ClickHouse. Run OPTIMIZE TABLE to apply immediately, then verify with analyze endpoint.");

			log.info("Timeframe format migration completed in {}s", duration);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to migrate timeframe format: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Migration failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Get distinct timeframes in the database with their counts. Useful for diagnosing
	 * data format issues.
	 *
	 * GET /v1/marketdata/admin/timeframes
	 */
	@GetMapping("/timeframes")
	public ResponseEntity<Map<String, Object>> getDistinctTimeframes() {
		log.info("=== Admin API: Get Distinct Timeframes ===");

		try {
			io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository repo = getMarketDataRepository();
			List<Map<String, Object>> timeframes = repo.findDistinctTimeframesWithCounts();

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("timeframes", timeframes);
			response.put("totalFormats", timeframes.size());

			log.info("Found {} distinct timeframe formats", timeframes.size());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get distinct timeframes: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Failed: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * Helper to get ClickHouse repository bean.
	 */
	private io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository getMarketDataRepository() {
		return marketDataRepository;
	}

	/**
	 * Helper method to convert list to JSON string for history storage.
	 */
	private String toJson(List<String> list) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
		}
		catch (Exception e) {
			log.warn("Failed to convert to JSON: {}", e.getMessage());
			return list != null ? list.toString() : "[]";
		}
	}

	/**
	 * Request DTO for backfill
	 */
	public static class BackfillRequest {

		public List<String> timeframes;

		public int years = 7;

	}

	/**
	 * Request DTO for custom backfill
	 */
	public static class CustomBackfillRequest {

		public List<String> symbols;

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		public LocalDateTime startDate;

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		public LocalDateTime endDate;

		public String timeframe = "1D";

	}

}
