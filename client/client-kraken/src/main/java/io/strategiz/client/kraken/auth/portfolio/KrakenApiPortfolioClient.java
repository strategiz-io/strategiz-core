package io.strategiz.client.kraken.auth.portfolio;

import io.strategiz.client.kraken.auth.KrakenApiAuthClient;
import io.strategiz.client.kraken.auth.manager.KrakenCredentialManager;
import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching portfolio data from Kraken API
 */
@Service
public class KrakenApiPortfolioClient {

	private static final Logger log = LoggerFactory.getLogger(KrakenApiPortfolioClient.class);

	private final KrakenApiAuthClient apiAuthClient;

	private final KrakenCredentialManager credentialManager;

	public KrakenApiPortfolioClient(KrakenApiAuthClient apiAuthClient, KrakenCredentialManager credentialManager) {
		this.apiAuthClient = apiAuthClient;
		this.credentialManager = credentialManager;
	}

	/**
	 * Get portfolio data for a user
	 * @param userId User ID
	 * @return Portfolio data including balances
	 */
	public Map<String, Object> getPortfolio(String userId) {
		try {
			// Get credentials from Vault
			KrakenApiCredentials credentials = credentialManager.getCredentials(userId);
			if (credentials == null) {
				log.warn("No Kraken credentials found for user: {}", userId);
				return null;
			}

			// Fetch account balance
			Map<String, Object> balanceData = apiAuthClient
				.getAccountBalance(credentials.getApiKey(), credentials.getApiSecret(), credentials.getOtp())
				.block();

			if (balanceData == null) {
				log.warn("No balance data returned for user: {}", userId);
				return null;
			}

			// Process and return portfolio data
			return processPortfolioData(balanceData);

		}
		catch (Exception e) {
			log.error("Error fetching portfolio for user: {}", userId, e);
			return null;
		}
	}

	/**
	 * Get account balances for a user
	 * @param userId User ID
	 * @return Account balances
	 */
	public Map<String, Object> getBalances(String userId) {
		try {
			// Get credentials from Vault
			KrakenApiCredentials credentials = credentialManager.getCredentials(userId);
			if (credentials == null) {
				log.warn("No Kraken credentials found for user: {}", userId);
				return null;
			}

			// Fetch balances
			Map<String, Object> response = apiAuthClient
				.getAccountBalance(credentials.getApiKey(), credentials.getApiSecret(), credentials.getOtp())
				.block();

			if (response == null || !response.containsKey("result")) {
				log.warn("No balance data returned for user: {}", userId);
				return null;
			}

			return (Map<String, Object>) response.get("result");

		}
		catch (Exception e) {
			log.error("Error fetching balances for user: {}", userId, e);
			return null;
		}
	}

	/**
	 * Get open positions for a user
	 * @param userId User ID
	 * @return Open positions
	 */
	public Map<String, Object> getOpenPositions(String userId) {
		try {
			// Get credentials from Vault
			KrakenApiCredentials credentials = credentialManager.getCredentials(userId);
			if (credentials == null) {
				log.warn("No Kraken credentials found for user: {}", userId);
				return null;
			}

			// Fetch open positions
			Map<String, Object> response = apiAuthClient
				.getPortfolio(credentials.getApiKey(), credentials.getApiSecret(), credentials.getOtp())
				.block();

			if (response == null || !response.containsKey("result")) {
				log.warn("No position data returned for user: {}", userId);
				return new HashMap<>();
			}

			return (Map<String, Object>) response.get("result");

		}
		catch (Exception e) {
			log.error("Error fetching open positions for user: {}", userId, e);
			return null;
		}
	}

	/**
	 * Get trade history for a user
	 * @param userId User ID
	 * @return Trade history
	 */
	public Map<String, Object> getTradeHistory(String userId) {
		try {
			// Get credentials from Vault
			KrakenApiCredentials credentials = credentialManager.getCredentials(userId);
			if (credentials == null) {
				log.warn("No Kraken credentials found for user: {}", userId);
				return null;
			}

			// Fetch trade history
			Map<String, Object> response = apiAuthClient
				.getTradeHistory(credentials.getApiKey(), credentials.getApiSecret(), credentials.getOtp())
				.block();

			if (response == null || !response.containsKey("result")) {
				log.warn("No trade data returned for user: {}", userId);
				return new HashMap<>();
			}

			return (Map<String, Object>) response.get("result");

		}
		catch (Exception e) {
			log.error("Error fetching trade history for user: {}", userId, e);
			return null;
		}
	}

	/**
	 * Test connection with provided credentials
	 * @param credentials Kraken API credentials to test
	 * @return true if connection successful, false otherwise
	 */
	public boolean testConnection(KrakenApiCredentials credentials) {
		try {
			if (credentials == null) {
				return false;
			}

			// Test connection by fetching account balance
			Map<String, Object> response = apiAuthClient
				.getAccountBalance(credentials.getApiKey(), credentials.getApiSecret(), credentials.getOtp())
				.block();

			// Check if response is valid and has no errors
			if (response == null) {
				return false;
			}

			if (response.containsKey("error")) {
				Object errors = response.get("error");
				if (errors instanceof List && !((List<?>) errors).isEmpty()) {
					log.warn("Connection test failed with errors: {}", errors);
					return false;
				}
			}

			return response.containsKey("result");

		}
		catch (Exception e) {
			log.error("Connection test failed: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Get portfolio data for a user by userId (convenience method)
	 * @param userId User ID
	 * @return Portfolio data
	 */
	public Map<String, Object> getUserPortfolio(String userId) {
		return getPortfolio(userId);
	}

	/**
	 * Process raw portfolio data from Kraken API
	 * @param response Raw API response
	 * @return Processed portfolio data
	 */
	private Map<String, Object> processPortfolioData(Map<String, Object> response) {
		Map<String, Object> portfolio = new HashMap<>();

		// Check for errors
		if (response.containsKey("error")) {
			Object errors = response.get("error");
			if (errors instanceof List && !((List<?>) errors).isEmpty()) {
				portfolio.put("error", errors);
				return portfolio;
			}
		}

		// Extract result data
		if (response.containsKey("result")) {
			Map<String, Object> result = (Map<String, Object>) response.get("result");
			portfolio.put("balances", result);

			// Calculate total value (simplified)
			portfolio.put("totalAssets", result.size());
		}

		return portfolio;
	}

}