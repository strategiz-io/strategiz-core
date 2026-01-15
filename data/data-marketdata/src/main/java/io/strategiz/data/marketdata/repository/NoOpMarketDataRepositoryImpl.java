package io.strategiz.data.marketdata.repository;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * No-op implementation of MarketDataRepository for local development. Used when neither
 * ClickHouse nor TimescaleDB is enabled. This bean is registered in DataMarketDataConfig
 * when no other MarketDataRepository exists.
 */
public class NoOpMarketDataRepositoryImpl implements MarketDataRepository {

	private static final Logger log = LoggerFactory.getLogger(NoOpMarketDataRepositoryImpl.class);

	public NoOpMarketDataRepositoryImpl() {
		log.warn("Using No-Op MarketDataRepository - market data operations will not persist");
		log.warn("Enable ClickHouse or TimescaleDB for production use");
	}

	@Override
	public MarketDataEntity save(MarketDataEntity entity) {
		log.debug("No-op save: {}", entity);
		return entity;
	}

	@Override
	public List<MarketDataEntity> saveAll(List<MarketDataEntity> entities) {
		log.debug("No-op saveAll: {} entities", entities.size());
		return entities;
	}

	@Override
	public Optional<MarketDataEntity> findById(String id) {
		log.debug("No-op findById: {}", id);
		return Optional.empty();
	}

	@Override
	public List<MarketDataEntity> findBySymbol(String symbol) {
		log.debug("No-op findBySymbol: {}", symbol);
		return Collections.emptyList();
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date) {
		log.debug("No-op findBySymbolAndDate: {} on {}", symbol, date);
		return Collections.emptyList();
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
		log.debug("No-op findBySymbolAndDateRange: {} from {} to {}", symbol, startDate, endDate);
		return Collections.emptyList();
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate,
			String timeframe, int limit) {
		log.debug("No-op findBySymbolAndDateRange: {} from {} to {} with timeframe {} limit {}", symbol, startDate,
				endDate, timeframe, limit);
		return Collections.emptyList();
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe) {
		log.debug("No-op findBySymbolAndTimeframe: {} with {}", symbol, timeframe);
		return Collections.emptyList();
	}

	@Override
	public List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date) {
		log.debug("No-op findBySymbolsAndDate: {} symbols on {}", symbols.size(), date);
		return Collections.emptyList();
	}

	@Override
	public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
		log.debug("No-op findLatestBySymbol: {}", symbol);
		return Optional.empty();
	}

	@Override
	public List<MarketDataEntity> findLatestBySymbols(List<String> symbols) {
		log.debug("No-op findLatestBySymbols: {} symbols", symbols.size());
		return Collections.emptyList();
	}

	@Override
	public int deleteOlderThan(LocalDate cutoffDate) {
		log.debug("No-op deleteOlderThan: {}", cutoffDate);
		return 0;
	}

	@Override
	public long countBySymbol(String symbol) {
		log.debug("No-op countBySymbol: {}", symbol);
		return 0;
	}

	@Override
	public boolean existsBySymbolAndDate(String symbol, LocalDate date) {
		log.debug("No-op existsBySymbolAndDate: {} on {}", symbol, date);
		return false;
	}

	@Override
	public List<String> findDistinctSymbols() {
		log.debug("No-op findDistinctSymbols");
		return Collections.emptyList();
	}

	@Override
	public DateRange getDateRangeForSymbol(String symbol) {
		log.debug("No-op getDateRangeForSymbol: {}", symbol);
		return null;
	}

}
