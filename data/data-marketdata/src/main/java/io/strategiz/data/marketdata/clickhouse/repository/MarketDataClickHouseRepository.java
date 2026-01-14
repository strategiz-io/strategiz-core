package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ClickHouse implementation of market data repository. Uses JdbcTemplate for direct SQL
 * access to ClickHouse Cloud. Replaces TimescaleDB/JPA repository.
 */
@Repository
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class MarketDataClickHouseRepository {

	private static final Logger log = LoggerFactory.getLogger(MarketDataClickHouseRepository.class);

	private final JdbcTemplate jdbcTemplate;

	private final MarketDataRowMapper rowMapper = new MarketDataRowMapper();

	public MarketDataClickHouseRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Find market data by symbol within a time range and optional timeframe filter.
	 */
	public List<MarketDataEntity> findBySymbolAndTimeRange(String symbol, Instant startTime, Instant endTime,
			String timeframe) {
		String sql;
		Object[] params;

		if (timeframe != null) {
			sql = """
					SELECT * FROM market_data
					WHERE symbol = ? AND timestamp >= toDateTime64(?, 3) AND timestamp < toDateTime64(?, 3) AND timeframe = ?
					ORDER BY timestamp ASC
					""";
			params = new Object[] { symbol, startTime.getEpochSecond() + (startTime.getNano() / 1_000_000_000.0),
					endTime.getEpochSecond() + (endTime.getNano() / 1_000_000_000.0), timeframe };
			log.info("Executing ClickHouse query with timeframe filter: symbol={}, start={}, end={}, timeframe={}",
				symbol, startTime, endTime, timeframe);
		}
		else {
			sql = """
					SELECT * FROM market_data
					WHERE symbol = ? AND timestamp >= toDateTime64(?, 3) AND timestamp < toDateTime64(?, 3)
					ORDER BY timestamp ASC
					""";
			params = new Object[] { symbol, startTime.getEpochSecond() + (startTime.getNano() / 1_000_000_000.0),
					endTime.getEpochSecond() + (endTime.getNano() / 1_000_000_000.0) };
			log.info("Executing ClickHouse query without timeframe filter: symbol={}, start={}, end={}",
				symbol, startTime, endTime);
		}

		List<MarketDataEntity> results = jdbcTemplate.query(sql, rowMapper, params);
		log.info("ClickHouse query returned {} results for symbol={}", results.size(), symbol);
		return results;
	}

	/**
	 * Find market data by symbol and timeframe, limited and ordered by timestamp
	 * descending.
	 */
	public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe, int limit) {
		String sql = """
				SELECT * FROM market_data
				WHERE symbol = ? AND timeframe = ?
				ORDER BY timestamp DESC
				LIMIT ?
				""";
		return jdbcTemplate.query(sql, rowMapper, symbol, timeframe, limit);
	}

	/**
	 * Find the latest market data for a symbol (any timeframe).
	 */
	public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
		String sql = """
				SELECT * FROM market_data
				WHERE symbol = ?
				ORDER BY timestamp DESC
				LIMIT 1
				""";
		List<MarketDataEntity> results = jdbcTemplate.query(sql, rowMapper, symbol);
		if (!results.isEmpty()) {
			MarketDataEntity latest = results.get(0);
			log.info("Latest data for {}: timestamp={}, timeframe={}, close={}",
				symbol, latest.getTimestamp(), latest.getTimeframe(), latest.getClose());
		}
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find the latest market data for a symbol with specific timeframe.
	 */
	public Optional<MarketDataEntity> findLatestBySymbolAndTimeframe(String symbol, String timeframe) {
		String sql = """
				SELECT * FROM market_data
				WHERE symbol = ? AND timeframe = ?
				ORDER BY timestamp DESC
				LIMIT 1
				""";
		List<MarketDataEntity> results = jdbcTemplate.query(sql, rowMapper, symbol, timeframe);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Get all distinct symbols in the database.
	 */
	public List<String> findDistinctSymbols() {
		String sql = "SELECT DISTINCT symbol FROM market_data ORDER BY symbol";
		return jdbcTemplate.queryForList(sql, String.class);
	}

	/**
	 * Get all distinct timeframes for a symbol.
	 */
	public List<String> findDistinctTimeframesBySymbol(String symbol) {
		String sql = "SELECT DISTINCT timeframe FROM market_data WHERE symbol = ?";
		return jdbcTemplate.queryForList(sql, String.class, symbol);
	}

	/**
	 * Count records by symbol.
	 */
	public long countBySymbol(String symbol) {
		String sql = "SELECT COUNT(*) FROM market_data WHERE symbol = ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, symbol);
		return count != null ? count : 0;
	}

	/**
	 * Count records by symbol and timeframe.
	 */
	public long countBySymbolAndTimeframe(String symbol, String timeframe) {
		String sql = "SELECT COUNT(*) FROM market_data WHERE symbol = ? AND timeframe = ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, symbol, timeframe);
		return count != null ? count : 0;
	}

	/**
	 * Check if data exists for a symbol at a specific timestamp and timeframe.
	 */
	public boolean existsBySymbolAndTimestampAndTimeframe(String symbol, Instant timestamp, String timeframe) {
		String sql = "SELECT COUNT(*) FROM market_data WHERE symbol = ? AND timestamp = ? AND timeframe = ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, symbol, Timestamp.from(timestamp), timeframe);
		return count != null && count > 0;
	}

	/**
	 * Insert market data using ClickHouse batch insert. Uses ReplacingMergeTree semantics
	 * for upsert behavior.
	 */
	public void save(MarketDataEntity entity) {
		String sql = """
				INSERT INTO market_data (
				    symbol, timeframe, timestamp, open_price, high_price, low_price, close_price,
				    volume, vwap, trades, change_amount, change_percent, data_source, data_quality,
				    asset_type, exchange, collected_at, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		jdbcTemplate.update(sql, entity.getSymbol(), entity.getTimeframe(),
				entity.getTimestamp() != null ? Timestamp.from(Instant.ofEpochMilli(entity.getTimestamp())) : null,
				entity.getOpen(), entity.getHigh(), entity.getLow(), entity.getClose(), entity.getVolume(),
				entity.getVwap(), entity.getTrades(), entity.getChangeAmount(), entity.getChangePercent(),
				entity.getDataSource(), entity.getDataQuality(), entity.getAssetType(), entity.getExchange(),
				entity.getCollectedAt() != null ? Timestamp.from(Instant.ofEpochMilli(entity.getCollectedAt())) : null,
				Timestamp.from(Instant.now()));
	}

	/**
	 * Batch insert market data for efficient bulk loading.
	 */
	public void saveAll(List<MarketDataEntity> entities) {
		if (entities.isEmpty()) {
			return;
		}

		String sql = """
				INSERT INTO market_data (
				    symbol, timeframe, timestamp, open_price, high_price, low_price, close_price,
				    volume, vwap, trades, change_amount, change_percent, data_source, data_quality,
				    asset_type, exchange, collected_at, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		List<Object[]> batchArgs = entities.stream()
			.map(e -> new Object[] { e.getSymbol(), e.getTimeframe(),
					e.getTimestamp() != null ? Timestamp.from(Instant.ofEpochMilli(e.getTimestamp())) : null,
					e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVolume(), e.getVwap(), e.getTrades(),
					e.getChangeAmount(), e.getChangePercent(), e.getDataSource(), e.getDataQuality(), e.getAssetType(),
					e.getExchange(),
					e.getCollectedAt() != null ? Timestamp.from(Instant.ofEpochMilli(e.getCollectedAt())) : null,
					Timestamp.from(Instant.now()) })
			.toList();

		jdbcTemplate.batchUpdate(sql, batchArgs);
		log.debug("Batch inserted {} market data records", entities.size());
	}

	/**
	 * Delete records older than a cutoff timestamp.
	 */
	public int deleteOlderThan(Instant cutoff) {
		String sql = "ALTER TABLE market_data DELETE WHERE timestamp < ?";
		jdbcTemplate.update(sql, Timestamp.from(cutoff));
		return 0; // ClickHouse doesn't return affected rows for mutations
	}

	/**
	 * Count corrupted 1D bars (timestamps not at midnight UTC).
	 * Used for analysis before cleanup.
	 */
	public long countCorrupted1DBars() {
		String sql = """
				SELECT COUNT(*) FROM market_data
				WHERE timeframe = '1D' AND toHour(timestamp) != 0
				""";
		Long count = jdbcTemplate.queryForObject(sql, Long.class);
		return count != null ? count : 0;
	}

	/**
	 * Delete corrupted 1D bars (timestamps not at midnight UTC).
	 * Returns immediately; deletion is async in ClickHouse.
	 */
	public void deleteCorrupted1DBars() {
		String sql = "ALTER TABLE market_data DELETE WHERE timeframe = '1D' AND toHour(timestamp) != 0";
		jdbcTemplate.update(sql);
		log.info("Submitted DELETE for corrupted 1D bars (non-midnight UTC timestamps)");
	}

	/**
	 * Count corrupted 1H bars (timestamps not on-the-hour).
	 * Used for analysis before cleanup.
	 */
	public long countCorrupted1HBars() {
		String sql = """
				SELECT COUNT(*) FROM market_data
				WHERE timeframe = '1H' AND toMinute(timestamp) != 0
				""";
		Long count = jdbcTemplate.queryForObject(sql, Long.class);
		return count != null ? count : 0;
	}

	/**
	 * Delete corrupted 1H bars (timestamps not on-the-hour).
	 * Returns immediately; deletion is async in ClickHouse.
	 */
	public void deleteCorrupted1HBars() {
		String sql = "ALTER TABLE market_data DELETE WHERE timeframe = '1H' AND toMinute(timestamp) != 0";
		jdbcTemplate.update(sql);
		log.info("Submitted DELETE for corrupted 1H bars (timestamps not on-the-hour)");
	}

	/**
	 * Delete corrupted 1W bars (timestamps not at midnight UTC).
	 * Returns immediately; deletion is async in ClickHouse.
	 */
	public void deleteCorrupted1WBars() {
		String sql = "ALTER TABLE market_data DELETE WHERE timeframe = '1W' AND toHour(timestamp) != 0";
		jdbcTemplate.update(sql);
		log.info("Submitted DELETE for corrupted 1W bars (non-midnight UTC timestamps)");
	}

	/**
	 * Delete corrupted 1M bars (timestamps not at midnight UTC).
	 * Returns immediately; deletion is async in ClickHouse.
	 */
	public void deleteCorrupted1MBars() {
		String sql = "ALTER TABLE market_data DELETE WHERE timeframe = '1M' AND toHour(timestamp) != 0";
		jdbcTemplate.update(sql);
		log.info("Submitted DELETE for corrupted 1M bars (non-midnight UTC timestamps)");
	}

	/**
	 * Delete ALL corrupted bars across all timeframes (non-UTC timestamps).
	 * Returns immediately; deletion is async in ClickHouse.
	 */
	public void deleteAllCorruptedBars() {
		// Delete 1D bars not at midnight
		jdbcTemplate.update("ALTER TABLE market_data DELETE WHERE timeframe = '1D' AND toHour(timestamp) != 0");
		// Delete 1W bars not at midnight
		jdbcTemplate.update("ALTER TABLE market_data DELETE WHERE timeframe = '1W' AND toHour(timestamp) != 0");
		// Delete 1M bars not at midnight
		jdbcTemplate.update("ALTER TABLE market_data DELETE WHERE timeframe = '1M' AND toHour(timestamp) != 0");
		// Delete 1H bars not on-the-hour
		jdbcTemplate.update("ALTER TABLE market_data DELETE WHERE timeframe = '1H' AND toMinute(timestamp) != 0");
		log.warn("Submitted DELETE for ALL corrupted bars across all timeframes");
	}

	/**
	 * Optimize table to apply pending mutations (deletions) immediately.
	 * This forces ClickHouse to merge data parts and apply ALTER TABLE DELETE.
	 */
	public void optimizeTableFinal() {
		String sql = "OPTIMIZE TABLE market_data FINAL";
		jdbcTemplate.update(sql);
		log.info("Submitted OPTIMIZE TABLE FINAL for market_data");
	}

	/**
	 * Migrate timeframe format from long format to short format.
	 * Converts: 1H->1H, 4Hour->4H, 1D->1D, 1W->1W, 1M->1M
	 *
	 * Note: Minute formats (1Min, 5Min, 15Min, 30Min) stay unchanged.
	 * Note: Data already stored as 1D stays unchanged.
	 */
	public void migrateTimeframeToShortFormat() {
		log.info("=== Starting timeframe format migration to short format ===");

		// Migrate 1H -> 1H
		log.info("Migrating 1H -> 1H...");
		jdbcTemplate.update("ALTER TABLE market_data UPDATE timeframe = '1H' WHERE timeframe = '1H'");

		// Migrate 4Hour -> 4H
		log.info("Migrating 4Hour -> 4H...");
		jdbcTemplate.update("ALTER TABLE market_data UPDATE timeframe = '4H' WHERE timeframe = '4Hour'");

		// Migrate 1D -> 1D (in case any exists)
		log.info("Migrating 1D -> 1D...");
		jdbcTemplate.update("ALTER TABLE market_data UPDATE timeframe = '1D' WHERE timeframe = '1D'");

		// Migrate 1W -> 1W
		log.info("Migrating 1W -> 1W...");
		jdbcTemplate.update("ALTER TABLE market_data UPDATE timeframe = '1W' WHERE timeframe = '1W'");

		// Migrate 1M -> 1M
		log.info("Migrating 1M -> 1M...");
		jdbcTemplate.update("ALTER TABLE market_data UPDATE timeframe = '1M' WHERE timeframe = '1M'");

		log.info("=== Timeframe migration submitted. Run OPTIMIZE TABLE FINAL to apply immediately. ===");
	}

	/**
	 * Get timestamp analysis for a timeframe.
	 * Returns counts grouped by hour of day to identify corruption patterns.
	 */
	public List<Map<String, Object>> analyzeTimestampsByTimeframe(String timeframe) {
		String sql = """
				SELECT
				    toHour(timestamp) as hour,
				    COUNT(*) as count
				FROM market_data
				WHERE timeframe = ?
				GROUP BY hour
				ORDER BY hour
				""";
		return jdbcTemplate.queryForList(sql, timeframe);
	}

	/**
	 * Find all symbols that have data within a time range.
	 */
	public List<String> findSymbolsWithDataInRange(Instant startTime, Instant endTime) {
		String sql = """
				SELECT DISTINCT symbol FROM market_data
				WHERE timestamp >= ? AND timestamp < ?
				""";
		return jdbcTemplate.queryForList(sql, String.class, Timestamp.from(startTime), Timestamp.from(endTime));
	}

	/**
	 * Row mapper for converting ResultSet to MarketDataEntity.
	 */
	private static class MarketDataRowMapper implements RowMapper<MarketDataEntity> {

		@Override
		public MarketDataEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			MarketDataEntity entity = new MarketDataEntity();
			entity.setSymbol(rs.getString("symbol"));
			entity.setTimeframe(rs.getString("timeframe"));

			Timestamp timestamp = rs.getTimestamp("timestamp");
			if (timestamp != null) {
				entity.setTimestamp(timestamp.toInstant().toEpochMilli());
			}

			entity.setOpen(getBigDecimal(rs, "open_price"));
			entity.setHigh(getBigDecimal(rs, "high_price"));
			entity.setLow(getBigDecimal(rs, "low_price"));
			entity.setClose(getBigDecimal(rs, "close_price"));
			entity.setVolume(getBigDecimal(rs, "volume"));
			entity.setVwap(getBigDecimal(rs, "vwap"));
			entity.setTrades(rs.getLong("trades"));
			entity.setChangeAmount(getBigDecimal(rs, "change_amount"));
			entity.setChangePercent(getBigDecimal(rs, "change_percent"));
			entity.setDataSource(rs.getString("data_source"));
			entity.setDataQuality(rs.getString("data_quality"));
			entity.setAssetType(rs.getString("asset_type"));
			entity.setExchange(rs.getString("exchange"));

			Timestamp collectedAt = rs.getTimestamp("collected_at");
			if (collectedAt != null) {
				entity.setCollectedAt(collectedAt.toInstant().toEpochMilli());
			}

			// Note: createdAt field exists in ClickHouse but not in MarketDataEntity
			// Skipping mapping of created_at column

			return entity;
		}

		private BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
			BigDecimal value = rs.getBigDecimal(column);
			return rs.wasNull() ? null : value;
		}

	}

}
