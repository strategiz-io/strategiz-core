package io.strategiz.data.fundamentals.timescale.repository;

import io.strategiz.data.fundamentals.timescale.entity.FundamentalsTimescaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for company fundamentals in TimescaleDB.
 * Provides time-series optimized queries for historical fundamental data.
 *
 * Common query patterns:
 * - Find latest fundamentals for a symbol
 * - Find fundamentals by symbol and period type (quarterly, annual, TTM)
 * - Find fundamentals in date range
 * - Stock screener queries (filter by P/E, dividend yield, etc.)
 *
 * @see FundamentalsTimescaleEntity
 */
@Repository
public interface FundamentalsTimescaleRepository extends JpaRepository<FundamentalsTimescaleEntity, String> {

    /**
     * Find latest fundamentals for a symbol (any period type).
     * Returns the most recent record based on fiscal_period.
     *
     * @param symbol the stock symbol (e.g., "AAPL")
     * @return the latest fundamentals record, or empty if not found
     */
    @Query("SELECT f FROM FundamentalsTimescaleEntity f " +
           "WHERE f.symbol = :symbol " +
           "ORDER BY f.fiscalPeriod DESC " +
           "LIMIT 1")
    Optional<FundamentalsTimescaleEntity> findLatestBySymbol(@Param("symbol") String symbol);

    /**
     * Find latest fundamentals for a symbol and specific period type.
     *
     * @param symbol the stock symbol
     * @param periodType the period type (QUARTERLY, ANNUAL, TTM)
     * @return the latest fundamentals for the specified period type
     */
    Optional<FundamentalsTimescaleEntity> findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(
        String symbol,
        String periodType
    );

    /**
     * Find all fundamentals for a symbol, ordered by fiscal period descending.
     *
     * @param symbol the stock symbol
     * @return list of fundamentals records for the symbol
     */
    List<FundamentalsTimescaleEntity> findBySymbolOrderByFiscalPeriodDesc(String symbol);

    /**
     * Find fundamentals by symbol and period type, ordered by fiscal period.
     *
     * @param symbol the stock symbol
     * @param periodType the period type (QUARTERLY, ANNUAL, TTM)
     * @return list of fundamentals records
     */
    List<FundamentalsTimescaleEntity> findBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(
        String symbol,
        String periodType
    );

    /**
     * Find fundamentals for a symbol within a date range.
     *
     * @param symbol the stock symbol
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of fundamentals records in the date range
     */
    @Query("SELECT f FROM FundamentalsTimescaleEntity f " +
           "WHERE f.symbol = :symbol " +
           "AND f.fiscalPeriod >= :startDate " +
           "AND f.fiscalPeriod <= :endDate " +
           "ORDER BY f.fiscalPeriod DESC")
    List<FundamentalsTimescaleEntity> findBySymbolAndDateRange(
        @Param("symbol") String symbol,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find fundamentals for multiple symbols (latest for each).
     * Useful for batch queries in strategy execution or portfolio analysis.
     *
     * @param symbols list of stock symbols
     * @return list of latest fundamentals for each symbol
     */
    @Query("SELECT f FROM FundamentalsTimescaleEntity f " +
           "WHERE f.symbol IN :symbols " +
           "AND f.fiscalPeriod = (" +
           "  SELECT MAX(f2.fiscalPeriod) FROM FundamentalsTimescaleEntity f2 " +
           "  WHERE f2.symbol = f.symbol" +
           ")")
    List<FundamentalsTimescaleEntity> findLatestBySymbols(@Param("symbols") List<String> symbols);

    /**
     * Stock screener: Find symbols with P/E ratio in a specific range.
     * Only considers latest fundamentals (TTM or most recent period).
     *
     * @param minPE minimum P/E ratio
     * @param maxPE maximum P/E ratio
     * @return list of symbols matching the criteria
     */
    @Query("SELECT DISTINCT f.symbol FROM FundamentalsTimescaleEntity f " +
           "WHERE f.priceToEarnings >= :minPE " +
           "AND f.priceToEarnings <= :maxPE " +
           "AND f.fiscalPeriod = (" +
           "  SELECT MAX(f2.fiscalPeriod) FROM FundamentalsTimescaleEntity f2 " +
           "  WHERE f2.symbol = f.symbol" +
           ")")
    List<String> findSymbolsByPERatioRange(
        @Param("minPE") BigDecimal minPE,
        @Param("maxPE") BigDecimal maxPE
    );

    /**
     * Stock screener: Find symbols with dividend yield above a threshold.
     *
     * @param minYield minimum dividend yield (percentage)
     * @return list of symbols with dividend yield >= minYield
     */
    @Query("SELECT DISTINCT f.symbol FROM FundamentalsTimescaleEntity f " +
           "WHERE f.dividendYield >= :minYield " +
           "AND f.fiscalPeriod = (" +
           "  SELECT MAX(f2.fiscalPeriod) FROM FundamentalsTimescaleEntity f2 " +
           "  WHERE f2.symbol = f.symbol" +
           ") " +
           "ORDER BY f.dividendYield DESC")
    List<String> findSymbolsByDividendYield(@Param("minYield") BigDecimal minYield);

    /**
     * Stock screener: Find symbols with ROE (Return on Equity) above threshold.
     *
     * @param minRoe minimum ROE (percentage)
     * @return list of symbols with ROE >= minRoe
     */
    @Query("SELECT DISTINCT f.symbol FROM FundamentalsTimescaleEntity f " +
           "WHERE f.returnOnEquity >= :minRoe " +
           "AND f.fiscalPeriod = (" +
           "  SELECT MAX(f2.fiscalPeriod) FROM FundamentalsTimescaleEntity f2 " +
           "  WHERE f2.symbol = f.symbol" +
           ") " +
           "ORDER BY f.returnOnEquity DESC")
    List<String> findSymbolsByROE(@Param("minRoe") BigDecimal minRoe);

    /**
     * Get all distinct symbols with fundamentals data.
     *
     * @return list of all symbols in the table
     */
    @Query("SELECT DISTINCT f.symbol FROM FundamentalsTimescaleEntity f ORDER BY f.symbol")
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
    boolean existsBySymbolAndFiscalPeriodAndPeriodType(
        String symbol,
        LocalDate fiscalPeriod,
        String periodType
    );

    /**
     * Delete all fundamentals records for a symbol.
     * Useful for cleanup or re-importing data.
     *
     * @param symbol the stock symbol
     */
    void deleteBySymbol(String symbol);
}
