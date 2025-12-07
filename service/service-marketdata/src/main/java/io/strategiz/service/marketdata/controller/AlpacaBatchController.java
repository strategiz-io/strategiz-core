package io.strategiz.service.marketdata.controller;

import io.strategiz.business.marketdata.AlpacaCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin REST controller for managing Alpaca market data collection
 *
 * Endpoints:
 * - POST /api/admin/alpaca/backfill/full - Full backfill (3 months, all symbols)
 * - POST /api/admin/alpaca/backfill/test - Test backfill (3 symbols × 1 week)
 * - POST /api/admin/alpaca/backfill/custom - Custom backfill (specific symbols/dates)
 * - POST /api/admin/alpaca/incremental - Force incremental collection
 * - GET  /api/admin/alpaca/status - Get collection status/stats
 *
 * Security: Should be restricted to admin users only (add security annotations)
 *
 * Architecture: Controllers call services directly (business logic layer)
 */
@RestController
@RequestMapping("/api/admin/alpaca")
public class AlpacaBatchController {

    private static final Logger log = LoggerFactory.getLogger(AlpacaBatchController.class);

    private final AlpacaCollectionService collectionService;

    public AlpacaBatchController(AlpacaCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * Execute full backfill - configurable months of data for all default symbols
     *
     * POST /api/admin/alpaca/backfill/full
     * Request body: { "timeframe": "1Min", "months": 3 } (optional, defaults to 1Min and 3 months)
     *
     * WARNING: This is resource-intensive and may take 30+ minutes for 3 months
     * For 1 year of 1Min data, expect 8+ hours
     */
    @PostMapping("/backfill/full")
    public ResponseEntity<Map<String, Object>> executeFullBackfill(
            @RequestBody(required = false) Map<String, Object> request) {

        String timeframe = request != null && request.containsKey("timeframe")
                ? (String) request.get("timeframe")
                : "1Min";

        int months = request != null && request.containsKey("months")
                ? ((Number) request.get("months")).intValue()
                : 3;

        log.info("=== Admin API: Full Backfill Request (timeframe: {}, months: {}) ===", timeframe, months);

        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusMonths(months);

            long startTime = System.currentTimeMillis();
            AlpacaCollectionService.CollectionResult result = collectionService.backfillIntradayData(startDate, endDate, timeframe);
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Full backfill completed");
            response.put("timeframe", timeframe);
            response.put("months", months);
            response.put("symbolsProcessed", result.totalSymbolsProcessed);
            response.put("dataPointsStored", result.totalDataPointsStored);
            response.put("errors", result.errorCount);
            response.put("durationSeconds", duration);

            log.info("Full backfill completed: {} symbols, {} bars, {} errors, {}s",
                    result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Full backfill failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Backfill failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Execute test backfill - 3 symbols × 1 week for testing
     *
     * POST /api/admin/alpaca/backfill/test
     *
     * Symbols: AAPL, MSFT, GOOGL
     * Duration: 1 week of 1Min bars
     * Expected: ~30,000 total bars (10k per symbol)
     */
    @PostMapping("/backfill/test")
    public ResponseEntity<Map<String, Object>> executeTestBackfill() {
        log.info("=== Admin API: Test Backfill Request ===");

        try {
            List<String> testSymbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusWeeks(1);

            long startTime = System.currentTimeMillis();
            AlpacaCollectionService.CollectionResult result =
                    collectionService.backfillIntradayData(testSymbols, startDate, endDate, "1Min");
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test backfill completed");
            response.put("testSymbols", testSymbols);
            response.put("duration", "1 week");
            response.put("symbolsProcessed", result.totalSymbolsProcessed);
            response.put("dataPointsStored", result.totalDataPointsStored);
            response.put("errors", result.errorCount);
            response.put("durationSeconds", duration);

            log.info("Test backfill completed: {} symbols, {} bars, {} errors, {}s",
                    result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Test backfill failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Test backfill failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Execute custom backfill with specific parameters
     *
     * POST /api/admin/alpaca/backfill/custom
     * Request body:
     * {
     *   "symbols": ["AAPL", "MSFT", "GOOGL"],
     *   "startDate": "2024-11-01T09:30:00",
     *   "endDate": "2024-11-23T16:00:00",
     *   "timeframe": "1Min"
     * }
     */
    @PostMapping("/backfill/custom")
    public ResponseEntity<Map<String, Object>> executeCustomBackfill(
            @RequestBody CustomBackfillRequest request) {

        log.info("=== Admin API: Custom Backfill Request ===");
        log.info("Symbols: {}", request.symbols);
        log.info("Date range: {} to {}", request.startDate, request.endDate);
        log.info("Timeframe: {}", request.timeframe);

        try {
            long startTime = System.currentTimeMillis();
            AlpacaCollectionService.CollectionResult result = collectionService.backfillIntradayData(
                    request.symbols,
                    request.startDate,
                    request.endDate,
                    request.timeframe
            );
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

            log.info("Custom backfill completed: {} symbols, {} bars, {} errors, {}s",
                    result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Custom backfill failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Custom backfill failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Force incremental collection (last 2 hours of data)
     *
     * POST /api/admin/alpaca/incremental
     * Request body: { "timeframe": "1Min" } (optional)
     *
     * Useful for:
     * - Manual data refresh
     * - Testing incremental logic
     * - Recovering from missed scheduled runs
     */
    @PostMapping("/incremental")
    public ResponseEntity<Map<String, Object>> executeIncrementalCollection(
            @RequestBody(required = false) Map<String, String> request) {

        String timeframe = request != null && request.containsKey("timeframe")
                ? request.get("timeframe")
                : "1Min";

        int lookbackHours = 2;

        log.info("=== Admin API: Incremental Collection Request (timeframe: {}, lookback: {}h) ===",
                timeframe, lookbackHours);

        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusHours(lookbackHours);

            long startTime = System.currentTimeMillis();
            AlpacaCollectionService.CollectionResult result =
                    collectionService.backfillIntradayData(startDate, endDate, timeframe);
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Incremental collection failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Incremental collection failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Execute custom incremental collection for specific symbols
     *
     * POST /api/admin/alpaca/incremental/custom
     * Request body:
     * {
     *   "symbols": ["AAPL", "MSFT"],
     *   "timeframe": "1Min"
     * }
     */
    @PostMapping("/incremental/custom")
    public ResponseEntity<Map<String, Object>> executeCustomIncremental(
            @RequestBody CustomIncrementalRequest request) {

        log.info("=== Admin API: Custom Incremental Request ===");
        log.info("Symbols: {}", request.symbols);
        log.info("Timeframe: {}", request.timeframe);

        try {
            int lookbackHours = 2;
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusHours(lookbackHours);

            long startTime = System.currentTimeMillis();
            AlpacaCollectionService.CollectionResult result = collectionService.backfillIntradayData(
                    request.symbols,
                    startDate,
                    endDate,
                    request.timeframe
            );
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Custom incremental collection completed");
            response.put("symbols", request.symbols);
            response.put("timeframe", request.timeframe);
            response.put("lookbackHours", lookbackHours);
            response.put("symbolsProcessed", result.totalSymbolsProcessed);
            response.put("dataPointsStored", result.totalDataPointsStored);
            response.put("errors", result.errorCount);
            response.put("durationSeconds", duration);

            log.info("Custom incremental completed: {} symbols, {} bars, {} errors, {}s",
                    result.totalSymbolsProcessed, result.totalDataPointsStored, result.errorCount, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Custom incremental collection failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Custom incremental failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get status and configuration information
     *
     * GET /api/admin/alpaca/status
     *
     * Returns configuration and basic stats
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.debug("Admin API: Status request");

        Map<String, Object> status = new HashMap<>();
        status.put("status", "operational");
        status.put("service", "AlpacaCollectionService");
        status.put("dataSource", "ALPACA");
        status.put("dataFeed", "IEX (free tier)");
        status.put("defaultTimeframe", "1Min");
        status.put("defaultBackfillMonths", 3);
        status.put("threadPoolSize", 10);
        status.put("batchSize", 500);

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("fullBackfill", "POST /api/admin/alpaca/backfill/full");
        endpoints.put("testBackfill", "POST /api/admin/alpaca/backfill/test");
        endpoints.put("customBackfill", "POST /api/admin/alpaca/backfill/custom");
        endpoints.put("incremental", "POST /api/admin/alpaca/incremental");
        endpoints.put("customIncremental", "POST /api/admin/alpaca/incremental/custom");
        status.put("availableEndpoints", endpoints);

        return ResponseEntity.ok(status);
    }

    /**
     * Debug endpoint to test AlpacaHistoricalClient directly
     * GET /api/admin/alpaca/debug/test-api
     */
    @GetMapping("/debug/test-api")
    public ResponseEntity<Map<String, Object>> debugTestApi(
            @RequestParam(defaultValue = "AAPL") String symbol,
            @RequestParam(defaultValue = "1Day") String timeframe,
            @RequestParam(defaultValue = "7") int days) {
        log.info("=== Admin API: Debug Test API for {} (timeframe: {}, days: {}) ===", symbol, timeframe, days);

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("timeframe", timeframe);
        result.put("days", days);

        try {
            // Import AlpacaHistoricalClient
            java.time.LocalDateTime endDate = java.time.LocalDateTime.now();
            java.time.LocalDateTime startDate = endDate.minusDays(days);

            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());

            // Use reflection to get the historicalClient from collectionService
            java.lang.reflect.Field field = collectionService.getClass().getDeclaredField("historicalClient");
            field.setAccessible(true);
            Object client = field.get(collectionService);

            if (client == null) {
                result.put("status", "error");
                result.put("message", "AlpacaHistoricalClient is null!");
                return ResponseEntity.ok(result);
            }

            // Get the getBars method
            java.lang.reflect.Method getBars = client.getClass().getMethod("getBars",
                String.class, java.time.LocalDateTime.class, java.time.LocalDateTime.class, String.class);

            // Call getBars with the specified timeframe
            log.info("Calling getBars with symbol={}, start={}, end={}, timeframe={}", symbol, startDate, endDate, timeframe);
            Object bars = getBars.invoke(client, symbol, startDate, endDate, timeframe);

            if (bars == null) {
                result.put("status", "error");
                result.put("message", "getBars returned null");
            } else if (bars instanceof java.util.List) {
                java.util.List<?> barList = (java.util.List<?>) bars;
                result.put("status", "success");
                result.put("barsCount", barList.size());
                if (!barList.isEmpty()) {
                    result.put("firstBar", barList.get(0).toString());
                    result.put("lastBar", barList.get(barList.size() - 1).toString());
                }
            }

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("exceptionType", e.getClass().getName());
            if (e.getCause() != null) {
                result.put("cause", e.getCause().getMessage());
            }
            log.error("Debug test API failed", e);
        }

        return ResponseEntity.ok(result);
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

        public String timeframe = "1Min";
    }

    /**
     * Request DTO for custom incremental collection
     */
    public static class CustomIncrementalRequest {
        public List<String> symbols;
        public String timeframe = "1Min";
    }
}
