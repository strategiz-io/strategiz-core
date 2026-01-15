package io.strategiz.data.marketdata.clickhouse.service;

import io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ClickHouse implementation of MarketDataRepository. When enabled, this becomes the
 * primary implementation, replacing TimescaleDB for market data.
 */
@Repository
@Primary
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class ClickHouseMarketDataRepositoryImpl implements MarketDataRepository {

	private static final Logger log = LoggerFactory.getLogger(ClickHouseMarketDataRepositoryImpl.class);

	private final MarketDataClickHouseRepository clickHouseRepo;

	@Autowired
	public ClickHouseMarketDataRepositoryImpl(MarketDataClickHouseRepository clickHouseRepo) {
		this.clickHouseRepo = clickHouseRepo;
		log.info("ClickHouse MarketDataRepository initialized - using ClickHouse Cloud for market data");
	}

	@Override
	public MarketDataEntity save(MarketDataEntity entity) {
		MarketDataEntity ts = entity;
		clickHouseRepo.save(ts);
		return entity;
	}

	@Override
	public List<MarketDataEntity> saveAll(List<MarketDataEntity> entities) {
		List<MarketDataEntity> tsEntities = entities.stream().collect(Collectors.toList());
		clickHouseRepo.saveAll(tsEntities);
		return entities;
	}

	@Override
	public Optional<MarketDataEntity> findById(String id) {
		log.warn("findById not supported for ClickHouse - use findBySymbolAndTimeRange instead");
		return Optional.empty();
	}

	@Override
	public List<MarketDataEntity> findBySymbol(String symbol) {
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeframe(symbol, "1D", 500);
		return results;
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay, null);
		return results;
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
		Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeRange(symbol, start, end, null);
		return results;
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate,
			String timeframe, int limit) {
		Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeRange(symbol, start, end, timeframe);

		if (results.size() > limit) {
			results = results.subList(0, limit);
		}

		return results;
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe) {
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeframe(symbol, timeframe, 500);
		return results;
	}

	@Override
	public List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		return symbols.stream()
			.flatMap(symbol -> clickHouseRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay, null).stream())
			.collect(Collectors.toList());
	}

	@Override
	public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
		return clickHouseRepo.findLatestBySymbol(symbol);
	}

	@Override
	public List<MarketDataEntity> findLatestBySymbols(List<String> symbols) {
		return symbols.stream()
			.map(clickHouseRepo::findLatestBySymbol)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	@Override
	public int deleteOlderThan(LocalDate cutoffDate) {
		Instant cutoff = cutoffDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		return clickHouseRepo.deleteOlderThan(cutoff);
	}

	@Override
	public long countBySymbol(String symbol) {
		return clickHouseRepo.countBySymbol(symbol);
	}

	@Override
	public boolean existsBySymbolAndDate(String symbol, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay, null);
		return !results.isEmpty();
	}

	@Override
	public List<String> findDistinctSymbols() {
		return clickHouseRepo.findDistinctSymbols();
	}

	@Override
	public DateRange getDateRangeForSymbol(String symbol) {
		Optional<MarketDataEntity> latest = clickHouseRepo.findLatestBySymbol(symbol);
		if (latest.isEmpty()) {
			return null;
		}

		Instant start = Instant.parse("2000-01-01T00:00:00Z");
		Instant end = Instant.now();
		List<MarketDataEntity> results = clickHouseRepo.findBySymbolAndTimeRange(symbol, start, end, null);

		if (results.isEmpty()) {
			return null;
		}

		LocalDate startDate = Instant.ofEpochMilli(results.get(0).getTimestamp()).atZone(ZoneOffset.UTC).toLocalDate();
		LocalDate endDate = Instant.ofEpochMilli(latest.get().getTimestamp()).atZone(ZoneOffset.UTC).toLocalDate();

		return new DateRange(startDate, endDate);
	}

}
