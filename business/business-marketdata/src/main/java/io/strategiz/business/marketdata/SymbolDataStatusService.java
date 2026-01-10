package io.strategiz.business.marketdata;

import io.strategiz.business.marketdata.exception.MarketDataErrorDetails;
import io.strategiz.data.marketdata.clickhouse.repository.SymbolDataStatusClickHouseRepository;
import io.strategiz.data.marketdata.entity.SymbolDataStatusEntity;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for tracking per-symbol data freshness in ClickHouse.
 *
 * Tracks the status of each symbol/timeframe combination:
 * - Last update timestamp
 * - Record count (number of bars collected)
 * - Consecutive failure tracking
 * - Status classification (ACTIVE, STALE, FAILED)
 *
 * Enables:
 * - Stale symbol detection for incremental jobs
 * - Data quality monitoring
 * - Failure pattern analysis
 * - Symbol-level diagnostics
 */
@Service
public class SymbolDataStatusService {

    private static final Logger log = LoggerFactory.getLogger(SymbolDataStatusService.class);

    // Thresholds for status classification
    private static final long STALE_THRESHOLD_HOURS_DAILY = 48;  // 48 hours for daily data
    private static final long STALE_THRESHOLD_HOURS_INTRADAY = 24;  // 24 hours for intraday data
    private static final int FAILURE_THRESHOLD = 3;  // Consider failed after 3 consecutive failures

    private final SymbolDataStatusClickHouseRepository symbolDataStatusRepository;

    @Autowired
    public SymbolDataStatusService(SymbolDataStatusClickHouseRepository symbolDataStatusRepository) {
        this.symbolDataStatusRepository = symbolDataStatusRepository;
    }

    /**
     * Update status after successful data collection for a symbol.
     *
     * @param symbol The symbol (e.g., "AAPL", "BTC/USD")
     * @param timeframe Timeframe (e.g., "1Day", "1Hour")
     * @param recordCount Number of records collected
     * @param lastBarTimestamp Timestamp of most recent bar
     */
    public void recordSuccessfulUpdate(String symbol, String timeframe, long recordCount, Instant lastBarTimestamp) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "symbol cannot be null or empty"
            );
        }
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "timeframe cannot be null or empty"
            );
        }

        Instant now = Instant.now();

        SymbolDataStatusEntity entity = new SymbolDataStatusEntity(symbol, timeframe, now);
        entity.setLastBarTimestamp(lastBarTimestamp);
        entity.setRecordCount(recordCount);
        entity.setConsecutiveFailures(0);  // Reset on success
        entity.setLastError(null);
        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(now);

        symbolDataStatusRepository.save(entity);

        log.debug("Updated status for {} {} - {} records, last bar: {}",
            symbol, timeframe, recordCount, lastBarTimestamp);
    }

    /**
     * Update status after failed data collection for a symbol.
     *
     * @param symbol The symbol
     * @param timeframe Timeframe
     * @param errorMessage Error message from the failure
     */
    public void recordFailedUpdate(String symbol, String timeframe, String errorMessage) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "symbol cannot be null or empty"
            );
        }
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "timeframe cannot be null or empty"
            );
        }

        Instant now = Instant.now();

        // Get current status to increment failure count
        List<Map<String, Object>> currentStatus = symbolDataStatusRepository.findLatestStatusBySymbol(symbol);

        int consecutiveFailures = 1;  // Default for first failure
        for (Map<String, Object> statusMap : currentStatus) {
            String tf = (String) statusMap.get("timeframe");
            if (timeframe.equals(tf)) {
                // Found existing status, increment failures
                Object failuresObj = statusMap.get("consecutive_failures");
                if (failuresObj instanceof Integer) {
                    consecutiveFailures = (Integer) failuresObj + 1;
                }
                break;
            }
        }

        String status = (consecutiveFailures >= FAILURE_THRESHOLD) ? "FAILED" : "STALE";

        SymbolDataStatusEntity entity = new SymbolDataStatusEntity(symbol, timeframe, now);
        entity.setLastBarTimestamp(null);  // No new data collected
        entity.setRecordCount(0L);
        entity.setConsecutiveFailures(consecutiveFailures);
        entity.setLastError(errorMessage);
        entity.setStatus(status);
        entity.setUpdatedAt(now);

        symbolDataStatusRepository.save(entity);

        log.warn("Recorded failure for {} {} - consecutive failures: {}, status: {}",
            symbol, timeframe, consecutiveFailures, status);
    }

    /**
     * Get current status for a single symbol across all timeframes.
     *
     * @param symbol The symbol to query
     * @return List of status maps (one per timeframe)
     */
    public List<Map<String, Object>> getSymbolStatus(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "symbol cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> statusList = symbolDataStatusRepository.findLatestStatusBySymbol(symbol);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved status for {} in {}ms - {} timeframes",
            symbol, duration, statusList.size());

        return statusList;
    }

    /**
     * Get stale symbols for a specific timeframe that need refresh.
     *
     * @param timeframe Timeframe to check
     * @param page Page number (0-indexed)
     * @param pageSize Number of symbols per page
     * @return List of stale symbol status maps
     */
    public List<Map<String, Object>> getStaleSymbols(String timeframe, int page, int pageSize) {
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "timeframe cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        // Determine stale threshold based on timeframe
        long staleHours = timeframe.contains("Day") ? STALE_THRESHOLD_HOURS_DAILY : STALE_THRESHOLD_HOURS_INTRADAY;
        Instant staleThreshold = Instant.now().minus(staleHours, ChronoUnit.HOURS);

        Pageable pageable = PageRequest.of(page, pageSize);
        List<Map<String, Object>> staleSymbols = symbolDataStatusRepository.findStaleSymbols(
            timeframe,
            staleThreshold,
            pageable
        );

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} stale symbols for {} in {}ms (page {} of size {})",
            staleSymbols.size(), timeframe, duration, page, pageSize);

        return staleSymbols;
    }

    /**
     * Get symbols with consecutive failures exceeding threshold.
     *
     * @param minFailures Minimum consecutive failure count
     * @return List of failing symbol status maps
     */
    public List<Map<String, Object>> getFailingSymbols(int minFailures) {
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> failingSymbols = symbolDataStatusRepository.findFailingSymbols(minFailures);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} failing symbols (>= {} failures) in {}ms",
            failingSymbols.size(), minFailures, duration);

        return failingSymbols;
    }

    /**
     * Get freshness statistics aggregated by timeframe.
     *
     * @return List of arrays: [timeframe, symbol_count, avg_age_seconds]
     */
    public List<Object[]> getFreshnessStats() {
        long startTime = System.currentTimeMillis();

        List<Object[]> stats = symbolDataStatusRepository.getFreshnessStats();

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved freshness stats for {} timeframes in {}ms",
            stats.size(), duration);

        return stats;
    }

    /**
     * Get all symbols with their latest status for a specific timeframe.
     *
     * @param timeframe The timeframe to filter by
     * @param page Page number (0-indexed)
     * @param pageSize Number of symbols per page
     * @return List of symbol status maps
     */
    public List<Map<String, Object>> getSymbolsByTimeframe(String timeframe, int page, int pageSize) {
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "timeframe cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, pageSize);
        List<Map<String, Object>> symbols = symbolDataStatusRepository.findByTimeframe(timeframe, pageable);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} symbols for {} in {}ms (page {} of size {})",
            symbols.size(), timeframe, duration, page, pageSize);

        return symbols;
    }

    /**
     * Count symbols by status for a timeframe.
     *
     * @param timeframe The timeframe to filter by
     * @param status The status to count (ACTIVE, STALE, FAILED)
     * @return Count of symbols matching criteria
     */
    public long countSymbolsByStatus(String timeframe, String status) {
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "timeframe cannot be null or empty"
            );
        }
        if (status == null || status.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "status cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Long count = symbolDataStatusRepository.countByTimeframeAndStatus(timeframe, status);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Count for {} with status {} in {}ms: {} symbols",
            timeframe, status, duration, count);

        return count != null ? count : 0L;
    }

    /**
     * Search symbols by name pattern with latest status.
     *
     * @param pattern SQL LIKE pattern (e.g., "AAPL%", "%BTC%")
     * @param page Page number (0-indexed)
     * @param pageSize Number of symbols per page
     * @return List of matching symbol status maps
     */
    public List<Map<String, Object>> searchSymbols(String pattern, int page, int pageSize) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "pattern cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, pageSize);
        List<Map<String, Object>> symbols = symbolDataStatusRepository.searchSymbols(pattern, pageable);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Symbol search for pattern '{}' in {}ms: {} results (page {} of size {})",
            pattern, duration, symbols.size(), page, pageSize);

        return symbols;
    }

    /**
     * Get overall data quality distribution across all symbols/timeframes.
     *
     * @return List of [status, count] arrays
     */
    public List<Object[]> getStatusDistribution() {
        long startTime = System.currentTimeMillis();

        List<Object[]> distribution = symbolDataStatusRepository.getStatusDistribution();

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved status distribution in {}ms - {} statuses",
            duration, distribution.size());

        return distribution;
    }
}
