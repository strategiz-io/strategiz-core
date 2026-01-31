package io.strategiz.client.fmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.fmp.config.FmpConfig;
import io.strategiz.client.fmp.dto.FmpNewsArticle;
import io.strategiz.client.fmp.dto.FmpPressRelease;
import io.strategiz.client.fmp.error.FmpErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for fetching news data from Financial Modeling Prep API.
 *
 * <p>
 * Note: This class is NOT a @Component. It is instantiated as a @Bean by ClientFmpConfig
 * to ensure proper dependency ordering (fmpRestTemplate and fmpRateLimiter must exist
 * first).
 * </p>
 *
 * <p>
 * FMP News Endpoints:
 * <ul>
 * <li>Stock News: /api/v3/stock_news</li>
 * <li>General News: /api/v4/general_news</li>
 * <li>Press Releases: /api/v3/press-releases/{symbol}</li>
 * <li>Forex News: /api/v4/forex_news</li>
 * <li>Crypto News: /api/v4/crypto_news</li>
 * </ul>
 * </p>
 *
 * <p>
 * FMP API Documentation: https://site.financialmodelingprep.com/developer/docs
 * </p>
 */
public class FmpNewsClient {

	private static final Logger log = LoggerFactory.getLogger(FmpNewsClient.class);

	private static final int DEFAULT_LIMIT = 50;

	private static final int MAX_LIMIT = 100;

	private final FmpConfig config;

	private final RestTemplate restTemplate;

	private final Bucket rateLimiter;

	private final ObjectMapper objectMapper;

	public FmpNewsClient(FmpConfig config, RestTemplate restTemplate, Bucket rateLimiter, ObjectMapper objectMapper) {
		this.config = config;
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get stock news for specific symbols.
	 * @param symbols List of stock symbols (e.g., ["AAPL", "MSFT"])
	 * @param limit Maximum number of articles to return (default: 50, max: 100)
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getStockNews(List<String> symbols, int limit) {
		if (symbols == null || symbols.isEmpty()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbols list cannot be null or empty");
		}

		ensureConfigured();
		int effectiveLimit = Math.min(Math.max(1, limit), MAX_LIMIT);

		String tickers = symbols.stream().map(String::toUpperCase).collect(Collectors.joining(","));

		log.debug("Fetching stock news for symbols: {} (limit: {})", tickers, effectiveLimit);

		String url = String.format("%s/stock_news?tickers=%s&limit=%d&apikey=%s", config.getBaseUrl(), tickers,
				effectiveLimit, config.getApiKey());

		return fetchNews(url, "stock news for " + tickers);
	}

	/**
	 * Get stock news for a single symbol.
	 * @param symbol Stock symbol
	 * @param limit Maximum number of articles to return
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getStockNews(String symbol, int limit) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbol cannot be null or empty");
		}
		return getStockNews(List.of(symbol), limit);
	}

	/**
	 * Get stock news for a single symbol with default limit.
	 * @param symbol Stock symbol
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getStockNews(String symbol) {
		return getStockNews(symbol, DEFAULT_LIMIT);
	}

	/**
	 * Get general market news.
	 * @param page Page number (0-indexed)
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getGeneralNews(int page) {
		ensureConfigured();

		log.debug("Fetching general market news (page: {})", page);

		// General news uses v4 endpoint
		String baseUrl = config.getBaseUrl().replace("/v3", "/v4");
		String url = String.format("%s/general_news?page=%d&apikey=%s", baseUrl, Math.max(0, page), config.getApiKey());

		return fetchNews(url, "general market news");
	}

	/**
	 * Get general market news (first page).
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getGeneralNews() {
		return getGeneralNews(0);
	}

	/**
	 * Get forex news.
	 * @param page Page number (0-indexed)
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getForexNews(int page) {
		ensureConfigured();

		log.debug("Fetching forex news (page: {})", page);

		String baseUrl = config.getBaseUrl().replace("/v3", "/v4");
		String url = String.format("%s/forex_news?page=%d&apikey=%s", baseUrl, Math.max(0, page), config.getApiKey());

		return fetchNews(url, "forex news");
	}

	/**
	 * Get forex news (first page).
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getForexNews() {
		return getForexNews(0);
	}

	/**
	 * Get crypto news.
	 * @param page Page number (0-indexed)
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getCryptoNews(int page) {
		ensureConfigured();

		log.debug("Fetching crypto news (page: {})", page);

		String baseUrl = config.getBaseUrl().replace("/v3", "/v4");
		String url = String.format("%s/crypto_news?page=%d&apikey=%s", baseUrl, Math.max(0, page), config.getApiKey());

		return fetchNews(url, "crypto news");
	}

	/**
	 * Get crypto news (first page).
	 * @return List of news articles
	 */
	public List<FmpNewsArticle> getCryptoNews() {
		return getCryptoNews(0);
	}

	/**
	 * Get press releases for a symbol.
	 * @param symbol Stock symbol
	 * @param limit Maximum number of press releases to return
	 * @return List of press releases
	 */
	public List<FmpPressRelease> getPressReleases(String symbol, int limit) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbol cannot be null or empty");
		}

		ensureConfigured();
		int effectiveLimit = Math.min(Math.max(1, limit), MAX_LIMIT);

		log.debug("Fetching press releases for symbol: {} (limit: {})", symbol, effectiveLimit);

		String url = String.format("%s/press-releases/%s?limit=%d&apikey=%s", config.getBaseUrl(), symbol.toUpperCase(),
				effectiveLimit, config.getApiKey());

		return fetchPressReleases(url, symbol);
	}

	/**
	 * Get press releases for a symbol with default limit.
	 * @param symbol Stock symbol
	 * @return List of press releases
	 */
	public List<FmpPressRelease> getPressReleases(String symbol) {
		return getPressReleases(symbol, DEFAULT_LIMIT);
	}

	/**
	 * Get combined news for multiple symbols (news + press releases).
	 * @param symbols List of stock symbols
	 * @param newsLimit Limit for news articles
	 * @param pressReleaseLimit Limit for press releases per symbol
	 * @return Combined list of news articles and press releases formatted for context
	 */
	public String getCombinedNewsContext(List<String> symbols, int newsLimit, int pressReleaseLimit) {
		StringBuilder context = new StringBuilder();

		// Get stock news
		try {
			List<FmpNewsArticle> news = getStockNews(symbols, newsLimit);
			if (!news.isEmpty()) {
				context.append("=== STOCK NEWS ===\n\n");
				for (FmpNewsArticle article : news) {
					context.append(article.toContextString()).append("\n\n");
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch stock news: {}", e.getMessage());
		}

		// Get press releases for each symbol
		for (String symbol : symbols) {
			try {
				List<FmpPressRelease> releases = getPressReleases(symbol, pressReleaseLimit);
				if (!releases.isEmpty()) {
					context.append("=== PRESS RELEASES (").append(symbol).append(") ===\n\n");
					for (FmpPressRelease release : releases) {
						context.append(release.toContextString()).append("\n\n");
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch press releases for {}: {}", symbol, e.getMessage());
			}
		}

		return context.toString();
	}

	/**
	 * Fetch news articles from the given URL.
	 */
	private List<FmpNewsArticle> fetchNews(String url, String description) {
		try {
			waitForRateLimiter();

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				log.warn("No data returned for {}", description);
				return new ArrayList<>();
			}

			List<FmpNewsArticle> articles = objectMapper.readValue(response.getBody(),
					new TypeReference<List<FmpNewsArticle>>() {
					});

			log.info("Successfully fetched {} articles for {}", articles != null ? articles.size() : 0, description);
			return articles != null ? articles : new ArrayList<>();

		}
		catch (HttpClientErrorException ex) {
			handleHttpError(ex, description);
			return new ArrayList<>();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while waiting for rate limiter", ex);
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limiter interrupted", ex);
		}
		catch (StrategizException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.error("Failed to fetch {}: {}", description, ex.getMessage(), ex);
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("Failed to fetch %s: %s", description, ex.getMessage()), ex);
		}
	}

	/**
	 * Fetch press releases from the given URL.
	 */
	private List<FmpPressRelease> fetchPressReleases(String url, String symbol) {
		try {
			waitForRateLimiter();

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				log.warn("No press releases returned for symbol: {}", symbol);
				return new ArrayList<>();
			}

			List<FmpPressRelease> releases = objectMapper.readValue(response.getBody(),
					new TypeReference<List<FmpPressRelease>>() {
					});

			log.info("Successfully fetched {} press releases for {}", releases != null ? releases.size() : 0, symbol);
			return releases != null ? releases : new ArrayList<>();

		}
		catch (HttpClientErrorException ex) {
			handleHttpError(ex, "press releases for " + symbol);
			return new ArrayList<>();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while waiting for rate limiter", ex);
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limiter interrupted", ex);
		}
		catch (StrategizException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.error("Failed to fetch press releases for {}: {}", symbol, ex.getMessage(), ex);
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("Failed to fetch press releases for %s: %s", symbol, ex.getMessage()), ex);
		}
	}

	/**
	 * Check if the client is properly configured with an API key.
	 * @return true if configured
	 */
	public boolean isConfigured() {
		return config.isConfigured();
	}

	/**
	 * Format news articles for AI context injection.
	 * @param articles List of news articles
	 * @param limit Maximum number of articles to include
	 * @return Formatted context string
	 */
	public String formatNewsForContext(List<FmpNewsArticle> articles, int limit) {
		if (articles == null || articles.isEmpty()) {
			return "No news articles available.\n";
		}

		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (FmpNewsArticle article : articles) {
			if (count >= limit) {
				break;
			}
			sb.append(article.toContextString()).append("\n\n");
			count++;
		}
		return sb.toString();
	}

	/**
	 * Format press releases for AI context injection.
	 * @param releases List of press releases
	 * @param limit Maximum number to include
	 * @return Formatted context string
	 */
	public String formatPressReleasesForContext(List<FmpPressRelease> releases, int limit) {
		if (releases == null || releases.isEmpty()) {
			return "No press releases available.\n";
		}

		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (FmpPressRelease release : releases) {
			if (count >= limit) {
				break;
			}
			sb.append(release.toContextString()).append("\n\n");
			count++;
		}
		return sb.toString();
	}

	/**
	 * Wait for rate limiter token.
	 */
	private void waitForRateLimiter() throws InterruptedException {
		if (!rateLimiter.tryConsume(1)) {
			log.debug("Rate limit reached, waiting for token...");
			boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
			if (!acquired) {
				throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API");
			}
		}
	}

	/**
	 * Ensure API is configured.
	 */
	private void ensureConfigured() {
		if (!config.isConfigured()) {
			throw new StrategizException(FmpErrorDetails.API_KEY_MISSING, "FMP API key is not configured");
		}
	}

	/**
	 * Handle HTTP errors from FMP API.
	 */
	private void handleHttpError(HttpClientErrorException ex, String description) {
		if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
			log.warn("Rate limit exceeded for FMP API (429)");
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API", ex);
		}
		else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			log.error("Unauthorized access to FMP API - check API key");
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE, "Invalid FMP API key", ex);
		}
		else {
			log.error("FMP API error for {}: {} - {}", description, ex.getStatusCode(), ex.getMessage());
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("FMP API error: %s", ex.getMessage()), ex);
		}
	}

}
