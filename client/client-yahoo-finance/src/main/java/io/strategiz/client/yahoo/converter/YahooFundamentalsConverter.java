package io.strategiz.client.yahoo.converter;

import io.strategiz.client.yahoo.model.*;
import io.strategiz.data.fundamentals.constants.PeriodType;
import io.strategiz.data.fundamentals.timescale.entity.FundamentalsTimescaleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Converter for transforming Yahoo Finance DTOs to FundamentalsTimescaleEntity.
 *
 * <p>
 * This converter performs the following transformations:
 * - Maps data from YahooFinancialData, YahooKeyStatistics, YahooBalanceSheet, YahooIncomeStatement
 * - Calculates derived metrics (margins, ratios) when not provided by Yahoo
 * - Handles null values and missing data gracefully
 * - Sets proper period type (TTM by default for current data)
 * - Sets fiscal period to current date (representing "latest available")
 * </p>
 */
@Component
public class YahooFundamentalsConverter {

	private static final Logger log = LoggerFactory.getLogger(YahooFundamentalsConverter.class);

	/**
	 * Convert YahooFundamentals DTO to FundamentalsTimescaleEntity.
	 *
	 * @param yahooFundamentals Yahoo Finance data
	 * @return TimescaleDB entity ready for persistence
	 */
	public FundamentalsTimescaleEntity toEntity(YahooFundamentals yahooFundamentals) {
		if (yahooFundamentals == null || yahooFundamentals.getSymbol() == null) {
			throw new IllegalArgumentException("YahooFundamentals and symbol cannot be null");
		}

		String symbol = yahooFundamentals.getSymbol();
		LocalDate fiscalPeriod = LocalDate.now(); // Current data = latest available
		String periodType = PeriodType.TTM; // Trailing Twelve Months (most common for current data)

		FundamentalsTimescaleEntity entity = new FundamentalsTimescaleEntity(symbol, fiscalPeriod, periodType);
		entity.setCollectedAt(Instant.now());

		// Extract sub-DTOs
		YahooFinancialData financial = yahooFundamentals.getFinancialData();
		YahooKeyStatistics stats = yahooFundamentals.getKeyStatistics();
		YahooBalanceSheet balanceSheet = yahooFundamentals.getBalanceSheet();
		YahooIncomeStatement incomeStatement = yahooFundamentals.getIncomeStatement();

		// Populate entity from each DTO
		mapFinancialData(entity, financial);
		mapKeyStatistics(entity, stats);
		mapBalanceSheet(entity, balanceSheet);
		mapIncomeStatement(entity, incomeStatement);

		// Calculate derived metrics
		calculateDerivedMetrics(entity);

		log.debug("Converted Yahoo fundamentals for {} to entity: {}", symbol, entity);
		return entity;
	}

	/**
	 * Map data from YahooFinancialData to entity.
	 */
	private void mapFinancialData(FundamentalsTimescaleEntity entity, YahooFinancialData financial) {
		if (financial == null) {
			return;
		}

		// Financial metrics
		entity.setEbitda(financial.getEbitda());
		entity.setRevenue(financial.getTotalRevenue());
		entity.setGrossProfit(financial.getGrossProfits());
		entity.setTotalDebt(financial.getTotalDebt());
		entity.setCashAndEquivalents(financial.getTotalCash());

		// Ratios
		entity.setQuickRatio(financial.getQuickRatio());
		entity.setCurrentRatio(financial.getCurrentRatio());
		entity.setDebtToEquity(financial.getDebtToEquity());
		entity.setReturnOnAssets(financial.getReturnOnAssets());
		entity.setReturnOnEquity(financial.getReturnOnEquity());

		// Margins (as fractions, multiply by 100 for percentage)
		entity.setGrossMargin(multiplyBy100(financial.getGrossMargins()));
		entity.setOperatingMargin(multiplyBy100(financial.getOperatingMargins()));
		entity.setProfitMargin(multiplyBy100(financial.getProfitMargins()));

		// Growth metrics (as fractions, multiply by 100 for percentage)
		entity.setRevenueGrowthYoy(multiplyBy100(financial.getRevenueGrowth()));
		entity.setEpsGrowthYoy(multiplyBy100(financial.getEarningsGrowth()));
	}

	/**
	 * Map data from YahooKeyStatistics to entity.
	 */
	private void mapKeyStatistics(FundamentalsTimescaleEntity entity, YahooKeyStatistics stats) {
		if (stats == null) {
			return;
		}

		// Valuation ratios
		entity.setPriceToEarnings(stats.getTrailingPE());
		entity.setPriceToBook(stats.getPriceToBook());
		entity.setPriceToSales(stats.getPriceToSales());
		entity.setPegRatio(stats.getPegRatio());
		entity.setEnterpriseValue(stats.getEnterpriseValue());
		entity.setEvToEbitda(stats.getEnterpriseToEbitda());

		// Share information
		if (stats.getSharesOutstanding() != null) {
			entity.setSharesOutstanding(new BigDecimal(stats.getSharesOutstanding()));
		}
		entity.setMarketCap(stats.getMarketCap());
		entity.setBookValuePerShare(stats.getBookValue());

		// EPS
		entity.setEpsDiluted(stats.getTrailingEps());

		// Dividends
		entity.setDividendPerShare(stats.getDividendRate());
		entity.setDividendYield(multiplyBy100(stats.getDividendYield())); // Convert to percentage
		entity.setPayoutRatio(multiplyBy100(stats.getPayoutRatio())); // Convert to percentage
	}

	/**
	 * Map data from YahooBalanceSheet to entity.
	 */
	private void mapBalanceSheet(FundamentalsTimescaleEntity entity, YahooBalanceSheet balanceSheet) {
		if (balanceSheet == null) {
			return;
		}

		// Assets
		entity.setTotalAssets(balanceSheet.getTotalAssets());
		entity.setCurrentAssets(balanceSheet.getTotalCurrentAssets());

		// Liabilities
		entity.setTotalLiabilities(balanceSheet.getTotalLiabilities());
		entity.setCurrentLiabilities(balanceSheet.getTotalCurrentLiabilities());

		// Equity
		entity.setShareholdersEquity(balanceSheet.getTotalStockholderEquity());

		// Debt
		if (balanceSheet.getLongTermDebt() != null) {
			BigDecimal longTermDebt = balanceSheet.getLongTermDebt();
			BigDecimal shortTermDebt = balanceSheet.getShortLongTermDebt();
			if (shortTermDebt != null) {
				entity.setTotalDebt(longTermDebt.add(shortTermDebt));
			}
			else {
				entity.setTotalDebt(longTermDebt);
			}
		}

		// Cash
		BigDecimal cash = balanceSheet.getCashAndCashEquivalents();
		if (cash == null) {
			cash = balanceSheet.getCash();
		}
		entity.setCashAndEquivalents(cash);
	}

	/**
	 * Map data from YahooIncomeStatement to entity.
	 */
	private void mapIncomeStatement(FundamentalsTimescaleEntity entity, YahooIncomeStatement income) {
		if (income == null) {
			return;
		}

		// Income statement items
		entity.setRevenue(income.getTotalRevenue());
		entity.setCostOfRevenue(income.getCostOfRevenue());
		entity.setGrossProfit(income.getGrossProfit());
		entity.setOperatingIncome(income.getOperatingIncome());
		entity.setNetIncome(income.getNetIncome());
	}

	/**
	 * Calculate derived metrics from existing data.
	 *
	 * Calculates ratios and margins that may not be provided directly by Yahoo Finance.
	 */
	private void calculateDerivedMetrics(FundamentalsTimescaleEntity entity) {

		// Calculate current ratio if not set
		if (entity.getCurrentRatio() == null && entity.getCurrentAssets() != null
				&& entity.getCurrentLiabilities() != null && !isZero(entity.getCurrentLiabilities())) {

			BigDecimal currentRatio = entity.getCurrentAssets().divide(entity.getCurrentLiabilities(), 4,
					RoundingMode.HALF_UP);
			entity.setCurrentRatio(currentRatio);
		}

		// Calculate debt to assets ratio if not set
		if (entity.getDebtToAssets() == null && entity.getTotalDebt() != null && entity.getTotalAssets() != null
				&& !isZero(entity.getTotalAssets())) {

			BigDecimal debtToAssets = entity.getTotalDebt().divide(entity.getTotalAssets(), 4, RoundingMode.HALF_UP);
			entity.setDebtToAssets(debtToAssets);
		}

		// Calculate gross margin if not set
		if (entity.getGrossMargin() == null && entity.getGrossProfit() != null && entity.getRevenue() != null
				&& !isZero(entity.getRevenue())) {

			BigDecimal grossMargin = entity.getGrossProfit()
				.divide(entity.getRevenue(), 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal("100"));
			entity.setGrossMargin(grossMargin);
		}

		// Calculate operating margin if not set
		if (entity.getOperatingMargin() == null && entity.getOperatingIncome() != null
				&& entity.getRevenue() != null && !isZero(entity.getRevenue())) {

			BigDecimal operatingMargin = entity.getOperatingIncome()
				.divide(entity.getRevenue(), 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal("100"));
			entity.setOperatingMargin(operatingMargin);
		}

		// Calculate profit margin if not set
		if (entity.getProfitMargin() == null && entity.getNetIncome() != null && entity.getRevenue() != null
				&& !isZero(entity.getRevenue())) {

			BigDecimal profitMargin = entity.getNetIncome()
				.divide(entity.getRevenue(), 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal("100"));
			entity.setProfitMargin(profitMargin);
		}

		// Calculate book value per share if not set
		if (entity.getBookValuePerShare() == null && entity.getShareholdersEquity() != null
				&& entity.getSharesOutstanding() != null && !isZero(entity.getSharesOutstanding())) {

			BigDecimal bookValuePerShare = entity.getShareholdersEquity().divide(entity.getSharesOutstanding(), 4,
					RoundingMode.HALF_UP);
			entity.setBookValuePerShare(bookValuePerShare);
		}
	}

	/**
	 * Helper: Multiply BigDecimal by 100 (convert fraction to percentage).
	 */
	private BigDecimal multiplyBy100(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.multiply(new BigDecimal("100"));
	}

	/**
	 * Helper: Check if BigDecimal is zero or null.
	 */
	private boolean isZero(BigDecimal value) {
		return value == null || value.compareTo(BigDecimal.ZERO) == 0;
	}

}
