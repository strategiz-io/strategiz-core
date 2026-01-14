package io.strategiz.business.fundamentals.converter;

import io.strategiz.client.fmp.dto.FmpFundamentals;
import io.strategiz.data.fundamentals.constants.PeriodType;
import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Converter for transforming Financial Modeling Prep DTOs to FundamentalsEntity.
 *
 * <p>
 * This converter performs the following transformations:
 * - Maps data from FmpFundamentals (single flat DTO)
 * - Handles null values and missing data gracefully
 * - Determines period type from FMP's period field (Q1, Q2, Q3, Q4, FY)
 * - Parses fiscal period date from FMP date field
 * </p>
 */
@Component
@ConditionalOnProperty(name = "strategiz.fmp.enabled", havingValue = "true", matchIfMissing = false)
public class FmpFundamentalsConverter {

	private static final Logger log = LoggerFactory.getLogger(FmpFundamentalsConverter.class);

	/**
	 * Convert FmpFundamentals DTO to FundamentalsEntity.
	 *
	 * @param fmpFundamentals FMP fundamentals data
	 * @return ClickHouse entity ready for persistence
	 */
	public FundamentalsEntity toEntity(FmpFundamentals fmpFundamentals) {
		if (fmpFundamentals == null || fmpFundamentals.getSymbol() == null) {
			throw new IllegalArgumentException("FmpFundamentals and symbol cannot be null");
		}

		String symbol = fmpFundamentals.getSymbol();
		LocalDate fiscalPeriod = parseFiscalPeriod(fmpFundamentals.getDate());
		String periodType = parsePeriodType(fmpFundamentals.getPeriod());

		FundamentalsEntity entity = new FundamentalsEntity(symbol, fiscalPeriod, periodType);
		entity.setCollectedAt(Instant.now());

		// Map all financial data from FMP DTO
		mapFmpData(entity, fmpFundamentals);

		log.debug("Converted FMP fundamentals for {} to entity: {}", symbol, entity);
		return entity;
	}

	/**
	 * Map all data from FmpFundamentals DTO to entity.
	 */
	private void mapFmpData(FundamentalsEntity entity, FmpFundamentals fmp) {
		// Income Statement
		entity.setRevenue(fmp.getRevenue());
		entity.setCostOfRevenue(fmp.getCostOfRevenue());
		entity.setGrossProfit(fmp.getGrossProfit());
		entity.setOperatingIncome(fmp.getOperatingIncome());
		entity.setNetIncome(fmp.getNetIncome());
		entity.setEpsDiluted(fmp.getEps());
		entity.setEbitda(fmp.getEbitda());

		// Balance Sheet
		entity.setTotalAssets(fmp.getTotalAssets());
		entity.setTotalLiabilities(fmp.getTotalLiabilities());
		entity.setShareholdersEquity(fmp.getTotalStockholdersEquity());
		entity.setCashAndEquivalents(fmp.getCash());
		entity.setTotalDebt(fmp.getTotalDebt());

		// Valuation Ratios
		entity.setPriceToEarnings(fmp.getPeRatio());
		entity.setPriceToBook(fmp.getPriceToBook());
		entity.setDebtToEquity(fmp.getDebtToEquity());

		// Profitability Ratios
		entity.setReturnOnEquity(fmp.getRoe());
		entity.setReturnOnAssets(fmp.getRoa());

		// Liquidity Ratios
		entity.setCurrentRatio(fmp.getCurrentRatio());
		entity.setQuickRatio(fmp.getQuickRatio());

		// Margins (already in percentage form from FMP)
		entity.setGrossMargin(fmp.getGrossMargin());
		entity.setOperatingMargin(fmp.getOperatingMargin());
		entity.setProfitMargin(fmp.getNetMargin());

		// Dividend Metrics
		entity.setDividendYield(fmp.getDividendYield());
		entity.setPayoutRatio(fmp.getPayoutRatio());
	}

	/**
	 * Parse FMP date string to LocalDate.
	 *
	 * FMP typically returns dates in format: "2024-12-31"
	 */
	private LocalDate parseFiscalPeriod(String dateString) {
		if (dateString == null || dateString.isBlank()) {
			log.warn("No date provided by FMP, using current date");
			return LocalDate.now();
		}

		try {
			return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
		}
		catch (Exception ex) {
			log.warn("Failed to parse FMP date '{}', using current date", dateString, ex);
			return LocalDate.now();
		}
	}

	/**
	 * Parse FMP period string to PeriodType constant.
	 *
	 * FMP periods: "Q1", "Q2", "Q3", "Q4", "FY", "TTM"
	 * Our periods: "QUARTERLY", "ANNUAL", "TTM"
	 */
	private String parsePeriodType(String period) {
		if (period == null || period.isBlank()) {
			log.warn("No period provided by FMP, using TTM");
			return PeriodType.TTM;
		}

		// Map FMP periods to our constants
		switch (period.toUpperCase()) {
			case "Q1":
			case "Q2":
			case "Q3":
			case "Q4":
				return PeriodType.QUARTERLY;
			case "FY":
				return PeriodType.ANNUAL;
			case "TTM":
				return PeriodType.TTM;
			default:
				log.warn("Unknown FMP period '{}', using TTM", period);
				return PeriodType.TTM;
		}
	}

}
