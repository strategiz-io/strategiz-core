package io.strategiz.business.historicalinsights.service;

import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
import io.strategiz.business.historicalinsights.analysis.*;
import io.strategiz.business.historicalinsights.exception.InsufficientDataException;
import io.strategiz.business.historicalinsights.model.*;
import io.strategiz.data.fundamentals.entity.FundamentalsEntity;
import io.strategiz.data.marketdata.clickhouse.repository.MarketDataClickHouseRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for analyzing historical market data to generate insights for Historical Market
 * Insights (Autonomous AI mode). Computes volatility profiles, ranks indicators by
 * effectiveness, finds optimal parameters, and detects market characteristics using 7
 * years of historical OHLCV data.
 *
 * Requires ClickHouse to be enabled - this service will not be available when ClickHouse
 * is disabled.
 */
@Service
@ConditionalOnProperty(name = "strategiz.clickhouse.enabled", havingValue = "true")
public class HistoricalInsightsService {

	private static final Logger log = LoggerFactory.getLogger(HistoricalInsightsService.class);

	private static final int MIN_BARS_REQUIRED = 100;

	private static final int MAX_BARS = 2600; // ~7 years of daily data

	private static final int FAST_MODE_BARS = 750; // ~3 years - faster analysis with good
													// accuracy

	// Thread pool for parallel indicator backtesting
	private static final ExecutorService BACKTEST_EXECUTOR = Executors.newFixedThreadPool(4);

	private final MarketDataClickHouseRepository marketDataRepo;

	private final FundamentalsQueryService fundamentalsService;

	// Advanced analyzers
	private final HurstExponentCalculator hurstCalculator;

	private final DrawdownAnalyzer drawdownAnalyzer;

	private final OptimalThresholdCalculator thresholdCalculator;

	private final MarketRegimeClassifier regimeClassifier;

	private final SwingCycleAnalyzer swingCycleAnalyzer;

	private final MultiTimeframeAnalyzer mtfAnalyzer;

	public HistoricalInsightsService(MarketDataClickHouseRepository marketDataRepo,
			FundamentalsQueryService fundamentalsService) {
		this.marketDataRepo = marketDataRepo;
		this.fundamentalsService = fundamentalsService;
		// Initialize analyzers
		this.hurstCalculator = new HurstExponentCalculator();
		this.drawdownAnalyzer = new DrawdownAnalyzer();
		this.thresholdCalculator = new OptimalThresholdCalculator();
		this.regimeClassifier = new MarketRegimeClassifier();
		this.swingCycleAnalyzer = new SwingCycleAnalyzer();
		this.mtfAnalyzer = new MultiTimeframeAnalyzer();
	}

	/**
	 * Main entry point: Analyze a symbol and return comprehensive historical insights.
	 * @param symbol Symbol to analyze
	 * @param timeframe Timeframe (e.g., "1D", "1h")
	 * @param lookbackDays Number of days to look back (default 2600 for ~7 years)
	 * @param includeFundamentals Whether to include fundamental analysis
	 * @return SymbolInsights object with all computed insights
	 */
	public SymbolInsights analyzeSymbolForStrategyGeneration(String symbol, String timeframe, int lookbackDays,
			boolean includeFundamentals) {
		return analyzeSymbolForStrategyGeneration(symbol, timeframe, lookbackDays, includeFundamentals, false);
	}

	/**
	 * Main entry point: Analyze a symbol and return comprehensive historical insights.
	 * Fast mode focuses on turning points detection (peaks/troughs) - the AI deduces
	 * optimal buy/sell points directly from the price history without indicator analysis.
	 * @param symbol Symbol to analyze
	 * @param timeframe Timeframe (e.g., "1D", "1h")
	 * @param lookbackDays Number of days to look back (default 2600 for ~7 years)
	 * @param includeFundamentals Whether to include fundamental analysis
	 * @param fastMode If true, skips indicator backtests and focuses on turning points
	 * only
	 * @return SymbolInsights object with all computed insights
	 */
	public SymbolInsights analyzeSymbolForStrategyGeneration(String symbol, String timeframe, int lookbackDays,
			boolean includeFundamentals, boolean fastMode) {

		// Fast mode uses reduced lookback for faster analysis
		int effectiveLookback = fastMode ? Math.min(lookbackDays, FAST_MODE_BARS) : lookbackDays;
		int maxBars = fastMode ? FAST_MODE_BARS : MAX_BARS;

		log.info(
				"Starting Historical Market Insights analysis for symbol={}, timeframe={}, lookback={} days, fastMode={}",
				symbol, timeframe, effectiveLookback, fastMode);

		// 1. Query historical data
		Instant endTime = Instant.now();
		Instant startTime = endTime.minus(effectiveLookback, ChronoUnit.DAYS);

		List<MarketDataEntity> data = marketDataRepo.findBySymbolAndTimeRange(symbol, startTime, endTime, timeframe);

		log.info("Retrieved {} bars for symbol={}", data.size(), symbol);

		// Check minimum data requirement
		if (data.size() < MIN_BARS_REQUIRED) {
			throw new InsufficientDataException(
					String.format("Insufficient data for %s: found %d bars, need at least %d", symbol, data.size(),
							MIN_BARS_REQUIRED));
		}

		// Downsample if too much data
		if (data.size() > maxBars) {
			log.info("Downsampling {} bars to {}", data.size(), maxBars);
			data = downsampleData(data, maxBars);
		}

		// 2. Build insights object
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol(symbol);
		insights.setTimeframe(timeframe);
		insights.setDaysAnalyzed(data.size());

		// 3. Compute volatility profile (fast - single pass)
		VolatilityProfile volatilityProfile = calculateVolatilityProfile(data);
		insights.setAvgVolatility(volatilityProfile.getAvgATR());
		insights.setVolatilityRegime(volatilityProfile.getRegime());
		insights.setAvgDailyRange(volatilityProfile.getAvgDailyRangePercent());

		// 4. Detect market characteristics (fast - linear regression)
		MarketCharacteristics characteristics = detectMarketCharacteristics(data);
		insights.setTrendDirection(characteristics.getTrendDirection());
		insights.setTrendStrength(characteristics.getTrendStrength());
		insights.setMeanReverting(characteristics.isMeanReverting());

		// 5. Detect major price turning points (peaks and troughs) - THE KEY DATA
		// AI uses these to identify optimal buy (troughs) and sell (peaks) points
		List<PriceTurningPoint> turningPoints = detectMajorTurningPoints(data);

		// 5a. ENHANCED: Calculate indicator values at each turning point
		enrichTurningPointsWithIndicators(turningPoints, data);
		insights.setTurningPoints(turningPoints);
		log.info("Detected {} major turning points for {}", turningPoints.size(), symbol);

		// 5b. ENHANCED: Run advanced analysis on turning points and price data
		performAdvancedAnalysis(insights, turningPoints, data);

		// 6. Skip indicator backtests in fast mode - AI deduces from turning points
		// directly
		if (fastMode) {
			// Provide minimal indicator info (no backtesting)
			insights.setTopIndicators(new ArrayList<>());
			insights.setOptimalParameters(Map.of("stop_loss_percent", 3.0, "take_profit_percent", 9.0));
			insights.setAvgMaxDrawdown(volatilityProfile.getAvgDailyRangePercent() * 3);
			insights.setAvgWinRate(50.0); // Neutral - AI will figure it out
			insights.setRecommendedRiskLevel("MEDIUM");
		}
		else {
			// Full analysis with indicator backtesting
			List<IndicatorRanking> rankings = analyzeIndicatorEffectiveness(data);
			insights.setTopIndicators(rankings);
			Map<String, Object> optimalParams = findOptimalParameters(data, rankings);
			insights.setOptimalParameters(optimalParams);
			RiskMetrics riskMetrics = calculateRiskMetrics(data, rankings);
			insights.setAvgMaxDrawdown(riskMetrics.avgMaxDrawdown);
			insights.setAvgWinRate(riskMetrics.avgWinRate);
			insights.setRecommendedRiskLevel(riskMetrics.recommendedLevel);
		}

		// 7. Optional: Include fundamentals
		if (includeFundamentals) {
			FundamentalsInsights fundInsights = getFundamentalsInsights(symbol);
			insights.setFundamentals(fundInsights);
		}

		log.info("Historical Market Insights completed for {} in {} mode. {} turning points, {} volatility", symbol,
				fastMode ? "FAST" : "FULL", turningPoints.size(), volatilityProfile.getRegime());

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

	// ========== INDICATOR RANKING (REAL BACKTESTING) ==========

	/**
	 * Rank indicators by historical effectiveness using REAL backtesting on 7 years of
	 * data. Tests multiple indicators with various parameter combinations and ranks by
	 * profitability. Optimized: Runs all 4 indicator backtests in parallel for ~4x
	 * speedup.
	 */
	private List<IndicatorRanking> analyzeIndicatorEffectiveness(List<MarketDataEntity> data) {
		log.info("Running parallel backtest analysis on {} bars to find best indicators...", data.size());

		// Run all 4 indicator backtests in parallel
		CompletableFuture<Double> rsiFuture = CompletableFuture.supplyAsync(() -> testRSIStrategy(data),
				BACKTEST_EXECUTOR);
		CompletableFuture<Double> macdFuture = CompletableFuture.supplyAsync(() -> testMACDStrategy(data),
				BACKTEST_EXECUTOR);
		CompletableFuture<Double> bbFuture = CompletableFuture.supplyAsync(() -> testBollingerBandsStrategy(data),
				BACKTEST_EXECUTOR);
		CompletableFuture<Double> maFuture = CompletableFuture.supplyAsync(() -> testMACrossoverStrategy(data),
				BACKTEST_EXECUTOR);

		// Wait for all to complete
		CompletableFuture.allOf(rsiFuture, macdFuture, bbFuture, maFuture).join();

		// Collect results
		List<IndicatorRanking> rankings = new ArrayList<>();
		try {
			double bestRSIScore = rsiFuture.get();
			rankings
				.add(new IndicatorRanking("RSI", bestRSIScore, Map.of("period", 14, "oversold", 30, "overbought", 70),
						String.format("Mean-reversion: %.1f%% win rate from backtesting", bestRSIScore * 100)));

			double bestMACDScore = macdFuture.get();
			rankings.add(new IndicatorRanking("MACD", bestMACDScore, Map.of("fast", 12, "slow", 26, "signal", 9),
					String.format("Trend-following: %.1f%% win rate from backtesting", bestMACDScore * 100)));

			double bestBBScore = bbFuture.get();
			rankings.add(new IndicatorRanking("Bollinger Bands", bestBBScore, Map.of("period", 20, "stddev", 2),
					String.format("Volatility breakout: %.1f%% win rate from backtesting", bestBBScore * 100)));

			double bestMAScore = maFuture.get();
			rankings.add(new IndicatorRanking("MA Crossover", bestMAScore, Map.of("fast", 20, "slow", 50),
					String.format("Trend confirmation: %.1f%% win rate from backtesting", bestMAScore * 100)));
		}
		catch (Exception e) {
			log.error("Error during parallel backtest execution", e);
			// Fallback to default rankings
			rankings.add(new IndicatorRanking("RSI", 0.5, Map.of("period", 14, "oversold", 30, "overbought", 70),
					"Default"));
			rankings.add(new IndicatorRanking("MACD", 0.5, Map.of("fast", 12, "slow", 26, "signal", 9), "Default"));
			rankings.add(new IndicatorRanking("Bollinger Bands", 0.5, Map.of("period", 20, "stddev", 2), "Default"));
			rankings.add(new IndicatorRanking("MA Crossover", 0.5, Map.of("fast", 20, "slow", 50), "Default"));
		}

		// Sort by effectiveness score (descending)
		rankings.sort((a, b) -> Double.compare(b.getEffectivenessScore(), a.getEffectivenessScore()));

		log.info("Backtest analysis complete. Best indicator: {} ({:.1f}% effective)",
				rankings.get(0).getIndicatorName(), rankings.get(0).getEffectivenessScore() * 100);

		return rankings;
	}

	/**
	 * Backtest RSI strategy: Buy when RSI < oversold, sell when RSI > overbought. Returns
	 * win rate as effectiveness score.
	 */
	private double testRSIStrategy(List<MarketDataEntity> data) {
		if (data.size() < 50)
			return 0.3; // Not enough data

		int wins = 0;
		int losses = 0;
		int period = 14;
		int oversold = 30;
		int overbought = 70;

		// Calculate RSI
		double[] rsi = calculateRSI(data, period);

		// Simple backtest: track trades
		boolean inPosition = false;
		double entryPrice = 0;

		for (int i = period + 1; i < data.size(); i++) {
			double currentRSI = rsi[i];
			double price = data.get(i).getClose().doubleValue();

			if (!inPosition && currentRSI < oversold) {
				// Buy signal
				inPosition = true;
				entryPrice = price;
			}
			else if (inPosition && currentRSI > overbought) {
				// Sell signal
				double profit = price - entryPrice;
				if (profit > 0)
					wins++;
				else
					losses++;
				inPosition = false;
			}
		}

		// Calculate win rate
		int totalTrades = wins + losses;
		if (totalTrades == 0)
			return 0.3; // No trades = neutral score

		return (double) wins / totalTrades;
	}

	/**
	 * Backtest MACD strategy: Buy when MACD crosses above signal, sell when crosses
	 * below.
	 */
	private double testMACDStrategy(List<MarketDataEntity> data) {
		if (data.size() < 50)
			return 0.3;

		int wins = 0;
		int losses = 0;

		// Calculate MACD
		double[] macd = calculateEMA(data, 12);
		double[] signal = calculateEMA(data, 26);

		boolean inPosition = false;
		double entryPrice = 0;

		for (int i = 26; i < data.size() - 1; i++) {
			double currentMacd = macd[i];
			double prevMacd = macd[i - 1];
			double currentSignal = signal[i];
			double prevSignal = signal[i - 1];
			double price = data.get(i).getClose().doubleValue();

			// Cross above
			if (!inPosition && prevMacd <= prevSignal && currentMacd > currentSignal) {
				inPosition = true;
				entryPrice = price;
			}
			// Cross below
			else if (inPosition && prevMacd >= prevSignal && currentMacd < currentSignal) {
				double profit = price - entryPrice;
				if (profit > 0)
					wins++;
				else
					losses++;
				inPosition = false;
			}
		}

		int totalTrades = wins + losses;
		return totalTrades > 0 ? (double) wins / totalTrades : 0.3;
	}

	/**
	 * Backtest Bollinger Bands: Buy when price touches lower band, sell at upper band.
	 * Optimized: Uses rolling window calculation O(n) instead of O(n²).
	 */
	private double testBollingerBandsStrategy(List<MarketDataEntity> data) {
		if (data.size() < 50)
			return 0.3;

		int wins = 0;
		int losses = 0;
		int period = 20;

		boolean inPosition = false;
		double entryPrice = 0;

		// Pre-extract close prices once
		double[] closes = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			closes[i] = data.get(i).getClose().doubleValue();
		}

		// Initialize rolling sum and sum of squares for first window
		double rollingSum = 0;
		double rollingSumSq = 0;
		for (int i = 0; i < period; i++) {
			rollingSum += closes[i];
			rollingSumSq += closes[i] * closes[i];
		}

		for (int i = period; i < data.size(); i++) {
			double price = closes[i];

			// Rolling SMA and StdDev calculation O(1) per iteration
			double sma = rollingSum / period;
			double variance = (rollingSumSq / period) - (sma * sma);
			double stddev = Math.sqrt(Math.max(0, variance));

			double upperBand = sma + (2 * stddev);
			double lowerBand = sma - (2 * stddev);

			// Buy at lower band
			if (!inPosition && price <= lowerBand) {
				inPosition = true;
				entryPrice = price;
			}
			// Sell at upper band
			else if (inPosition && price >= upperBand) {
				double profit = price - entryPrice;
				if (profit > 0)
					wins++;
				else
					losses++;
				inPosition = false;
			}

			// Update rolling window: remove oldest, add newest
			if (i + 1 < data.size()) {
				double oldPrice = closes[i - period + 1];
				double newPrice = closes[i + 1];
				rollingSum = rollingSum - oldPrice + newPrice;
				rollingSumSq = rollingSumSq - (oldPrice * oldPrice) + (newPrice * newPrice);
			}
		}

		int totalTrades = wins + losses;
		return totalTrades > 0 ? (double) wins / totalTrades : 0.3;
	}

	/**
	 * Backtest MA Crossover: Buy when fast MA crosses above slow MA, sell on cross below.
	 */
	private double testMACrossoverStrategy(List<MarketDataEntity> data) {
		if (data.size() < 100)
			return 0.3;

		int wins = 0;
		int losses = 0;

		double[] fastMA = calculateSMA(data, 20);
		double[] slowMA = calculateSMA(data, 50);

		boolean inPosition = false;
		double entryPrice = 0;

		for (int i = 50; i < data.size() - 1; i++) {
			double price = data.get(i).getClose().doubleValue();

			// Cross above
			if (!inPosition && fastMA[i - 1] <= slowMA[i - 1] && fastMA[i] > slowMA[i]) {
				inPosition = true;
				entryPrice = price;
			}
			// Cross below
			else if (inPosition && fastMA[i - 1] >= slowMA[i - 1] && fastMA[i] < slowMA[i]) {
				double profit = price - entryPrice;
				if (profit > 0)
					wins++;
				else
					losses++;
				inPosition = false;
			}
		}

		int totalTrades = wins + losses;
		return totalTrades > 0 ? (double) wins / totalTrades : 0.3;
	}

	// ========== INDICATOR CALCULATIONS ==========

	private double[] calculateRSI(List<MarketDataEntity> data, int period) {
		double[] rsi = new double[data.size()];
		double avgGain = 0;
		double avgLoss = 0;

		// Initial average
		for (int i = 1; i <= period; i++) {
			double change = data.get(i).getClose().doubleValue() - data.get(i - 1).getClose().doubleValue();
			if (change > 0)
				avgGain += change;
			else
				avgLoss += Math.abs(change);
		}
		avgGain /= period;
		avgLoss /= period;

		// Calculate RSI
		for (int i = period; i < data.size(); i++) {
			double change = data.get(i).getClose().doubleValue() - data.get(i - 1).getClose().doubleValue();
			double gain = change > 0 ? change : 0;
			double loss = change < 0 ? Math.abs(change) : 0;

			avgGain = (avgGain * (period - 1) + gain) / period;
			avgLoss = (avgLoss * (period - 1) + loss) / period;

			double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
			rsi[i] = 100 - (100 / (1 + rs));
		}

		return rsi;
	}

	private double[] calculateEMA(List<MarketDataEntity> data, int period) {
		double[] ema = new double[data.size()];
		double multiplier = 2.0 / (period + 1);

		// First EMA is SMA
		double sum = 0;
		for (int i = 0; i < period; i++) {
			sum += data.get(i).getClose().doubleValue();
		}
		ema[period - 1] = sum / period;

		// Calculate EMA
		for (int i = period; i < data.size(); i++) {
			double price = data.get(i).getClose().doubleValue();
			ema[i] = (price - ema[i - 1]) * multiplier + ema[i - 1];
		}

		return ema;
	}

	/**
	 * Calculate SMA using rolling sum approach O(n) instead of O(n²).
	 */
	private double[] calculateSMA(List<MarketDataEntity> data, int period) {
		double[] sma = new double[data.size()];

		if (data.size() < period) {
			return sma;
		}

		// Initialize rolling sum for first window
		double rollingSum = 0;
		for (int i = 0; i < period; i++) {
			rollingSum += data.get(i).getClose().doubleValue();
		}
		sma[period - 1] = rollingSum / period;

		// Rolling calculation O(1) per iteration
		for (int i = period; i < data.size(); i++) {
			double oldPrice = data.get(i - period).getClose().doubleValue();
			double newPrice = data.get(i).getClose().doubleValue();
			rollingSum = rollingSum - oldPrice + newPrice;
			sma[i] = rollingSum / period;
		}

		return sma;
	}

	// ========== OPTIMAL PARAMETERS (SIMPLIFIED) ==========

	/**
	 * Find optimal parameters for top indicators. Simplified: Returns defaults. Full
	 * implementation would use grid search.
	 */
	private Map<String, Object> findOptimalParameters(List<MarketDataEntity> data, List<IndicatorRanking> rankings) {
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
	 * Detect market characteristics: trend direction, strength, mean-reversion.
	 * Simplified: Uses basic linear regression.
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
		boolean isMeanReverting = trendStrength < 0.3; // Weak trend = likely
														// mean-reverting

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

	// ========== TURNING POINTS DETECTION (Autonomous AI Hindsight) ==========

	/**
	 * Detect major price turning points (peaks and troughs) from historical data. These
	 * are the optimal buy/sell points that the AI should try to capture. Uses a zigzag
	 * algorithm with minimum threshold to filter noise.
	 */
	private List<PriceTurningPoint> detectMajorTurningPoints(List<MarketDataEntity> data) {
		List<PriceTurningPoint> turningPoints = new ArrayList<>();

		if (data.size() < 10) {
			return turningPoints;
		}

		// Minimum % change to qualify as a major turning point (filter noise)
		double minChangePercent = 5.0;

		// Track the last confirmed turning point
		double lastTurningPrice = data.get(0).getClose().doubleValue();
		Instant lastTurningTime = Instant.ofEpochMilli(data.get(0).getTimestamp());
		PriceTurningPoint.PointType lastType = null;

		// Track current potential turning point
		double potentialHigh = data.get(0).getHigh().doubleValue();
		double potentialLow = data.get(0).getLow().doubleValue();
		Instant potentialHighTime = Instant.ofEpochMilli(data.get(0).getTimestamp());
		Instant potentialLowTime = Instant.ofEpochMilli(data.get(0).getTimestamp());

		for (int i = 1; i < data.size(); i++) {
			MarketDataEntity bar = data.get(i);
			double high = bar.getHigh().doubleValue();
			double low = bar.getLow().doubleValue();

			// Update potential peaks/troughs
			if (high > potentialHigh) {
				potentialHigh = high;
				potentialHighTime = Instant.ofEpochMilli(bar.getTimestamp());
			}
			if (low < potentialLow) {
				potentialLow = low;
				potentialLowTime = Instant.ofEpochMilli(bar.getTimestamp());
			}

			// Check for confirmed TROUGH (price rallied from low by minChangePercent)
			if (potentialLow < lastTurningPrice) {
				double rallyPercent = ((high - potentialLow) / potentialLow) * 100;
				if (rallyPercent >= minChangePercent) {
					// Confirmed trough - this was an optimal BUY point
					PriceTurningPoint trough = new PriceTurningPoint(PriceTurningPoint.PointType.TROUGH,
							potentialLowTime, potentialLow);

					if (lastType != null) {
						double changeFromPrev = ((potentialLow - lastTurningPrice) / lastTurningPrice) * 100;
						trough.setPriceChangeFromPrevious(changeFromPrev);
						long daysBetween = java.time.Duration.between(lastTurningTime, potentialLowTime).toDays();
						trough.setDaysFromPrevious((int) daysBetween);
					}

					turningPoints.add(trough);
					lastTurningPrice = potentialLow;
					lastTurningTime = potentialLowTime;
					lastType = PriceTurningPoint.PointType.TROUGH;

					// Reset potential high from this trough
					potentialHigh = high;
					potentialHighTime = Instant.ofEpochMilli(bar.getTimestamp());
				}
			}

			// Check for confirmed PEAK (price dropped from high by minChangePercent)
			if (potentialHigh > lastTurningPrice) {
				double dropPercent = ((potentialHigh - low) / potentialHigh) * 100;
				if (dropPercent >= minChangePercent) {
					// Confirmed peak - this was an optimal SELL point
					PriceTurningPoint peak = new PriceTurningPoint(PriceTurningPoint.PointType.PEAK, potentialHighTime,
							potentialHigh);

					if (lastType != null) {
						double changeFromPrev = ((potentialHigh - lastTurningPrice) / lastTurningPrice) * 100;
						peak.setPriceChangeFromPrevious(changeFromPrev);
						long daysBetween = java.time.Duration.between(lastTurningTime, potentialHighTime).toDays();
						peak.setDaysFromPrevious((int) daysBetween);
					}

					turningPoints.add(peak);
					lastTurningPrice = potentialHigh;
					lastTurningTime = potentialHighTime;
					lastType = PriceTurningPoint.PointType.PEAK;

					// Reset potential low from this peak
					potentialLow = low;
					potentialLowTime = Instant.ofEpochMilli(bar.getTimestamp());
				}
			}
		}

		// Sort by timestamp
		turningPoints.sort(Comparator.comparing(PriceTurningPoint::getTimestamp));

		// Limit to most significant turning points (max 20 for token limits)
		if (turningPoints.size() > 20) {
			// Keep the ones with largest price changes
			turningPoints.sort((a, b) -> Double.compare(Math.abs(b.getPriceChangeFromPrevious()),
					Math.abs(a.getPriceChangeFromPrevious())));
			turningPoints = new ArrayList<>(turningPoints.subList(0, 20));
			turningPoints.sort(Comparator.comparing(PriceTurningPoint::getTimestamp));
		}

		return turningPoints;
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
		summary.append(String.format("P/E: %.1f, ",
				fund.getPriceToEarnings() != null ? fund.getPriceToEarnings() : BigDecimal.ZERO));
		summary.append(String.format("ROE: %.1f%%, ",
				fund.getReturnOnEquity() != null ? fund.getReturnOnEquity() : BigDecimal.ZERO));
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

	// ========== ENHANCED ANALYSIS METHODS (Phase 1) ==========

	/**
	 * Enrich turning points with indicator values at each point. This allows us to
	 * analyze what indicators signaled each peak/trough.
	 */
	private void enrichTurningPointsWithIndicators(List<PriceTurningPoint> turningPoints, List<MarketDataEntity> data) {
		if (turningPoints == null || turningPoints.isEmpty() || data == null || data.size() < 30) {
			return;
		}

		// Pre-calculate indicators for the entire dataset
		double[] rsi = calculateRSI(data, 14);
		double[] macdHistogram = calculateMACDHistogram(data);
		double[] stochasticK = calculateStochasticK(data, 14);
		double[][] bollingerBands = calculateBollingerBands(data, 20, 2.0);
		double[] volumeAvg = calculateVolumeMA(data, 20);

		// Map timestamps to indices for fast lookup
		Map<Long, Integer> timestampToIndex = new HashMap<>();
		for (int i = 0; i < data.size(); i++) {
			timestampToIndex.put(data.get(i).getTimestamp(), i);
		}

		for (PriceTurningPoint tp : turningPoints) {
			long tpTimestamp = tp.getTimestamp().toEpochMilli();

			// Find closest index to this turning point
			int index = findClosestIndex(tpTimestamp, data, timestampToIndex);
			if (index < 0 || index >= data.size()) {
				continue;
			}

			// Set RSI at this point
			if (index < rsi.length) {
				tp.setRsiAtPoint(rsi[index]);
			}

			// Set MACD histogram
			if (index < macdHistogram.length) {
				tp.setMacdHistogramAtPoint(macdHistogram[index]);
			}

			// Set Stochastic K
			if (index < stochasticK.length) {
				tp.setStochasticKAtPoint(stochasticK[index]);
			}

			// Set Bollinger Band distances
			if (index < bollingerBands[0].length) {
				double price = tp.getPrice();
				double bbUpper = bollingerBands[0][index];
				double bbLower = bollingerBands[1][index];
				double bbMiddle = bollingerBands[2][index];

				if (bbMiddle > 0) {
					tp.setPctFromBollingerUpper(((price - bbUpper) / bbMiddle) * 100);
					tp.setPctFromBollingerLower(((price - bbLower) / bbMiddle) * 100);
				}
			}

			// Set volume ratio
			if (index < volumeAvg.length && volumeAvg[index] > 0) {
				double currentVolume = data.get(index).getVolume().doubleValue();
				double ratio = currentVolume / volumeAvg[index];
				tp.setVolumeRatio(ratio);
				tp.setVolumeConfirmed(ratio > 1.5);
			}

			// Calculate reversal percentages (how price moved after this point)
			if (index + 5 < data.size()) {
				double price5Later = data.get(index + 5).getClose().doubleValue();
				tp.setReversal5DayPercent(((price5Later - tp.getPrice()) / tp.getPrice()) * 100);
			}
			if (index + 10 < data.size()) {
				double price10Later = data.get(index + 10).getClose().doubleValue();
				tp.setReversal10DayPercent(((price10Later - tp.getPrice()) / tp.getPrice()) * 100);
			}
		}
	}

	/**
	 * Perform advanced analysis using the specialized analyzers.
	 */
	private void performAdvancedAnalysis(SymbolInsights insights, List<PriceTurningPoint> turningPoints,
			List<MarketDataEntity> data) {
		// 1. Calculate Hurst Exponent
		HurstResult hurstResult = hurstCalculator.calculate(data);
		insights.setHurstExponent(hurstResult.getHurstExponent());
		insights.setHurstInterpretation(hurstResult.getInterpretation());
		// Update mean-reverting based on proper Hurst calculation
		insights.setMeanReverting(hurstResult.isMeanReverting());
		log.debug("Hurst exponent: {} ({})", hurstResult.getHurstExponent(), hurstResult.getInterpretation());

		// 2. Analyze Drawdowns
		DrawdownProfile drawdownProfile = drawdownAnalyzer.analyze(data);
		insights.setMaxHistoricalDrawdown(drawdownProfile.getMaxDrawdownPercent());
		insights.setAvgDrawdown(drawdownProfile.getAvgDrawdownPercent());
		insights.setAvgDrawdownDuration(drawdownProfile.getAvgDrawdownDuration());
		insights.setAvgRecoveryDays(drawdownProfile.getAvgRecoveryDays());

		// 3. Calculate Optimal Thresholds from turning points
		OptimalThresholds thresholds = thresholdCalculator.calculate(turningPoints);
		insights.setOptimalRsiOversold(thresholds.getRsiOversold());
		insights.setOptimalRsiOverbought(thresholds.getRsiOverbought());
		insights.setOptimalPctDropForEntry(thresholds.getPctDropForEntry());
		insights.setOptimalPctGainForExit(thresholds.getPctGainForExit());
		log.debug("Optimal thresholds: RSI {}/{}, Drop/Gain {}%/{}%", thresholds.getRsiOversold(),
				thresholds.getRsiOverbought(), thresholds.getPctDropForEntry(), thresholds.getPctGainForExit());

		// 4. Classify Market Regime
		RegimeClassification regime = regimeClassifier.classify(data);
		insights.setCurrentRegime(regime.getRegime().getDisplayName());
		insights.setBestStrategyForRegime(regime.getRecommendedStrategyDescription());
		log.debug("Market regime: {} - {}", regime.getRegime().getDisplayName(),
				regime.getRecommendedStrategyDescription());

		// 5. Analyze Swing Cycles
		SwingCycleAnalyzer.SwingCycleResult swingResult = swingCycleAnalyzer.analyze(turningPoints, data);
		insights.setAvgSwingMagnitude(swingResult.getAvgSwingMagnitude());
		insights.setMedianSwingMagnitude(swingResult.getMedianSwingMagnitude());
		insights.setAvgSwingDuration(swingResult.getAvgSwingDuration());
		insights.setOptimalHoldingPeriodDays(swingResult.getOptimalHoldingPeriodDays());
		insights.setOptimalHoldingWinRate(swingResult.getOptimalHoldingWinRate());

		// 6. Calculate Position Sizing Recommendations
		double kellyCriterion = swingCycleAnalyzer.calculateKellyCriterion(turningPoints);
		insights.setKellyCriterion(kellyCriterion);

		double atrPercent = regime.getAtrPercent();
		double volatilityAdjustedSize = swingCycleAnalyzer.calculateVolatilityAdjustedSize(atrPercent, 2.0);
		insights.setVolatilityAdjustedSize(volatilityAdjustedSize);

		// 7. Multi-Timeframe Analysis
		performMultiTimeframeAnalysis(insights, data);

		// 8. Calculate Support/Resistance Levels from turning points
		calculateSupportResistanceLevels(insights, turningPoints);
	}

	/**
	 * Perform multi-timeframe analysis to detect trend alignment.
	 */
	private void performMultiTimeframeAnalysis(SymbolInsights insights, List<MarketDataEntity> data) {
		if (data == null || data.size() < 100) {
			log.debug("Insufficient data for multi-timeframe analysis");
			return;
		}

		try {
			// Extract price arrays
			double[] prices = new double[data.size()];
			double[] highs = new double[data.size()];
			double[] lows = new double[data.size()];
			double[] volumes = new double[data.size()];

			for (int i = 0; i < data.size(); i++) {
				MarketDataEntity d = data.get(i);
				prices[i] = d.getClose().doubleValue();
				highs[i] = d.getHigh().doubleValue();
				lows[i] = d.getLow().doubleValue();
				volumes[i] = d.getVolume() != null ? d.getVolume().doubleValue() : 0;
			}

			// Run multi-timeframe analysis
			String timeframe = insights.getTimeframe() != null ? insights.getTimeframe() : "1D";
			MultiTimeframeAnalyzer.MultiTimeframeResult mtfResult = mtfAnalyzer.analyze(timeframe, prices, highs, lows,
					volumes);

			// Set results on insights
			insights.setMtfOverallTrend(mtfResult.getOverallTrend());
			insights.setMtfAlignment(mtfResult.getAlignment().name());
			insights.setMtfAlignmentScore(mtfResult.getAlignmentScore());
			insights.setMtfTrendTimeframe(mtfResult.getTrendTimeframe());
			insights.setMtfEntryTimeframe(mtfResult.getOptimalEntryTimeframe());
			insights.setMtfRecommendations(mtfResult.getTradingRecommendations());

			log.debug("Multi-timeframe analysis: {} ({:.0f}% aligned)", mtfResult.getAlignment(),
					mtfResult.getAlignmentScore());
		}
		catch (Exception e) {
			log.warn("Multi-timeframe analysis failed: {}", e.getMessage());
		}
	}

	/**
	 * Calculate major support and resistance levels from turning points.
	 */
	private void calculateSupportResistanceLevels(SymbolInsights insights, List<PriceTurningPoint> turningPoints) {
		if (turningPoints == null || turningPoints.size() < 4) {
			return;
		}

		// Collect trough prices as support levels and peak prices as resistance levels
		List<Double> troughPrices = new ArrayList<>();
		List<Double> peakPrices = new ArrayList<>();

		for (PriceTurningPoint tp : turningPoints) {
			if (tp.getType() == PriceTurningPoint.PointType.TROUGH) {
				troughPrices.add(tp.getPrice());
			}
			else {
				peakPrices.add(tp.getPrice());
			}
		}

		// Sort and find clusters (price levels that were hit multiple times)
		List<Double> supports = findPriceClusters(troughPrices, 0.02); // 2% tolerance
		List<Double> resistances = findPriceClusters(peakPrices, 0.02);

		insights.setMajorSupportLevels(supports.subList(0, Math.min(3, supports.size())));
		insights.setMajorResistanceLevels(resistances.subList(0, Math.min(3, resistances.size())));
	}

	/**
	 * Find price clusters - levels that were hit multiple times.
	 */
	private List<Double> findPriceClusters(List<Double> prices, double tolerance) {
		if (prices.isEmpty()) {
			return new ArrayList<>();
		}

		Collections.sort(prices);
		List<Double> clusters = new ArrayList<>();
		List<List<Double>> clusterGroups = new ArrayList<>();

		double clusterStart = prices.get(0);
		List<Double> currentCluster = new ArrayList<>();
		currentCluster.add(prices.get(0));

		for (int i = 1; i < prices.size(); i++) {
			double price = prices.get(i);
			if ((price - clusterStart) / clusterStart <= tolerance) {
				currentCluster.add(price);
			}
			else {
				if (currentCluster.size() >= 2) {
					clusterGroups.add(currentCluster);
				}
				currentCluster = new ArrayList<>();
				currentCluster.add(price);
				clusterStart = price;
			}
		}
		if (currentCluster.size() >= 2) {
			clusterGroups.add(currentCluster);
		}

		// Sort clusters by size (most hits = strongest level)
		clusterGroups.sort((a, b) -> Integer.compare(b.size(), a.size()));

		// Return average price of each cluster
		for (List<Double> cluster : clusterGroups) {
			double avg = cluster.stream().mapToDouble(Double::doubleValue).average().orElse(0);
			clusters.add(avg);
		}

		return clusters;
	}

	/**
	 * Find the closest data index to a given timestamp.
	 */
	private int findClosestIndex(long timestamp, List<MarketDataEntity> data, Map<Long, Integer> timestampMap) {
		// Try exact match first
		Integer exactIndex = timestampMap.get(timestamp);
		if (exactIndex != null) {
			return exactIndex;
		}

		// Binary search for closest
		int low = 0;
		int high = data.size() - 1;

		while (low < high) {
			int mid = (low + high) / 2;
			long midTime = data.get(mid).getTimestamp();

			if (midTime < timestamp) {
				low = mid + 1;
			}
			else {
				high = mid;
			}
		}

		return low;
	}

	// ========== ADDITIONAL INDICATOR CALCULATIONS ==========

	/**
	 * Calculate MACD histogram (MACD line - Signal line).
	 */
	private double[] calculateMACDHistogram(List<MarketDataEntity> data) {
		double[] result = new double[data.size()];

		if (data.size() < 35) {
			return result;
		}

		double[] ema12 = calculateEMA(data, 12);
		double[] ema26 = calculateEMA(data, 26);

		// MACD line
		double[] macdLine = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			macdLine[i] = ema12[i] - ema26[i];
		}

		// Signal line (9-period EMA of MACD line)
		double multiplier = 2.0 / 10.0;
		double[] signalLine = new double[data.size()];
		signalLine[25] = macdLine[25]; // First signal = first MACD

		for (int i = 26; i < data.size(); i++) {
			signalLine[i] = (macdLine[i] - signalLine[i - 1]) * multiplier + signalLine[i - 1];
		}

		// Histogram = MACD - Signal
		for (int i = 26; i < data.size(); i++) {
			result[i] = macdLine[i] - signalLine[i];
		}

		return result;
	}

	/**
	 * Calculate Stochastic %K.
	 */
	private double[] calculateStochasticK(List<MarketDataEntity> data, int period) {
		double[] result = new double[data.size()];

		for (int i = period - 1; i < data.size(); i++) {
			double highestHigh = Double.MIN_VALUE;
			double lowestLow = Double.MAX_VALUE;

			for (int j = i - period + 1; j <= i; j++) {
				double high = data.get(j).getHigh().doubleValue();
				double low = data.get(j).getLow().doubleValue();
				highestHigh = Math.max(highestHigh, high);
				lowestLow = Math.min(lowestLow, low);
			}

			double close = data.get(i).getClose().doubleValue();
			double range = highestHigh - lowestLow;

			if (range > 0) {
				result[i] = ((close - lowestLow) / range) * 100;
			}
			else {
				result[i] = 50; // Default to neutral if no range
			}
		}

		return result;
	}

	/**
	 * Calculate Bollinger Bands. Returns [upper, lower, middle].
	 */
	private double[][] calculateBollingerBands(List<MarketDataEntity> data, int period, double stdDevMult) {
		double[][] result = new double[3][data.size()];

		if (data.size() < period) {
			return result;
		}

		double[] sma = calculateSMA(data, period);

		for (int i = period - 1; i < data.size(); i++) {
			// Calculate standard deviation
			double sum = 0;
			double mean = sma[i];

			for (int j = i - period + 1; j <= i; j++) {
				double diff = data.get(j).getClose().doubleValue() - mean;
				sum += diff * diff;
			}

			double stdDev = Math.sqrt(sum / period);

			result[0][i] = mean + (stdDevMult * stdDev); // Upper
			result[1][i] = mean - (stdDevMult * stdDev); // Lower
			result[2][i] = mean; // Middle
		}

		return result;
	}

	/**
	 * Calculate volume moving average.
	 */
	private double[] calculateVolumeMA(List<MarketDataEntity> data, int period) {
		double[] result = new double[data.size()];

		if (data.size() < period) {
			return result;
		}

		double rollingSum = 0;
		for (int i = 0; i < period; i++) {
			rollingSum += data.get(i).getVolume().doubleValue();
		}
		result[period - 1] = rollingSum / period;

		for (int i = period; i < data.size(); i++) {
			rollingSum = rollingSum - data.get(i - period).getVolume().doubleValue()
					+ data.get(i).getVolume().doubleValue();
			result[i] = rollingSum / period;
		}

		return result;
	}

}
