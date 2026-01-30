package io.strategiz.service.agents.context;

import io.strategiz.client.fmp.client.FmpFilingsClient;
import io.strategiz.client.fmp.client.FmpFundamentalsClient;
import io.strategiz.client.fmp.client.FmpNewsClient;
import io.strategiz.client.fmp.dto.FmpEarningsEvent;
import io.strategiz.client.fmp.dto.FmpNewsArticle;
import io.strategiz.client.fmp.dto.FmpPressRelease;
import io.strategiz.client.fmp.dto.FmpSECFiling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Provides real news context for AI agents using FMP API for news, earnings, and filings.
 *
 * <p>
 * Data sources:
 * <ul>
 * <li>FMP: Stock news, general market news, forex/crypto news, press releases</li>
 * <li>FMP: SEC filings, earnings calendar</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "strategiz.fmp.enabled", havingValue = "true")
public class NewsContextProvider {

	private static final Logger log = LoggerFactory.getLogger(NewsContextProvider.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private static final int DEFAULT_NEWS_LIMIT = 10;

	private static final int DEFAULT_PRESS_RELEASE_LIMIT = 5;

	private static final int DEFAULT_FILINGS_DAYS = 30;

	private final FmpNewsClient fmpNewsClient;

	private final FmpFilingsClient filingsClient;

	private final FmpFundamentalsClient fundamentalsClient;

	public NewsContextProvider(@Nullable FmpNewsClient fmpNewsClient, @Nullable FmpFilingsClient filingsClient,
			@Nullable FmpFundamentalsClient fundamentalsClient) {
		this.fmpNewsClient = fmpNewsClient;
		this.filingsClient = filingsClient;
		this.fundamentalsClient = fundamentalsClient;
	}

	/**
	 * Build comprehensive news context for AI injection.
	 * @param symbols List of symbols the user is interested in (watchlist)
	 * @param newsType Type of news to focus on (general, earnings, filings)
	 * @param sector Sector to focus on (optional)
	 * @return Formatted context string for AI consumption
	 */
	public String buildNewsContext(List<String> symbols, String newsType, String sector) {
		StringBuilder context = new StringBuilder();

		// Add timestamp
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		// Always include general market news
		appendMarketNews(context);

		// Add company-specific news if symbols provided
		if (symbols != null && !symbols.isEmpty()) {
			appendCompanyNews(context, symbols);
			appendPressReleases(context, symbols);
			appendSECFilings(context, symbols);
		}

		// Add earnings context if relevant
		if (newsType == null || "earnings".equalsIgnoreCase(newsType)) {
			appendUpcomingEarnings(context);
		}

		return context.toString();
	}

	/**
	 * Build context focused on a specific symbol.
	 */
	public String buildSymbolNewsContext(String symbol) {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
		context.append("NEWS FOCUS: ").append(symbol.toUpperCase()).append("\n\n");

		// Company news from FMP
		appendCompanyNewsForSymbol(context, symbol);

		// Press releases from FMP
		appendPressReleasesForSymbol(context, symbol);

		// SEC filings from FMP
		appendFilingsForSymbol(context, symbol);

		// Symbol-specific earnings from FMP
		if (fundamentalsClient != null && fundamentalsClient.isConfigured()) {
			List<FmpEarningsEvent> earnings = fundamentalsClient.getUpcomingEarnings(30)
				.stream()
				.filter(e -> symbol.equalsIgnoreCase(e.getSymbol()))
				.toList();

			if (!earnings.isEmpty()) {
				context.append("UPCOMING EARNINGS FOR ").append(symbol.toUpperCase()).append(":\n");
				context.append(fundamentalsClient.formatEarningsForContext(earnings, true));
				context.append("\n");
			}
		}

		return context.toString();
	}

	/**
	 * Build context for breaking/market-wide news only.
	 */
	public String buildMarketNewsContext() {
		StringBuilder context = new StringBuilder();
		context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

		appendMarketNews(context);
		appendUpcomingEarnings(context);

		return context.toString();
	}

	private void appendMarketNews(StringBuilder context) {
		if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
			context.append("MARKET NEWS: [News API not configured - enable FMP integration for real-time news]\n\n");
			return;
		}

		try {
			List<FmpNewsArticle> marketNews = fmpNewsClient.getGeneralNews();
			if (!marketNews.isEmpty()) {
				context.append("MARKET NEWS (Latest):\n");
				context.append("-".repeat(50)).append("\n");
				context.append(fmpNewsClient.formatNewsForContext(marketNews, DEFAULT_NEWS_LIMIT));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch market news: {}", e.getMessage());
			context.append("MARKET NEWS: [Unable to fetch - service temporarily unavailable]\n\n");
		}
	}

	private void appendCompanyNews(StringBuilder context, List<String> symbols) {
		if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
			return;
		}

		try {
			List<FmpNewsArticle> companyNews = fmpNewsClient.getStockNews(symbols, DEFAULT_NEWS_LIMIT);
			if (!companyNews.isEmpty()) {
				context.append("WATCHLIST NEWS:\n");
				context.append("Tracking: ").append(String.join(", ", symbols)).append("\n");
				context.append("-".repeat(50)).append("\n");
				context.append(fmpNewsClient.formatNewsForContext(companyNews, DEFAULT_NEWS_LIMIT));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch company news: {}", e.getMessage());
		}
	}

	private void appendCompanyNewsForSymbol(StringBuilder context, String symbol) {
		if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
			context.append("COMPANY NEWS: [News API not configured]\n\n");
			return;
		}

		try {
			List<FmpNewsArticle> news = fmpNewsClient.getStockNews(symbol, 15);
			if (!news.isEmpty()) {
				context.append("RECENT NEWS:\n");
				context.append("-".repeat(50)).append("\n");
				context.append(fmpNewsClient.formatNewsForContext(news, 15));
				context.append("\n");
			}
			else {
				context.append("RECENT NEWS: No recent news found for ").append(symbol).append("\n\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
			context.append("RECENT NEWS: [Unable to fetch]\n\n");
		}
	}

	private void appendPressReleases(StringBuilder context, List<String> symbols) {
		if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
			return;
		}

		StringBuilder pressContext = new StringBuilder();
		int totalReleases = 0;

		for (String symbol : symbols) {
			try {
				List<FmpPressRelease> releases = fmpNewsClient.getPressReleases(symbol, 3);
				if (!releases.isEmpty()) {
					totalReleases += releases.size();
					for (FmpPressRelease release : releases) {
						pressContext.append(release.toContextString()).append("\n\n");
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch press releases for {}: {}", symbol, e.getMessage());
			}
		}

		if (totalReleases > 0) {
			context.append("PRESS RELEASES:\n");
			context.append("-".repeat(50)).append("\n");
			context.append(pressContext);
		}
	}

	private void appendPressReleasesForSymbol(StringBuilder context, String symbol) {
		if (fmpNewsClient == null || !fmpNewsClient.isConfigured()) {
			return;
		}

		try {
			List<FmpPressRelease> releases = fmpNewsClient.getPressReleases(symbol, DEFAULT_PRESS_RELEASE_LIMIT);
			if (!releases.isEmpty()) {
				context.append("PRESS RELEASES:\n");
				context.append("-".repeat(50)).append("\n");
				context.append(fmpNewsClient.formatPressReleasesForContext(releases, DEFAULT_PRESS_RELEASE_LIMIT));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch press releases for {}: {}", symbol, e.getMessage());
		}
	}

	private void appendSECFilings(StringBuilder context, List<String> symbols) {
		if (filingsClient == null || !filingsClient.isConfigured()) {
			return;
		}

		try {
			List<FmpSECFiling> filings = filingsClient.getFilingsForSymbols(symbols, DEFAULT_FILINGS_DAYS);
			if (!filings.isEmpty()) {
				context.append("RECENT SEC FILINGS:\n");
				context.append("-".repeat(50)).append("\n");
				context.append(filingsClient.formatFilingsForContext(filings));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch SEC filings: {}", e.getMessage());
		}
	}

	private void appendFilingsForSymbol(StringBuilder context, String symbol) {
		if (filingsClient == null || !filingsClient.isConfigured()) {
			context.append("SEC FILINGS: [Filings API not configured]\n\n");
			return;
		}

		try {
			List<FmpSECFiling> filings = filingsClient.getMajorFilings(symbol, 90);
			if (!filings.isEmpty()) {
				context.append("SEC FILINGS (Last 90 days):\n");
				context.append("-".repeat(50)).append("\n");
				context.append(filingsClient.formatFilingsForContext(filings));
				context.append("\n");
			}
			else {
				context.append("SEC FILINGS: No recent filings for ").append(symbol).append("\n\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch filings for {}: {}", symbol, e.getMessage());
		}
	}

	private void appendUpcomingEarnings(StringBuilder context) {
		if (fundamentalsClient == null || !fundamentalsClient.isConfigured()) {
			return;
		}

		try {
			List<FmpEarningsEvent> upcoming = fundamentalsClient.getUpcomingEarnings(7);
			if (!upcoming.isEmpty()) {
				context.append("UPCOMING EARNINGS (Next 7 days):\n");
				context.append("-".repeat(50)).append("\n");
				// Limit to notable names
				List<FmpEarningsEvent> limited = upcoming.stream().limit(15).toList();
				context.append(fundamentalsClient.formatEarningsForContext(limited, true));
				context.append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch upcoming earnings: {}", e.getMessage());
		}
	}

	/**
	 * Check if the provider is configured with API keys.
	 */
	public boolean isConfigured() {
		return fmpNewsClient != null && fmpNewsClient.isConfigured();
	}

}
