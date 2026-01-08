package io.strategiz.service.marketdata.controller;

import io.strategiz.business.marketdata.JobExecutionHistoryBusiness;
import io.strategiz.business.marketdata.MarketDataCollectionService;
import io.strategiz.data.marketdata.constants.Timeframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * collection - POST /v1/marketdata/admin/incremental/all-timeframes - Incremental for
 * all timeframes - GET /v1/marketdata/admin/status - Get collection status/stats
 *
 * Security: Should be restricted to admin users only (add security annotations)
 *
 * Architecture: Controllers call services directly (business logic layer)
 */
@RestController
@RequestMapping("/v1/marketdata/admin")
public class MarketDataBatchController {

	private static final Logger log = LoggerFactory.getLogger(MarketDataBatchController.class);

	private final MarketDataCollectionService collectionService;

	private final JobExecutionHistoryBusiness jobExecutionHistoryBusiness;

	public MarketDataBatchController(MarketDataCollectionService collectionService,
			JobExecutionHistoryBusiness jobExecutionHistoryBusiness) {
		this.collectionService = collectionService;
		this.jobExecutionHistoryBusiness = jobExecutionHistoryBusiness;
	}

	/**
	 * Execute full backfill - configurable years of data for all symbols
	 *
	 * POST /v1/marketdata/admin/backfill/full Request body: { "timeframes": ["1Day",
	 * "1Hour", "1Week", "1Month"], "years": 7 } (optional)
	 *
	 * Processes all timeframes synchronously and returns results when complete.
	 *
	 * WARNING: This is resource-intensive and may take several hours
	 */
	@PostMapping("/backfill/full")
	public ResponseEntity<Map<String, Object>> executeFullBackfill(@RequestBody(required = false) BackfillRequest request) {

		List<String> timeframes = request != null && request.timeframes != null && !request.timeframes.isEmpty()
				? request.timeframes : Arrays.asList("1Day", "1Hour", "1Week", "1Month");

		int years = request != null && request.years > 0 ? request.years : 7;

		log.info("=== Admin API: Full Backfill Request ===");
		log.info("Timeframes: {}", timeframes);
		log.info("Years: {}", years);

		LocalDateTime endDate = LocalDateTime.now();
		LocalDateTime startDate = endDate.minusYears(years);

		// Validate timeframes
		List<String> validTimeframes = new ArrayList<>();
		for (String timeframe : timeframes) {
			String tf = timeframe.trim();
			if (!Timeframe.VALID_TIMEFRAMES.contains(tf)) {
				log.warn("Skipping invalid timeframe: {}", tf);
			} else {
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

				MarketDataCollectionService.CollectionResult result = collectionService.backfillIntradayData(
						startDate, endDate, timeframe);

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

				log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---",
						timeframe, tfDuration, result.totalSymbolsProcessed, result.totalDataPointsStored,
						result.errorCount);

			} catch (Exception e) {
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
	 * Symbols: AAPL, MSFT, GOOGL Duration: 1 week of 1Day bars
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
			LocalDateTime endDate = LocalDateTime.now();
			LocalDateTime startDate = endDate.minusWeeks(1);

			long startTime = System.currentTimeMillis();
			MarketDataCollectionService.CollectionResult result = collectionService.backfillIntradayData(testSymbols,
					startDate, endDate, "1Day");
			long duration = (System.currentTimeMillis() - startTime) / 1000;

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Test backfill completed");
			response.put("testSymbols", testSymbols);
			response.put("duration", "1 week");
			response.put("timeframe", "1Day");
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
	 * "2024-11-23T16:00:00", "timeframe": "1Day" }
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
	 * POST /v1/marketdata/admin/incremental Request body: { "timeframe": "1Day" }
	 * (optional)
	 *
	 * Useful for: - Manual data refresh - Testing incremental logic - Recovering from
	 * missed scheduled runs
	 */
	@PostMapping("/incremental")
	public ResponseEntity<Map<String, Object>> executeIncrementalCollection(
			@RequestBody(required = false) Map<String, String> request) {

		String timeframe = request != null && request.containsKey("timeframe") ? request.get("timeframe") : "1Day";

		int lookbackHours = 2;

		log.info("=== Admin API: Incremental Collection Request (timeframe: {}, lookback: {}h) ===", timeframe,
				lookbackHours);

		// Record job execution start - include timeframe in display name
		String context = lookbackHours + "hr lookback, S&P 500 symbols";
		String executionId = jobExecutionHistoryBusiness.recordJobStart("MARKETDATA_INCREMENTAL",
				"Market Data Incremental (" + timeframe + ")", context);

		try {
			LocalDateTime endDate = LocalDateTime.now();
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
	 * Execute incremental collection for ALL timeframes (1Min to 1Month)
	 *
	 * POST /v1/marketdata/admin/incremental/all-timeframes Request body: {
	 * "lookbackHours": 2 } (optional, defaults to 2 hours)
	 *
	 * This pulls the latest delta data across all 9 timeframes: 1Min, 5Min, 15Min,
	 * 30Min, 1Hour, 4Hour, 1Day, 1Week, 1Month
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

		LocalDateTime endDate = LocalDateTime.now();
		LocalDateTime startDate = endDate.minusHours(lookbackHours);

		try {
			for (String timeframe : allTimeframes) {
			try {
				log.info("--- Collecting timeframe: {} ---", timeframe);
				long tfStartTime = System.currentTimeMillis();

				MarketDataCollectionService.CollectionResult result = collectionService.backfillIntradayData(startDate,
						endDate, timeframe);

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

				log.info("--- Timeframe {} completed in {}s: {} symbols, {} bars, {} errors ---", timeframe, tfDuration,
						result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount);

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
	 * Returns: - status: IDLE, RUNNING, COMPLETED, FAILED - timeframe: Current
	 * timeframe being processed - symbolsProcessed: Number of symbols completed -
	 * totalSymbols: Total symbols in job - startTime: Job start time - currentSymbol:
	 * Symbol currently being processed - errorMessage: Error details if failed
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
		status.put("defaultTimeframes", Arrays.asList("1Day", "1Hour", "1Week", "1Month"));
		status.put("defaultBackfillYears", 7);
		status.put("threadPoolSize", 2);
		status.put("batchSize", 500);

		Map<String, String> endpoints = new HashMap<>();
		endpoints.put("fullBackfill", "POST /v1/marketdata/admin/backfill/full");
		endpoints.put("testBackfill", "POST /v1/marketdata/admin/backfill/test");
		endpoints.put("customBackfill", "POST /v1/marketdata/admin/backfill/custom");
		endpoints.put("incremental", "POST /v1/marketdata/admin/incremental");
		endpoints.put("allTimeframesIncremental", "POST /v1/marketdata/admin/incremental/all-timeframes");
		status.put("availableEndpoints", endpoints);

		return ResponseEntity.ok(status);
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

		public String timeframe = "1Day";

	}

}
