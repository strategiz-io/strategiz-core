package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import io.strategiz.data.fundamentals.repository.FundamentalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of FundamentalsRepository that delegates to the data access layer.
 * Provides a clean abstraction over the underlying data storage implementation.
 */
@Repository
public class FundamentalsRepositoryImpl implements FundamentalsRepository {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsRepositoryImpl.class);

	private final FundamentalsDataRepository dataRepo;

	public FundamentalsRepositoryImpl(FundamentalsDataRepository dataRepo) {
		this.dataRepo = dataRepo;
		log.info("FundamentalsRepository initialized");
	}

	@Override
	public FundamentalsEntity save(FundamentalsEntity entity) {
		return dataRepo.save(entity);
	}

	@Override
	public void saveAll(List<FundamentalsEntity> entities) {
		dataRepo.saveAll(entities);
	}

	@Override
	public Optional<FundamentalsEntity> findLatestBySymbol(String symbol) {
		return dataRepo.findLatestBySymbol(symbol);
	}

	@Override
	public Optional<FundamentalsEntity> findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol,
			String periodType) {
		return dataRepo.findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(symbol, periodType);
	}

	@Override
	public List<FundamentalsEntity> findBySymbolOrderByFiscalPeriodDesc(String symbol) {
		return dataRepo.findBySymbolOrderByFiscalPeriodDesc(symbol);
	}

	@Override
	public List<FundamentalsEntity> findBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol,
			String periodType) {
		return dataRepo.findBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(symbol, periodType);
	}

	@Override
	public List<FundamentalsEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate,
			LocalDate endDate) {
		return dataRepo.findBySymbolAndDateRange(symbol, startDate, endDate);
	}

	@Override
	public List<FundamentalsEntity> findLatestBySymbols(List<String> symbols) {
		return dataRepo.findLatestBySymbols(symbols);
	}

	@Override
	public List<String> findSymbolsByPERatioRange(BigDecimal minPE, BigDecimal maxPE) {
		return dataRepo.findSymbolsByPERatioRange(minPE, maxPE);
	}

	@Override
	public List<String> findSymbolsByDividendYield(BigDecimal minYield) {
		return dataRepo.findSymbolsByDividendYield(minYield);
	}

	@Override
	public List<String> findSymbolsByROE(BigDecimal minRoe) {
		return dataRepo.findSymbolsByROE(minRoe);
	}

	@Override
	public List<String> findDistinctSymbols() {
		return dataRepo.findDistinctSymbols();
	}

	@Override
	public long countBySymbol(String symbol) {
		return dataRepo.countBySymbol(symbol);
	}

	@Override
	public boolean existsBySymbolAndFiscalPeriodAndPeriodType(String symbol, LocalDate fiscalPeriod,
			String periodType) {
		return dataRepo.existsBySymbolAndFiscalPeriodAndPeriodType(symbol, fiscalPeriod, periodType);
	}

	@Override
	public void deleteBySymbol(String symbol) {
		dataRepo.deleteBySymbol(symbol);
	}

}
