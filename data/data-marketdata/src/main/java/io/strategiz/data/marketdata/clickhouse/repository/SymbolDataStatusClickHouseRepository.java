package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.marketdata.entity.SymbolDataStatusEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickHouse implementation of symbol data status repository. Tracks data freshness
 * for each symbol/timeframe combination.
 */
@Repository
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class SymbolDataStatusClickHouseRepository {

	private static final Logger log = LoggerFactory.getLogger(SymbolDataStatusClickHouseRepository.class);

	private final JdbcTemplate jdbcTemplate;

	private final SymbolDataStatusRowMapper rowMapper = new SymbolDataStatusRowMapper();

	public SymbolDataStatusClickHouseRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Save or update a symbol data status record.
	 */
	public SymbolDataStatusEntity save(SymbolDataStatusEntity entity) {
		String sql = """
				INSERT INTO symbol_data_status (
				    symbol, timeframe, last_update, last_bar_timestamp, record_count,
				    consecutive_failures, last_error, status, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		jdbcTemplate.update(sql, entity.getSymbol(), entity.getTimeframe(),
				Timestamp.from(entity.getLastUpdate()),
				entity.getLastBarTimestamp() != null ? Timestamp.from(entity.getLastBarTimestamp()) : null,
				entity.getRecordCount(), entity.getConsecutiveFailures(), entity.getLastError(), entity.getStatus(),
				Timestamp.from(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : Instant.now()));

		return entity;
	}

	/**
	 * Get latest status for a symbol across all timeframes.
	 */
	public List<Map<String, Object>> findLatestStatusBySymbol(String symbol) {
		String sql = """
				SELECT symbol, timeframe, last_update, record_count, status
				FROM symbol_data_status
				WHERE symbol = ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    WHERE symbol = ?
				    GROUP BY symbol, timeframe
				)
				ORDER BY timeframe
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("status", rs.getString("status"));
			return map;
		}, symbol, symbol);
	}

	/**
	 * Find stale symbols for a timeframe that haven't been updated recently.
	 */
	public List<Map<String, Object>> findStaleSymbols(String timeframe, Instant staleThreshold, Pageable pageable) {
		String sql = """
				SELECT symbol, timeframe, last_update, record_count, status
				FROM symbol_data_status
				WHERE timeframe = ?
				AND status = 'STALE'
				AND last_update < ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				ORDER BY last_update ASC
				LIMIT ? OFFSET ?
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("status", rs.getString("status"));
			return map;
		}, timeframe, Timestamp.from(staleThreshold), pageable.getPageSize(), pageable.getOffset());
	}

	/**
	 * Find symbols with consecutive failures exceeding threshold.
	 */
	public List<Map<String, Object>> findFailingSymbols(int threshold) {
		String sql = """
				SELECT symbol, timeframe, last_update, record_count,
				       consecutive_failures, last_error, status
				FROM symbol_data_status
				WHERE consecutive_failures > ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				ORDER BY consecutive_failures DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("consecutive_failures", rs.getInt("consecutive_failures"));
			map.put("last_error", rs.getString("last_error"));
			map.put("status", rs.getString("status"));
			return map;
		}, threshold);
	}

	/**
	 * Get freshness statistics aggregated by timeframe.
	 */
	public List<Object[]> getFreshnessStats() {
		String sql = """
				SELECT timeframe,
				       COUNT(*) as symbol_count,
				       AVG(dateDiff('second', last_update, now())) as avg_age_seconds
				FROM symbol_data_status
				WHERE (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				GROUP BY timeframe
				ORDER BY timeframe
				""";

		return jdbcTemplate.query(sql,
				(rs, rowNum) -> new Object[] { rs.getString("timeframe"), rs.getLong("symbol_count"),
						rs.getDouble("avg_age_seconds") });
	}

	/**
	 * Get all symbols with their latest status for a specific timeframe.
	 */
	public List<Map<String, Object>> findByTimeframe(String timeframe, Pageable pageable) {
		String sql = """
				SELECT symbol, timeframe, last_update, record_count, status
				FROM symbol_data_status
				WHERE timeframe = ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				ORDER BY symbol
				LIMIT ? OFFSET ?
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("status", rs.getString("status"));
			return map;
		}, timeframe, pageable.getPageSize(), pageable.getOffset());
	}

	/**
	 * Count symbols by status for a timeframe.
	 */
	public Long countByTimeframeAndStatus(String timeframe, String status) {
		String sql = """
				SELECT COUNT(DISTINCT symbol) FROM symbol_data_status
				WHERE timeframe = ?
				AND status = ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				""";

		return jdbcTemplate.queryForObject(sql, Long.class, timeframe, status);
	}

	/**
	 * Search symbols by name pattern with latest status.
	 */
	public List<Map<String, Object>> searchSymbols(String pattern, Pageable pageable) {
		String sql = """
				SELECT symbol, timeframe, last_update, record_count, status
				FROM symbol_data_status
				WHERE symbol LIKE ?
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				ORDER BY symbol, timeframe
				LIMIT ? OFFSET ?
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("status", rs.getString("status"));
			return map;
		}, pattern, pageable.getPageSize(), pageable.getOffset());
	}

	/**
	 * Get overall data quality distribution.
	 */
	public List<Object[]> getStatusDistribution() {
		String sql = """
				SELECT status, COUNT(*) as count
				FROM symbol_data_status
				WHERE (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				GROUP BY status
				ORDER BY count DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[] { rs.getString("status"), rs.getLong("count") });
	}

	/**
	 * Calculate freshness metrics for specific timeframes.
	 * Returns counts of fresh symbols (updated within threshold) per timeframe.
	 *
	 * @param timeframes List of timeframes to check (e.g., ["1Hour", "1Day", "1Week", "1Month"])
	 * @param freshnessThresholdMinutes Threshold in minutes (e.g., 15 for "within last 15 min")
	 * @return Map of timeframe -> freshness data (totalSymbols, freshSymbols, staleSymbols, failedSymbols, freshnessPercent)
	 */
	public List<Map<String, Object>> calculateFreshnessMetrics(List<String> timeframes, int freshnessThresholdMinutes) {
		// Build IN clause for timeframes
		String timeframeInClause = timeframes.stream()
			.map(tf -> "?")
			.collect(java.util.stream.Collectors.joining(","));

		String sql = String.format("""
				SELECT
				    timeframe,
				    COUNT(DISTINCT symbol) as total_symbols,
				    countIf(dateDiff('minute', last_update, now()) <= ?) as fresh_symbols,
				    countIf(dateDiff('minute', last_update, now()) > ? AND status = 'STALE') as stale_symbols,
				    countIf(status = 'FAILED') as failed_symbols,
				    ROUND((countIf(dateDiff('minute', last_update, now()) <= ?) * 100.0) / COUNT(DISTINCT symbol), 2) as freshness_percent
				FROM symbol_data_status
				WHERE timeframe IN (%s)
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				GROUP BY timeframe
				ORDER BY timeframe
				""", timeframeInClause);

		// Prepare parameters: freshnessThresholdMinutes (3 times) + timeframes
		Object[] params = new Object[3 + timeframes.size()];
		params[0] = freshnessThresholdMinutes;
		params[1] = freshnessThresholdMinutes;
		params[2] = freshnessThresholdMinutes;
		for (int i = 0; i < timeframes.size(); i++) {
			params[3 + i] = timeframes.get(i);
		}

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("timeframe", rs.getString("timeframe"));
			map.put("totalSymbols", rs.getLong("total_symbols"));
			map.put("freshSymbols", rs.getLong("fresh_symbols"));
			map.put("staleSymbols", rs.getLong("stale_symbols"));
			map.put("failedSymbols", rs.getLong("failed_symbols"));
			map.put("freshnessPercent", rs.getDouble("freshness_percent"));
			return map;
		}, params);
	}

	/**
	 * Get all symbols with their latest status across multiple timeframes.
	 * Used for the consolidated symbol view showing freshness for each symbol across all target timeframes.
	 *
	 * @param timeframes List of timeframes to include
	 * @param pageable Pagination parameters
	 * @return List of symbols with their status for each timeframe
	 */
	public List<Map<String, Object>> findSymbolsWithAllTimeframes(List<String> timeframes, Pageable pageable) {
		// Get unique symbols first
		String symbolsSql = """
				SELECT DISTINCT symbol
				FROM symbol_data_status
				ORDER BY symbol
				LIMIT ? OFFSET ?
				""";

		List<String> symbols = jdbcTemplate.query(symbolsSql,
			(rs, rowNum) -> rs.getString("symbol"),
			pageable.getPageSize(), pageable.getOffset());

		if (symbols.isEmpty()) {
			return List.of();
		}

		// For each symbol, get status across all target timeframes
		String statusSql = """
				SELECT
				    symbol,
				    timeframe,
				    last_update,
				    last_bar_timestamp,
				    record_count,
				    consecutive_failures,
				    last_error,
				    status,
				    dateDiff('minute', last_update, now()) as minutes_since_update
				FROM symbol_data_status
				WHERE symbol IN (%s)
				AND timeframe IN (%s)
				AND (symbol, timeframe, last_update) IN (
				    SELECT symbol, timeframe, MAX(last_update)
				    FROM symbol_data_status
				    GROUP BY symbol, timeframe
				)
				ORDER BY symbol, timeframe
				""";

		// Build IN clauses
		String symbolsIn = symbols.stream().map(s -> "?").collect(java.util.stream.Collectors.joining(","));
		String timeframesIn = timeframes.stream().map(tf -> "?").collect(java.util.stream.Collectors.joining(","));
		String finalSql = String.format(statusSql, symbolsIn, timeframesIn);

		// Prepare parameters
		Object[] params = new Object[symbols.size() + timeframes.size()];
		for (int i = 0; i < symbols.size(); i++) {
			params[i] = symbols.get(i);
		}
		for (int i = 0; i < timeframes.size(); i++) {
			params[symbols.size() + i] = timeframes.get(i);
		}

		return jdbcTemplate.query(finalSql, (rs, rowNum) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("symbol", rs.getString("symbol"));
			map.put("timeframe", rs.getString("timeframe"));
			map.put("last_update", rs.getTimestamp("last_update"));
			map.put("last_bar_timestamp", rs.getTimestamp("last_bar_timestamp"));
			map.put("record_count", rs.getLong("record_count"));
			map.put("consecutive_failures", rs.getInt("consecutive_failures"));
			map.put("last_error", rs.getString("last_error"));
			map.put("status", rs.getString("status"));
			map.put("minutes_since_update", rs.getLong("minutes_since_update"));
			return map;
		}, params);
	}

	/**
	 * Row mapper for SymbolDataStatusEntity.
	 */
	private static class SymbolDataStatusRowMapper implements RowMapper<SymbolDataStatusEntity> {

		@Override
		public SymbolDataStatusEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			SymbolDataStatusEntity entity = new SymbolDataStatusEntity();
			entity.setSymbol(rs.getString("symbol"));
			entity.setTimeframe(rs.getString("timeframe"));
			entity.setLastUpdate(rs.getTimestamp("last_update").toInstant());

			Timestamp lastBarTimestamp = rs.getTimestamp("last_bar_timestamp");
			if (lastBarTimestamp != null) {
				entity.setLastBarTimestamp(lastBarTimestamp.toInstant());
			}

			entity.setRecordCount(rs.getLong("record_count"));
			entity.setConsecutiveFailures(rs.getInt("consecutive_failures"));
			entity.setLastError(rs.getString("last_error"));
			entity.setStatus(rs.getString("status"));

			Timestamp updatedAt = rs.getTimestamp("updated_at");
			if (updatedAt != null) {
				entity.setUpdatedAt(updatedAt.toInstant());
			}

			return entity;
		}

	}

}
