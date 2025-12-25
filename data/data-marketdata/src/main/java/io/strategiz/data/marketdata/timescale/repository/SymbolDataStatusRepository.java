package io.strategiz.data.marketdata.timescale.repository;

import io.strategiz.data.marketdata.timescale.entity.SymbolDataStatusEntity;
import io.strategiz.data.marketdata.timescale.entity.SymbolDataStatusId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Spring Data JPA repository for symbol data status tracking.
 * Uses native SQL queries to leverage TimescaleDB's continuous aggregates
 * and materialized views for optimal performance.
 *
 * The 'symbol_latest_status' materialized view provides fast access to
 * the current status of each symbol/timeframe without scanning historical data.
 */
@Repository
public interface SymbolDataStatusRepository extends JpaRepository<SymbolDataStatusEntity, SymbolDataStatusId> {

    /**
     * Get latest status for a symbol across all timeframes.
     * Uses materialized view for instant results.
     *
     * @param symbol The symbol to query
     * @return List of status maps (one per timeframe)
     */
    @Query(value = "SELECT symbol, timeframe, last_update, record_count, status " +
                   "FROM symbol_latest_status " +
                   "WHERE symbol = :symbol " +
                   "ORDER BY timeframe",
           nativeQuery = true)
    List<Map<String, Object>> findLatestStatusBySymbol(@Param("symbol") String symbol);

    /**
     * Find stale symbols for a timeframe that haven't been updated recently.
     * Stale threshold typically 24 hours for incremental, 7 days for daily.
     *
     * @param timeframe The timeframe to check
     * @param staleThreshold Timestamp before which symbols are considered stale
     * @param pageable Pagination parameters
     * @return List of stale symbol status maps
     */
    @Query(value = "SELECT symbol, timeframe, last_update, record_count, status " +
                   "FROM symbol_latest_status " +
                   "WHERE timeframe = :timeframe " +
                   "AND status = 'STALE' " +
                   "AND last_update < :staleThreshold " +
                   "ORDER BY last_update ASC " +
                   "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}",
           nativeQuery = true)
    List<Map<String, Object>> findStaleSymbols(
        @Param("timeframe") String timeframe,
        @Param("staleThreshold") Instant staleThreshold,
        Pageable pageable
    );

    /**
     * Find symbols with consecutive failures exceeding threshold.
     * Useful for identifying problematic symbols requiring manual intervention.
     *
     * @param threshold Minimum consecutive failure count
     * @return List of failing symbol status maps
     */
    @Query(value = "SELECT s.symbol, s.timeframe, s.last_update, s.record_count, " +
                   "s.consecutive_failures, s.last_error, s.status " +
                   "FROM symbol_data_status s " +
                   "INNER JOIN (" +
                   "  SELECT symbol, timeframe, MAX(last_update) as max_update " +
                   "  FROM symbol_data_status " +
                   "  GROUP BY symbol, timeframe" +
                   ") latest ON s.symbol = latest.symbol " +
                   "  AND s.timeframe = latest.timeframe " +
                   "  AND s.last_update = latest.max_update " +
                   "WHERE s.consecutive_failures > :threshold " +
                   "ORDER BY s.consecutive_failures DESC",
           nativeQuery = true)
    List<Map<String, Object>> findFailingSymbols(@Param("threshold") int threshold);

    /**
     * Get freshness statistics aggregated by timeframe.
     * Returns count of symbols and average age for each timeframe.
     *
     * @return List of [timeframe, count, avg_age_seconds] arrays
     */
    @Query(value = "SELECT timeframe, " +
                   "COUNT(*) as symbol_count, " +
                   "AVG(EXTRACT(EPOCH FROM (NOW() - last_update))) as avg_age_seconds " +
                   "FROM symbol_latest_status " +
                   "GROUP BY timeframe " +
                   "ORDER BY timeframe",
           nativeQuery = true)
    List<Object[]> getFreshnessStats();

    /**
     * Get all symbols with their latest status for a specific timeframe.
     * Supports pagination for large result sets.
     *
     * @param timeframe The timeframe to filter by
     * @param pageable Pagination parameters
     * @return List of symbol status maps
     */
    @Query(value = "SELECT symbol, timeframe, last_update, record_count, status " +
                   "FROM symbol_latest_status " +
                   "WHERE timeframe = :timeframe " +
                   "ORDER BY symbol " +
                   "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}",
           nativeQuery = true)
    List<Map<String, Object>> findByTimeframe(
        @Param("timeframe") String timeframe,
        Pageable pageable
    );

    /**
     * Count symbols by status for a timeframe.
     *
     * @param timeframe The timeframe to filter by
     * @param status The status to count (ACTIVE, STALE, FAILED)
     * @return Count of symbols matching criteria
     */
    @Query(value = "SELECT COUNT(*) FROM symbol_latest_status " +
                   "WHERE timeframe = :timeframe " +
                   "AND status = :status",
           nativeQuery = true)
    Long countByTimeframeAndStatus(
        @Param("timeframe") String timeframe,
        @Param("status") String status
    );

    /**
     * Search symbols by name pattern with latest status.
     *
     * @param pattern SQL LIKE pattern (e.g., "AAPL%")
     * @param pageable Pagination parameters
     * @return List of matching symbol status maps
     */
    @Query(value = "SELECT symbol, timeframe, last_update, record_count, status " +
                   "FROM symbol_latest_status " +
                   "WHERE symbol LIKE :pattern " +
                   "ORDER BY symbol, timeframe " +
                   "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}",
           nativeQuery = true)
    List<Map<String, Object>> searchSymbols(
        @Param("pattern") String pattern,
        Pageable pageable
    );

    /**
     * Get overall data quality distribution.
     * Returns count of symbols in each status category.
     *
     * @return List of [status, count] arrays
     */
    @Query(value = "SELECT status, COUNT(*) as count " +
                   "FROM symbol_latest_status " +
                   "GROUP BY status " +
                   "ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getStatusDistribution();
}
