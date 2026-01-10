package io.strategiz.data.fundamentals.repository;

import io.strategiz.data.fundamentals.entity.FundamentalsEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for company fundamentals operations.
 * Abstracts the underlying storage implementation (database-agnostic).
 *
 * <p>This interface provides methods for:</p>
 * <ul>
 *   <li>Saving and batch-saving fundamentals data</li>
 *   <li>Querying fundamentals by symbol, period type, and date ranges</li>
 *   <li>Stock screening based on financial metrics (P/E, dividend yield, ROE)</li>
 *   <li>Utility operations (counting, existence checks, deletion)</li>
 * </ul>
 */
public interface FundamentalsRepository {

	/**
	 * Save a single fundamentals record.
	 *
	 * @param entity the fundamentals entity to save
	 * @return the saved entity
	 */
	FundamentalsEntity save(FundamentalsEntity entity);

	/**
	 * Save multiple fundamentals records in batch.
	 *
	 * @param entities the list of entities to save
	 */
	void saveAll(List<FundamentalsEntity> entities);

	/**
	 * Find latest fundamentals for a symbol (any period type).
	 * Returns the most recent record based on fiscal_period.
	 *
	 * @param symbol the stock symbol (e.g., "AAPL")
	 * @return the latest fundamentals record, or empty if not found
	 */
	Optional<FundamentalsEntity> findLatestBySymbol(String symbol);

	/**
	 * Find latest fundamentals for a symbol and specific period type.
	 *
	 * @param symbol the stock symbol
	 * @param periodType the period type (QUARTERLY, ANNUAL, TTM)
	 * @return the latest fundamentals for the specified period type
	 */
	Optional<FundamentalsEntity> findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol,
			String periodType);

	/**
	 * Find all fundamentals for a symbol, ordered by fiscal period descending.
	 *
	 * @param symbol the stock symbol
	 * @return list of fundamentals records for the symbol
	 */
	List<FundamentalsEntity> findBySymbolOrderByFiscalPeriodDesc(String symbol);

	/**
	 * Find fundamentals by symbol and period type, ordered by fiscal period.
	 *
	 * @param symbol the stock symbol
	 * @param periodType the period type (QUARTERLY, ANNUAL, TTM)
	 * @return list of fundamentals records
	 */
	List<FundamentalsEntity> findBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol,
			String periodType);

	/**
	 * Find fundamentals for a symbol within a date range.
	 *
	 * @param symbol the stock symbol
	 * @param startDate the start date (inclusive)
	 * @param endDate the end date (inclusive)
	 * @return list of fundamentals records in the date range
	 */
	List<FundamentalsEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate);

	/**
	 * Find fundamentals for multiple symbols (latest for each).
	 * Useful for batch queries in strategy execution or portfolio analysis.
	 *
	 * @param symbols list of stock symbols
	 * @return list of latest fundamentals for each symbol
	 */
	List<FundamentalsEntity> findLatestBySymbols(List<String> symbols);

	/**
	 * Stock screener: Find symbols with P/E ratio in a specific range.
	 * Only considers latest fundamentals (TTM or most recent period).
	 *
	 * @param minPE minimum P/E ratio
	 * @param maxPE maximum P/E ratio
	 * @return list of symbols matching the criteria
	 */
	List<String> findSymbolsByPERatioRange(BigDecimal minPE, BigDecimal maxPE);

	/**
	 * Stock screener: Find symbols with dividend yield above a threshold.
	 *
	 * @param minYield minimum dividend yield (percentage)
	 * @return list of symbols with dividend yield >= minYield
	 */
	List<String> findSymbolsByDividendYield(BigDecimal minYield);

	/**
	 * Stock screener: Find symbols with ROE (Return on Equity) above threshold.
	 *
	 * @param minRoe minimum ROE (percentage)
	 * @return list of symbols with ROE >= minRoe
	 */
	List<String> findSymbolsByROE(BigDecimal minRoe);

	/**
	 * Get all distinct symbols with fundamentals data.
	 *
	 * @return list of all symbols in the repository
	 */
	List<String> findDistinctSymbols();

	/**
	 * Count the number of fundamentals records for a symbol.
	 *
	 * @param symbol the stock symbol
	 * @return the number of records
	 */
	long countBySymbol(String symbol);

	/**
	 * Check if fundamentals exist for a symbol and fiscal period.
	 *
	 * @param symbol the stock symbol
	 * @param fiscalPeriod the fiscal period
	 * @param periodType the period type
	 * @return true if exists, false otherwise
	 */
	boolean existsBySymbolAndFiscalPeriodAndPeriodType(String symbol, LocalDate fiscalPeriod, String periodType);

	/**
	 * Delete all fundamentals records for a symbol.
	 * Useful for cleanup or re-importing data.
	 *
	 * @param symbol the stock symbol
	 */
	void deleteBySymbol(String symbol);

}
