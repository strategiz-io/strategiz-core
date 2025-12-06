package io.strategiz.client.robinhood;

import io.strategiz.client.robinhood.model.RobinhoodLoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Main client for interacting with the Robinhood API.
 * This class coordinates OAuth operations and data retrieval.
 *
 * Note: This is an unofficial API integration. Robinhood may change it at any time.
 *
 * Key features:
 * - Password grant OAuth flow with MFA support
 * - Portfolio and position data retrieval
 * - Stock quote fetching
 * - Crypto holdings (via Nummus)
 */
@Component
public class RobinhoodClient {

    private static final Logger log = LoggerFactory.getLogger(RobinhoodClient.class);

    private final RobinhoodOAuthClient oauthClient;
    private final RobinhoodDataClient dataClient;

    public RobinhoodClient(RobinhoodOAuthClient oauthClient, RobinhoodDataClient dataClient) {
        this.oauthClient = oauthClient;
        this.dataClient = dataClient;
        log.info("RobinhoodClient initialized");
    }

    // ========== OAuth Operations ==========

    /**
     * Initiate login with username and password.
     *
     * @param username Robinhood username (email)
     * @param password Robinhood password
     * @param challengeType MFA type - "sms" or "email"
     * @return Login result (may require MFA)
     */
    public RobinhoodLoginResult login(String username, String password, String challengeType) {
        return oauthClient.login(username, password, challengeType);
    }

    /**
     * Login with all parameters including MFA code.
     *
     * @param username Robinhood username
     * @param password Robinhood password
     * @param challengeType MFA type
     * @param deviceToken Device token
     * @param mfaCode MFA code
     * @param challengeId Challenge ID
     * @return Login result
     */
    public RobinhoodLoginResult login(String username, String password, String challengeType,
                                       String deviceToken, String mfaCode, String challengeId) {
        return oauthClient.login(username, password, challengeType, deviceToken, mfaCode, challengeId);
    }

    /**
     * Respond to MFA challenge.
     *
     * @param challengeId Challenge ID
     * @param code MFA code
     * @return true if validated
     */
    public boolean respondToChallenge(String challengeId, String code) {
        return oauthClient.respondToChallenge(challengeId, code);
    }

    /**
     * Complete login after MFA.
     *
     * @param username Robinhood username
     * @param password Robinhood password
     * @param deviceToken Device token
     * @param challengeId Validated challenge ID
     * @return Login result with tokens
     */
    public RobinhoodLoginResult completeLoginAfterMfa(String username, String password,
                                                       String deviceToken, String challengeId) {
        return oauthClient.completeLoginAfterMfa(username, password, deviceToken, challengeId);
    }

    /**
     * Refresh access token.
     *
     * @param refreshToken Refresh token
     * @return New token data
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        return oauthClient.refreshAccessToken(refreshToken);
    }

    // ========== Data Operations ==========

    /**
     * Get user information.
     *
     * @param accessToken Access token
     * @return User info
     */
    public Map<String, Object> getUser(String accessToken) {
        return dataClient.getUser(accessToken);
    }

    /**
     * Get all accounts.
     *
     * @param accessToken Access token
     * @return List of accounts
     */
    public List<Map<String, Object>> getAccounts(String accessToken) {
        return dataClient.getAccounts(accessToken);
    }

    /**
     * Get all portfolios.
     *
     * @param accessToken Access token
     * @return List of portfolios
     */
    public List<Map<String, Object>> getPortfolios(String accessToken) {
        return dataClient.getPortfolios(accessToken);
    }

    /**
     * Get portfolio for account.
     *
     * @param accessToken Access token
     * @param accountNumber Account number
     * @return Portfolio summary
     */
    public Map<String, Object> getPortfolio(String accessToken, String accountNumber) {
        return dataClient.getPortfolio(accessToken, accountNumber);
    }

    /**
     * Get all stock positions.
     *
     * @param accessToken Access token
     * @return List of positions
     */
    public List<Map<String, Object>> getPositions(String accessToken) {
        return dataClient.getPositions(accessToken);
    }

    /**
     * Get non-zero stock positions.
     *
     * @param accessToken Access token
     * @return List of positions with quantity > 0
     */
    public List<Map<String, Object>> getNonzeroPositions(String accessToken) {
        return dataClient.getPositions(accessToken, true);
    }

    /**
     * Get crypto holdings.
     *
     * @param accessToken Access token
     * @return List of crypto holdings
     */
    public List<Map<String, Object>> getCryptoHoldings(String accessToken) {
        return dataClient.getCryptoHoldings(accessToken);
    }

    /**
     * Get quote for a symbol.
     *
     * @param accessToken Access token
     * @param symbol Stock symbol
     * @return Quote data
     */
    public Map<String, Object> getQuote(String accessToken, String symbol) {
        return dataClient.getQuote(accessToken, symbol);
    }

    /**
     * Get quotes for multiple symbols.
     *
     * @param accessToken Access token
     * @param symbols List of symbols
     * @return Map of symbol -> quote
     */
    public Map<String, Object> getQuotes(String accessToken, List<String> symbols) {
        return dataClient.getQuotes(accessToken, symbols);
    }

    /**
     * Get instrument details.
     *
     * @param accessToken Access token
     * @param instrumentUrl Instrument URL
     * @return Instrument details
     */
    public Map<String, Object> getInstrument(String accessToken, String instrumentUrl) {
        return dataClient.getInstrument(accessToken, instrumentUrl);
    }

    /**
     * Get instrument by symbol.
     *
     * @param accessToken Access token
     * @param symbol Stock symbol
     * @return Instrument details
     */
    public Map<String, Object> getInstrumentBySymbol(String accessToken, String symbol) {
        return dataClient.getInstrumentBySymbol(accessToken, symbol);
    }

    /**
     * Test connection.
     *
     * @param accessToken Access token
     * @return true if valid
     */
    public boolean testConnection(String accessToken) {
        return dataClient.testConnection(accessToken);
    }
}
