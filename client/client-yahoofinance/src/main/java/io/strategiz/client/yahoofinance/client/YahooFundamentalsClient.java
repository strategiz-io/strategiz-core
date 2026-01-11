package io.strategiz.client.yahoofinance.client;

import io.strategiz.client.yahoofinance.error.YahooFinanceErrorDetails;
import io.strategiz.client.yahoofinance.model.YahooBalanceSheet;
import io.strategiz.client.yahoofinance.model.YahooFinancialData;
import io.strategiz.client.yahoofinance.model.YahooFundamentals;
import io.strategiz.client.yahoofinance.model.YahooIncomeStatement;
import io.strategiz.client.yahoofinance.model.YahooKeyStatistics;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching company fundamental data from Yahoo Finance API.
 *
 * <p>
 * This client uses Yahoo Finance's unofficial API to fetch fundamental financial data including:
 * - Financial ratios (P/E, P/B, margins, ROE, etc.)
 * - Balance sheet data (assets, liabilities, equity)
 * - Income statement data (revenue, EBITDA, net income)
 * - Key statistics (market cap, EPS, shares outstanding)
 * </p>
 *
 * <p>
 * Rate Limiting: Implements configurable delays between requests (default 150ms) to avoid
 * overwhelming Yahoo's servers. Supports exponential backoff on failures.
 * </p>
 *
 * <p>
 * Error Handling: Uses structured error codes from {@link YahooFinanceErrorDetails} and
 * throws {@link StrategizException} for all failures.
 * </p>
 */
@Component
public class YahooFundamentalsClient {

	private static final Logger log = LoggerFactory.getLogger(YahooFundamentalsClient.class);

	private final RestTemplate restTemplate;

	@Value("${yahoo.finance.base-url:https://query2.finance.yahoo.com}")
	private String baseUrl;

	@Value("${yahoo.finance.delay-ms:150}")
	private long delayMs;

	@Value("${yahoo.finance.max-retries:5}")
	private int maxRetries;

	@Value("${yahoo.finance.retry-delay-ms:30000}")
	private long retryDelayMs;

	@Value("${yahoo.finance.timeout-ms:10000}")
	private long timeoutMs;

	// Cookie and crumb for Yahoo Finance authentication
	private volatile String cookie;

	private volatile String crumb;

	private volatile long crumbExpiration = 0;

	public YahooFundamentalsClient(RestTemplate yahooFinanceRestTemplate) {
		this.restTemplate = yahooFinanceRestTemplate;
	}

	/**
	 * Fetch all fundamental data for a single symbol.
	 *
	 * @param symbol Stock symbol (e.g., "AAPL", "MSFT")
	 * @return YahooFundamentals with all available data
	 * @throws StrategizException if the request fails
	 */
	public YahooFundamentals getFundamentals(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(YahooFinanceErrorDetails.INVALID_SYMBOL,
					"Symbol cannot be null or empty");
		}

		log.debug("Fetching fundamentals for symbol: {}", symbol);

		try {
			// Ensure we have valid cookie and crumb
			ensureAuthenticated();

			// Build Yahoo Finance quote summary URL with crumb
			String url = String.format("%s/v10/finance/quoteSummary/%s?crumb=%s&modules="
					+ "financialData,defaultKeyStatistics,balanceSheetHistory,incomeStatementHistory", baseUrl,
					symbol, crumb);

			// Execute request with retries
			ResponseEntity<Map> response = executeWithRetry(url, Map.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				throw new StrategizException(YahooFinanceErrorDetails.NO_DATA_AVAILABLE,
						String.format("No data returned for symbol: %s", symbol));
			}

			// Parse response into YahooFundamentals
			YahooFundamentals fundamentals = parseResponse(symbol, response.getBody());

			log.debug("Successfully fetched fundamentals for {}", symbol);
			return fundamentals;

		}
		catch (HttpClientErrorException ex) {
			// If we get 401, refresh authentication and retry once
			if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				log.warn("Got 401 Unauthorized, refreshing cookie/crumb and retrying");
				refreshAuthentication();
				return getFundamentals(symbol); // Retry once
			}
			else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				throw new StrategizException(YahooFinanceErrorDetails.INVALID_SYMBOL,
						String.format("Symbol not found: %s", symbol), ex);
			}
			else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				throw new StrategizException(YahooFinanceErrorDetails.RATE_LIMIT_EXCEEDED,
						"Rate limit exceeded for Yahoo Finance API", ex);
			}
			else {
				throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
						String.format("API error for symbol %s: %s", symbol, ex.getMessage()), ex);
			}
		}
		catch (HttpServerErrorException ex) {
			throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
					String.format("Yahoo Finance server error for symbol %s", symbol), ex);
		}
		catch (StrategizException ex) {
			throw ex; // Re-throw StrategizException
		}
		catch (Exception ex) {
			throw new StrategizException(YahooFinanceErrorDetails.NETWORK_ERROR,
					String.format("Failed to fetch fundamentals for %s", symbol), ex);
		}
	}

	/**
	 * Fetch fundamentals for multiple symbols with rate limiting.
	 *
	 * @param symbols List of stock symbols
	 * @return Map of symbol to YahooFundamentals (excludes symbols with errors)
	 */
	public Map<String, YahooFundamentals> batchGetFundamentals(List<String> symbols) {
		log.info("Fetching fundamentals for {} symbols", symbols.size());

		Map<String, YahooFundamentals> results = new HashMap<>();
		int successCount = 0;
		int errorCount = 0;

		for (int i = 0; i < symbols.size(); i++) {
			String symbol = symbols.get(i);

			try {
				YahooFundamentals fundamentals = getFundamentals(symbol);
				results.put(symbol, fundamentals);
				successCount++;

				log.debug("Progress: {}/{} symbols processed ({})", i + 1, symbols.size(), symbol);

				// Rate limiting: delay between requests
				if (i < symbols.size() - 1) {
					Thread.sleep(delayMs);
				}
			}
			catch (StrategizException ex) {
				log.warn("Failed to fetch fundamentals for {}: {}", symbol, ex.getMessage());
				errorCount++;
				// Continue with next symbol
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.error("Batch processing interrupted", ex);
				break;
			}
		}

		log.info("Batch complete: {} successful, {} errors", successCount, errorCount);
		return results;
	}

	/**
	 * Ensure we have valid authentication (cookie and crumb).
	 * Refreshes if expired or missing.
	 */
	private synchronized void ensureAuthenticated() {
		long now = System.currentTimeMillis();

		// Check if crumb is expired (valid for 1 hour)
		if (cookie == null || crumb == null || now >= crumbExpiration) {
			log.debug("Cookie/crumb missing or expired, refreshing authentication");
			refreshAuthentication();
		}
	}

	/**
	 * Refresh Yahoo Finance cookie and crumb.
	 */
	private synchronized void refreshAuthentication() {
		try {
			log.info("Fetching new Yahoo Finance cookie and crumb");

			// Step 1: Get cookie from Yahoo Finance homepage
			String homepageUrl = "https://finance.yahoo.com";

			// Create headers with User-Agent for homepage request
			org.springframework.http.HttpHeaders homepageHeaders = new org.springframework.http.HttpHeaders();
			homepageHeaders.set("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
			homepageHeaders.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			homepageHeaders.set("Accept-Language", "en-US,en;q=0.5");

			org.springframework.http.HttpEntity<String> homepageEntity = new org.springframework.http.HttpEntity<>(homepageHeaders);

			ResponseEntity<String> homepageResponse = restTemplate.exchange(homepageUrl,
					org.springframework.http.HttpMethod.GET, homepageEntity, String.class);

			// Extract cookie from Set-Cookie header
			List<String> cookies = homepageResponse.getHeaders().get("Set-Cookie");
			if (cookies == null || cookies.isEmpty()) {
				throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
						"Failed to get cookie from Yahoo Finance");
			}

			// Combine all cookies
			this.cookie = String.join("; ", cookies);

			// Step 2: Get crumb from crumb endpoint
			String crumbUrl = "https://query2.finance.yahoo.com/v1/test/getcrumb";

			// Create headers with cookie
			org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
			headers.set("Cookie", this.cookie);
			headers.set("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

			org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

			ResponseEntity<String> crumbResponse = restTemplate.exchange(crumbUrl, org.springframework.http.HttpMethod.GET,
					entity, String.class);

			if (crumbResponse.getBody() == null || crumbResponse.getBody().isBlank()) {
				throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
						"Failed to get crumb from Yahoo Finance");
			}

			this.crumb = crumbResponse.getBody().trim();

			// Set expiration to 1 hour from now
			this.crumbExpiration = System.currentTimeMillis() + (60 * 60 * 1000);

			log.info("Successfully refreshed Yahoo Finance authentication (crumb: {}, expiration: {})", this.crumb,
					new java.util.Date(this.crumbExpiration));

		}
		catch (Exception ex) {
			log.error("Failed to refresh Yahoo Finance authentication", ex);
			throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
					"Failed to authenticate with Yahoo Finance", ex);
		}
	}

	/**
	 * Execute HTTP request with exponential backoff retry logic.
	 * Includes cookie authentication header.
	 */
	private <T> ResponseEntity<T> executeWithRetry(String url, Class<T> responseType) {
		int attempt = 0;
		long currentDelay = retryDelayMs;

		while (attempt < maxRetries) {
			try {
				// Create headers with cookie
				org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
				if (cookie != null) {
					headers.set("Cookie", cookie);
				}
				headers.set("User-Agent",
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

				org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

				return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, responseType);
			}
			catch (HttpServerErrorException ex) {
				attempt++;
				if (attempt >= maxRetries) {
					throw ex;
				}

				log.warn("Request failed (attempt {}/{}), retrying in {}ms", attempt, maxRetries, currentDelay);

				try {
					Thread.sleep(currentDelay);
					currentDelay *= 2; // Exponential backoff
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new StrategizException(YahooFinanceErrorDetails.NETWORK_ERROR, "Request interrupted", ie);
				}
			}
		}

		throw new StrategizException(YahooFinanceErrorDetails.TIMEOUT_ERROR,
				String.format("Max retries (%d) exceeded", maxRetries));
	}

	/**
	 * Parse Yahoo Finance API response into YahooFundamentals DTO.
	 *
	 * @param symbol Stock symbol
	 * @param responseBody Raw API response
	 * @return Parsed YahooFundamentals
	 */
	@SuppressWarnings("unchecked")
	private YahooFundamentals parseResponse(String symbol, Map<String, Object> responseBody) {
		try {
			Map<String, Object> quoteSummary = (Map<String, Object>) responseBody.get("quoteSummary");
			if (quoteSummary == null) {
				throw new StrategizException(YahooFinanceErrorDetails.PARSE_ERROR, "Missing quoteSummary in response");
			}

			List<Map<String, Object>> results = (List<Map<String, Object>>) quoteSummary.get("result");
			if (results == null || results.isEmpty()) {
				throw new StrategizException(YahooFinanceErrorDetails.NO_DATA_AVAILABLE,
						String.format("No data in quoteSummary.result for %s", symbol));
			}

			Map<String, Object> data = results.get(0);

			YahooFundamentals fundamentals = new YahooFundamentals(symbol);

			// Parse each module if present
			if (data.containsKey("financialData")) {
				fundamentals.setFinancialData(parseFinancialData((Map<String, Object>) data.get("financialData")));
			}
			if (data.containsKey("defaultKeyStatistics")) {
				fundamentals.setKeyStatistics(
						parseKeyStatistics((Map<String, Object>) data.get("defaultKeyStatistics")));
			}
			if (data.containsKey("balanceSheetHistory")) {
				Map<String, Object> bsHistory = (Map<String, Object>) data.get("balanceSheetHistory");
				if (bsHistory != null && bsHistory.containsKey("balanceSheetStatements")) {
					List<Map<String, Object>> statements = (List<Map<String, Object>>) bsHistory
						.get("balanceSheetStatements");
					if (statements != null && !statements.isEmpty()) {
						fundamentals.setBalanceSheet(parseBalanceSheet(statements.get(0)));
					}
				}
			}
			if (data.containsKey("incomeStatementHistory")) {
				Map<String, Object> isHistory = (Map<String, Object>) data.get("incomeStatementHistory");
				if (isHistory != null && isHistory.containsKey("incomeStatementHistory")) {
					List<Map<String, Object>> statements = (List<Map<String, Object>>) isHistory
						.get("incomeStatementHistory");
					if (statements != null && !statements.isEmpty()) {
						fundamentals.setIncomeStatement(parseIncomeStatement(statements.get(0)));
					}
				}
			}

			return fundamentals;
		}
		catch (ClassCastException | NullPointerException ex) {
			throw new StrategizException(YahooFinanceErrorDetails.PARSE_ERROR,
					String.format("Failed to parse response for %s", symbol), ex);
		}
	}

	/**
	 * Parse Financial Data section.
	 */
	@SuppressWarnings("unchecked")
	private YahooFinancialData parseFinancialData(Map<String, Object> data) {
		YahooFinancialData financialData = new YahooFinancialData();

		financialData.setCurrentPrice(extractBigDecimal(data, "currentPrice"));
		financialData.setTargetHighPrice(extractBigDecimal(data, "targetHighPrice"));
		financialData.setTargetLowPrice(extractBigDecimal(data, "targetLowPrice"));
		financialData.setTargetMeanPrice(extractBigDecimal(data, "targetMeanPrice"));
		financialData.setRecommendationMean(extractBigDecimal(data, "recommendationMean"));
		financialData.setNumberOfAnalystOpinions(extractInteger(data, "numberOfAnalystOpinions"));
		financialData.setTotalCash(extractBigDecimal(data, "totalCash"));
		financialData.setTotalCashPerShare(extractBigDecimal(data, "totalCashPerShare"));
		financialData.setEbitda(extractBigDecimal(data, "ebitda"));
		financialData.setTotalDebt(extractBigDecimal(data, "totalDebt"));
		financialData.setQuickRatio(extractBigDecimal(data, "quickRatio"));
		financialData.setCurrentRatio(extractBigDecimal(data, "currentRatio"));
		financialData.setTotalRevenue(extractBigDecimal(data, "totalRevenue"));
		financialData.setDebtToEquity(extractBigDecimal(data, "debtToEquity"));
		financialData.setRevenuePerShare(extractBigDecimal(data, "revenuePerShare"));
		financialData.setReturnOnAssets(extractBigDecimal(data, "returnOnAssets"));
		financialData.setReturnOnEquity(extractBigDecimal(data, "returnOnEquity"));
		financialData.setGrossProfits(extractBigDecimal(data, "grossProfits"));
		financialData.setFreeCashflow(extractBigDecimal(data, "freeCashflow"));
		financialData.setOperatingCashflow(extractBigDecimal(data, "operatingCashflow"));
		financialData.setEarningsGrowth(extractBigDecimal(data, "earningsGrowth"));
		financialData.setRevenueGrowth(extractBigDecimal(data, "revenueGrowth"));
		financialData.setGrossMargins(extractBigDecimal(data, "grossMargins"));
		financialData.setEbitdaMargins(extractBigDecimal(data, "ebitdaMargins"));
		financialData.setOperatingMargins(extractBigDecimal(data, "operatingMargins"));
		financialData.setProfitMargins(extractBigDecimal(data, "profitMargins"));

		return financialData;
	}

	/**
	 * Parse Key Statistics section.
	 */
	@SuppressWarnings("unchecked")
	private YahooKeyStatistics parseKeyStatistics(Map<String, Object> data) {
		YahooKeyStatistics keyStats = new YahooKeyStatistics();

		keyStats.setMarketCap(extractBigDecimal(data, "marketCap"));
		keyStats.setEnterpriseValue(extractBigDecimal(data, "enterpriseValue"));
		keyStats.setTrailingPE(extractBigDecimal(data, "trailingPE"));
		keyStats.setForwardPE(extractBigDecimal(data, "forwardPE"));
		keyStats.setPriceToBook(extractBigDecimal(data, "priceToBook"));
		keyStats.setPriceToSales(extractBigDecimal(data, "priceToSales"));
		keyStats.setPegRatio(extractBigDecimal(data, "pegRatio"));
		keyStats.setEnterpriseToRevenue(extractBigDecimal(data, "enterpriseToRevenue"));
		keyStats.setEnterpriseToEbitda(extractBigDecimal(data, "enterpriseToEbitda"));
		keyStats.setSharesOutstanding(extractLong(data, "sharesOutstanding"));
		keyStats.setFloatShares(extractLong(data, "floatShares"));
		keyStats.setSharesShort(extractLong(data, "sharesShort"));
		keyStats.setShortRatio(extractBigDecimal(data, "shortRatio"));
		keyStats.setShortPercentOfFloat(extractBigDecimal(data, "shortPercentOfFloat"));
		keyStats.setBeta(extractBigDecimal(data, "beta"));
		keyStats.setBookValue(extractBigDecimal(data, "bookValue"));
		keyStats.setPriceToBookValue(extractBigDecimal(data, "priceToBook"));
		keyStats.setTrailingEps(extractBigDecimal(data, "trailingEps"));
		keyStats.setForwardEps(extractBigDecimal(data, "forwardEps"));
		keyStats.setFiftyTwoWeekLow(extractBigDecimal(data, "fiftyTwoWeekLow"));
		keyStats.setFiftyTwoWeekHigh(extractBigDecimal(data, "fiftyTwoWeekHigh"));
		keyStats.setFiftyDayAverage(extractBigDecimal(data, "fiftyDayAverage"));
		keyStats.setTwoHundredDayAverage(extractBigDecimal(data, "twoHundredDayAverage"));
		keyStats.setDividendRate(extractBigDecimal(data, "dividendRate"));
		keyStats.setDividendYield(extractBigDecimal(data, "dividendYield"));
		keyStats.setExDividendDate(extractLong(data, "exDividendDate"));
		keyStats.setPayoutRatio(extractBigDecimal(data, "payoutRatio"));
		keyStats.setFiveYearAvgDividendYield(extractBigDecimal(data, "fiveYearAvgDividendYield"));

		return keyStats;
	}

	/**
	 * Parse Balance Sheet section.
	 */
	@SuppressWarnings("unchecked")
	private YahooBalanceSheet parseBalanceSheet(Map<String, Object> data) {
		YahooBalanceSheet balanceSheet = new YahooBalanceSheet();

		balanceSheet.setTotalAssets(extractBigDecimal(data, "totalAssets"));
		balanceSheet.setTotalLiabilities(extractBigDecimal(data, "totalLiab"));
		balanceSheet.setTotalStockholderEquity(extractBigDecimal(data, "totalStockholderEquity"));
		balanceSheet.setTotalCurrentAssets(extractBigDecimal(data, "totalCurrentAssets"));
		balanceSheet.setTotalCurrentLiabilities(extractBigDecimal(data, "totalCurrentLiabilities"));
		balanceSheet.setCash(extractBigDecimal(data, "cash"));
		balanceSheet.setCashAndCashEquivalents(extractBigDecimal(data, "cashAndCashEquivalents"));
		balanceSheet.setShortTermInvestments(extractBigDecimal(data, "shortTermInvestments"));
		balanceSheet.setNetReceivables(extractBigDecimal(data, "netReceivables"));
		balanceSheet.setInventory(extractBigDecimal(data, "inventory"));
		balanceSheet.setOtherCurrentAssets(extractBigDecimal(data, "otherCurrentAssets"));
		balanceSheet.setPropertyPlantEquipment(extractBigDecimal(data, "propertyPlantEquipment"));
		balanceSheet.setGoodWill(extractBigDecimal(data, "goodWill"));
		balanceSheet.setIntangibleAssets(extractBigDecimal(data, "intangibleAssets"));
		balanceSheet.setLongTermInvestments(extractBigDecimal(data, "longTermInvestments"));
		balanceSheet.setOtherAssets(extractBigDecimal(data, "otherAssets"));
		balanceSheet.setAccountsPayable(extractBigDecimal(data, "accountsPayable"));
		balanceSheet.setShortLongTermDebt(extractBigDecimal(data, "shortLongTermDebt"));
		balanceSheet.setOtherCurrentLiabilities(extractBigDecimal(data, "otherCurrentLiab"));
		balanceSheet.setLongTermDebt(extractBigDecimal(data, "longTermDebt"));
		balanceSheet.setOtherLiabilities(extractBigDecimal(data, "otherLiab"));
		balanceSheet.setCommonStock(extractBigDecimal(data, "commonStock"));
		balanceSheet.setRetainedEarnings(extractBigDecimal(data, "retainedEarnings"));
		balanceSheet.setTreasuryStock(extractBigDecimal(data, "treasuryStock"));
		balanceSheet.setCapitalSurplus(extractBigDecimal(data, "capitalSurplus"));

		return balanceSheet;
	}

	/**
	 * Parse Income Statement section.
	 */
	@SuppressWarnings("unchecked")
	private YahooIncomeStatement parseIncomeStatement(Map<String, Object> data) {
		YahooIncomeStatement incomeStatement = new YahooIncomeStatement();

		incomeStatement.setTotalRevenue(extractBigDecimal(data, "totalRevenue"));
		incomeStatement.setCostOfRevenue(extractBigDecimal(data, "costOfRevenue"));
		incomeStatement.setGrossProfit(extractBigDecimal(data, "grossProfit"));
		incomeStatement.setResearchDevelopment(extractBigDecimal(data, "researchDevelopment"));
		incomeStatement.setSellingGeneralAdministrative(extractBigDecimal(data, "sellingGeneralAdministrative"));
		incomeStatement.setTotalOperatingExpenses(extractBigDecimal(data, "totalOperatingExpenses"));
		incomeStatement.setOperatingIncome(extractBigDecimal(data, "operatingIncome"));
		incomeStatement.setTotalOtherIncomeExpenseNet(extractBigDecimal(data, "totalOtherIncomeExpenseNet"));
		incomeStatement.setEbit(extractBigDecimal(data, "ebit"));
		incomeStatement.setInterestExpense(extractBigDecimal(data, "interestExpense"));
		incomeStatement.setIncomeBeforeTax(extractBigDecimal(data, "incomeBeforeTax"));
		incomeStatement.setIncomeTaxExpense(extractBigDecimal(data, "incomeTaxExpense"));
		incomeStatement.setNetIncome(extractBigDecimal(data, "netIncome"));
		incomeStatement.setNetIncomeApplicableToCommonShares(extractBigDecimal(data, "netIncomeApplicableToCommonShares"));
		incomeStatement.setDiscontinuedOperations(extractBigDecimal(data, "discontinuedOperations"));
		incomeStatement.setExtraordinaryItems(extractBigDecimal(data, "extraordinaryItems"));
		incomeStatement.setEffectOfAccountingCharges(extractBigDecimal(data, "effectOfAccountingCharges"));
		incomeStatement.setOtherItems(extractBigDecimal(data, "otherItems"));
		incomeStatement.setMinorityInterest(extractBigDecimal(data, "minorityInterest"));

		return incomeStatement;
	}

	/**
	 * Extract BigDecimal from Yahoo Finance response.
	 * Yahoo returns numeric values as: {"raw": 123.45, "fmt": "123.45"}
	 */
	@SuppressWarnings("unchecked")
	private java.math.BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
		if (!data.containsKey(key)) {
			return null;
		}

		Object value = data.get(key);
		if (value == null) {
			return null;
		}

		// If it's already a number, convert directly
		if (value instanceof Number) {
			return new java.math.BigDecimal(value.toString());
		}

		// If it's a map with "raw" field, extract the raw value
		if (value instanceof Map) {
			Map<String, Object> valueMap = (Map<String, Object>) value;
			Object rawValue = valueMap.get("raw");
			if (rawValue != null && rawValue instanceof Number) {
				return new java.math.BigDecimal(rawValue.toString());
			}
		}

		return null;
	}

	/**
	 * Extract Integer from Yahoo Finance response.
	 */
	@SuppressWarnings("unchecked")
	private Integer extractInteger(Map<String, Object> data, String key) {
		java.math.BigDecimal decimal = extractBigDecimal(data, key);
		return decimal != null ? decimal.intValue() : null;
	}

	/**
	 * Extract Long from Yahoo Finance response.
	 */
	@SuppressWarnings("unchecked")
	private Long extractLong(Map<String, Object> data, String key) {
		java.math.BigDecimal decimal = extractBigDecimal(data, key);
		return decimal != null ? decimal.longValue() : null;
	}

}
