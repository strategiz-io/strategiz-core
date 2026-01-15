package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.marketdata.entity.MarketDataCoverageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * ClickHouse repository for market data coverage snapshots.
 * Stores periodic (daily) snapshots of coverage metrics for trend analysis.
 */
@Repository
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class MarketDataCoverageClickHouseRepository {

	private static final Logger log = LoggerFactory.getLogger(MarketDataCoverageClickHouseRepository.class);

	private final JdbcTemplate jdbcTemplate;

	public MarketDataCoverageClickHouseRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Save a coverage snapshot to ClickHouse.
	 */
	public MarketDataCoverageEntity save(MarketDataCoverageEntity entity) {
		String sql = """
				INSERT INTO market_data_coverage_snapshot (
				    snapshot_id, calculated_at, total_symbols, total_timeframes,
				    overall_freshness_percent, fresh_pairs, stale_pairs, failed_pairs, total_pairs,
				    tf_1hour_total, tf_1hour_fresh, tf_1hour_stale, tf_1hour_failed, tf_1hour_percent,
				    tf_1hour_total_bars, tf_1hour_avg_bars_per_symbol,
				    tf_1hour_date_range_start, tf_1hour_date_range_end,
				    tf_1day_total, tf_1day_fresh, tf_1day_stale, tf_1day_failed, tf_1day_percent,
				    tf_1day_total_bars, tf_1day_avg_bars_per_symbol,
				    tf_1day_date_range_start, tf_1day_date_range_end,
				    tf_1week_total, tf_1week_fresh, tf_1week_stale, tf_1week_failed, tf_1week_percent,
				    tf_1week_total_bars, tf_1week_avg_bars_per_symbol,
				    tf_1week_date_range_start, tf_1week_date_range_end,
				    tf_1month_total, tf_1month_fresh, tf_1month_stale, tf_1month_failed, tf_1month_percent,
				    tf_1month_total_bars, tf_1month_avg_bars_per_symbol,
				    tf_1month_date_range_start, tf_1month_date_range_end,
				    total_rows, storage_bytes, estimated_cost_per_month,
				    quality_good, quality_partial, quality_poor,
				    triggered_by, calculation_duration_ms,
				    gaps_json, missing_symbols_json
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
				          ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		// Extract timeframe metrics
		Map<String, Object> tf1h = extractTimeframeMetrics(entity, "1h");
		Map<String, Object> tf1D = extractTimeframeMetrics(entity, "1D");
		Map<String, Object> tf1W = extractTimeframeMetrics(entity, "1W");
		Map<String, Object> tf1M = extractTimeframeMetrics(entity, "1M");

		// Calculate overall freshness from timeframe data
		long totalFresh = (long) tf1h.get("fresh") + (long) tf1D.get("fresh") +
				(long) tf1W.get("fresh") + (long) tf1M.get("fresh");
		long totalStale = (long) tf1h.get("stale") + (long) tf1D.get("stale") +
				(long) tf1W.get("stale") + (long) tf1M.get("stale");
		long totalFailed = (long) tf1h.get("failed") + (long) tf1D.get("failed") +
				(long) tf1W.get("failed") + (long) tf1M.get("failed");
		long totalPairs = totalFresh + totalStale + totalFailed;
		double overallPercent = totalPairs > 0 ? (totalFresh * 100.0 / totalPairs) : 0.0;

		jdbcTemplate.update(sql,
				entity.getSnapshotId(),
				Timestamp.from(toInstant(entity.getCalculatedAt())),
				entity.getTotalSymbols(),
				entity.getTotalTimeframes(),
				overallPercent,
				totalFresh,
				totalStale,
				totalFailed,
				totalPairs,

				// 1h metrics
				tf1h.get("total"), tf1h.get("fresh"), tf1h.get("stale"), tf1h.get("failed"),
				tf1h.get("percent"),
				tf1h.get("totalBars"), tf1h.get("avgBars"),
				tf1h.get("dateStart"), tf1h.get("dateEnd"),

				// 1D metrics
				tf1D.get("total"), tf1D.get("fresh"), tf1D.get("stale"), tf1D.get("failed"),
				tf1D.get("percent"),
				tf1D.get("totalBars"), tf1D.get("avgBars"),
				tf1D.get("dateStart"), tf1D.get("dateEnd"),

				// 1W metrics
				tf1W.get("total"), tf1W.get("fresh"), tf1W.get("stale"), tf1W.get("failed"),
				tf1W.get("percent"),
				tf1W.get("totalBars"), tf1W.get("avgBars"),
				tf1W.get("dateStart"), tf1W.get("dateEnd"),

				// 1M metrics
				tf1M.get("total"), tf1M.get("fresh"), tf1M.get("stale"), tf1M.get("failed"),
				tf1M.get("percent"),
				tf1M.get("totalBars"), tf1M.get("avgBars"),
				tf1M.get("dateStart"), tf1M.get("dateEnd"),

				// Storage stats
				entity.getStorage() != null ? entity.getStorage().getTimescaleDbRowCount() : 0L,
				entity.getStorage() != null ? entity.getStorage().getTimescaleDbSizeBytes() : 0L,
				entity.getStorage() != null ? entity.getStorage().getEstimatedCostPerMonth() : 0.0,

				// Quality stats
				entity.getDataQuality() != null ? entity.getDataQuality().getGoodQuality() : 0,
				entity.getDataQuality() != null ? entity.getDataQuality().getPartialQuality() : 0,
				entity.getDataQuality() != null ? entity.getDataQuality().getPoorQuality() : 0,

				// Metadata
				"system", // triggered_by (default)
				0, // calculation_duration_ms (not tracked yet)

				// JSON strings
				toJsonString(entity.getGaps()),
				"[]" // missing_symbols_json (not implemented yet)
		);

		log.debug("Saved coverage snapshot: {}", entity.getSnapshotId());
		return entity;
	}

	/**
	 * Get the latest coverage snapshot.
	 */
	public Optional<MarketDataCoverageEntity> findLatest() {
		String sql = """
				SELECT * FROM market_data_coverage_snapshot
				ORDER BY calculated_at DESC
				LIMIT 1
				""";

		List<MarketDataCoverageEntity> results = jdbcTemplate.query(sql, new CoverageSnapshotRowMapper());
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Get recent coverage snapshots for trend analysis.
	 */
	public List<MarketDataCoverageEntity> findRecent(int limit) {
		String sql = """
				SELECT * FROM market_data_coverage_snapshot
				ORDER BY calculated_at DESC
				LIMIT ?
				""";

		return jdbcTemplate.query(sql, new CoverageSnapshotRowMapper(), limit);
	}

	/**
	 * Get coverage snapshots within a date range.
	 */
	public List<MarketDataCoverageEntity> findByDateRange(Instant startDate, Instant endDate) {
		String sql = """
				SELECT * FROM market_data_coverage_snapshot
				WHERE calculated_at BETWEEN ? AND ?
				ORDER BY calculated_at DESC
				""";

		return jdbcTemplate.query(sql, new CoverageSnapshotRowMapper(),
				Timestamp.from(startDate), Timestamp.from(endDate));
	}

	/**
	 * Delete snapshots older than a certain date (for cleanup).
	 */
	public int deleteOlderThan(Instant cutoffDate) {
		String sql = "ALTER TABLE market_data_coverage_snapshot DELETE WHERE calculated_at < ?";
		return jdbcTemplate.update(sql, Timestamp.from(cutoffDate));
	}

	// Helper methods

	private Map<String, Object> extractTimeframeMetrics(MarketDataCoverageEntity entity, String timeframe) {
		Map<String, Object> metrics = new HashMap<>();

		if (entity.getByTimeframe() != null && entity.getByTimeframe().containsKey(timeframe)) {
			MarketDataCoverageEntity.TimeframeCoverage coverage = entity.getByTimeframe().get(timeframe);

			metrics.put("total", entity.getTotalSymbols());
			metrics.put("fresh", (long) coverage.getSymbolsWithData());
			metrics.put("stale", (long) (coverage.getMissingSymbols() != null ? coverage.getMissingSymbols().size() : 0));
			metrics.put("failed", 0L); // Not tracked in old entity
			metrics.put("percent", coverage.getCoveragePercent());
			metrics.put("totalBars", coverage.getTotalBars());
			// Convert Long to Double for ClickHouse Float64 column
			metrics.put("avgBars", coverage.getAvgBarsPerSymbol() != null ? coverage.getAvgBarsPerSymbol().doubleValue() : 0.0);
			metrics.put("dateStart", coverage.getDateRangeStart() != null ? coverage.getDateRangeStart() : "");
			metrics.put("dateEnd", coverage.getDateRangeEnd() != null ? coverage.getDateRangeEnd() : "");
		}
		else {
			// Default values
			metrics.put("total", 0);
			metrics.put("fresh", 0L);
			metrics.put("stale", 0L);
			metrics.put("failed", 0L);
			metrics.put("percent", 0.0);
			metrics.put("totalBars", 0L);
			metrics.put("avgBars", 0.0);
			metrics.put("dateStart", "");
			metrics.put("dateEnd", "");
		}

		return metrics;
	}

	private Instant toInstant(com.google.cloud.Timestamp timestamp) {
		if (timestamp == null) {
			return Instant.now();
		}
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	private String toJsonString(List<?> list) {
		if (list == null || list.isEmpty()) {
			return "[]";
		}
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
		}
		catch (Exception e) {
			log.warn("Failed to convert list to JSON: {}", e.getMessage());
			return "[]";
		}
	}

	/**
	 * Row mapper for coverage snapshot entity.
	 */
	private static class CoverageSnapshotRowMapper implements RowMapper<MarketDataCoverageEntity> {

		@Override
		public MarketDataCoverageEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			MarketDataCoverageEntity entity = new MarketDataCoverageEntity();

			entity.setSnapshotId(rs.getString("snapshot_id"));
			entity.setCalculatedAt(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
					rs.getTimestamp("calculated_at").getTime() / 1000,
					(int) ((rs.getTimestamp("calculated_at").getTime() % 1000) * 1000000)));

			entity.setTotalSymbols(rs.getInt("total_symbols"));
			entity.setTotalTimeframes(rs.getInt("total_timeframes"));

			// Reconstruct timeframe coverage map
			Map<String, MarketDataCoverageEntity.TimeframeCoverage> byTimeframe = new HashMap<>();
			byTimeframe.put("1h", mapTimeframeCoverage(rs, "tf_1hour_"));
			byTimeframe.put("1D", mapTimeframeCoverage(rs, "tf_1day_"));
			byTimeframe.put("1W", mapTimeframeCoverage(rs, "tf_1week_"));
			byTimeframe.put("1M", mapTimeframeCoverage(rs, "tf_1month_"));
			entity.setByTimeframe(byTimeframe);

			// Storage stats
			MarketDataCoverageEntity.StorageStats storage = new MarketDataCoverageEntity.StorageStats();
			storage.setTimescaleDbRowCount(rs.getLong("total_rows"));
			storage.setTimescaleDbSizeBytes(rs.getLong("storage_bytes"));
			storage.setEstimatedCostPerMonth(rs.getDouble("estimated_cost_per_month"));
			entity.setStorage(storage);

			// Quality stats
			MarketDataCoverageEntity.QualityStats quality = new MarketDataCoverageEntity.QualityStats();
			quality.setGoodQuality(rs.getInt("quality_good"));
			quality.setPartialQuality(rs.getInt("quality_partial"));
			quality.setPoorQuality(rs.getInt("quality_poor"));
			entity.setDataQuality(quality);

			// Gaps (simplified - would need JSON parsing for full implementation)
			entity.setGaps(new ArrayList<>());

			return entity;
		}

		private MarketDataCoverageEntity.TimeframeCoverage mapTimeframeCoverage(ResultSet rs, String prefix)
				throws SQLException {
			MarketDataCoverageEntity.TimeframeCoverage coverage = new MarketDataCoverageEntity.TimeframeCoverage();
			coverage.setSymbolsWithData(rs.getInt(prefix + "fresh"));
			coverage.setCoveragePercent(rs.getDouble(prefix + "percent"));
			coverage.setTotalBars(rs.getLong(prefix + "total_bars"));
			// Convert Double from ClickHouse Float64 to Long for entity
			coverage.setAvgBarsPerSymbol((long) rs.getDouble(prefix + "avg_bars_per_symbol"));
			coverage.setDateRangeStart(rs.getString(prefix + "date_range_start"));
			coverage.setDateRangeEnd(rs.getString(prefix + "date_range_end"));
			coverage.setMissingSymbols(new ArrayList<>()); // Simplified
			return coverage;
		}

	}

}
