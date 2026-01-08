package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
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
	public List<MarketDataTimescaleEntity> findBySymbolAndTimeRange(String symbol, Instant startTime, Instant endTime,
			String timeframe) {
		String sql;
		Object[] params;

		if (timeframe != null) {
			sql = """
					SELECT * FROM market_data
					WHERE symbol = ? AND timestamp >= ? AND timestamp < ? AND timeframe = ?
					ORDER BY timestamp ASC
					""";
			params = new Object[] { symbol, Timestamp.from(startTime), Timestamp.from(endTime), timeframe };
		}
		else {
			sql = """
					SELECT * FROM market_data
					WHERE symbol = ? AND timestamp >= ? AND timestamp < ?
					ORDER BY timestamp ASC
					""";
			params = new Object[] { symbol, Timestamp.from(startTime), Timestamp.from(endTime) };
		}

		return jdbcTemplate.query(sql, rowMapper, params);
	}

	/**
	 * Find market data by symbol and timeframe, limited and ordered by timestamp
	 * descending.
	 */
	public List<MarketDataTimescaleEntity> findBySymbolAndTimeframe(String symbol, String timeframe, int limit) {
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
	public Optional<MarketDataTimescaleEntity> findLatestBySymbol(String symbol) {
		String sql = """
				SELECT * FROM market_data
				WHERE symbol = ?
				ORDER BY timestamp DESC
				LIMIT 1
				""";
		List<MarketDataTimescaleEntity> results = jdbcTemplate.query(sql, rowMapper, symbol);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find the latest market data for a symbol with specific timeframe.
	 */
	public Optional<MarketDataTimescaleEntity> findLatestBySymbolAndTimeframe(String symbol, String timeframe) {
		String sql = """
				SELECT * FROM market_data
				WHERE symbol = ? AND timeframe = ?
				ORDER BY timestamp DESC
				LIMIT 1
				""";
		List<MarketDataTimescaleEntity> results = jdbcTemplate.query(sql, rowMapper, symbol, timeframe);
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
	public void save(MarketDataTimescaleEntity entity) {
		String sql = """
				INSERT INTO market_data (
				    symbol, timeframe, timestamp, open_price, high_price, low_price, close_price,
				    volume, vwap, trades, change_amount, change_percent, data_source, data_quality,
				    asset_type, exchange, collected_at, created_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		jdbcTemplate.update(sql, entity.getSymbol(), entity.getTimeframe(), Timestamp.from(entity.getTimestamp()),
				entity.getOpen(), entity.getHigh(), entity.getLow(), entity.getClose(), entity.getVolume(),
				entity.getVwap(), entity.getTrades(), entity.getChangeAmount(), entity.getChangePercent(),
				entity.getDataSource(), entity.getDataQuality(), entity.getAssetType(), entity.getExchange(),
				entity.getCollectedAt() != null ? Timestamp.from(entity.getCollectedAt()) : null,
				Timestamp.from(entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now()));
	}

	/**
	 * Batch insert market data for efficient bulk loading.
	 */
	public void saveAll(List<MarketDataTimescaleEntity> entities) {
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
			.map(e -> new Object[] { e.getSymbol(), e.getTimeframe(), Timestamp.from(e.getTimestamp()), e.getOpen(),
					e.getHigh(), e.getLow(), e.getClose(), e.getVolume(), e.getVwap(), e.getTrades(),
					e.getChangeAmount(), e.getChangePercent(), e.getDataSource(), e.getDataQuality(), e.getAssetType(),
					e.getExchange(), e.getCollectedAt() != null ? Timestamp.from(e.getCollectedAt()) : null,
					Timestamp.from(e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now()) })
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
	 * Row mapper for converting ResultSet to MarketDataTimescaleEntity.
	 */
	private static class MarketDataRowMapper implements RowMapper<MarketDataTimescaleEntity> {

		@Override
		public MarketDataTimescaleEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			MarketDataTimescaleEntity entity = new MarketDataTimescaleEntity();
			entity.setSymbol(rs.getString("symbol"));
			entity.setTimeframe(rs.getString("timeframe"));
			entity.setTimestamp(rs.getTimestamp("timestamp").toInstant());
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
				entity.setCollectedAt(collectedAt.toInstant());
			}

			Timestamp createdAt = rs.getTimestamp("created_at");
			if (createdAt != null) {
				entity.setCreatedAt(createdAt.toInstant());
			}

			return entity;
		}

		private BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
			BigDecimal value = rs.getBigDecimal(column);
			return rs.wasNull() ? null : value;
		}

	}

}
