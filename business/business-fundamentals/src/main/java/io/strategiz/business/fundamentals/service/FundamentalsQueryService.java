package io.strategiz.business.fundamentals.service;

import io.strategiz.business.fundamentals.exception.FundamentalsErrorDetails;
import io.strategiz.data.fundamentals.constants.PeriodType;
import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import io.strategiz.data.fundamentals.repository.FundamentalsRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for querying company fundamentals data.
 *
 * <p>
 * Provides business-friendly methods for:
 * - Retrieving latest fundamentals for a symbol
 * - Stock screening by financial metrics
 * - Python strategy integration (map format)
 * </p>
 *
 * Requires ClickHouse to be enabled.
 */
@Service
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class FundamentalsQueryService {

	private static final Logger log = LoggerFactory.getLogger(FundamentalsQueryService.class);

	private final FundamentalsRepository repository;

	public FundamentalsQueryService(FundamentalsRepository repository) {
		this.repository = repository;
	}

	/**
	 * Get latest fundamentals for a symbol.
	 *
	 * @param symbol Stock symbol
	 * @return FundamentalsEntity
	 * @throws StrategizException if not found
	 */
	public FundamentalsEntity getLatestFundamentals(String symbol) {
		log.debug("Fetching latest fundamentals for {}", symbol);

		return repository.findLatestBySymbol(symbol)
			.orElseThrow(() -> new StrategizException(FundamentalsErrorDetails.NO_FUNDAMENTALS_FOUND,
					String.format("No fundamentals found for symbol: %s", symbol)));
	}

	/**
	 * Get latest fundamentals for a symbol (returns null if not found).
	 *
	 * @param symbol Stock symbol
	 * @return FundamentalsEntity or null
	 */
	public FundamentalsEntity getLatestFundamentalsOrNull(String symbol) {
		return repository.findLatestBySymbol(symbol).orElse(null);
	}

	/**
	 * Get fundamentals for a specific period type.
	 *
	 * @param symbol Stock symbol
	 * @param periodType Period type (QUARTERLY, ANNUAL, TTM)
	 * @return FundamentalsEntity
	 * @throws StrategizException if not found or invalid period type
	 */
	public FundamentalsEntity getFundamentalsByPeriodType(String symbol, String periodType) {
		if (!PeriodType.isValid(periodType)) {
			throw new StrategizException(FundamentalsErrorDetails.INVALID_PERIOD_TYPE,
					String.format("Invalid period type: %s. Must be one of: %s", periodType,
							PeriodType.VALID_PERIOD_TYPES));
		}

		log.debug("Fetching {} fundamentals for {}", periodType, symbol);

		return repository.findFirstBySymbolAndPeriodTypeOrderByFiscalPeriodDesc(symbol, periodType)
			.orElseThrow(() -> new StrategizException(FundamentalsErrorDetails.NO_FUNDAMENTALS_FOUND, String
				.format("No %s fundamentals found for symbol: %s", periodType, symbol)));
	}

	/**
	 * Get fundamentals formatted for Python strategy execution.
	 *
	 * Returns a map with all fundamental metrics that can be injected into Python
	 * global namespace.
	 *
	 * @param symbol Stock symbol
	 * @return Map of fundamental metrics (snake_case keys)
	 */
	public Map<String, Object> getFundamentalsForStrategy(String symbol) {
		FundamentalsEntity entity = getLatestFundamentalsOrNull(symbol);

		if (entity == null) {
			log.debug("No fundamentals found for {}, returning empty map", symbol);
			return Collections.emptyMap();
		}

		Map<String, Object> fundamentals = new HashMap<>();

		// Identity
		fundamentals.put("symbol", entity.getSymbol());
		fundamentals.put("period_type", entity.getPeriodType());
		fundamentals.put("fiscal_period", entity.getFiscalPeriod().toString());

		// Income Statement
		putIfNotNull(fundamentals, "revenue", entity.getRevenue());
		putIfNotNull(fundamentals, "cost_of_revenue", entity.getCostOfRevenue());
		putIfNotNull(fundamentals, "gross_profit", entity.getGrossProfit());
		putIfNotNull(fundamentals, "operating_income", entity.getOperatingIncome());
		putIfNotNull(fundamentals, "ebitda", entity.getEbitda());
		putIfNotNull(fundamentals, "net_income", entity.getNetIncome());
		putIfNotNull(fundamentals, "eps_basic", entity.getEpsBasic());
		putIfNotNull(fundamentals, "eps_diluted", entity.getEpsDiluted());

		// Margins & Profitability
		putIfNotNull(fundamentals, "gross_margin", entity.getGrossMargin());
		putIfNotNull(fundamentals, "operating_margin", entity.getOperatingMargin());
		putIfNotNull(fundamentals, "profit_margin", entity.getProfitMargin());
		putIfNotNull(fundamentals, "return_on_equity", entity.getReturnOnEquity());
		putIfNotNull(fundamentals, "return_on_assets", entity.getReturnOnAssets());

		// Valuation Ratios
		putIfNotNull(fundamentals, "pe_ratio", entity.getPriceToEarnings());
		putIfNotNull(fundamentals, "pb_ratio", entity.getPriceToBook());
		putIfNotNull(fundamentals, "ps_ratio", entity.getPriceToSales());
		putIfNotNull(fundamentals, "peg_ratio", entity.getPegRatio());
		putIfNotNull(fundamentals, "enterprise_value", entity.getEnterpriseValue());
		putIfNotNull(fundamentals, "ev_to_ebitda", entity.getEvToEbitda());

		// Balance Sheet
		putIfNotNull(fundamentals, "total_assets", entity.getTotalAssets());
		putIfNotNull(fundamentals, "total_liabilities", entity.getTotalLiabilities());
		putIfNotNull(fundamentals, "shareholders_equity", entity.getShareholdersEquity());
		putIfNotNull(fundamentals, "current_assets", entity.getCurrentAssets());
		putIfNotNull(fundamentals, "current_liabilities", entity.getCurrentLiabilities());
		putIfNotNull(fundamentals, "total_debt", entity.getTotalDebt());
		putIfNotNull(fundamentals, "cash_and_equivalents", entity.getCashAndEquivalents());

		// Liquidity & Leverage
		putIfNotNull(fundamentals, "current_ratio", entity.getCurrentRatio());
		putIfNotNull(fundamentals, "quick_ratio", entity.getQuickRatio());
		putIfNotNull(fundamentals, "debt_to_equity", entity.getDebtToEquity());
		putIfNotNull(fundamentals, "debt_to_assets", entity.getDebtToAssets());

		// Dividends
		putIfNotNull(fundamentals, "dividend_per_share", entity.getDividendPerShare());
		putIfNotNull(fundamentals, "dividend_yield", entity.getDividendYield());
		putIfNotNull(fundamentals, "payout_ratio", entity.getPayoutRatio());

		// Share Info
		putIfNotNull(fundamentals, "shares_outstanding", entity.getSharesOutstanding());
		putIfNotNull(fundamentals, "market_cap", entity.getMarketCap());
		putIfNotNull(fundamentals, "book_value_per_share", entity.getBookValuePerShare());

		// Growth Metrics
		putIfNotNull(fundamentals, "revenue_growth_yoy", entity.getRevenueGrowthYoy());
		putIfNotNull(fundamentals, "eps_growth_yoy", entity.getEpsGrowthYoy());

		log.debug("Prepared {} fundamental metrics for strategy execution ({})", fundamentals.size(), symbol);
		return fundamentals;
	}

	/**
	 * Find symbols with P/E ratio in specified range.
	 *
	 * @param minPE Minimum P/E ratio
	 * @param maxPE Maximum P/E ratio
	 * @return List of symbols matching criteria
	 */
	public List<String> findSymbolsByPERatio(BigDecimal minPE, BigDecimal maxPE) {
		if (minPE == null || maxPE == null || minPE.compareTo(maxPE) > 0) {
			throw new StrategizException(FundamentalsErrorDetails.INVALID_QUERY_PARAMETERS,
					"Invalid P/E ratio range");
		}

		log.debug("Finding symbols with P/E ratio between {} and {}", minPE, maxPE);
		return repository.findSymbolsByPERatioRange(minPE, maxPE);
	}

	/**
	 * Find symbols with dividend yield above threshold.
	 *
	 * @param minYield Minimum dividend yield (as percentage)
	 * @return List of symbols matching criteria
	 */
	public List<String> findSymbolsByDividendYield(BigDecimal minYield) {
		if (minYield == null || minYield.compareTo(BigDecimal.ZERO) < 0) {
			throw new StrategizException(FundamentalsErrorDetails.INVALID_QUERY_PARAMETERS,
					"Invalid dividend yield");
		}

		log.debug("Finding symbols with dividend yield >= {}%", minYield);
		return repository.findSymbolsByDividendYield(minYield);
	}

	/**
	 * Helper: Put value in map only if not null.
	 */
	private void putIfNotNull(Map<String, Object> map, String key, Object value) {
		if (value != null) {
			map.put(key, value);
		}
	}

}
