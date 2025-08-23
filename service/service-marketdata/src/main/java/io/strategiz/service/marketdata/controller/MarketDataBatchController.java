package io.strategiz.service.marketdata.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.marketdata.batch.MarketDataBatchJob;
import io.strategiz.service.marketdata.batch.MarketDataBatchJob.BatchJobStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing and monitoring market data batch jobs
 * Provides endpoints to check status and manually trigger collections
 */
@RestController
@RequestMapping("/v1/market-data/batch")
@Tag(name = "Market Data Batch", description = "Manage daily market data collection")
public class MarketDataBatchController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataBatchController.class);
    
    private final MarketDataBatchJob batchJob;
    
    @Autowired
    public MarketDataBatchController(MarketDataBatchJob batchJob) {
        this.batchJob = batchJob;
    }
    
    /**
     * Get the current status of the batch job
     * Shows if it's running, when it last ran, and API usage
     */
    @GetMapping("/status")
    @Operation(summary = "Get batch job status", description = "Returns current status and API usage information")
    public ResponseEntity<BatchJobStatusResponse> getStatus() {
        try {
            BatchJobStatus status = batchJob.getStatus();
            
            BatchJobStatusResponse response = new BatchJobStatusResponse(
                status.isRunning(),
                status.getLastRun() != null ? status.getLastRun().toString() : "Never",
                status.getApiCallsToday(),
                status.getRemainingApiCalls(),
                status.getMaxApiCallsPerDay(),
                getNextScheduledRun()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting batch job status", e);
            return handleException(e, "Failed to get batch job status");
        }
    }
    
    /**
     * Manually trigger the batch job for specific symbols
     * Requires admin privileges and counts against daily API limit
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger batch job", description = "Manually run data collection for specified symbols (admin only)")
    public ResponseEntity<Map<String, Object>> triggerManually(@RequestBody ManualTriggerRequest request) {
        try {
            log.info("Manual batch job trigger requested by admin for {} symbols", request.getSymbols().size());
            
            // Validate request
            if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No symbols provided"
                ));
            }
            
            if (request.getSymbols().size() > 20) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Maximum 20 symbols allowed for manual trigger"
                ));
            }
            
            // Trigger the job
            batchJob.triggerManually(request.getSymbols());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Batch job triggered successfully",
                "symbolsQueued", request.getSymbols().size()
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error triggering batch job manually", e);
            return handleException(e, "Failed to trigger batch job");
        }
    }
    
    /**
     * Calculate when the next scheduled run will occur
     */
    private String getNextScheduledRun() {
        // Since it runs at 2 AM EST daily, calculate next occurrence
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/New_York"));
        java.time.ZonedDateTime nextRun = now.withHour(2).withMinute(0).withSecond(0);
        
        // If it's already past 2 AM today, next run is tomorrow
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        
        return nextRun.toString();
    }
    
    /**
     * Response object for batch job status
     */
    public static class BatchJobStatusResponse {
        private final boolean running;
        private final String lastRun;
        private final int apiCallsToday;
        private final int remainingApiCalls;
        private final int maxApiCallsPerDay;
        private final String nextScheduledRun;
        
        public BatchJobStatusResponse(boolean running, String lastRun, int apiCallsToday, 
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
    
    /**
     * Request object for manual trigger
     */
    public static class ManualTriggerRequest {
        private List<String> symbols;
        
        public List<String> getSymbols() { return symbols; }
        public void setSymbols(List<String> symbols) { this.symbols = symbols; }
    }
}