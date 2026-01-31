package io.strategiz.service.agents.context;

import io.strategiz.business.fundamentals.service.FundamentalsQueryService;
import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.client.fmp.dto.FmpEarningsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Provides comprehensive earnings context for AI agents. Combines FMP earnings calendar
 * with historical fundamentals data.
 */
@Component
public class EarningsContextProvider {

	private static final Logger log = LoggerFactory.getLogger(EarningsContextProvider.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private static final int DEFAULT_CALENDAR_DAYS = 14;

	private static final int DEFAULT_HISTORICAL_DAYS = 30;

	private final FmpFundamentalsClient earningsClient;

	private final FundamentalsQueryService fundamentalsQueryService;

	@Autowired
	public EarningsContextProvider(FmpFundamentalsClient earningsClient,
			Optional<FundamentalsQueryService> fundamentalsQueryService) {
		this.earningsClient = earningsClient;
		this.fundamentalsQueryService = fundamentalsQueryService.orElse(null);
		if (this.fundamentalsQueryService == null) {
			log.info("FundamentalsQueryService not available - fundamentals context will be limited");
		}
	}

	/**
	 * Build comprehensive earnings context for AI injection.
	 * @param symbols List of symbols the user is interested in
	 * @param focusType Type of focus (calendar, historical, or both)
	 * @return Formatted context string for AI consumption
	 */
	public String buildEarningsContext(List<String> symbols, String focusType) {
		StringBuilder context = new StringBuilder();

		// Add timestamp
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		// Always include upcoming earnings calendar
		appendUpcomingEarningsCalendar(context, symbols);

		// If specific symbols provided, add their historical data
		if (symbols != null && !symbols.isEmpty()) {
			appendSymbolEarningsHistory(context, symbols);
		}

		// Add recent earnings results (surprises, beats/misses)
		appendRecentEarningsSummary(context);

		return context.toString();
	}

	/**
	 * Build context focused on a specific symbol's earnings.
	 */
	public String buildSymbolEarningsContext(String symbol) {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
		context.append("EARNINGS FOCUS: ").append(symbol.toUpperCase()).append("\n\n");

		// Symbol-specific upcoming earnings
		appendSymbolUpcomingEarnings(context, symbol);

		// Historical fundamentals for this symbol
		appendSymbolFundamentals(context, symbol);

		// Recent earnings results for the symbol
		appendSymbolRecentEarnings(context, symbol);

		return context.toString();
	}

	/**
	 * Build context focused on upcoming earnings only.
	 */
	public String buildUpcomingEarningsContext() {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		appendUpcomingEarningsCalendar(context, null);
		appendRecentEarningsSummary(context);

		return context.toString();
	}

	private void appendUpcomingEarningsCalendar(StringBuilder context, List<String> filterSymbols) {
		if (!earningsClient.isConfigured()) {
			context.append(
					"UPCOMING EARNINGS: [Earnings API not configured - provide API key for real-time calendar]\n\n");
			return;
		}

		try {
			List<FmpEarningsEvent> upcoming;

			if (filterSymbols != null && !filterSymbols.isEmpty()) {
				// Get earnings for specific symbols
				upcoming = earningsClient.getEarningsForSymbols(filterSymbols, DEFAULT_CALENDAR_DAYS);
			}
			else {
				// Get all upcoming earnings
				upcoming = earningsClient.getUpcomingEarnings(DEFAULT_CALENDAR_DAYS);
			}

			if (!upcoming.isEmpty()) {
				context.append("UPCOMING EARNINGS (Next ").append(DEFAULT_CALENDAR_DAYS).append(" days):\n");
				context.append("-".repeat(60)).append("\n");
				context.append(earningsClient.formatEarningsForContext(upcoming, true));
				context.append("\n");
			}
			else {
				if (filterSymbols != null && !filterSymbols.isEmpty()) {
					context.append("UPCOMING EARNINGS: No earnings scheduled for your watchlist in the next ");
					context.append(DEFAULT_CALENDAR_DAYS).append(" days\n\n");
				}
				else {
					context.append("UPCOMING EARNINGS: [No major earnings scheduled in the next ");
					context.append(DEFAULT_CALENDAR_DAYS).append(" days]\n\n");
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch upcoming earnings: {}", e.getMessage());
			context.append("UPCOMING EARNINGS: [Unable to fetch calendar - service temporarily unavailable]\n\n");
		}
	}

	private void appendSymbolUpcomingEarnings(StringBuilder context, String symbol) {
		if (!earningsClient.isConfigured()) {
			context.append("UPCOMING EARNINGS: [API not configured]\n\n");
			return;
		}

		try {
			List<FmpEarningsEvent> upcoming = earningsClient.getEarningsForSymbols(List.of(symbol), 60); // Look
																											// 60
																											// days
																											// ahead
																											// for
																											// specific
																											// symbol

			if (!upcoming.isEmpty()) {
				context.append("UPCOMING EARNINGS:\n");
				context.append("-".repeat(50)).append("\n");
				context.append(earningsClient.formatEarningsForContext(upcoming, true));
				context.append("\n");
			}
			else {
				context.append("UPCOMING EARNINGS: No earnings scheduled for ").append(symbol);
				context.append(" in the next 60 days\n\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch upcoming earnings for {}: {}", symbol, e.getMessage());
			context.append("UPCOMING EARNINGS: [Unable to fetch]\n\n");
		}
	}

	private void appendSymbolEarningsHistory(StringBuilder context, List<String> symbols) {
		context.append("HISTORICAL EARNINGS DATA:\n");
		context.append("-".repeat(60)).append("\n");

		for (String symbol : symbols) {
			appendSymbolFundamentals(context, symbol);
		}
	}

	private void appendSymbolFundamentals(StringBuilder context, String symbol) {
		if (fundamentalsQueryService == null) {
			context.append("\n").append(symbol.toUpperCase()).append(": [Fundamentals service not available]\n");
			return;
		}

		try {
			var fundamentals = fundamentalsQueryService.getLatestFundamentalsOrNull(symbol);
			if (fundamentals != null) {
				context.append("\n").append(symbol.toUpperCase()).append(" Fundamentals:\n");
				context.append("  • EPS (Diluted): $").append(formatNumber(fundamentals.getEpsDiluted())).append("\n");
				context.append("  • EPS Growth YoY: ")
					.append(formatPercent(fundamentals.getEpsGrowthYoy()))
					.append("\n");
				context.append("  • P/E Ratio: ").append(formatNumber(fundamentals.getPriceToEarnings())).append("\n");

				if (fundamentals.getRevenueGrowthYoy() != null) {
					context.append("  • Revenue Growth YoY: ")
						.append(formatPercent(fundamentals.getRevenueGrowthYoy()))
						.append("\n");
				}
				if (fundamentals.getProfitMargin() != null) {
					context.append("  • Profit Margin: ")
						.append(formatPercent(fundamentals.getProfitMargin()))
						.append("\n");
				}

				context.append("\n");
			}
			else {
				context.append("\n").append(symbol.toUpperCase()).append(": No fundamentals data available\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to get fundamentals for {}: {}", symbol, e.getMessage());
			context.append("\n").append(symbol.toUpperCase()).append(": [Unable to fetch fundamentals]\n");
		}
	}

	private void appendSymbolRecentEarnings(StringBuilder context, String symbol) {
		if (!earningsClient.isConfigured()) {
			return;
		}

		try {
			// Get recent earnings results for this symbol
			List<FmpEarningsEvent> recent = earningsClient.getRecentEarnings(90)
				.stream()
				.filter(e -> symbol.equalsIgnoreCase(e.getSymbol()))
				.filter(e -> e.getEpsActual() != null)
				.limit(4) // Last 4 quarters
				.toList();

			if (!recent.isEmpty()) {
				context.append("RECENT EARNINGS RESULTS (Last 4 Quarters):\n");
				context.append("-".repeat(50)).append("\n");
				context.append(earningsClient.formatEarningsForContext(recent, false));
				context.append("\n");

				// Calculate beat rate
				long beats = recent.stream()
					.filter(e -> e.getEpsSurprisePercent() != null && e.getEpsSurprisePercent() > 0)
					.count();
				context.append(String.format("Beat Rate: %d/%d (%.0f%%)\n\n", beats, recent.size(),
						(beats * 100.0 / recent.size())));
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch recent earnings for {}: {}", symbol, e.getMessage());
		}
	}

	private void appendRecentEarningsSummary(StringBuilder context) {
		if (!earningsClient.isConfigured()) {
			return;
		}

		try {
			// Get overall earnings summary stats
			String summary = earningsClient.getEarningsSummaryStats(DEFAULT_HISTORICAL_DAYS);
			context.append(summary);
			context.append("\n");
		}
		catch (Exception e) {
			log.warn("Failed to fetch earnings summary: {}", e.getMessage());
		}
	}

	private String formatNumber(Number value) {
		if (value == null) {
			return "N/A";
		}
		return String.format("%.2f", value.doubleValue());
	}

	private String formatPercent(Number value) {
		if (value == null) {
			return "N/A";
		}
		return String.format("%.1f%%", value.doubleValue());
	}

	/**
	 * Check if the provider is configured with API key.
	 */
	public boolean isConfigured() {
		return earningsClient.isConfigured();
	}

}
