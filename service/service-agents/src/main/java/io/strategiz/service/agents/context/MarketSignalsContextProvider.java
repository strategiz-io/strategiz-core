package io.strategiz.service.agents.context;

import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.client.fmp.client.FmpNewsClient;
import io.strategiz.client.fmp.client.FmpQuoteClient;
import io.strategiz.client.fmp.client.FmpTechnicalClient;
import io.strategiz.client.fmp.dto.FmpNewsArticle;
import io.strategiz.client.fmp.dto.FmpQuote;
import io.strategiz.client.fmp.dto.FmpTechnicalIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Provides market signals context for the Scout Agent. Includes real-time market data, sector
 * performance, technical indicators, and news from Financial Modeling Prep API.
 */
@Component
public class MarketSignalsContextProvider {

	private static final Logger log = LoggerFactory.getLogger(MarketSignalsContextProvider.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	// Sector ETFs for tracking rotation
	private static final List<String> SECTOR_ETFS = List.of("XLK", // Technology
			"XLF", // Financials
			"XLE", // Energy
			"XLV", // Healthcare
			"XLI", // Industrials
			"XLY", // Consumer Discretionary
			"XLP", // Consumer Staples
			"XLRE", // Real Estate
			"XLU", // Utilities
			"XLB", // Materials
			"XLC" // Communication Services
	);

	// Major index ETFs
	private static final List<String> INDEX_ETFS = List.of("SPY", "QQQ", "IWM", "DIA");

	private final FmpNewsClient newsClient;

	private final FmpQuoteClient quoteClient;

	private final FmpTechnicalClient technicalClient;

	private final FmpFundamentalsClient fundamentalsClient;

	public MarketSignalsContextProvider(FmpNewsClient newsClient, FmpQuoteClient quoteClient,
			FmpTechnicalClient technicalClient, FmpFundamentalsClient fundamentalsClient) {
		this.newsClient = newsClient;
		this.quoteClient = quoteClient;
		this.technicalClient = technicalClient;
		this.fundamentalsClient = fundamentalsClient;
	}

	/**
	 * Build comprehensive market signals context for AI injection.
	 * @param symbols User's watchlist symbols (optional)
	 * @return Formatted context string for AI consumption
	 */
	public String buildMarketSignalsContext(List<String> symbols) {
		StringBuilder context = new StringBuilder();

		// Add timestamp
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		// Market overview with live prices
		appendMarketOverview(context);

		// Technical signals for indices
		appendTechnicalSignals(context);

		// Sector performance with live prices
		appendSectorPerformance(context);

		// Market news for sentiment
		appendMarketNews(context);

		// If user has watchlist symbols
		if (symbols != null && !symbols.isEmpty()) {
			appendWatchlistContext(context, symbols);
		}

		return context.toString();
	}

	/**
	 * Build context focused on market conditions.
	 */
	public String buildMarketConditionsContext() {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		appendMarketOverview(context);
		appendTechnicalSignals(context);
		appendMarketNews(context);

		return context.toString();
	}

	/**
	 * Build context for sector rotation analysis.
	 */
	public String buildSectorRotationContext() {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
		context.append("SECTOR ROTATION ANALYSIS:\n");
		context.append("-".repeat(50)).append("\n\n");

		appendSectorPerformance(context);
		appendSectorGuidance(context);

		return context.toString();
	}

	private void appendMarketOverview(StringBuilder context) {
		context.append("MARKET OVERVIEW:\n");
		context.append("-".repeat(50)).append("\n");

		// Market session
		LocalDateTime now = LocalDateTime.now();
		int hour = now.getHour();
		String session;
		if (hour < 9 || (hour == 9 && now.getMinute() < 30)) {
			session = "Pre-Market";
		}
		else if (hour >= 16) {
			session = "After-Hours";
		}
		else {
			session = "Regular Trading Hours";
		}
		context.append("Session: ").append(session).append(" (EST)\n\n");

		// Live index prices
		if (quoteClient.isConfigured()) {
			try {
				List<FmpQuote> indexQuotes = quoteClient.getIndexQuotes();
				if (!indexQuotes.isEmpty()) {
					context.append("Index Prices (Live):\n");
					for (FmpQuote quote : indexQuotes) {
						context.append("  ").append(quote.toDetailedContextString()).append("\n");
					}
					context.append("\n");
				}

				// VIX volatility indicator
				FmpQuote vix = quoteClient.getVixQuote();
				if (vix != null && vix.getPrice() != null) {
					double vixValue = vix.getPrice().doubleValue();
					String sentiment = interpretVix(vixValue);
					context.append(String.format("VIX: %.2f (%s)\n\n", vixValue, sentiment));
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch market overview quotes: {}", e.getMessage());
				appendFallbackMarketOverview(context);
			}
		}
		else {
			appendFallbackMarketOverview(context);
		}
	}

	private void appendFallbackMarketOverview(StringBuilder context) {
		context.append("Key Benchmarks to Monitor:\n");
		context.append("  SPY (S&P 500) - Broad market direction\n");
		context.append("  QQQ (NASDAQ-100) - Tech sector proxy\n");
		context.append("  IWM (Russell 2000) - Small cap sentiment\n");
		context.append("  VIX - Market volatility/fear gauge\n");
		context.append("[Live data: API not configured]\n\n");
	}

	private void appendTechnicalSignals(StringBuilder context) {
		context.append("TECHNICAL SIGNALS:\n");
		context.append("-".repeat(50)).append("\n");

		if (!technicalClient.isConfigured()) {
			appendFallbackTechnicalGuidance(context);
			return;
		}

		// RSI for major indices
		try {
			context.append("RSI (14-period):\n");
			for (String symbol : INDEX_ETFS) {
				try {
					FmpTechnicalIndicator rsi = technicalClient.getRSI(symbol);
					if (rsi != null && rsi.getRsi() != null) {
						context.append(String.format("  %s: %.1f (%s)\n", symbol, rsi.getRsi(), rsi.interpretRsi()));
					}
				}
				catch (Exception e) {
					log.debug("Failed to fetch RSI for {}: {}", symbol, e.getMessage());
				}
			}
			context.append("\n");

			// MACD signals for SPY and QQQ
			context.append("MACD Signals:\n");
			for (String symbol : List.of("SPY", "QQQ")) {
				try {
					FmpTechnicalIndicator macd = technicalClient.getMACD(symbol);
					if (macd != null && macd.getMacd() != null) {
						String signal = macd.isMacdBullish() ? "bullish" : "bearish";
						context.append(String.format("  %s: %s (hist: %.3f)\n", symbol, signal,
								macd.getMacdHist() != null ? macd.getMacdHist() : 0.0));
					}
				}
				catch (Exception e) {
					log.debug("Failed to fetch MACD for {}: {}", symbol, e.getMessage());
				}
			}
			context.append("\n");

			// SMA position for SPY
			try {
				FmpTechnicalIndicator sma50 = technicalClient.getSMA("SPY", 50);
				FmpTechnicalIndicator sma200 = technicalClient.getSMA("SPY", 200);
				if (sma50 != null && sma200 != null) {
					context.append("SPY Moving Averages:\n");
					if (sma50.getSma() != null) {
						String trend = sma50.isPriceAboveSma() ? "above" : "below";
						context.append(String.format("  SMA(50): $%.2f (price %s)\n", sma50.getSma(), trend));
					}
					if (sma200.getSma() != null) {
						String trend = sma200.isPriceAboveSma() ? "above" : "below";
						context.append(String.format("  SMA(200): $%.2f (price %s)\n", sma200.getSma(), trend));
					}
					context.append("\n");
				}
			}
			catch (Exception e) {
				log.debug("Failed to fetch SMAs for SPY: {}", e.getMessage());
			}

		}
		catch (Exception e) {
			log.warn("Failed to fetch technical signals: {}", e.getMessage());
			appendFallbackTechnicalGuidance(context);
		}
	}

	private void appendFallbackTechnicalGuidance(StringBuilder context) {
		context.append("Key indicators to evaluate:\n\n");
		context.append("Trend Indicators:\n");
		context.append("  SMA 20/50/200 - Short/medium/long-term trends\n");
		context.append("  EMA 9/21 - Fast-moving signals\n\n");
		context.append("Momentum Indicators:\n");
		context.append("  RSI - Oversold (<30) / Overbought (>70)\n");
		context.append("  MACD - Crossovers signal momentum shifts\n\n");
		context.append("[Live data: API not configured]\n\n");
	}

	private void appendSectorPerformance(StringBuilder context) {
		context.append("SECTOR PERFORMANCE:\n");
		context.append("-".repeat(50)).append("\n");

		if (!quoteClient.isConfigured()) {
			appendFallbackSectorAnalysis(context);
			return;
		}

		try {
			List<FmpQuote> sectorQuotes = quoteClient.getSectorETFQuotes();
			if (!sectorQuotes.isEmpty()) {
				// Sort by change percent descending
				sectorQuotes.sort((a, b) -> {
					if (b.getChangePercent() == null) {
						return -1;
					}
					if (a.getChangePercent() == null) {
						return 1;
					}
					return b.getChangePercent().compareTo(a.getChangePercent());
				});

				context.append("Today's Performance (sorted by performance):\n");
				for (FmpQuote quote : sectorQuotes) {
					String sector = getSectorName(quote.getSymbol());
					String changeStr = formatChangePercent(quote);
					context.append(String.format("  %s (%s): %s\n", quote.getSymbol(), sector, changeStr));
				}
				context.append("\n");

				// Identify leaders and laggards
				if (sectorQuotes.size() >= 3) {
					context.append("Sector Signals:\n");
					context.append("  Leaders: ")
						.append(sectorQuotes.get(0).getSymbol())
						.append(", ")
						.append(sectorQuotes.get(1).getSymbol())
						.append(", ")
						.append(sectorQuotes.get(2).getSymbol())
						.append("\n");
					context.append("  Laggards: ")
						.append(sectorQuotes.get(sectorQuotes.size() - 1).getSymbol())
						.append(", ")
						.append(sectorQuotes.get(sectorQuotes.size() - 2).getSymbol())
						.append(", ")
						.append(sectorQuotes.get(sectorQuotes.size() - 3).getSymbol())
						.append("\n");
				}
				context.append("\n");
			}
			else {
				appendFallbackSectorAnalysis(context);
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch sector quotes: {}", e.getMessage());
			appendFallbackSectorAnalysis(context);
		}

		// Sector rotation guidance
		context.append("Sector Rotation Cycle:\n");
		context.append("  Early Recovery: Consumer Disc., Financials, Industrials\n");
		context.append("  Mid-Cycle: Technology, Industrials, Materials\n");
		context.append("  Late Cycle: Energy, Healthcare, Consumer Staples\n");
		context.append("  Recession: Utilities, Healthcare, Consumer Staples\n\n");
	}

	private void appendFallbackSectorAnalysis(StringBuilder context) {
		context.append("Monitor these sector ETFs for rotation signals:\n\n");
		context.append("| Sector | ETF | Description |\n");
		context.append("|--------|-----|-------------|\n");
		context.append("| Technology | XLK | Tech giants, semiconductors |\n");
		context.append("| Financials | XLF | Banks, insurance, asset mgmt |\n");
		context.append("| Energy | XLE | Oil, gas, energy equipment |\n");
		context.append("| Healthcare | XLV | Pharma, biotech, medical devices |\n");
		context.append("| Industrials | XLI | Defense, machinery, airlines |\n");
		context.append("| Consumer Disc. | XLY | Retail, automotive, leisure |\n");
		context.append("| Consumer Staples | XLP | Food, beverage, household |\n");
		context.append("| Real Estate | XLRE | REITs, property |\n");
		context.append("| Utilities | XLU | Electric, gas, water |\n");
		context.append("| Materials | XLB | Chemicals, mining, paper |\n");
		context.append("| Communication | XLC | Media, telecom, internet |\n");
		context.append("\n[Live data: API not configured]\n\n");
	}

	private void appendSectorGuidance(StringBuilder context) {
		context.append("SECTOR ANALYSIS GUIDANCE:\n");
		context.append("-".repeat(50)).append("\n");
		context.append("When analyzing sector rotation, consider:\n\n");
		context.append("1. Relative Strength: Compare sector performance vs SPY\n");
		context.append("2. Volume Trends: Rising volume with price = conviction\n");
		context.append("3. Leadership: Which sectors are outperforming this week/month?\n");
		context.append("4. Laggards: Which sectors are underperforming (potential catch-up trades)?\n");
		context.append("5. Economic Cycle: Current phase suggests focus on specific sectors\n\n");
	}

	private void appendMarketNews(StringBuilder context) {
		if (!newsClient.isConfigured()) {
			context.append("MARKET NEWS: [FMP API not configured - provide API key for real-time news]\n\n");
			return;
		}

		try {
			List<FmpNewsArticle> marketNews = newsClient.getGeneralNews();
			if (!marketNews.isEmpty()) {
				context.append("MARKET-MOVING NEWS:\n");
				context.append("-".repeat(50)).append("\n");
				context.append(newsClient.formatNewsForContext(marketNews, 8));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch market news: {}", e.getMessage());
			context.append("MARKET NEWS: [Unable to fetch - service temporarily unavailable]\n\n");
		}
	}

	private void appendWatchlistContext(StringBuilder context, List<String> symbols) {
		context.append("YOUR WATCHLIST:\n");
		context.append("-".repeat(50)).append("\n");
		context.append("Tracking: ").append(String.join(", ", symbols)).append("\n\n");

		// Get live quotes for watchlist
		if (quoteClient.isConfigured()) {
			try {
				List<FmpQuote> quotes = quoteClient.getBatchQuotes(symbols);
				if (!quotes.isEmpty()) {
					context.append("Current Prices:\n");
					for (FmpQuote quote : quotes) {
						context.append("  ").append(quote.toDetailedContextString()).append("\n");
					}
					context.append("\n");
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch watchlist quotes: {}", e.getMessage());
			}
		}

		// Get news for watchlist symbols
		if (newsClient.isConfigured()) {
			try {
				List<FmpNewsArticle> watchlistNews = newsClient.getStockNews(symbols, 5);
				if (!watchlistNews.isEmpty()) {
					context.append("WATCHLIST NEWS:\n");
					context.append(newsClient.formatNewsForContext(watchlistNews, 5));
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch watchlist news: {}", e.getMessage());
			}
		}

		// Get technical summary for top 3 watchlist symbols
		if (technicalClient.isConfigured() && !symbols.isEmpty()) {
			try {
				context.append("TECHNICAL SUMMARY:\n");
				int count = 0;
				for (String symbol : symbols) {
					if (count >= 3) {
						break;
					}
					try {
						String summary = technicalClient.getTechnicalSummary(symbol);
						context.append(summary).append("\n");
						count++;
					}
					catch (Exception e) {
						log.debug("Failed to get technical summary for {}: {}", symbol, e.getMessage());
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch technical summaries: {}", e.getMessage());
			}
		}
	}

	/**
	 * Check if the provider is configured with API key.
	 */
	public boolean isConfigured() {
		return newsClient.isConfigured() || quoteClient.isConfigured();
	}

	/**
	 * Interpret VIX value for market sentiment.
	 */
	private String interpretVix(double vixValue) {
		if (vixValue < 12) {
			return "Very low - extreme complacency";
		}
		else if (vixValue < 15) {
			return "Low - complacency";
		}
		else if (vixValue < 20) {
			return "Normal volatility";
		}
		else if (vixValue < 25) {
			return "Elevated - caution";
		}
		else if (vixValue < 30) {
			return "High - fear";
		}
		else {
			return "Very high - extreme fear";
		}
	}

	/**
	 * Get sector name from ETF symbol.
	 */
	private String getSectorName(String symbol) {
		return switch (symbol) {
			case "XLK" -> "Technology";
			case "XLF" -> "Financials";
			case "XLE" -> "Energy";
			case "XLV" -> "Healthcare";
			case "XLI" -> "Industrials";
			case "XLY" -> "Consumer Disc.";
			case "XLP" -> "Consumer Staples";
			case "XLRE" -> "Real Estate";
			case "XLU" -> "Utilities";
			case "XLB" -> "Materials";
			case "XLC" -> "Communication";
			default -> symbol;
		};
	}

	/**
	 * Format change percent for display.
	 */
	private String formatChangePercent(FmpQuote quote) {
		if (quote.getChangePercent() == null) {
			return "N/A";
		}
		String sign = quote.getChangePercent().doubleValue() >= 0 ? "+" : "";
		return String.format("%s%.2f%%", sign, quote.getChangePercent());
	}

}
