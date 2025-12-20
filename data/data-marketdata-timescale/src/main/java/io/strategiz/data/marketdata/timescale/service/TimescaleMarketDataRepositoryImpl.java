package io.strategiz.data.marketdata.timescale.service;

import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import io.strategiz.data.marketdata.timescale.entity.MarketDataTimescaleEntity;
import io.strategiz.data.marketdata.timescale.repository.MarketDataTimescaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TimescaleDB implementation of MarketDataRepository. When enabled, this
 * becomes the primary implementation, replacing Firestore for market data.
 */
@Repository
@Primary
@ConditionalOnProperty(name = "strategiz.timescale.enabled", havingValue = "true")
public class TimescaleMarketDataRepositoryImpl implements MarketDataRepository {

	private static final Logger log = LoggerFactory.getLogger(TimescaleMarketDataRepositoryImpl.class);

	private final MarketDataTimescaleRepository timescaleRepo;

	private final MarketDataConverter converter;

	@Autowired
	public TimescaleMarketDataRepositoryImpl(MarketDataTimescaleRepository timescaleRepo,
			MarketDataConverter converter) {
		this.timescaleRepo = timescaleRepo;
		this.converter = converter;
		log.info("TimescaleDB MarketDataRepository initialized - using TimescaleDB for market data");
	}

	@Override
	@Transactional("timescaleTransactionManager")
	public MarketDataEntity save(MarketDataEntity entity) {
		MarketDataTimescaleEntity ts = converter.toTimescale(entity);
		MarketDataTimescaleEntity saved = timescaleRepo.save(ts);
		return converter.toFirestore(saved);
	}

	@Override
	@Transactional("timescaleTransactionManager")
	public List<MarketDataEntity> saveAll(List<MarketDataEntity> entities) {
		List<MarketDataTimescaleEntity> tsEntities = entities.stream()
			.map(converter::toTimescale)
			.collect(Collectors.toList());
		List<MarketDataTimescaleEntity> saved = timescaleRepo.saveAll(tsEntities);
		return saved.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public Optional<MarketDataEntity> findById(String id) {
		// ID format: SYMBOL_TIMESTAMP_TIMEFRAME - parse it
		// For now, return empty as TimescaleDB uses composite key
		log.warn("findById not optimized for TimescaleDB - use findBySymbolAndTimeRange instead");
		return Optional.empty();
	}

	@Override
	public List<MarketDataEntity> findBySymbol(String symbol) {
		// Get last 500 records for symbol
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeframe(symbol, "1Day", 500);
		return results.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay,
				null);
		return results.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
		Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeRange(symbol, start, end, null);
		return results.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate,
			String timeframe, int limit) {
		Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeRange(symbol, start, end, timeframe);

		// Apply limit
		if (results.size() > limit) {
			results = results.subList(0, limit);
		}

		return results.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe) {
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeframe(symbol, timeframe, 500);
		return results.stream().map(converter::toFirestore).collect(Collectors.toList());
	}

	@Override
	public List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		return symbols.stream()
			.flatMap(symbol -> timescaleRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay, null).stream())
			.map(converter::toFirestore)
			.collect(Collectors.toList());
	}

	@Override
	public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
		return timescaleRepo.findLatestBySymbol(symbol).map(converter::toFirestore);
	}

	@Override
	public List<MarketDataEntity> findLatestBySymbols(List<String> symbols) {
		return symbols.stream()
			.map(timescaleRepo::findLatestBySymbol)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.map(converter::toFirestore)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional("timescaleTransactionManager")
	public int deleteOlderThan(LocalDate cutoffDate) {
		Instant cutoff = cutoffDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		return timescaleRepo.deleteOlderThan(cutoff);
	}

	@Override
	public long countBySymbol(String symbol) {
		return timescaleRepo.countBySymbol(symbol);
	}

	@Override
	public boolean existsBySymbolAndDate(String symbol, LocalDate date) {
		Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeRange(symbol, startOfDay, endOfDay,
				null);
		return !results.isEmpty();
	}

	@Override
	public List<String> findDistinctSymbols() {
		return timescaleRepo.findDistinctSymbols();
	}

	@Override
	public DateRange getDateRangeForSymbol(String symbol) {
		// Get oldest and newest records
		Optional<MarketDataTimescaleEntity> latest = timescaleRepo.findLatestBySymbol(symbol);
		if (latest.isEmpty()) {
			return null;
		}

		// For simplicity, query with wide range to find oldest
		Instant start = Instant.parse("2000-01-01T00:00:00Z");
		Instant end = Instant.now();
		List<MarketDataTimescaleEntity> results = timescaleRepo.findBySymbolAndTimeRange(symbol, start, end, null);

		if (results.isEmpty()) {
			return null;
		}

		LocalDate startDate = results.get(0).getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
		LocalDate endDate = latest.get().getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();

		return new DateRange(startDate, endDate);
	}

}
