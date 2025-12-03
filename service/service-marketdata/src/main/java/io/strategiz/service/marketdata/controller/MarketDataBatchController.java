package io.strategiz.service.marketdata.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.business.marketdata.MarketDataCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing and monitoring market data collection
 *
 * Provides endpoints to:
 * - Check collection status
 * - Manually trigger data collection
 *
 * Architecture: Controllers call services directly (business logic layer)
 */
@RestController
@RequestMapping("/v1/market-data/batch")
@Tag(name = "Market Data Batch", description = "Manage daily market data collection")
@ConditionalOnProperty(name = "polygon.batch.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataBatchController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataBatchController.class);

    private final MarketDataCollectionService collectionService;

    @Autowired
    public MarketDataBatchController(MarketDataCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * Get the current status of market data collection
     * Shows API usage and collection statistics
     */
    @GetMapping("/status")
    @Operation(summary = "Get collection status", description = "Returns current status and API usage information")
    public ResponseEntity<StatusResponse> getStatus() {
        try {
            // For now, return basic operational status
            // In the future, can add collection statistics from the service
            StatusResponse response = new StatusResponse(
                false,  // isRunning - would need to track this in service
                "Never",  // lastRun - would need to track this in service
                0,  // apiCallsToday - Yahoo Finance is free, no limits
                0,  // remainingApiCalls
                0,  // maxApiCallsPerDay
                getNextScheduledRun()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting collection status", e);
            throw handleException(e, "Failed to get collection status");
        }
    }

    /**
     * Manually trigger data collection
     * Runs standard incremental collection (fetches missing days for all default symbols)
     * Requires admin privileges - no strict API limits (Yahoo Finance is free)
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger collection", description = "Manually run data collection (admin only)")
    public ResponseEntity<Map<String, Object>> triggerManually() {
        try {
            log.info("Manual market data collection trigger requested by admin");

            long startTime = System.currentTimeMillis();
            MarketDataCollectionService.CollectionResult result = collectionService.collectDailyData();
            long executionTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Data collection completed successfully",
                "symbolsProcessed", result.totalSymbolsProcessed,
                "dataPointsStored", result.totalDataPointsStored,
                "errorCount", result.errorCount,
                "executionTimeSeconds", executionTimeSeconds
            ));

        } catch (Exception e) {
            log.error("Error triggering data collection manually", e);
            throw handleException(e, "Failed to trigger data collection");
        }
    }

    /**
     * Calculate when the next scheduled run will occur
     */
    private String getNextScheduledRun() {
        // Since it runs at 2 AM and 2 PM EST daily, calculate next occurrence
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/New_York"));
        java.time.ZonedDateTime nextRun;

        // Check if we need 2 AM or 2 PM next
        if (now.getHour() < 2) {
            nextRun = now.withHour(2).withMinute(0).withSecond(0);
        } else if (now.getHour() < 14) {
            nextRun = now.withHour(14).withMinute(0).withSecond(0);
        } else {
            // After 2 PM, next run is tomorrow at 2 AM
            nextRun = now.plusDays(1).withHour(2).withMinute(0).withSecond(0);
        }

        return nextRun.toString();
    }

    /**
     * Response object for collection status
     */
    public static class StatusResponse {
        private final boolean running;
        private final String lastRun;
        private final int apiCallsToday;
        private final int remainingApiCalls;
        private final int maxApiCallsPerDay;
        private final String nextScheduledRun;

        public StatusResponse(boolean running, String lastRun, int apiCallsToday,
                                     int remainingApiCalls, int maxApiCallsPerDay, String nextScheduledRun) {
            this.running = running;
            this.lastRun = lastRun;
            this.apiCallsToday = apiCallsToday;
            this.remainingApiCalls = remainingApiCalls;
            this.maxApiCallsPerDay = maxApiCallsPerDay;
            this.nextScheduledRun = nextScheduledRun;
        }

        // Getters
        public boolean isRunning() { return running; }
        public String getLastRun() { return lastRun; }
        public int getApiCallsToday() { return apiCallsToday; }
        public int getRemainingApiCalls() { return remainingApiCalls; }
        public int getMaxApiCallsPerDay() { return maxApiCallsPerDay; }
        public String getNextScheduledRun() { return nextScheduledRun; }
    }

    @Override
    protected String getModuleName() {
        return "marketdata";
    }
}
