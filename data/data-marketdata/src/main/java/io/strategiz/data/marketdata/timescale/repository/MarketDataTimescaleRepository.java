package io.strategiz.data.marketdata.timescale.repository;

import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TimescaleDB market data.
 * Provides time-series optimized queries leveraging TimescaleDB hypertables.
 */
@Repository
public interface MarketDataTimescaleRepository extends JpaRepository<MarketDataTimescaleEntity, MarketDataTimescaleId> {

    /**
     * Find market data by symbol within a time range and optional timeframe filter.
     * Results ordered by timestamp ascending for charting.
     */
    @Query("SELECT m FROM MarketDataTimescaleEntity m " +
           "WHERE m.symbol = :symbol " +
           "AND m.timestamp >= :startTime " +
           "AND m.timestamp < :endTime " +
           "AND (:timeframe IS NULL OR m.timeframe = :timeframe) " +
           "ORDER BY m.timestamp ASC")
    List<MarketDataTimescaleEntity> findBySymbolAndTimeRange(
        @Param("symbol") String symbol,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("timeframe") String timeframe
    );

    /**
     * Find market data by symbol and timeframe, limited and ordered by timestamp descending.
     */
    @Query("SELECT m FROM MarketDataTimescaleEntity m " +
           "WHERE m.symbol = :symbol " +
           "AND m.timeframe = :timeframe " +
           "ORDER BY m.timestamp DESC " +
           "LIMIT :limit")
    List<MarketDataTimescaleEntity> findBySymbolAndTimeframe(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("limit") int limit
    );

    /**
     * Find the latest market data for a symbol (any timeframe).
     */
    @Query("SELECT m FROM MarketDataTimescaleEntity m " +
           "WHERE m.symbol = :symbol " +
           "ORDER BY m.timestamp DESC " +
           "LIMIT 1")
    Optional<MarketDataTimescaleEntity> findLatestBySymbol(@Param("symbol") String symbol);

    /**
     * Find the latest market data for a symbol with specific timeframe.
     */
    @Query("SELECT m FROM MarketDataTimescaleEntity m " +
           "WHERE m.symbol = :symbol " +
           "AND m.timeframe = :timeframe " +
           "ORDER BY m.timestamp DESC " +
           "LIMIT 1")
    Optional<MarketDataTimescaleEntity> findLatestBySymbolAndTimeframe(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe
    );

    /**
     * Get all distinct symbols in the database.
     */
    @Query("SELECT DISTINCT m.symbol FROM MarketDataTimescaleEntity m ORDER BY m.symbol")
    List<String> findDistinctSymbols();

    /**
     * Get all distinct timeframes for a symbol.
     */
    @Query("SELECT DISTINCT m.timeframe FROM MarketDataTimescaleEntity m WHERE m.symbol = :symbol")
    List<String> findDistinctTimeframesBySymbol(@Param("symbol") String symbol);

    /**
     * Count records by symbol.
     */
    long countBySymbol(String symbol);

    /**
     * Count records by symbol and timeframe.
     */
    long countBySymbolAndTimeframe(String symbol, String timeframe);

    /**
     * Check if data exists for a symbol at a specific timestamp and timeframe.
     */
    boolean existsBySymbolAndTimestampAndTimeframe(String symbol, Instant timestamp, String timeframe);

    /**
     * Delete records older than a cutoff timestamp.
     * Used for data retention policies.
     */
    @Modifying
    @Query("DELETE FROM MarketDataTimescaleEntity m WHERE m.timestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    /**
     * Find all symbols that have data within a time range.
     */
    @Query("SELECT DISTINCT m.symbol FROM MarketDataTimescaleEntity m " +
           "WHERE m.timestamp >= :startTime AND m.timestamp < :endTime")
    List<String> findSymbolsWithDataInRange(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Native upsert that avoids SELECT before INSERT.
     * Uses PostgreSQL INSERT ... ON CONFLICT to efficiently upsert data.
     */
    @Modifying
    @Query(value = "INSERT INTO market_data (symbol, timeframe, timestamp, open_price, high_price, low_price, " +
           "close_price, volume, vwap, trades, change_amount, change_percent, data_source, data_quality, " +
           "asset_type, exchange, collected_at, created_at) " +
           "VALUES (:symbol, :timeframe, :timestamp, :openPrice, :highPrice, :lowPrice, :closePrice, " +
           ":volume, :vwap, :trades, :changeAmount, :changePercent, :dataSource, :dataQuality, " +
           ":assetType, :exchange, :collectedAt, :createdAt) " +
           "ON CONFLICT (symbol, timeframe, timestamp) DO UPDATE SET " +
           "open_price = EXCLUDED.open_price, high_price = EXCLUDED.high_price, low_price = EXCLUDED.low_price, " +
           "close_price = EXCLUDED.close_price, volume = EXCLUDED.volume, vwap = EXCLUDED.vwap, " +
           "trades = EXCLUDED.trades, change_amount = EXCLUDED.change_amount, change_percent = EXCLUDED.change_percent, " +
           "data_source = EXCLUDED.data_source, data_quality = EXCLUDED.data_quality, collected_at = EXCLUDED.collected_at",
           nativeQuery = true)
    void upsert(
        @Param("symbol") String symbol,
        @Param("timeframe") String timeframe,
        @Param("timestamp") Instant timestamp,
        @Param("openPrice") Double openPrice,
        @Param("highPrice") Double highPrice,
        @Param("lowPrice") Double lowPrice,
        @Param("closePrice") Double closePrice,
        @Param("volume") Long volume,
        @Param("vwap") Double vwap,
        @Param("trades") Long trades,
        @Param("changeAmount") Double changeAmount,
        @Param("changePercent") Double changePercent,
        @Param("dataSource") String dataSource,
        @Param("dataQuality") String dataQuality,
        @Param("assetType") String assetType,
        @Param("exchange") String exchange,
        @Param("collectedAt") Instant collectedAt,
        @Param("createdAt") Instant createdAt
    );
}
