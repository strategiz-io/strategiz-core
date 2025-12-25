package io.strategiz.service.console.controller;

import io.strategiz.business.marketdata.SymbolDataStatusService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.SymbolDetailResponse;
import io.strategiz.service.console.model.response.SymbolStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Console admin controller for per-symbol data freshness monitoring.
 * Provides endpoints for viewing symbol status, searching, and filtering.
 *
 * Endpoints:
 * - GET /v1/console/marketdata/symbols/status - Paginated symbol list with filters
 * - GET /v1/console/marketdata/symbols/{symbol} - Detailed status for one symbol
 * - GET /v1/console/marketdata/symbols/stale - Symbols needing refresh
 * - GET /v1/console/marketdata/symbols/failing - Symbols with consecutive failures
 */
@RestController
@RequestMapping("/v1/console/marketdata/symbols")
@Tag(name = "Console Market Data Symbols", description = "Admin endpoints for per-symbol data monitoring")
public class ConsoleMarketDataSymbolController extends BaseController {

    private static final String MODULE_NAME = "CONSOLE";

    private final SymbolDataStatusService symbolStatusService;

    @Autowired
    public ConsoleMarketDataSymbolController(SymbolDataStatusService symbolStatusService) {
        this.symbolStatusService = symbolStatusService;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Get paginated symbol status list with optional filters.
     *
     * @param request HTTP request containing adminUserId attribute
     * @param timeframe Filter by timeframe (optional)
     * @param status Filter by status (ACTIVE, STALE, FAILED) (optional)
     * @param search Search symbols by pattern (optional)
     * @param page Page number (0-indexed)
     * @param pageSize Number of symbols per page
     * @return Paginated symbol status response
     */
    @GetMapping("/status")
    @Operation(summary = "Get symbol status list", description = "Retrieves paginated symbol status with optional filters")
    public ResponseEntity<SymbolStatusResponse> getSymbolStatus(
            HttpServletRequest request,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getSymbolStatus", adminUserId,
            String.format("timeframe=%s, status=%s, search=%s, page=%d", timeframe, status, search, page));

        List<Map<String, Object>> symbolData;
        long totalCount;

        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            // Search by symbol pattern
            String pattern = "%" + search.toUpperCase() + "%";
            symbolData = symbolStatusService.searchSymbols(pattern, page, pageSize);
            // TODO: Get total count for search (would need new repository method)
            totalCount = symbolData.size();  // Approximation

        } else if (timeframe != null && !timeframe.trim().isEmpty()) {
            // Filter by timeframe
            symbolData = symbolStatusService.getSymbolsByTimeframe(timeframe, page, pageSize);
            // Count by status if provided
            if (status != null && !status.trim().isEmpty()) {
                totalCount = symbolStatusService.countSymbolsByStatus(timeframe, status);
            } else {
                // TODO: Get total count for timeframe (would need new repository method)
                totalCount = symbolData.size();  // Approximation
            }

        } else {
            // Get all symbols (would need pagination query across all timeframes)
            // For now, default to 1Day timeframe
            String defaultTimeframe = "1Day";
            symbolData = symbolStatusService.getSymbolsByTimeframe(defaultTimeframe, page, pageSize);
            totalCount = symbolData.size();  // Approximation
        }

        // Convert to response DTOs
        List<SymbolStatusResponse.SymbolStatus> symbols = symbolData.stream()
            .map(this::convertToSymbolStatus)
            .collect(Collectors.toList());

        // Calculate summary stats
        SymbolStatusResponse.StatusSummary summary = calculateStatusSummary();

        // Build response
        SymbolStatusResponse response = new SymbolStatusResponse(
            symbols,
            page,
            pageSize,
            totalCount,
            (int) Math.ceil((double) totalCount / pageSize),
            summary
        );

        log.debug("Returning {} symbols (page {} of {})", symbols.size(), page, response.getTotalPages());
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed status for a single symbol across all timeframes.
     *
     * @param request HTTP request containing adminUserId attribute
     * @param symbol Symbol to query
     * @return Detailed symbol status
     */
    @GetMapping("/{symbol}")
    @Operation(summary = "Get symbol detail", description = "Retrieves detailed status for a single symbol across all timeframes")
    public ResponseEntity<SymbolDetailResponse> getSymbolDetail(
            HttpServletRequest request,
            @PathVariable String symbol) {

        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getSymbolDetail", adminUserId, "symbol=" + symbol);

        List<Map<String, Object>> statusList = symbolStatusService.getSymbolStatus(symbol);

        if (statusList.isEmpty()) {
            log.warn("No status found for symbol: {}", symbol);
            return ResponseEntity.notFound().build();
        }

        SymbolDetailResponse response = convertToSymbolDetail(symbol, statusList);

        log.debug("Returning detail for symbol {} with {} timeframes", symbol, response.getTimeframes().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get stale symbols that need refresh for a specific timeframe.
     *
     * @param request HTTP request containing adminUserId attribute
     * @param timeframe Timeframe to check
     * @param page Page number (0-indexed)
     * @param pageSize Number of symbols per page
     * @return Paginated list of stale symbols
     */
    @GetMapping("/stale")
    @Operation(summary = "Get stale symbols", description = "Retrieves symbols that need data refresh")
    public ResponseEntity<List<Map<String, Object>>> getStaleSymbols(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1Day") String timeframe,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getStaleSymbols", adminUserId, "timeframe=" + timeframe);

        List<Map<String, Object>> staleSymbols = symbolStatusService.getStaleSymbols(timeframe, page, pageSize);

        log.debug("Returning {} stale symbols for timeframe {}", staleSymbols.size(), timeframe);
        return ResponseEntity.ok(staleSymbols);
    }

    /**
     * Get symbols with consecutive failures.
     *
     * @param request HTTP request containing adminUserId attribute
     * @param minFailures Minimum consecutive failure count (default 3)
     * @return List of failing symbols
     */
    @GetMapping("/failing")
    @Operation(summary = "Get failing symbols", description = "Retrieves symbols with consecutive failures")
    public ResponseEntity<List<Map<String, Object>>> getFailingSymbols(
            HttpServletRequest request,
            @RequestParam(defaultValue = "3") int minFailures) {

        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getFailingSymbols", adminUserId, "minFailures=" + minFailures);

        List<Map<String, Object>> failingSymbols = symbolStatusService.getFailingSymbols(minFailures);

        log.debug("Returning {} failing symbols", failingSymbols.size());
        return ResponseEntity.ok(failingSymbols);
    }

    /**
     * Get freshness statistics aggregated by timeframe.
     *
     * @param request HTTP request containing adminUserId attribute
     * @return List of freshness stats per timeframe
     */
    @GetMapping("/stats/freshness")
    @Operation(summary = "Get freshness statistics", description = "Retrieves aggregate freshness stats by timeframe")
    public ResponseEntity<List<Map<String, Object>>> getFreshnessStats(HttpServletRequest request) {

        String adminUserId = (String) request.getAttribute("adminUserId");
        logRequest("getFreshnessStats", adminUserId);

        List<Object[]> stats = symbolStatusService.getFreshnessStats();

        // Convert to map format for JSON response
        List<Map<String, Object>> result = stats.stream()
            .map(stat -> {
                Map<String, Object> map = new HashMap<>();
                map.put("timeframe", stat[0]);
                map.put("symbolCount", stat[1]);
                map.put("avgAgeSeconds", stat[2]);
                return map;
            })
            .collect(Collectors.toList());

        log.debug("Returning freshness stats for {} timeframes", result.size());
        return ResponseEntity.ok(result);
    }

    // Converter methods

    /**
     * Convert repository map to SymbolStatus DTO.
     */
    private SymbolStatusResponse.SymbolStatus convertToSymbolStatus(Map<String, Object> data) {
        SymbolStatusResponse.SymbolStatus status = new SymbolStatusResponse.SymbolStatus();
        status.setSymbol((String) data.get("symbol"));
        status.setName((String) data.getOrDefault("name", data.get("symbol")));
        status.setSector((String) data.getOrDefault("sector", "Unknown"));

        // Build timeframe status map
        Map<String, SymbolStatusResponse.TimeframeStatus> timeframeStatus = new HashMap<>();
        String timeframe = (String) data.get("timeframe");
        if (timeframe != null) {
            SymbolStatusResponse.TimeframeStatus tfStatus = new SymbolStatusResponse.TimeframeStatus();
            tfStatus.setStatus((String) data.get("status"));
            tfStatus.setLastUpdate(formatInstant(data.get("last_update")));
            tfStatus.setLastBarTimestamp(formatInstant(data.get("last_bar_timestamp")));
            tfStatus.setRecordCount((Long) data.get("record_count"));
            tfStatus.setConsecutiveFailures((Integer) data.getOrDefault("consecutive_failures", 0));
            tfStatus.setLastError((String) data.get("last_error"));
            timeframeStatus.put(timeframe, tfStatus);
        }
        status.setTimeframeStatus(timeframeStatus);

        // Determine overall status (simplified - would aggregate across timeframes in production)
        status.setOverallStatus((String) data.getOrDefault("status", "UNKNOWN"));

        return status;
    }

    /**
     * Convert repository data to SymbolDetailResponse.
     */
    private SymbolDetailResponse convertToSymbolDetail(String symbol, List<Map<String, Object>> statusList) {
        SymbolDetailResponse response = new SymbolDetailResponse();
        response.setSymbol(symbol);
        response.setName(symbol);  // TODO: Fetch actual name from symbol service
        response.setSector("Unknown");  // TODO: Fetch from symbol metadata
        response.setAssetType("STOCK");  // TODO: Determine from symbol

        // Build timeframe details list
        List<SymbolDetailResponse.TimeframeDetail> timeframes = statusList.stream()
            .map(this::convertToTimeframeDetail)
            .collect(Collectors.toList());
        response.setTimeframes(timeframes);

        // Determine overall status (worst status across timeframes)
        String overallStatus = "ACTIVE";
        if (timeframes.stream().anyMatch(tf -> "FAILED".equals(tf.getStatus()))) {
            overallStatus = "FAILED";
        } else if (timeframes.stream().anyMatch(tf -> "STALE".equals(tf.getStatus()))) {
            overallStatus = "STALE";
        }
        response.setOverallStatus(overallStatus);

        // Extract recent errors
        List<SymbolDetailResponse.ErrorRecord> errors = statusList.stream()
            .filter(data -> data.get("last_error") != null)
            .map(this::convertToErrorRecord)
            .limit(5)  // Show last 5 errors
            .collect(Collectors.toList());
        response.setRecentErrors(errors);

        return response;
    }

    private SymbolDetailResponse.TimeframeDetail convertToTimeframeDetail(Map<String, Object> data) {
        SymbolDetailResponse.TimeframeDetail detail = new SymbolDetailResponse.TimeframeDetail();
        detail.setTimeframe((String) data.get("timeframe"));
        detail.setStatus((String) data.get("status"));
        detail.setLastUpdate(formatInstant(data.get("last_update")));
        detail.setLastBarTimestamp(formatInstant(data.get("last_bar_timestamp")));
        detail.setRecordCount((Long) data.get("record_count"));
        detail.setConsecutiveFailures((Integer) data.getOrDefault("consecutive_failures", 0));
        detail.setLastError((String) data.get("last_error"));

        // Determine data quality based on status and record count
        String quality = "GOOD";
        String status = (String) data.get("status");
        if ("FAILED".equals(status)) {
            quality = "POOR";
        } else if ("STALE".equals(status)) {
            quality = "PARTIAL";
        }
        detail.setDataQuality(quality);

        return detail;
    }

    private SymbolDetailResponse.ErrorRecord convertToErrorRecord(Map<String, Object> data) {
        SymbolDetailResponse.ErrorRecord error = new SymbolDetailResponse.ErrorRecord();
        error.setTimestamp(formatInstant(data.get("last_update")));
        error.setTimeframe((String) data.get("timeframe"));
        error.setErrorMessage((String) data.get("last_error"));
        error.setErrorType("API_ERROR");  // TODO: Parse error message to determine type
        return error;
    }

    /**
     * Calculate status summary across all symbols.
     */
    private SymbolStatusResponse.StatusSummary calculateStatusSummary() {
        List<Object[]> distribution = symbolStatusService.getStatusDistribution();

        int activeCount = 0;
        int staleCount = 0;
        int failedCount = 0;

        for (Object[] row : distribution) {
            String status = (String) row[0];
            Long count = (Long) row[1];

            if ("ACTIVE".equals(status)) {
                activeCount = count.intValue();
            } else if ("STALE".equals(status)) {
                staleCount = count.intValue();
            } else if ("FAILED".equals(status)) {
                failedCount = count.intValue();
            }
        }

        return new SymbolStatusResponse.StatusSummary(activeCount, staleCount, failedCount);
    }

    /**
     * Format Instant or Object to ISO 8601 string.
     */
    private String formatInstant(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Instant) {
            return DateTimeFormatter.ISO_INSTANT.format((Instant) obj);
        }
        if (obj instanceof java.sql.Timestamp) {
            return DateTimeFormatter.ISO_INSTANT.format(((java.sql.Timestamp) obj).toInstant());
        }
        return obj.toString();
    }
}
