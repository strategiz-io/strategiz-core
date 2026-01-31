package io.strategiz.business.provider.robinhood;

import io.strategiz.business.provider.robinhood.model.RobinhoodConnectionResult;
import io.strategiz.client.robinhood.RobinhoodClient;
import io.strategiz.client.robinhood.error.RobinhoodErrors;
import io.strategiz.client.robinhood.model.RobinhoodLoginResult;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.repository.CreateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.provider.repository.UpdateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for Robinhood provider integration. Handles credential-based OAuth flows
 * (password grant) with MFA support.
 *
 * Key differences from standard OAuth providers (Schwab, Coinbase): - Uses password grant
 * instead of authorization code flow - Requires user credentials to be sent to backend -
 * Multi-step MFA flow (login -> MFA code -> complete)
 *
 * Note: This is an unofficial API integration.
 */
@Component
public class RobinhoodProviderBusiness implements ProviderIntegrationHandler {

	private static final Logger log = LoggerFactory.getLogger(RobinhoodProviderBusiness.class);

	private static final String PROVIDER_ID = "robinhood";

	private static final String PROVIDER_NAME = "Robinhood";

	private static final String PROVIDER_TYPE = "equity";

	private static final String PROVIDER_CATEGORY = "brokerage";

	private final RobinhoodClient robinhoodClient;

	private final CreateProviderIntegrationRepository createProviderIntegrationRepository;

	private final ReadProviderIntegrationRepository readProviderIntegrationRepository;

	private final UpdateProviderIntegrationRepository updateProviderIntegrationRepository;

	private final PortfolioProviderRepository portfolioProviderRepository;

	private final SecretManager secretManager;

	@Autowired(required = false)
	private PortfolioSummaryManager portfolioSummaryManager;

	@Autowired
	public RobinhoodProviderBusiness(RobinhoodClient robinhoodClient,
			CreateProviderIntegrationRepository createProviderIntegrationRepository,
			ReadProviderIntegrationRepository readProviderIntegrationRepository,
			UpdateProviderIntegrationRepository updateProviderIntegrationRepository,
			@Autowired(required = false) PortfolioProviderRepository portfolioProviderRepository,
			@Qualifier("vaultSecretService") SecretManager secretManager) {
		this.robinhoodClient = robinhoodClient;
		this.createProviderIntegrationRepository = createProviderIntegrationRepository;
		this.readProviderIntegrationRepository = readProviderIntegrationRepository;
		this.updateProviderIntegrationRepository = updateProviderIntegrationRepository;
		this.portfolioProviderRepository = portfolioProviderRepository;
		this.secretManager = secretManager;
	}

	/**
	 * Initiate Robinhood login with credentials. This may trigger MFA if enabled on the
	 * account.
	 * @param userId User ID
	 * @param username Robinhood username (email)
	 * @param password Robinhood password
	 * @param mfaType Preferred MFA type - "sms" or "email"
	 * @return Connection result (may require MFA completion)
	 */
	public RobinhoodConnectionResult initiateLogin(String userId, String username, String password, String mfaType) {
		validateRequired("userId", userId);
		validateRequired("username", username);
		validateRequired("password", password);

		log.info("Initiating Robinhood login for user: {}", userId);

		try {
			// Store credentials temporarily for MFA completion
			storeTemporaryCredentials(userId, username, password);

			// Attempt login
			RobinhoodLoginResult loginResult = robinhoodClient.login(username, password,
					mfaType != null ? mfaType : "sms");

			return processLoginResult(userId, loginResult);

		}
		catch (Exception e) {
			log.error("Failed to initiate Robinhood login for user: {}", userId, e);

			RobinhoodConnectionResult result = new RobinhoodConnectionResult();
			result.setUserId(userId);
			result.setProviderId(PROVIDER_ID);
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
			result.setErrorMessage(e.getMessage());
			return result;
		}
	}

	/**
	 * Complete MFA challenge with the code received via SMS/email.
	 * @param userId User ID
	 * @param challengeId Challenge ID from initial login
	 * @param mfaCode MFA code received
	 * @param deviceToken Device token from initial login
	 * @return Connection result
	 */
	public RobinhoodConnectionResult completeMfaChallenge(String userId, String challengeId, String mfaCode,
			String deviceToken) {
		validateRequired("userId", userId);
		validateRequired("challengeId", challengeId);
		validateRequired("mfaCode", mfaCode);
		validateRequired("deviceToken", deviceToken);

		log.info("Completing MFA challenge for user: {}", userId);

		try {
			// Respond to challenge
			boolean validated = robinhoodClient.respondToChallenge(challengeId, mfaCode);

			if (!validated) {
				RobinhoodConnectionResult result = new RobinhoodConnectionResult();
				result.setUserId(userId);
				result.setProviderId(PROVIDER_ID);
				result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
				result.setErrorMessage("Invalid MFA code");
				result.setErrorCode("INVALID_MFA_CODE");
				return result;
			}

			// Retrieve stored credentials
			Map<String, Object> credentials = retrieveTemporaryCredentials(userId);
			String username = (String) credentials.get("username");
			String password = (String) credentials.get("password");

			// Complete login with validated challenge
			RobinhoodLoginResult loginResult = robinhoodClient.completeLoginAfterMfa(username, password, deviceToken,
					challengeId);

			// Clear temporary credentials
			clearTemporaryCredentials(userId);

			return processLoginResult(userId, loginResult);

		}
		catch (Exception e) {
			log.error("Failed to complete MFA for user: {}", userId, e);

			RobinhoodConnectionResult result = new RobinhoodConnectionResult();
			result.setUserId(userId);
			result.setProviderId(PROVIDER_ID);
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
			result.setErrorMessage(e.getMessage());
			return result;
		}
	}

	/**
	 * Process login result and create connection result.
	 */
	private RobinhoodConnectionResult processLoginResult(String userId, RobinhoodLoginResult loginResult) {
		RobinhoodConnectionResult result = new RobinhoodConnectionResult();
		result.setUserId(userId);
		result.setProviderId(PROVIDER_ID);
		result.setProviderName(PROVIDER_NAME);

		if (loginResult.isSuccess()) {
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.SUCCESS);
			result.setAccessToken(loginResult.getAccessToken());
			result.setRefreshToken(loginResult.getRefreshToken());
			result.setExpiresAt(
					Instant.now().plusSeconds(loginResult.getExpiresIn() != null ? loginResult.getExpiresIn() : 86400));
			result.setConnectedAt(Instant.now());

			// Store tokens and complete integration
			completeIntegration(userId, result);

		}
		else if (loginResult.isMfaRequired()) {
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.MFA_REQUIRED);
			result.setChallenge(loginResult.getChallenge());
			result.setChallengeType(loginResult.getChallengeType());
			result.setDeviceToken(loginResult.getDeviceToken());

		}
		else if (loginResult.isDeviceApprovalRequired()) {
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.DEVICE_APPROVAL);
			result.setDeviceToken(loginResult.getDeviceToken());
			result.setErrorMessage("Please approve this device in your Robinhood app");

		}
		else {
			result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
			result.setErrorMessage(loginResult.getErrorMessage());
			result.setErrorCode(loginResult.getErrorCode());
		}

		return result;
	}

	/**
	 * Complete the integration after successful authentication.
	 */
	private void completeIntegration(String userId, RobinhoodConnectionResult result) {
		try {
			// Store tokens in Vault
			storeTokensInVault(userId, result.getAccessToken(), result.getRefreshToken(), result.getExpiresAt());

			// Create or update provider integration
			Optional<ProviderIntegrationEntity> existing = readProviderIntegrationRepository
				.findByUserIdAndProviderId(userId, PROVIDER_ID);

			if (existing.isPresent()) {
				ProviderIntegrationEntity entity = existing.get();
				entity.setStatus("connected");
				updateProviderIntegrationRepository.updateWithUserId(entity, userId);
			}
			else {
				ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "credentials", userId);
				entity.setStatus("connected");
				createProviderIntegrationRepository.createForUser(entity, userId);
			}

			// Fetch account info
			try {
				List<Map<String, Object>> accounts = robinhoodClient.getAccounts(result.getAccessToken());
				if (!accounts.isEmpty()) {
					result.setAccountInfo(accounts.get(0));
				}
			}
			catch (Exception e) {
				log.warn("Could not fetch Robinhood accounts: {}", e.getMessage());
			}

			// Fetch and store portfolio data
			try {
				fetchAndStorePortfolioData(userId, result.getAccessToken());
			}
			catch (Exception e) {
				log.warn("Could not fetch portfolio data during connection: {}", e.getMessage());
			}

			log.info("Successfully completed Robinhood integration for user: {}", userId);

		}
		catch (Exception e) {
			log.error("Error completing Robinhood integration for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to complete Robinhood integration: " + e.getMessage());
		}
	}

	/**
	 * Sync provider data for a user.
	 */
	public PortfolioProviderEntity syncProviderData(String userId) {
		log.info("Syncing Robinhood provider data for user: {}", userId);

		try {
			String accessToken = getValidAccessToken(userId);
			return fetchAndStorePortfolioData(userId, accessToken);

		}
		catch (Exception e) {
			log.error("Failed to sync Robinhood data for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to sync Robinhood data: " + e.getMessage());
		}
	}

	/**
	 * Fetch and store portfolio data. Returns the stored PortfolioProviderEntity summary.
	 */
	private PortfolioProviderEntity fetchAndStorePortfolioData(String userId, String accessToken) {
		if (portfolioProviderRepository == null) {
			log.warn("Portfolio provider repository not available");
			return null;
		}

		try {
			// Get stock positions
			List<Map<String, Object>> positions = robinhoodClient.getNonzeroPositions(accessToken);

			// Get crypto holdings
			List<Map<String, Object>> cryptoHoldings = robinhoodClient.getCryptoHoldings(accessToken);

			// Get portfolio summary
			List<Map<String, Object>> portfolios = robinhoodClient.getPortfolios(accessToken);

			// Transform to PortfolioProviderEntity (summary only)
			PortfolioProviderEntity entity = transformToPortfolioProviderEntity(positions, cryptoHoldings, portfolios);

			// Store in users/{userId}/portfolio/{providerId}
			PortfolioProviderEntity saved = portfolioProviderRepository.save(entity, userId);

			log.info("Stored Robinhood portfolio summary for user: {} - totalValue={}, holdingsCount={}", userId,
					saved.getTotalValue(), saved.getHoldingsCount());

			// Refresh portfolio summary to include this provider's data
			if (portfolioSummaryManager != null) {
				try {
					portfolioSummaryManager.refreshPortfolioSummary(userId);
					log.info("Refreshed portfolio summary after storing Robinhood data for user: {}", userId);
				}
				catch (Exception e) {
					log.warn("Failed to refresh portfolio summary for user {}: {}", userId, e.getMessage());
				}
			}

			return saved;

		}
		catch (Exception e) {
			log.error("Failed to fetch/store Robinhood portfolio data: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Transform Robinhood data to PortfolioProviderEntity (summary only). Holdings are
	 * stored separately in subcollection.
	 */
	private PortfolioProviderEntity transformToPortfolioProviderEntity(List<Map<String, Object>> positions,
			List<Map<String, Object>> cryptoHoldings, List<Map<String, Object>> portfolios) {

		PortfolioProviderEntity entity = new PortfolioProviderEntity();
		entity.setProviderId(PROVIDER_ID);
		entity.setProviderName(PROVIDER_NAME);
		entity.setProviderType(PROVIDER_TYPE);
		entity.setStatus("connected");
		entity.setConnectionType("credentials");
		entity.setLastSyncedAt(Instant.now());

		// Calculate total value and holdings count
		BigDecimal totalValue = BigDecimal.ZERO;
		int holdingsCount = 0;

		// Sum up stock position values (simplified - using quantity only for count)
		for (Map<String, Object> position : positions) {
			BigDecimal quantity = extractBigDecimal(position, "quantity");
			if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
				holdingsCount++;
				// For accurate value, would need to fetch current prices
				// For now, just tracking count
			}
		}

		// Sum up crypto values
		for (Map<String, Object> crypto : cryptoHoldings) {
			BigDecimal quantity = extractBigDecimal(crypto, "quantity");
			BigDecimal equity = extractBigDecimal(crypto, "equity");
			if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
				holdingsCount++;
				if (equity != null) {
					totalValue = totalValue.add(equity);
				}
			}
		}

		// Get cash balance from portfolio
		BigDecimal cashBalance = BigDecimal.ZERO;
		if (!portfolios.isEmpty()) {
			Map<String, Object> portfolio = portfolios.get(0);
			cashBalance = extractBigDecimal(portfolio, "withdrawable_amount");
			if (cashBalance == null) {
				cashBalance = BigDecimal.ZERO;
			}

			// Try to get total equity from portfolio (more accurate than summing)
			BigDecimal portfolioEquity = extractBigDecimal(portfolio, "equity");
			if (portfolioEquity != null) {
				totalValue = portfolioEquity;
			}
		}

		entity.setTotalValue(totalValue.add(cashBalance));
		entity.setCashBalance(cashBalance);
		entity.setHoldingsCount(holdingsCount);
		entity.setDayChange(BigDecimal.ZERO); // Would need previous day data
		entity.setDayChangePercent(BigDecimal.ZERO);

		return entity;
	}

	/**
	 * Get valid access token, refreshing if needed.
	 */
	private String getValidAccessToken(String userId) {
		String secretKey = "users." + userId + ".providers.robinhood";
		Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

		if (secretData == null || secretData.isEmpty()) {
			throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND, "No Robinhood tokens found for user: " + userId);
		}

		String accessToken = (String) secretData.get("accessToken");
		String refreshToken = (String) secretData.get("refreshToken");
		String expiresAtStr = (String) secretData.get("expiresAt");

		// Check if token needs refresh
		if (expiresAtStr != null) {
			try {
				Instant expiresAt = Instant.parse(expiresAtStr);
				if (expiresAt.isBefore(Instant.now().plusSeconds(300))) {
					log.info("Robinhood token expiring soon, refreshing for user: {}", userId);
					return refreshAccessToken(userId, refreshToken);
				}
			}
			catch (Exception e) {
				log.warn("Could not parse token expiration: {}", e.getMessage());
			}
		}

		return accessToken;
	}

	/**
	 * Refresh access token.
	 */
	private String refreshAccessToken(String userId, String refreshToken) {
		try {
			Map<String, Object> tokenData = robinhoodClient.refreshAccessToken(refreshToken);

			String newAccessToken = (String) tokenData.get("access_token");
			String newRefreshToken = (String) tokenData.get("refresh_token");
			Integer expiresIn = (Integer) tokenData.get("expires_in");

			if (newRefreshToken == null) {
				newRefreshToken = refreshToken;
			}

			Instant expiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 86400);
			storeTokensInVault(userId, newAccessToken, newRefreshToken, expiresAt);

			return newAccessToken;

		}
		catch (Exception e) {
			log.error("Failed to refresh Robinhood token for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to refresh Robinhood token. Please reconnect your account.");
		}
	}

	/**
	 * Store tokens in Vault.
	 */
	private void storeTokensInVault(String userId, String accessToken, String refreshToken, Instant expiresAt) {
		String secretKey = "users." + userId + ".providers.robinhood";

		Map<String, Object> secretData = new HashMap<>();
		secretData.put("accessToken", accessToken);
		secretData.put("refreshToken", refreshToken);
		secretData.put("expiresAt", expiresAt.toString());
		secretData.put("provider", PROVIDER_ID);
		secretData.put("storedAt", Instant.now().toString());

		secretManager.createSecret(secretKey, secretData);
		log.debug("Stored Robinhood tokens in Vault for user: {}", userId);
	}

	/**
	 * Store temporary credentials for MFA completion.
	 */
	private void storeTemporaryCredentials(String userId, String username, String password) {
		String secretKey = "users." + userId + ".providers.robinhood.temp";

		Map<String, Object> secretData = new HashMap<>();
		secretData.put("username", username);
		secretData.put("password", password);
		secretData.put("storedAt", Instant.now().toString());
		secretData.put("expiresAt", Instant.now().plusSeconds(600).toString()); // 10 min
																				// expiry

		secretManager.createSecret(secretKey, secretData);
	}

	/**
	 * Retrieve temporary credentials.
	 */
	private Map<String, Object> retrieveTemporaryCredentials(String userId) {
		String secretKey = "users." + userId + ".providers.robinhood.temp";
		Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

		if (secretData == null || secretData.isEmpty()) {
			throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
					"No temporary credentials found. Please restart the login process.");
		}

		return secretData;
	}

	/**
	 * Clear temporary credentials.
	 */
	private void clearTemporaryCredentials(String userId) {
		try {
			String secretKey = "users." + userId + ".providers.robinhood.temp";
			secretManager.deleteSecret(secretKey);
		}
		catch (Exception e) {
			log.warn("Could not clear temporary credentials: {}", e.getMessage());
		}
	}

	// Utility methods

	private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null)
			return null;
		if (value instanceof BigDecimal)
			return (BigDecimal) value;
		if (value instanceof Number)
			return new BigDecimal(value.toString());
		if (value instanceof String) {
			try {
				return new BigDecimal((String) value);
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private void validateRequired(String paramName, String paramValue) {
		if (paramValue == null || paramValue.trim().isEmpty()) {
			throw new StrategizException(ErrorCode.VALIDATION_ERROR,
					"Required parameter '" + paramName + "' is missing");
		}
	}

	// ProviderIntegrationHandler interface implementation

	@Override
	public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
		log.info("Testing Robinhood connection for user: {}", userId);
		// Robinhood uses credential flow, can't test without credentials
		// Return true to indicate the flow should proceed
		return true;
	}

	@Override
	public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
		log.info("Creating Robinhood integration for user: {}", userId);

		try {
			// Create pending integration
			ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "credentials", userId);
			entity.setStatus("pending");
			createProviderIntegrationRepository.createForUser(entity, userId);

			// Build result - Robinhood requires credential flow, not redirect
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("flowType", "credentials");
			metadata.put("requiresMfa", true);
			metadata.put("mfaTypes", new String[] { "sms", "email" });
			metadata.put("note", "Submit credentials via /v1/providers/robinhood/login endpoint");

			ProviderIntegrationResult result = new ProviderIntegrationResult();
			result.setSuccess(true);
			result.setMessage("Robinhood integration initiated. Submit credentials to complete.");
			result.setMetadata(metadata);
			return result;

		}
		catch (Exception e) {
			log.error("Error creating Robinhood integration: {}", e.getMessage());
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to create Robinhood integration: " + e.getMessage());
		}
	}

	@Override
	public String getProviderId() {
		return PROVIDER_ID;
	}

}
