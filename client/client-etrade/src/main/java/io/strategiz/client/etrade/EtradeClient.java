package io.strategiz.client.etrade;

import io.strategiz.client.etrade.auth.EtradeOAuthClient;
import io.strategiz.client.etrade.client.EtradeDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Main orchestrator for E*TRADE API operations.
 * This class provides a unified interface for OAuth and data operations.
 *
 * E*TRADE API uses OAuth 1.0a authentication with HMAC-SHA1 signing.
 *
 * Key differences from OAuth 2.0:
 * - No refresh tokens - access tokens expire after 2 hours of inactivity
 * - Each request must be signed with HMAC-SHA1
 * - Uses consumer key/secret instead of client ID/secret
 *
 * Usage:
 * 1. Call getRequestToken() to start OAuth flow
 * 2. Redirect user to generateAuthorizationUrl(requestToken)
 * 3. Receive callback with oauth_verifier
 * 4. Call getAccessToken(requestToken, requestTokenSecret, verifier)
 * 5. Use access token + secret for all API calls
 */
@Component
public class EtradeClient {

	private static final Logger log = LoggerFactory.getLogger(EtradeClient.class);

	private final EtradeOAuthClient oauthClient;

	private final EtradeDataClient dataClient;

	public EtradeClient(EtradeOAuthClient oauthClient, EtradeDataClient dataClient) {
		this.oauthClient = oauthClient;
		this.dataClient = dataClient;
		log.info("EtradeClient initialized");
	}

	// ==================== OAuth Operations ====================

	/**
	 * Step 1: Get a request token to start OAuth flow.
	 * @return Map with oauth_token and oauth_token_secret
	 */
	public Map<String, String> getRequestToken() {
		return oauthClient.getRequestToken();
	}

	/**
	 * Step 2: Generate authorization URL for user to approve.
	 * @param requestToken The request token from step 1
	 * @return URL to redirect user to
	 */
	public String generateAuthorizationUrl(String requestToken) {
		return oauthClient.generateAuthorizationUrl(requestToken);
	}

	/**
	 * Step 3: Exchange request token + verifier for access token.
	 * @param requestToken The request token from step 1
	 * @param requestTokenSecret The request token secret from step 1
	 * @param verifier The oauth_verifier from callback
	 * @return Map with oauth_token (access token) and oauth_token_secret
	 */
	public Map<String, String> getAccessToken(String requestToken, String requestTokenSecret, String verifier) {
		return oauthClient.getAccessToken(requestToken, requestTokenSecret, verifier);
	}

	/**
	 * Renew access token to keep session alive.
	 * @param accessToken Current access token
	 * @param accessTokenSecret Current access token secret
	 * @return Renewed token info
	 */
	public Map<String, String> renewAccessToken(String accessToken, String accessTokenSecret) {
		return oauthClient.renewAccessToken(accessToken, accessTokenSecret);
	}

	/**
	 * Revoke access token (logout).
	 * @param accessToken Access token to revoke
	 * @param accessTokenSecret Access token secret
	 */
	public void revokeAccessToken(String accessToken, String accessTokenSecret) {
		oauthClient.revokeAccessToken(accessToken, accessTokenSecret);
	}

	/**
	 * Validate OAuth configuration.
	 */
	public void validateConfiguration() {
		oauthClient.validateConfiguration();
	}

	// ==================== Account Operations ====================

	/**
	 * Get all user accounts.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @return List of accounts
	 */
	public List<Map<String, Object>> getAccounts(String accessToken, String accessTokenSecret) {
		return dataClient.getAccounts(accessToken, accessTokenSecret);
	}

	/**
	 * Get account balance.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey Account ID key
	 * @return Balance information
	 */
	public Map<String, Object> getAccountBalance(String accessToken, String accessTokenSecret, String accountIdKey) {
		return dataClient.getAccountBalance(accessToken, accessTokenSecret, accountIdKey);
	}

	/**
	 * Get portfolio (positions) for an account.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey Account ID key
	 * @return Portfolio with positions
	 */
	public Map<String, Object> getPortfolio(String accessToken, String accessTokenSecret, String accountIdKey) {
		return dataClient.getPortfolio(accessToken, accessTokenSecret, accountIdKey);
	}

	/**
	 * Get transactions for an account.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey Account ID key
	 * @param startDate Start date (MMDDYYYY format)
	 * @param endDate End date (MMDDYYYY format)
	 * @return List of transactions
	 */
	public List<Map<String, Object>> getTransactions(String accessToken, String accessTokenSecret, String accountIdKey,
			String startDate, String endDate) {
		return dataClient.getTransactions(accessToken, accessTokenSecret, accountIdKey, startDate, endDate);
	}

	// ==================== Market Data Operations ====================

	/**
	 * Get quotes for multiple symbols.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbols List of symbols
	 * @return Quote data
	 */
	public Map<String, Object> getQuotes(String accessToken, String accessTokenSecret, List<String> symbols) {
		return dataClient.getQuotes(accessToken, accessTokenSecret, symbols);
	}

	/**
	 * Get quote for a single symbol.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbol Stock symbol
	 * @return Quote data
	 */
	public Map<String, Object> getQuote(String accessToken, String accessTokenSecret, String symbol) {
		return dataClient.getQuote(accessToken, accessTokenSecret, symbol);
	}

	/**
	 * Get last price for a symbol.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbol Stock symbol
	 * @return Last price or null
	 */
	public Double getLastPrice(String accessToken, String accessTokenSecret, String symbol) {
		return dataClient.getLastPrice(accessToken, accessTokenSecret, symbol);
	}

	// ==================== Connection Testing ====================

	/**
	 * Test connection using access token.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @return true if connection is valid
	 */
	public boolean testConnection(String accessToken, String accessTokenSecret) {
		return dataClient.testConnection(accessToken, accessTokenSecret);
	}

	// ==================== Getters for Configuration ====================

	public String getConsumerKey() {
		return oauthClient.getConsumerKey();
	}

	public String getRedirectUri() {
		return oauthClient.getRedirectUri();
	}

	public String getApiUrl() {
		return oauthClient.getApiUrl();
	}

}
