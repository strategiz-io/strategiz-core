package io.strategiz.business.historicalinsights.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-timeframe analysis for improved strategy generation.
 *
 * Key concepts:
 * - Higher timeframe (HTF) determines overall trend direction
 * - Lower timeframe (LTF) provides precise entry/exit timing
 * - Alignment across timeframes increases probability of success
 * - Divergence signals potential reversals or continuation failures
 *
 * Timeframe hierarchy:
 * - Monthly (1M) → Weekly (1W) → Daily (1D) → 4-Hour (4h) → 1-Hour (1h) → 15-Min (15m)
 */
@Component
public class MultiTimeframeAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(MultiTimeframeAnalyzer.class);

	/**
	 * Standard timeframe hierarchy from highest to lowest.
	 */
	public static final String[] TIMEFRAME_HIERARCHY = { "1M", "1W", "1D", "4h", "1h", "15m", "5m", "1m" };

	/**
	 * Result of multi-timeframe analysis.
	 */
	public static class MultiTimeframeResult {

		private String primaryTimeframe;

		private Map<String, TimeframeAnalysis> timeframeAnalyses;

		private TimeframeAlignment alignment;

		private String overallTrend;

		private double alignmentScore; // 0-100, higher = more aligned

		private List<String> tradingRecommendations;

		private String optimalEntryTimeframe;

		private String trendTimeframe;

		public MultiTimeframeResult() {
			this.timeframeAnalyses = new HashMap<>();
			this.tradingRecommendations = new ArrayList<>();
		}

		public String getPrimaryTimeframe() {
			return primaryTimeframe;
		}

		public void setPrimaryTimeframe(String primaryTimeframe) {
			this.primaryTimeframe = primaryTimeframe;
		}

		public Map<String, TimeframeAnalysis> getTimeframeAnalyses() {
			return timeframeAnalyses;
		}

		public void setTimeframeAnalyses(Map<String, TimeframeAnalysis> timeframeAnalyses) {
			this.timeframeAnalyses = timeframeAnalyses;
		}

		public TimeframeAlignment getAlignment() {
			return alignment;
		}

		public void setAlignment(TimeframeAlignment alignment) {
			this.alignment = alignment;
		}

		public String getOverallTrend() {
			return overallTrend;
		}

		public void setOverallTrend(String overallTrend) {
			this.overallTrend = overallTrend;
		}

		public double getAlignmentScore() {
			return alignmentScore;
		}

		public void setAlignmentScore(double alignmentScore) {
			this.alignmentScore = alignmentScore;
		}

		public List<String> getTradingRecommendations() {
			return tradingRecommendations;
		}

		public void setTradingRecommendations(List<String> tradingRecommendations) {
			this.tradingRecommendations = tradingRecommendations;
		}

		public String getOptimalEntryTimeframe() {
			return optimalEntryTimeframe;
		}

		public void setOptimalEntryTimeframe(String optimalEntryTimeframe) {
			this.optimalEntryTimeframe = optimalEntryTimeframe;
		}

		public String getTrendTimeframe() {
			return trendTimeframe;
		}

		public void setTrendTimeframe(String trendTimeframe) {
			this.trendTimeframe = trendTimeframe;
		}

		/**
		 * Format as AI prompt section.
		 */
		public String toPromptFormat() {
			StringBuilder sb = new StringBuilder();
			sb.append("## MULTI-TIMEFRAME ANALYSIS\n\n");

			sb.append(String.format("**Overall Trend**: %s\n", overallTrend));
			sb.append(String.format("**Alignment Score**: %.0f%% (%s)\n", alignmentScore, alignment));
			sb.append(String.format("**Trend Timeframe**: %s (use for direction)\n", trendTimeframe));
			sb.append(String.format("**Entry Timeframe**: %s (use for timing)\n\n", optimalEntryTimeframe));

			sb.append("### Timeframe Breakdown:\n");
			for (Map.Entry<String, TimeframeAnalysis> entry : timeframeAnalyses.entrySet()) {
				TimeframeAnalysis ta = entry.getValue();
				sb.append(String.format("- **%s**: %s (RSI: %.0f, MACD: %s)\n", entry.getKey(), ta.getTrend(),
						ta.getRsi(), ta.getMacdSignal()));
			}

			if (!tradingRecommendations.isEmpty()) {
				sb.append("\n### Trading Recommendations:\n");
				for (String rec : tradingRecommendations) {
					sb.append(String.format("→ %s\n", rec));
				}
			}

			return sb.toString();
		}

	}

	/**
	 * Analysis for a single timeframe.
	 */
	public static class TimeframeAnalysis {

		private String timeframe;

		private String trend; // BULLISH, BEARISH, NEUTRAL

		private double trendStrength; // 0-100

		private double rsi;

		private String macdSignal; // BULLISH, BEARISH, NEUTRAL

		private double macdHistogram;

		private String priceVsEma; // ABOVE, BELOW, AT

		private double percentFromEma20;

		private double percentFromEma50;

		private String bollingerPosition; // UPPER, MIDDLE, LOWER

		private boolean isOversold;

		private boolean isOverbought;

		public TimeframeAnalysis(String timeframe) {
			this.timeframe = timeframe;
		}

		public String getTimeframe() {
			return timeframe;
		}

		public String getTrend() {
			return trend;
		}

		public void setTrend(String trend) {
			this.trend = trend;
		}

		public double getTrendStrength() {
			return trendStrength;
		}

		public void setTrendStrength(double trendStrength) {
			this.trendStrength = trendStrength;
		}

		public double getRsi() {
			return rsi;
		}

		public void setRsi(double rsi) {
			this.rsi = rsi;
		}

		public String getMacdSignal() {
			return macdSignal;
		}

		public void setMacdSignal(String macdSignal) {
			this.macdSignal = macdSignal;
		}

		public double getMacdHistogram() {
			return macdHistogram;
		}

		public void setMacdHistogram(double macdHistogram) {
			this.macdHistogram = macdHistogram;
		}

		public String getPriceVsEma() {
			return priceVsEma;
		}

		public void setPriceVsEma(String priceVsEma) {
			this.priceVsEma = priceVsEma;
		}

		public double getPercentFromEma20() {
			return percentFromEma20;
		}

		public void setPercentFromEma20(double percentFromEma20) {
			this.percentFromEma20 = percentFromEma20;
		}

		public double getPercentFromEma50() {
			return percentFromEma50;
		}

		public void setPercentFromEma50(double percentFromEma50) {
			this.percentFromEma50 = percentFromEma50;
		}

		public String getBollingerPosition() {
			return bollingerPosition;
		}

		public void setBollingerPosition(String bollingerPosition) {
			this.bollingerPosition = bollingerPosition;
		}

		public boolean isOversold() {
			return isOversold;
		}

		public void setOversold(boolean oversold) {
			isOversold = oversold;
		}

		public boolean isOverbought() {
			return isOverbought;
		}

		public void setOverbought(boolean overbought) {
			isOverbought = overbought;
		}

	}

	/**
	 * Timeframe alignment states.
	 */
	public enum TimeframeAlignment {

		FULLY_ALIGNED_BULLISH("All timeframes bullish - strong uptrend"),
		FULLY_ALIGNED_BEARISH("All timeframes bearish - strong downtrend"),
		MOSTLY_ALIGNED_BULLISH("Most timeframes bullish - probable uptrend"),
		MOSTLY_ALIGNED_BEARISH("Most timeframes bearish - probable downtrend"),
		DIVERGING_HTF_BULLISH("Higher timeframe bullish, lower bearish - pullback in uptrend"),
		DIVERGING_HTF_BEARISH("Higher timeframe bearish, lower bullish - bounce in downtrend"),
		MIXED("No clear alignment - ranging or transitional market"),
		TRANSITIONAL("Trend change in progress");

		private final String description;

		TimeframeAlignment(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

	}

	/**
	 * Analyze multiple timeframes for a symbol. This method simulates analysis based on the primary
	 * timeframe data. In production, it would fetch data for each timeframe.
	 *
	 * @param primaryTimeframe The primary trading timeframe
	 * @param prices Array of closing prices (most recent last)
	 * @param highs Array of high prices
	 * @param lows Array of low prices
	 * @param volumes Array of volumes
	 * @return MultiTimeframeResult with analysis
	 */
	public MultiTimeframeResult analyze(String primaryTimeframe, double[] prices, double[] highs, double[] lows,
			double[] volumes) {

		MultiTimeframeResult result = new MultiTimeframeResult();
		result.setPrimaryTimeframe(primaryTimeframe);

		if (prices == null || prices.length < 50) {
			log.warn("Insufficient price data for multi-timeframe analysis");
			result.setAlignment(TimeframeAlignment.MIXED);
			result.setAlignmentScore(50);
			return result;
		}

		// Determine timeframes to analyze based on primary
		List<String> timeframesToAnalyze = getTimeframesToAnalyze(primaryTimeframe);

		// Analyze each timeframe
		for (String tf : timeframesToAnalyze) {
			TimeframeAnalysis analysis = analyzeTimeframe(tf, primaryTimeframe, prices, highs, lows);
			result.getTimeframeAnalyses().put(tf, analysis);
		}

		// Determine alignment
		determineAlignment(result);

		// Generate recommendations
		generateRecommendations(result);

		// Set trend and entry timeframes
		result.setTrendTimeframe(getHigherTimeframe(primaryTimeframe));
		result.setOptimalEntryTimeframe(primaryTimeframe);

		log.info("Multi-timeframe analysis complete: {} alignment, {:.0f}% score", result.getAlignment(),
				result.getAlignmentScore());

		return result;
	}

	/**
	 * Get the list of timeframes to analyze based on primary timeframe.
	 */
	private List<String> getTimeframesToAnalyze(String primaryTimeframe) {
		List<String> timeframes = new ArrayList<>();

		// Always include primary timeframe
		timeframes.add(primaryTimeframe);

		// Add higher timeframe for trend
		String higher = getHigherTimeframe(primaryTimeframe);
		if (higher != null) {
			timeframes.add(higher);
		}

		// Add even higher timeframe for macro trend
		if (higher != null) {
			String macro = getHigherTimeframe(higher);
			if (macro != null) {
				timeframes.add(macro);
			}
		}

		// Add lower timeframe for entry timing
		String lower = getLowerTimeframe(primaryTimeframe);
		if (lower != null) {
			timeframes.add(lower);
		}

		return timeframes;
	}

	/**
	 * Get the next higher timeframe.
	 */
	public String getHigherTimeframe(String timeframe) {
		for (int i = TIMEFRAME_HIERARCHY.length - 1; i > 0; i--) {
			if (TIMEFRAME_HIERARCHY[i].equalsIgnoreCase(timeframe)) {
				return TIMEFRAME_HIERARCHY[i - 1];
			}
		}
		return null;
	}

	/**
	 * Get the next lower timeframe.
	 */
	public String getLowerTimeframe(String timeframe) {
		for (int i = 0; i < TIMEFRAME_HIERARCHY.length - 1; i++) {
			if (TIMEFRAME_HIERARCHY[i].equalsIgnoreCase(timeframe)) {
				return TIMEFRAME_HIERARCHY[i + 1];
			}
		}
		return null;
	}

	/**
	 * Analyze a single timeframe. Simulates higher/lower timeframe by adjusting lookback periods.
	 */
	private TimeframeAnalysis analyzeTimeframe(String timeframe, String primaryTimeframe, double[] prices,
			double[] highs, double[] lows) {

		TimeframeAnalysis analysis = new TimeframeAnalysis(timeframe);

		// Calculate multiplier for simulating different timeframes
		int multiplier = getTimeframeMultiplier(timeframe, primaryTimeframe);

		// Adjust lookback periods based on timeframe multiplier
		int rsiPeriod = 14 * multiplier;
		int emaPeriod20 = 20 * multiplier;
		int emaPeriod50 = 50 * multiplier;

		// Ensure we have enough data
		int len = prices.length;
		rsiPeriod = Math.min(rsiPeriod, len / 3);
		emaPeriod20 = Math.min(emaPeriod20, len / 2);
		emaPeriod50 = Math.min(emaPeriod50, len - 1);

		// Calculate RSI
		double rsi = calculateRSI(prices, rsiPeriod);
		analysis.setRsi(rsi);
		analysis.setOversold(rsi < 30);
		analysis.setOverbought(rsi > 70);

		// Calculate EMAs
		double ema20 = calculateEMA(prices, emaPeriod20);
		double ema50 = calculateEMA(prices, emaPeriod50);
		double currentPrice = prices[len - 1];

		analysis.setPercentFromEma20(((currentPrice - ema20) / ema20) * 100);
		analysis.setPercentFromEma50(((currentPrice - ema50) / ema50) * 100);

		if (currentPrice > ema20 * 1.01) {
			analysis.setPriceVsEma("ABOVE");
		}
		else if (currentPrice < ema20 * 0.99) {
			analysis.setPriceVsEma("BELOW");
		}
		else {
			analysis.setPriceVsEma("AT");
		}

		// Calculate MACD
		double[] macdResult = calculateMACD(prices, 12 * multiplier, 26 * multiplier, 9 * multiplier);
		analysis.setMacdHistogram(macdResult[2]); // histogram

		if (macdResult[2] > 0 && macdResult[0] > macdResult[1]) {
			analysis.setMacdSignal("BULLISH");
		}
		else if (macdResult[2] < 0 && macdResult[0] < macdResult[1]) {
			analysis.setMacdSignal("BEARISH");
		}
		else {
			analysis.setMacdSignal("NEUTRAL");
		}

		// Determine trend
		determineTrend(analysis, currentPrice, ema20, ema50);

		// Calculate Bollinger Band position
		double[] bb = calculateBollingerBands(prices, 20 * multiplier, 2.0);
		if (currentPrice >= bb[0]) {
			analysis.setBollingerPosition("UPPER");
		}
		else if (currentPrice <= bb[2]) {
			analysis.setBollingerPosition("LOWER");
		}
		else {
			analysis.setBollingerPosition("MIDDLE");
		}

		return analysis;
	}

	/**
	 * Determine the trend for a timeframe.
	 */
	private void determineTrend(TimeframeAnalysis analysis, double price, double ema20, double ema50) {
		boolean aboveEma20 = price > ema20;
		boolean aboveEma50 = price > ema50;
		boolean ema20AboveEma50 = ema20 > ema50;
		boolean bullishMacd = "BULLISH".equals(analysis.getMacdSignal());

		int bullishPoints = 0;
		if (aboveEma20)
			bullishPoints++;
		if (aboveEma50)
			bullishPoints++;
		if (ema20AboveEma50)
			bullishPoints++;
		if (bullishMacd)
			bullishPoints++;

		if (bullishPoints >= 3) {
			analysis.setTrend("BULLISH");
			analysis.setTrendStrength(bullishPoints * 25.0);
		}
		else if (bullishPoints <= 1) {
			analysis.setTrend("BEARISH");
			analysis.setTrendStrength((4 - bullishPoints) * 25.0);
		}
		else {
			analysis.setTrend("NEUTRAL");
			analysis.setTrendStrength(50.0);
		}
	}

	/**
	 * Get multiplier for adjusting indicator periods based on timeframe.
	 */
	private int getTimeframeMultiplier(String targetTimeframe, String primaryTimeframe) {
		int targetIdx = -1, primaryIdx = -1;

		for (int i = 0; i < TIMEFRAME_HIERARCHY.length; i++) {
			if (TIMEFRAME_HIERARCHY[i].equalsIgnoreCase(targetTimeframe))
				targetIdx = i;
			if (TIMEFRAME_HIERARCHY[i].equalsIgnoreCase(primaryTimeframe))
				primaryIdx = i;
		}

		if (targetIdx == -1 || primaryIdx == -1)
			return 1;

		// Higher timeframe = smaller index = larger multiplier
		int diff = primaryIdx - targetIdx;

		if (diff <= 0)
			return 1;
		if (diff == 1)
			return 4; // e.g., 4h to 1D
		if (diff == 2)
			return 20; // e.g., 1h to 1D
		return Math.min(50, (int) Math.pow(4, diff));
	}

	/**
	 * Determine overall alignment across timeframes.
	 */
	private void determineAlignment(MultiTimeframeResult result) {
		Map<String, TimeframeAnalysis> analyses = result.getTimeframeAnalyses();

		int bullishCount = 0, bearishCount = 0, neutralCount = 0;
		String htfTrend = null;
		String ltfTrend = null;

		for (TimeframeAnalysis ta : analyses.values()) {
			switch (ta.getTrend()) {
				case "BULLISH" -> bullishCount++;
				case "BEARISH" -> bearishCount++;
				default -> neutralCount++;
			}
		}

		int total = analyses.size();

		// Get HTF and LTF trends for divergence detection
		String primaryTf = result.getPrimaryTimeframe();
		String htf = getHigherTimeframe(primaryTf);
		String ltf = getLowerTimeframe(primaryTf);

		if (htf != null && analyses.containsKey(htf)) {
			htfTrend = analyses.get(htf).getTrend();
		}
		if (ltf != null && analyses.containsKey(ltf)) {
			ltfTrend = analyses.get(ltf).getTrend();
		}

		// Determine alignment
		if (bullishCount == total) {
			result.setAlignment(TimeframeAlignment.FULLY_ALIGNED_BULLISH);
			result.setOverallTrend("STRONG_UPTREND");
			result.setAlignmentScore(100);
		}
		else if (bearishCount == total) {
			result.setAlignment(TimeframeAlignment.FULLY_ALIGNED_BEARISH);
			result.setOverallTrend("STRONG_DOWNTREND");
			result.setAlignmentScore(100);
		}
		else if (bullishCount >= total * 0.75) {
			result.setAlignment(TimeframeAlignment.MOSTLY_ALIGNED_BULLISH);
			result.setOverallTrend("UPTREND");
			result.setAlignmentScore(75 + (bullishCount - total * 0.75) * 25 / (total * 0.25));
		}
		else if (bearishCount >= total * 0.75) {
			result.setAlignment(TimeframeAlignment.MOSTLY_ALIGNED_BEARISH);
			result.setOverallTrend("DOWNTREND");
			result.setAlignmentScore(75 + (bearishCount - total * 0.75) * 25 / (total * 0.25));
		}
		else if ("BULLISH".equals(htfTrend) && "BEARISH".equals(ltfTrend)) {
			result.setAlignment(TimeframeAlignment.DIVERGING_HTF_BULLISH);
			result.setOverallTrend("PULLBACK_IN_UPTREND");
			result.setAlignmentScore(60);
		}
		else if ("BEARISH".equals(htfTrend) && "BULLISH".equals(ltfTrend)) {
			result.setAlignment(TimeframeAlignment.DIVERGING_HTF_BEARISH);
			result.setOverallTrend("BOUNCE_IN_DOWNTREND");
			result.setAlignmentScore(60);
		}
		else if (neutralCount >= total * 0.5) {
			result.setAlignment(TimeframeAlignment.TRANSITIONAL);
			result.setOverallTrend("TRANSITIONAL");
			result.setAlignmentScore(40);
		}
		else {
			result.setAlignment(TimeframeAlignment.MIXED);
			result.setOverallTrend("RANGING");
			result.setAlignmentScore(30);
		}
	}

	/**
	 * Generate trading recommendations based on multi-timeframe analysis.
	 */
	private void generateRecommendations(MultiTimeframeResult result) {
		List<String> recs = new ArrayList<>();

		switch (result.getAlignment()) {
			case FULLY_ALIGNED_BULLISH -> {
				recs.add("All timeframes bullish - HIGH probability long setup");
				recs.add("Use pullbacks to EMA20 on entry timeframe for entries");
				recs.add("Trail stop using ATR - let profits run");
				recs.add("Avoid shorting until higher timeframe shows weakness");
			}
			case FULLY_ALIGNED_BEARISH -> {
				recs.add("All timeframes bearish - HIGH probability short setup or cash");
				recs.add("Avoid buying dips - trend is strongly down");
				recs.add("Wait for higher timeframe to turn neutral before longs");
				recs.add("Consider inverse ETFs or puts if bearish exposure desired");
			}
			case MOSTLY_ALIGNED_BULLISH -> {
				recs.add("Majority bullish - favor long entries on pullbacks");
				recs.add("Wait for lower timeframe to align before entry");
				recs.add("Use tighter stops until fully aligned");
			}
			case MOSTLY_ALIGNED_BEARISH -> {
				recs.add("Majority bearish - avoid longs, wait for clarity");
				recs.add("Bounces likely to fail - don't chase");
				recs.add("Wait for multiple timeframe alignment before acting");
			}
			case DIVERGING_HTF_BULLISH -> {
				recs.add("HTF bullish but LTF pulling back - WAIT for LTF to turn");
				recs.add("This is a potential buy-the-dip opportunity");
				recs.add("Enter when LTF RSI < 40 and starts turning up");
				recs.add("Stop below recent swing low");
			}
			case DIVERGING_HTF_BEARISH -> {
				recs.add("HTF bearish but LTF bouncing - LOW probability long");
				recs.add("This bounce will likely fail - consider shorting bounce");
				recs.add("Wait for LTF to roll over for confirmation");
				recs.add("Do NOT buy this bounce");
			}
			case TRANSITIONAL -> {
				recs.add("Trend is changing - reduce position size");
				recs.add("Wait for new trend to establish");
				recs.add("Consider mean reversion strategies in tight range");
			}
			case MIXED -> {
				recs.add("No clear trend - AVOID trend-following strategies");
				recs.add("Use mean reversion (buy support, sell resistance)");
				recs.add("Reduce position size significantly");
				recs.add("Wait for alignment before larger positions");
			}
		}

		// Add RSI-based recommendations
		for (TimeframeAnalysis ta : result.getTimeframeAnalyses().values()) {
			if (ta.isOversold() && result.getOverallTrend().contains("UP")) {
				recs.add(String.format("%s RSI oversold (%.0f) in uptrend - potential buy zone",
						ta.getTimeframe(), ta.getRsi()));
			}
			if (ta.isOverbought() && result.getOverallTrend().contains("DOWN")) {
				recs.add(String.format("%s RSI overbought (%.0f) in downtrend - avoid buying",
						ta.getTimeframe(), ta.getRsi()));
			}
		}

		result.setTradingRecommendations(recs);
	}

	// ========================================================================
	// INDICATOR CALCULATIONS
	// ========================================================================

	private double calculateRSI(double[] prices, int period) {
		if (prices.length < period + 1)
			return 50;

		double gainSum = 0, lossSum = 0;

		for (int i = prices.length - period; i < prices.length; i++) {
			double change = prices[i] - prices[i - 1];
			if (change > 0)
				gainSum += change;
			else
				lossSum -= change;
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;

		if (avgLoss == 0)
			return 100;
		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	private double calculateEMA(double[] prices, int period) {
		if (prices.length < period)
			return prices[prices.length - 1];

		double multiplier = 2.0 / (period + 1);
		double ema = prices[prices.length - period];

		for (int i = prices.length - period + 1; i < prices.length; i++) {
			ema = (prices[i] - ema) * multiplier + ema;
		}

		return ema;
	}

	private double[] calculateMACD(double[] prices, int fastPeriod, int slowPeriod, int signalPeriod) {
		double fastEma = calculateEMA(prices, Math.min(fastPeriod, prices.length - 1));
		double slowEma = calculateEMA(prices, Math.min(slowPeriod, prices.length - 1));
		double macdLine = fastEma - slowEma;

		// For signal line, we'd need historical MACD values
		// Simplified: use current MACD as approximation
		double signalLine = macdLine * 0.9; // Approximation
		double histogram = macdLine - signalLine;

		return new double[] { macdLine, signalLine, histogram };
	}

	private double[] calculateBollingerBands(double[] prices, int period, double stdMult) {
		if (prices.length < period) {
			double price = prices[prices.length - 1];
			return new double[] { price * 1.02, price, price * 0.98 };
		}

		// Calculate SMA
		double sum = 0;
		for (int i = prices.length - period; i < prices.length; i++) {
			sum += prices[i];
		}
		double sma = sum / period;

		// Calculate standard deviation
		double variance = 0;
		for (int i = prices.length - period; i < prices.length; i++) {
			variance += Math.pow(prices[i] - sma, 2);
		}
		double std = Math.sqrt(variance / period);

		return new double[] { sma + stdMult * std, // Upper band
				sma, // Middle band
				sma - stdMult * std // Lower band
		};
	}

}
