package io.strategiz.data.marketdata.clickhouse.repository;

import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data repository for company fundamentals. Stores company financial metrics (P/E, EPS,
 * revenue, etc.) for stock screening and analysis.
 *
 * Requires ClickHouse to be enabled.
 */
@Repository
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class FundamentalsDataRepository {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsDataRepository.class);

	private final JdbcTemplate jdbcTemplate;

	private final FundamentalsRowMapper rowMapper = new FundamentalsRowMapper();

	public FundamentalsDataRepository(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Save a fundamentals record.
	 */
	public FundamentalsEntity save(FundamentalsEntity entity) {
		String sql = """
				INSERT INTO company_fundamentals (
				    id, symbol, fiscal_period, period_type,
				    revenue, cost_of_revenue, gross_profit, operating_income, ebitda, net_income,
				    eps_basic, eps_diluted, gross_margin, operating_margin, profit_margin,
				    return_on_equity, return_on_assets, price_to_earnings, price_to_book, price_to_sales,
				    peg_ratio, enterprise_value, ev_to_ebitda, total_assets, total_liabilities,
				    shareholders_equity, current_assets, current_liabilities, total_debt, cash_and_equivalents,
				    current_ratio, quick_ratio, debt_to_equity, debt_to_assets,
				    dividend_per_share, dividend_yield, payout_ratio, shares_outstanding,
				    market_cap, book_value_per_share, revenue_growth_yoy, eps_growth_yoy,
				    data_source, currency, collected_at, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
				          ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		Instant now = Instant.now();
		jdbcTemplate.update(sql, entity.getId(), entity.getSymbol(), java.sql.Date.valueOf(entity.getFiscalPeriod()),
				entity.getPeriodType(), entity.getRevenue(), entity.getCostOfRevenue(), entity.getGrossProfit(),
				entity.getOperatingIncome(), entity.getEbitda(), entity.getNetIncome(), entity.getEpsBasic(),
				entity.getEpsDiluted(), entity.getGrossMargin(), entity.getOperatingMargin(), entity.getProfitMargin(),
				entity.getReturnOnEquity(), entity.getReturnOnAssets(), entity.getPriceToEarnings(),
				entity.getPriceToBook(), entity.getPriceToSales(), entity.getPegRatio(), entity.getEnterpriseValue(),
				entity.getEvToEbitda(), entity.getTotalAssets(), entity.getTotalLiabilities(),
				entity.getShareholdersEquity(), entity.getCurrentAssets(), entity.getCurrentLiabilities(),
				entity.getTotalDebt(), entity.getCashAndEquivalents(), entity.getCurrentRatio(), entity.getQuickRatio(),
				entity.getDebtToEquity(), entity.getDebtToAssets(), entity.getDividendPerShare(),
				entity.getDividendYield(), entity.getPayoutRatio(), entity.getSharesOutstanding(),
				entity.getMarketCap(), entity.getBookValuePerShare(), entity.getRevenueGrowthYoy(),
				entity.getEpsGrowthYoy(), entity.getDataSource(), entity.getCurrency(),
				entity.getCollectedAt() != null ? Timestamp.from(entity.getCollectedAt()) : Timestamp.from(now),
				entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt()) : Timestamp.from(now),
				Timestamp.from(now));

		return entity;
	}

	/**
	 * Save multiple fundamentals records in a batch.
	 */
	public void saveAll(List<FundamentalsEntity> entities) {
		if (entities.isEmpty()) {
			return;
		}

		log.info("Batch saving {} fundamentals records", entities.size());

		for (FundamentalsEntity entity : entities) {
			save(entity);
		}

		log.info("Batch save completed: {} records", entities.size());
	}

	/**
	 * Find latest fundamentals for a symbol.
	 */
	public Optional<FundamentalsEntity> findLatestBySymbol(String symbol) {
		String sql = """
				SELECT * FROM company_fundamentals
				WHERE symbol = ?
				ORDER BY fiscal_period DESC
				LIMIT 1
				""";

		List<FundamentalsEntity> results = jdbcTemplate.query(sql, rowMapper, symbol);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find latest fundamentals for a symbol and specific period type.
	 */
	public Optional<FundamentalsEntity> findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol,
			String periodType) {
		String sql = """
				SELECT * FROM company_fundamentals
				WHERE symbol = ? AND period_type = ?
				ORDER BY fiscal_period DESC
				LIMIT 1
				""";

		List<FundamentalsEntity> results = jdbcTemplate.query(sql, rowMapper, symbol, periodType);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
	}

	/**
	 * Find all fundamentals for a symbol.
	 */
	public List<FundamentalsEntity> findBySymbolOrderByFiscalPeriodDesc(String symbol) {
		String sql = """
				SELECT * FROM company_fundamentals
				WHERE symbol = ?
				ORDER BY fiscal_period DESC
				""";

		return jdbcTemplate.query(sql, rowMapper, symbol);
	}

	/**
	 * Find fundamentals by symbol and period type.
	 */
	public List<FundamentalsEntity> findBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(String symbol, String periodType) {
		String sql = """
				SELECT * FROM company_fundamentals
				WHERE symbol = ? AND period_type = ?
				ORDER BY fiscal_period DESC
				""";

		return jdbcTemplate.query(sql, rowMapper, symbol, periodType);
	}

	/**
	 * Find fundamentals within a date range.
	 */
	public List<FundamentalsEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
		String sql = """
				SELECT * FROM company_fundamentals
				WHERE symbol = ?
				AND fiscal_period >= ?
				AND fiscal_period <= ?
				ORDER BY fiscal_period DESC
				""";

		return jdbcTemplate.query(sql, rowMapper, symbol, java.sql.Date.valueOf(startDate),
				java.sql.Date.valueOf(endDate));
	}

	/**
	 * Find latest fundamentals for multiple symbols.
	 */
	public List<FundamentalsEntity> findLatestBySymbols(List<String> symbols) {
		if (symbols.isEmpty()) {
			return List.of();
		}

		String placeholders = String.join(",", symbols.stream().map(s -> "?").toList());
		String sql = String.format("""
				SELECT * FROM company_fundamentals
				WHERE symbol IN (%s)
				AND (symbol, fiscal_period) IN (
				    SELECT symbol, MAX(fiscal_period)
				    FROM company_fundamentals
				    WHERE symbol IN (%s)
				    GROUP BY symbol
				)
				""", placeholders, placeholders);

		Object[] params = new Object[symbols.size() * 2];
		for (int i = 0; i < symbols.size(); i++) {
			params[i] = symbols.get(i);
			params[symbols.size() + i] = symbols.get(i);
		}

		return jdbcTemplate.query(sql, rowMapper, params);
	}

	/**
	 * Stock screener: Find symbols with P/E ratio in range.
	 */
	public List<String> findSymbolsByPERatioRange(BigDecimal minPE, BigDecimal maxPE) {
		String sql = """
				SELECT DISTINCT symbol FROM company_fundamentals
				WHERE price_to_earnings >= ?
				AND price_to_earnings <= ?
				AND (symbol, fiscal_period) IN (
				    SELECT symbol, MAX(fiscal_period)
				    FROM company_fundamentals
				    GROUP BY symbol
				)
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("symbol"), minPE, maxPE);
	}

	/**
	 * Stock screener: Find symbols with dividend yield above threshold.
	 */
	public List<String> findSymbolsByDividendYield(BigDecimal minYield) {
		String sql = """
				SELECT DISTINCT symbol FROM company_fundamentals
				WHERE dividend_yield >= ?
				AND (symbol, fiscal_period) IN (
				    SELECT symbol, MAX(fiscal_period)
				    FROM company_fundamentals
				    GROUP BY symbol
				)
				ORDER BY dividend_yield DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("symbol"), minYield);
	}

	/**
	 * Stock screener: Find symbols with ROE above threshold.
	 */
	public List<String> findSymbolsByROE(BigDecimal minRoe) {
		String sql = """
				SELECT DISTINCT symbol FROM company_fundamentals
				WHERE return_on_equity >= ?
				AND (symbol, fiscal_period) IN (
				    SELECT symbol, MAX(fiscal_period)
				    FROM company_fundamentals
				    GROUP BY symbol
				)
				ORDER BY return_on_equity DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("symbol"), minRoe);
	}

	/**
	 * Get all distinct symbols.
	 */
	public List<String> findDistinctSymbols() {
		String sql = "SELECT DISTINCT symbol FROM company_fundamentals ORDER BY symbol";
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("symbol"));
	}

	/**
	 * Count records for a symbol.
	 */
	public long countBySymbol(String symbol) {
		String sql = "SELECT COUNT(*) FROM company_fundamentals WHERE symbol = ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, symbol);
		return count != null ? count : 0;
	}

	/**
	 * Check if record exists.
	 */
	public boolean existsBySymbolAndFiscalPeriodAndPeriodType(String symbol, LocalDate fiscalPeriod,
			String periodType) {
		String sql = """
				SELECT COUNT(*) FROM company_fundamentals
				WHERE symbol = ? AND fiscal_period = ? AND period_type = ?
				""";

		Long count = jdbcTemplate.queryForObject(sql, Long.class, symbol, java.sql.Date.valueOf(fiscalPeriod),
				periodType);
		return count != null && count > 0;
	}

	/**
	 * Delete all records for a symbol.
	 */
	public void deleteBySymbol(String symbol) {
		String sql = "ALTER TABLE company_fundamentals DELETE WHERE symbol = ?";
		jdbcTemplate.update(sql, symbol);
	}

	/**
	 * Get total count of all fundamentals records.
	 */
	public long countAll() {
		String sql = "SELECT COUNT(*) FROM company_fundamentals";
		Long count = jdbcTemplate.queryForObject(sql, Long.class);
		return count != null ? count : 0;
	}

	/**
	 * Get count of distinct symbols.
	 */
	public int countDistinctSymbols() {
		String sql = "SELECT COUNT(DISTINCT symbol) FROM company_fundamentals";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
		return count != null ? count : 0;
	}

	/**
	 * Get date range of fiscal periods (earliest and latest). Returns a map with
	 * "earliest" and "latest" keys.
	 */
	public java.util.Map<String, LocalDate> getDateRange() {
		String sql = "SELECT MIN(fiscal_period) as earliest, MAX(fiscal_period) as latest FROM company_fundamentals";

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
			java.util.Map<String, LocalDate> range = new java.util.HashMap<>();
			java.sql.Date earliest = rs.getDate("earliest");
			java.sql.Date latest = rs.getDate("latest");
			if (earliest != null) {
				range.put("earliest", earliest.toLocalDate());
			}
			if (latest != null) {
				range.put("latest", latest.toLocalDate());
			}
			return range;
		});
	}

	/**
	 * Get count of records grouped by period type. Returns a map with period_type as key
	 * and count as value.
	 */
	public java.util.Map<String, Long> countByPeriodType() {
		String sql = "SELECT period_type, COUNT(*) as cnt FROM company_fundamentals GROUP BY period_type";

		java.util.Map<String, Long> result = new java.util.HashMap<>();
		jdbcTemplate.query(sql, (rs) -> {
			result.put(rs.getString("period_type"), rs.getLong("cnt"));
		});
		return result;
	}

	/**
	 * Row mapper for FundamentalsEntity.
	 */
	private static class FundamentalsRowMapper implements RowMapper<FundamentalsEntity> {

		@Override
		public FundamentalsEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			FundamentalsEntity entity = new FundamentalsEntity();
			entity.setId(rs.getString("id"));
			entity.setSymbol(rs.getString("symbol"));
			entity.setFiscalPeriod(rs.getDate("fiscal_period").toLocalDate());
			entity.setPeriodType(rs.getString("period_type"));

			// Income statement
			entity.setRevenue(rs.getBigDecimal("revenue"));
			entity.setCostOfRevenue(rs.getBigDecimal("cost_of_revenue"));
			entity.setGrossProfit(rs.getBigDecimal("gross_profit"));
			entity.setOperatingIncome(rs.getBigDecimal("operating_income"));
			entity.setEbitda(rs.getBigDecimal("ebitda"));
			entity.setNetIncome(rs.getBigDecimal("net_income"));
			entity.setEpsBasic(rs.getBigDecimal("eps_basic"));
			entity.setEpsDiluted(rs.getBigDecimal("eps_diluted"));

			// Margins
			entity.setGrossMargin(rs.getBigDecimal("gross_margin"));
			entity.setOperatingMargin(rs.getBigDecimal("operating_margin"));
			entity.setProfitMargin(rs.getBigDecimal("profit_margin"));
			entity.setReturnOnEquity(rs.getBigDecimal("return_on_equity"));
			entity.setReturnOnAssets(rs.getBigDecimal("return_on_assets"));

			// Valuation
			entity.setPriceToEarnings(rs.getBigDecimal("price_to_earnings"));
			entity.setPriceToBook(rs.getBigDecimal("price_to_book"));
			entity.setPriceToSales(rs.getBigDecimal("price_to_sales"));
			entity.setPegRatio(rs.getBigDecimal("peg_ratio"));
			entity.setEnterpriseValue(rs.getBigDecimal("enterprise_value"));
			entity.setEvToEbitda(rs.getBigDecimal("ev_to_ebitda"));

			// Balance sheet
			entity.setTotalAssets(rs.getBigDecimal("total_assets"));
			entity.setTotalLiabilities(rs.getBigDecimal("total_liabilities"));
			entity.setShareholdersEquity(rs.getBigDecimal("shareholders_equity"));
			entity.setCurrentAssets(rs.getBigDecimal("current_assets"));
			entity.setCurrentLiabilities(rs.getBigDecimal("current_liabilities"));
			entity.setTotalDebt(rs.getBigDecimal("total_debt"));
			entity.setCashAndEquivalents(rs.getBigDecimal("cash_and_equivalents"));

			// Liquidity ratios
			entity.setCurrentRatio(rs.getBigDecimal("current_ratio"));
			entity.setQuickRatio(rs.getBigDecimal("quick_ratio"));
			entity.setDebtToEquity(rs.getBigDecimal("debt_to_equity"));
			entity.setDebtToAssets(rs.getBigDecimal("debt_to_assets"));

			// Dividends
			entity.setDividendPerShare(rs.getBigDecimal("dividend_per_share"));
			entity.setDividendYield(rs.getBigDecimal("dividend_yield"));
			entity.setPayoutRatio(rs.getBigDecimal("payout_ratio"));

			// Shares
			entity.setSharesOutstanding(rs.getBigDecimal("shares_outstanding"));
			entity.setMarketCap(rs.getBigDecimal("market_cap"));
			entity.setBookValuePerShare(rs.getBigDecimal("book_value_per_share"));

			// Growth
			entity.setRevenueGrowthYoy(rs.getBigDecimal("revenue_growth_yoy"));
			entity.setEpsGrowthYoy(rs.getBigDecimal("eps_growth_yoy"));

			// Metadata
			entity.setDataSource(rs.getString("data_source"));
			entity.setCurrency(rs.getString("currency"));

			Timestamp collectedAt = rs.getTimestamp("collected_at");
			if (collectedAt != null) {
				entity.setCollectedAt(collectedAt.toInstant());
			}

			Timestamp createdAt = rs.getTimestamp("created_at");
			if (createdAt != null) {
				entity.setCreatedAt(createdAt.toInstant());
			}

			Timestamp updatedAt = rs.getTimestamp("updated_at");
			if (updatedAt != null) {
				entity.setUpdatedAt(updatedAt.toInstant());
			}

			return entity;
		}

	}

}
