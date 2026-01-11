package io.strategiz.business.historicalinsights.service;

import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
import io.strategiz.business.historicalinsights.exception.InsufficientDataException;
import io.strategiz.business.historicalinsights.model.*;
import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for analyzing historical market data to generate insights for Alpha Mode.
 * Computes volatility profiles, ranks indicators by effectiveness, finds optimal parameters,
 * and detects market characteristics using 7 years of historical OHLCV data.
 */
@Service
public class HistoricalInsightsService {

	private static final Logger log = LoggerFactory.getLogger(HistoricalInsightsService.class);

	private static final int MIN_BARS_REQUIRED = 100;

	private static final int MAX_BARS = 2600; // ~7 years of daily data

	private final MarketDataClickHouseRepository marketDataRepo;

	private final FundamentalsQueryService fundamentalsService;

	public HistoricalInsightsService(MarketDataClickHouseRepository marketDataRepo,
			FundamentalsQueryService fundamentalsService) {
		this.marketDataRepo = marketDataRepo;
		this.fundamentalsService = fundamentalsService;
	}

	/**
	 * Main entry point: Analyze a symbol and return comprehensive historical insights.
	 * @param symbol Symbol to analyze
	 * @param timeframe Timeframe (e.g., "1D", "1H")
	 * @param lookbackDays Number of days to look back (default 2600 for ~7 years)
	 * @param includeFundamentals Whether to include fundamental analysis
	 * @return SymbolInsights object with all computed insights
	 */
	public SymbolInsights analyzeSymbolForStrategyGeneration(String symbol, String timeframe, int lookbackDays,
			boolean includeFundamentals) {

		log.info("Starting Alpha Mode analysis for symbol={}, timeframe={}, lookback={} days, fundamentals={}",
				symbol, timeframe, lookbackDays, includeFundamentals);

		// 1. Query historical data
		Instant endTime = Instant.now();
		Instant startTime = endTime.minus(lookbackDays, ChronoUnit.DAYS);

		List<MarketDataEntity> data = marketDataRepo.findBySymbolAndTimeRange(symbol, startTime, endTime, timeframe);

		log.info("Retrieved {} bars for symbol={}", data.size(), symbol);

		// Check minimum data requirement
		if (data.size() < MIN_BARS_REQUIRED) {
			throw new InsufficientDataException(
					String.format("Insufficient data for %s: found %d bars, need at least %d", symbol, data.size(),
							MIN_BARS_REQUIRED));
		}

		// Downsample if too much data
		if (data.size() > MAX_BARS) {
			log.info("Downsampling {} bars to {}", data.size(), MAX_BARS);
			data = downsampleData(data, MAX_BARS);
		}

		// 2. Build insights object
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol(symbol);
		insights.setTimeframe(timeframe);
		insights.setDaysAnalyzed(lookbackDays);

		// 3. Compute volatility profile
		VolatilityProfile volatilityProfile = calculateVolatilityProfile(data);
		insights.setAvgVolatility(volatilityProfile.getAvgATR());
		insights.setVolatilityRegime(volatilityProfile.getRegime());
		insights.setAvgDailyRange(volatilityProfile.getAvgDailyRangePercent());

		// 4. Rank indicators by effectiveness
		List<IndicatorRanking> rankings = analyzeIndicatorEffectiveness(data);
		insights.setTopIndicators(rankings);

		// 5. Find optimal parameters
		Map<String, Object> optimalParams = findOptimalParameters(data, rankings);
		insights.setOptimalParameters(optimalParams);

		// 6. Detect market characteristics
		MarketCharacteristics characteristics = detectMarketCharacteristics(data);
		insights.setTrendDirection(characteristics.getTrendDirection());
		insights.setTrendStrength(characteristics.getTrendStrength());
		insights.setMeanReverting(characteristics.isMeanReverting());

		// 7. Calculate risk metrics
		RiskMetrics riskMetrics = calculateRiskMetrics(data, rankings);
		insights.setAvgMaxDrawdown(riskMetrics.avgMaxDrawdown);
		insights.setAvgWinRate(riskMetrics.avgWinRate);
		insights.setRecommendedRiskLevel(riskMetrics.recommendedLevel);

		// 8. Optional: Include fundamentals
		if (includeFundamentals) {
			FundamentalsInsights fundInsights = getFundamentalsInsights(symbol);
			insights.setFundamentals(fundInsights);
		}

		log.info("Alpha Mode analysis completed for {}. Top indicator: {}, Volatility: {}, Win Rate: {}%", symbol,
				rankings.isEmpty() ? "N/A" : rankings.get(0).getIndicatorName(), volatilityProfile.getRegime(),
				String.format("%.1f", riskMetrics.avgWinRate));

		return insights;
	}

	/**
	 * Fallback method when insufficient data: analyze with whatever data is available.
	 */
	public SymbolInsights analyzeWithPartialData(String symbol, String timeframe) {
		log.warn("Attempting partial analysis for symbol={} with limited data", symbol);

		// Query last 365 days
		Instant endTime = Instant.now();
		Instant startTime = endTime.minus(365, ChronoUnit.DAYS);

		List<MarketDataEntity> data = marketDataRepo.findBySymbolAndTimeRange(symbol, startTime, endTime, timeframe);

		if (data.isEmpty()) {
			throw new InsufficientDataException("No historical data available for " + symbol);
		}

		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol(symbol);
		insights.setTimeframe(timeframe);
		insights.setDaysAnalyzed(data.size());

		// Only compute basic metrics
		VolatilityProfile volatilityProfile = calculateVolatilityProfile(data);
		insights.setAvgVolatility(volatilityProfile.getAvgATR());
		insights.setVolatilityRegime(volatilityProfile.getRegime());
		insights.setAvgDailyRange(volatilityProfile.getAvgDailyRangePercent());

		// Set default/placeholder values for other fields
		insights.setTopIndicators(new ArrayList<>());
		insights.setOptimalParameters(new HashMap<>());
		insights.setTrendDirection("UNKNOWN");
		insights.setTrendStrength(0.0);
		insights.setMeanReverting(false);
		insights.setAvgMaxDrawdown(0.0);
		insights.setAvgWinRate(50.0); // Neutral
		insights.setRecommendedRiskLevel("MEDIUM");

		return insights;
	}

	// ========== VOLATILITY CALCULATION ==========

	/**
	 * Calculate volatility profile using 14-day ATR and classify regime.
	 */
	private VolatilityProfile calculateVolatilityProfile(List<MarketDataEntity> data) {
		if (data.size() < 15) {
			return new VolatilityProfile(0.0, "UNKNOWN", 0.0);
		}

		double sumATR = 0.0;
		double sumDailyRange = 0.0;
		int atrPeriod = 14;
		int count = 0;

		// Calculate ATR for each bar after the first 14
		for (int i = atrPeriod; i < data.size(); i++) {
			MarketDataEntity current = data.get(i);
			MarketDataEntity previous = data.get(i - 1);

			double high = current.getHigh().doubleValue();
			double low = current.getLow().doubleValue();
			double prevClose = previous.getClose().doubleValue();

			// True Range = max(high - low, |high - prevClose|, |low - prevClose|)
			double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));

			sumATR += tr;
			count++;

			// Daily range percentage
			double close = current.getClose().doubleValue();
			if (close > 0) {
				double dailyRangePercent = ((high - low) / close) * 100.0;
				sumDailyRange += dailyRangePercent;
			}
		}

		double avgATR = count > 0 ? sumATR / count : 0.0;
		double avgDailyRangePercent = count > 0 ? sumDailyRange / count : 0.0;

		// Classify regime based on ATR as % of average price
		double avgClose = data.stream().mapToDouble(d -> d.getClose().doubleValue()).average().orElse(1.0);
		double atrPercent = (avgATR / avgClose) * 100.0;

		String regime;
		if (atrPercent < 1.5) {
			regime = "LOW";
		}
		else if (atrPercent < 3.0) {
			regime = "MEDIUM";
		}
		else if (atrPercent < 5.0) {
			regime = "HIGH";
		}
		else {
			regime = "EXTREME";
		}

		log.debug("Volatility profile: avgATR={}, atrPercent={}%, regime={}", avgATR, atrPercent, regime);

		return new VolatilityProfile(avgATR, regime, avgDailyRangePercent);
	}

	// ========== INDICATOR RANKING (SIMPLIFIED VERSION) ==========

	/**
	 * Rank indicators by historical effectiveness. Simplified implementation: Returns
	 * hardcoded rankings based on general effectiveness. Full implementation would run actual
	 * backtests.
	 */
	private List<IndicatorRanking> analyzeIndicatorEffectiveness(List<MarketDataEntity> data) {
		List<IndicatorRanking> rankings = new ArrayList<>();

		// Simplified: Return general rankings
		// TODO: Implement actual backtesting logic
		rankings.add(new IndicatorRanking("RSI", 0.65, Map.of("period", 14, "oversold", 30, "overbought", 70),
				"Effective for mean-reversion strategies"));
		rankings.add(new IndicatorRanking("MACD", 0.60, Map.of("fast", 12, "slow", 26, "signal", 9),
				"Strong trend-following indicator"));
		rankings.add(new IndicatorRanking("Bollinger Bands", 0.58, Map.of("period", 20, "stddev", 2),
				"Good for volatility-based entries"));
		rankings.add(new IndicatorRanking("MA Crossover", 0.55, Map.of("fast", 20, "slow", 50),
				"Classic trend confirmation"));
		rankings.add(new IndicatorRanking("VWAP", 0.52, Map.of(), "Useful for intraday strategies"));

		log.debug("Indicator rankings computed (simplified): {} indicators ranked", rankings.size());

		return rankings;
	}

	// ========== OPTIMAL PARAMETERS (SIMPLIFIED) ==========

	/**
	 * Find optimal parameters for top indicators. Simplified: Returns defaults. Full
	 * implementation would use grid search.
	 */
	private Map<String, Object> findOptimalParameters(List<MarketDataEntity> data,
			List<IndicatorRanking> rankings) {
		Map<String, Object> params = new HashMap<>();

		// Extract from top ranking
		if (!rankings.isEmpty()) {
			IndicatorRanking top = rankings.get(0);
			params.putAll(top.getOptimalSettings());
		}

		// Add recommended risk management based on volatility
		params.put("stop_loss_percent", 3.0);
		params.put("take_profit_percent", 9.0);

		return params;
	}

	// ========== MARKET CHARACTERISTICS ==========

	/**
	 * Detect market characteristics: trend direction, strength, mean-reversion. Simplified:
	 * Uses basic linear regression.
	 */
	private MarketCharacteristics detectMarketCharacteristics(List<MarketDataEntity> data) {
		if (data.size() < 30) {
			return new MarketCharacteristics("SIDEWAYS", 0.0, false, 0.5);
		}

		// Simple linear regression on close prices
		double[] closes = data.stream().mapToDouble(d -> d.getClose().doubleValue()).toArray();

		double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
		int n = closes.length;

		for (int i = 0; i < n; i++) {
			sumX += i;
			sumY += closes[i];
			sumXY += i * closes[i];
			sumX2 += i * i;
		}

		double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
		double avgClose = sumY / n;

		// Determine trend direction
		String trendDirection;
		if (slope > avgClose * 0.001) {
			trendDirection = "BULLISH";
		}
		else if (slope < -avgClose * 0.001) {
			trendDirection = "BEARISH";
		}
		else {
			trendDirection = "SIDEWAYS";
		}

		// Trend strength (R-squared approximation)
		double trendStrength = Math.min(Math.abs(slope) / avgClose * 100.0, 1.0);

		// Mean-reversion (simplified: Hurst exponent approximation)
		// Placeholder: real Hurst calculation is complex
		boolean isMeanReverting = trendStrength < 0.3; // Weak trend = likely mean-reverting

		log.debug("Market characteristics: trend={}, strength={}, meanReverting={}", trendDirection, trendStrength,
				isMeanReverting);

		return new MarketCharacteristics(trendDirection, trendStrength, isMeanReverting, 0.5);
	}

	// ========== RISK METRICS ==========

	private static class RiskMetrics {

		double avgMaxDrawdown;

		double avgWinRate;

		String recommendedLevel;

	}

	/**
	 * Calculate basic risk metrics. Simplified implementation.
	 */
	private RiskMetrics calculateRiskMetrics(List<MarketDataEntity> data, List<IndicatorRanking> rankings) {
		RiskMetrics metrics = new RiskMetrics();

		// Simplified: Base on indicator effectiveness
		if (!rankings.isEmpty()) {
			metrics.avgWinRate = rankings.get(0).getEffectivenessScore() * 100.0;
		}
		else {
			metrics.avgWinRate = 50.0; // Neutral
		}

		// Estimate drawdown from volatility
		double maxMove = data.stream()
			.mapToDouble(d -> Math.abs(d.getHigh().doubleValue() - d.getLow().doubleValue()))
			.max()
			.orElse(0.0);
		double avgClose = data.stream().mapToDouble(d -> d.getClose().doubleValue()).average().orElse(1.0);
		metrics.avgMaxDrawdown = (maxMove / avgClose) * 100.0;

		// Recommended risk level
		if (metrics.avgWinRate > 60) {
			metrics.recommendedLevel = "MEDIUM";
		}
		else if (metrics.avgWinRate > 55) {
			metrics.recommendedLevel = "MEDIUM";
		}
		else {
			metrics.recommendedLevel = "LOW";
		}

		return metrics;
	}

	// ========== FUNDAMENTALS ==========

	private FundamentalsInsights getFundamentalsInsights(String symbol) {
		FundamentalsEntity fund = fundamentalsService.getLatestFundamentalsOrNull(symbol);

		if (fund == null) {
			log.warn("No fundamentals data available for symbol={}", symbol);
			return null;
		}
		FundamentalsInsights insights = new FundamentalsInsights();
		insights.setSymbol(symbol);
		insights.setPeRatio(fund.getPriceToEarnings());
		insights.setPbRatio(fund.getPriceToBook());
		insights.setPsRatio(fund.getPriceToSales());
		insights.setRoe(fund.getReturnOnEquity());
		insights.setProfitMargin(fund.getProfitMargin());
		insights.setRevenueGrowthYoY(fund.getRevenueGrowthYoy());
		insights.setEpsGrowthYoY(fund.getEpsGrowthYoy());
		insights.setDividendYield(fund.getDividendYield());

		// Build summary
		StringBuilder summary = new StringBuilder();
		summary.append(String.format("P/E: %.1f, ", fund.getPriceToEarnings() != null ? fund.getPriceToEarnings() : BigDecimal.ZERO));
		summary.append(String.format("ROE: %.1f%%, ", fund.getReturnOnEquity() != null ? fund.getReturnOnEquity() : BigDecimal.ZERO));
		summary.append(String.format("Margin: %.1f%%",
				fund.getProfitMargin() != null ? fund.getProfitMargin() : BigDecimal.ZERO));
		insights.setSummary(summary.toString());

		return insights;
	}

	// ========== UTILITY METHODS ==========

	/**
	 * Downsample data intelligently to reduce size while preserving key features.
	 */
	private List<MarketDataEntity> downsampleData(List<MarketDataEntity> data, int targetSize) {
		if (data.size() <= targetSize) {
			return data;
		}

		int step = data.size() / targetSize;
		List<MarketDataEntity> downsampled = new ArrayList<>();

		for (int i = 0; i < data.size(); i += step) {
			downsampled.add(data.get(i));
			if (downsampled.size() >= targetSize) {
				break;
			}
		}

		return downsampled;
	}

}
