package io.strategiz.business.provider.etrade;

import io.strategiz.business.provider.etrade.model.EtradeConnectionResult;
import io.strategiz.client.etrade.EtradeClient;
import io.strategiz.client.etrade.error.EtradeErrors;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic for E*TRADE provider integration.
 * Handles OAuth 1.0a flows, API interactions, and business rules specific to E*TRADE.
 *
 * Key differences from Schwab (OAuth 2.0):
 * - Uses OAuth 1.0a with HMAC-SHA1 signing
 * - No refresh tokens - access tokens expire after 2 hours of inactivity
 * - Three-legged flow: Request Token → Authorize → Access Token
 * - Stores both accessToken AND accessTokenSecret (OAuth 1.0a requirement)
 *
 * OAuth 1.0a Flow:
 * 1. Get request token from E*TRADE
 * 2. Store request token temporarily (in memory cache)
 * 3. Redirect user to E*TRADE authorization page
 * 4. Receive callback with oauth_verifier
 * 5. Exchange request token + verifier for access token
 * 6. Store access token + secret in Vault
 */
@Component
public class EtradeProviderBusiness implements ProviderIntegrationHandler {

	private static final Logger log = LoggerFactory.getLogger(EtradeProviderBusiness.class);

	private static final String PROVIDER_ID = "etrade";

	private static final String PROVIDER_NAME = "E*TRADE";

	private static final String PROVIDER_TYPE = "equity";

	private static final String PROVIDER_CATEGORY = "brokerage";

	private final EtradeClient etradeClient;

	private final PortfolioProviderRepository portfolioProviderRepository;

	private final ProviderHoldingsRepository providerHoldingsRepository;

	private final SecretManager secretManager;

	@Autowired(required = false)
	private PortfolioSummaryManager portfolioSummaryManager;

	// Temporary storage for request tokens during OAuth flow
	// Key: state (userId-uuid), Value: {requestToken, requestTokenSecret}
	private final ConcurrentHashMap<String, Map<String, String>> pendingOAuthRequests = new ConcurrentHashMap<>();

	@Autowired
	public EtradeProviderBusiness(EtradeClient etradeClient, PortfolioProviderRepository portfolioProviderRepository,
			ProviderHoldingsRepository providerHoldingsRepository,
			@Qualifier("vaultSecretService") SecretManager secretManager) {
		this.etradeClient = etradeClient;
		this.portfolioProviderRepository = portfolioProviderRepository;
		this.providerHoldingsRepository = providerHoldingsRepository;
		this.secretManager = secretManager;
	}

	/**
	 * Start OAuth 1.0a flow by getting request token and generating authorization
	 * URL.
	 * @param userId The user requesting authorization
	 * @param state Security state parameter (userId-uuid)
	 * @return OAuth authorization URL
	 */
	public String startOAuthFlow(String userId, String state) {
		log.info("Starting E*TRADE OAuth 1.0a flow for user: {}", userId);

		try {
			// Step 1: Get request token from E*TRADE
			Map<String, String> requestTokenData = etradeClient.getRequestToken();

			String requestToken = requestTokenData.get("oauth_token");
			String requestTokenSecret = requestTokenData.get("oauth_token_secret");

			if (requestToken == null || requestTokenSecret == null) {
				throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED,
						"Failed to obtain E*TRADE request token");
			}

			// Step 2: Store request token temporarily for callback
			Map<String, String> pendingRequest = new HashMap<>();
			pendingRequest.put("requestToken", requestToken);
			pendingRequest.put("requestTokenSecret", requestTokenSecret);
			pendingRequest.put("userId", userId);
			pendingRequest.put("createdAt", Instant.now().toString());
			pendingOAuthRequests.put(state, pendingRequest);

			// Step 3: Generate authorization URL
			String authUrl = etradeClient.generateAuthorizationUrl(requestToken);

			log.info("Generated E*TRADE OAuth URL for user: {}", userId);
			return authUrl;

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Failed to start E*TRADE OAuth flow for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to start E*TRADE OAuth: " + e.getMessage());
		}
	}

	/**
	 * Handle OAuth callback from E*TRADE.
	 * @param state The state parameter to retrieve pending request
	 * @param oauthVerifier The oauth_verifier from callback
	 * @return Connection result with tokens
	 */
	public EtradeConnectionResult handleOAuthCallback(String state, String oauthVerifier) {
		validateRequired("state", state);
		validateRequired("oauthVerifier", oauthVerifier);

		// Retrieve pending request
		Map<String, String> pendingRequest = pendingOAuthRequests.remove(state);
		if (pendingRequest == null) {
			throw new StrategizException(EtradeErrors.ETRADE_OAUTH_ERROR,
					"OAuth session expired or invalid state. Please try connecting again.");
		}

		String requestToken = pendingRequest.get("requestToken");
		String requestTokenSecret = pendingRequest.get("requestTokenSecret");
		String userId = pendingRequest.get("userId");

		try {
			log.info("Exchanging E*TRADE request token for access token for user: {}", userId);

			// Exchange request token + verifier for access token
			Map<String, String> accessTokenData = etradeClient.getAccessToken(requestToken, requestTokenSecret,
					oauthVerifier);

			String accessToken = accessTokenData.get("oauth_token");
			String accessTokenSecret = accessTokenData.get("oauth_token_secret");

			if (accessToken == null || accessTokenSecret == null) {
				throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED,
						"E*TRADE token exchange did not return access tokens");
			}

			log.info("Successfully obtained E*TRADE access tokens for user: {}", userId);

			// Build connection result
			EtradeConnectionResult result = new EtradeConnectionResult();
			result.setUserId(userId);
			result.setProviderId(PROVIDER_ID);
			result.setProviderName(PROVIDER_NAME);
			result.setAccessToken(accessToken);
			result.setAccessTokenSecret(accessTokenSecret);
			result.setConnectedAt(Instant.now());
			result.setStatus("connected");

			// Try to get account info (optional - don't fail OAuth if this fails)
			try {
				List<Map<String, Object>> accounts = etradeClient.getAccounts(accessToken, accessTokenSecret);
				if (accounts != null && !accounts.isEmpty()) {
					result.setAccountInfo(accounts.get(0));
				}
			}
			catch (Exception accountsError) {
				log.warn("Could not fetch E*TRADE accounts during OAuth callback (will sync later): {}",
						accountsError.getMessage());
			}

			log.info("Successfully connected E*TRADE for user: {}", userId);
			return result;

		}
		catch (StrategizException e) {
			log.error("Failed to handle E*TRADE OAuth callback for user: {}: {}", userId, e.getMessage());
			throw e;
		}
		catch (Exception e) {
			log.error("Failed to handle E*TRADE OAuth callback for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to complete E*TRADE OAuth: " + e.getMessage());
		}
	}

	/**
	 * Complete OAuth flow and store tokens.
	 * @param state The state parameter
	 * @param oauthVerifier The oauth_verifier from callback
	 */
	public void completeOAuthFlow(String state, String oauthVerifier) {
		log.info("Completing E*TRADE OAuth flow for state: {}", state);

		try {
			// Handle OAuth callback
			EtradeConnectionResult result = handleOAuthCallback(state, oauthVerifier);
			String userId = result.getUserId();

			// Store tokens in Vault
			storeTokensInVault(userId, result.getAccessToken(), result.getAccessTokenSecret());

			// Create or update provider
			Optional<PortfolioProviderEntity> existingProvider = portfolioProviderRepository
				.findByUserIdAndProviderId(userId, PROVIDER_ID);

			PortfolioProviderEntity entity;
			if (existingProvider.isPresent()) {
				entity = existingProvider.get();
				entity.setStatus("connected");
				log.info("Updating existing E*TRADE provider to connected for user: {}", userId);
			}
			else {
				entity = new PortfolioProviderEntity(PROVIDER_ID, "oauth", userId);
				entity.setProviderName(PROVIDER_NAME);
				entity.setProviderType(PROVIDER_TYPE);
				entity.setProviderCategory(PROVIDER_CATEGORY);
				entity.setStatus("connected");
				log.info("Creating new E*TRADE provider for user: {}", userId);
			}

			portfolioProviderRepository.save(entity, userId);

			// Fetch and store portfolio data
			try {
				fetchAndStorePortfolioData(userId, result.getAccessToken(), result.getAccessTokenSecret());
				log.info("Successfully fetched and stored E*TRADE portfolio data for user: {}", userId);
			}
			catch (Exception e) {
				log.error("Failed to fetch portfolio data for user: {} (OAuth still succeeded)", userId, e);
			}

		}
		catch (Exception e) {
			log.error("Error completing E*TRADE OAuth flow: {}", e.getMessage(), e);

			// Try to get userId from the pending request if available
			Map<String, String> pendingRequest = pendingOAuthRequests.get(state);
			if (pendingRequest != null) {
				String userId = pendingRequest.get("userId");
				try {
					Optional<PortfolioProviderEntity> existingProvider = portfolioProviderRepository
						.findByUserIdAndProviderId(userId, PROVIDER_ID);
					if (existingProvider.isPresent()) {
						PortfolioProviderEntity entity = existingProvider.get();
						entity.setStatus("error");
						entity.setErrorMessage(e.getMessage());
						portfolioProviderRepository.save(entity, userId);
					}
				}
				catch (Exception updateError) {
					log.error("Failed to update error status: {}", updateError.getMessage());
				}
			}

			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to complete E*TRADE OAuth flow: " + e.getMessage());
		}
	}

	/**
	 * Get the user ID from a state parameter. Used by callback service.
	 * @param state The state parameter
	 * @return User ID or null if not found
	 */
	public String getUserIdFromState(String state) {
		Map<String, String> pendingRequest = pendingOAuthRequests.get(state);
		return pendingRequest != null ? pendingRequest.get("userId") : null;
	}

	/**
	 * Sync provider data for a user.
	 * @param userId The user ID
	 * @return ProviderHoldingsEntity with synced data
	 */
	public ProviderHoldingsEntity syncProviderData(String userId) {
		log.info("Syncing E*TRADE provider data for user: {}", userId);

		try {
			// Get stored tokens
			Map<String, String> tokens = getStoredTokens(userId);
			String accessToken = tokens.get("accessToken");
			String accessTokenSecret = tokens.get("accessTokenSecret");

			// Try to renew token to keep session alive
			try {
				etradeClient.renewAccessToken(accessToken, accessTokenSecret);
			}
			catch (Exception e) {
				log.warn("Token renewal failed for user {}, session may have expired: {}", userId, e.getMessage());
			}

			// Fetch and store portfolio data
			fetchAndStorePortfolioData(userId, accessToken, accessTokenSecret);

			// Return stored data
			Optional<ProviderHoldingsEntity> holdings = providerHoldingsRepository.findByUserIdAndProviderId(userId,
					PROVIDER_ID);

			if (holdings.isPresent()) {
				log.info("Successfully synced E*TRADE data for user: {}", userId);
				return holdings.get();
			}
			else {
				throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
						"No E*TRADE provider data found after sync");
			}

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Failed to sync E*TRADE provider data for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to sync E*TRADE provider data: " + e.getMessage());
		}
	}

	/**
	 * Get stored access tokens from Vault.
	 */
	private Map<String, String> getStoredTokens(String userId) {
		String secretKey = "users." + userId + ".providers.etrade.accessToken";
		Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

		if (secretData == null || secretData.isEmpty()) {
			throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
					"No E*TRADE tokens found for user: " + userId + ". Please reconnect your account.");
		}

		String accessToken = (String) secretData.get("accessToken");
		String accessTokenSecret = (String) secretData.get("accessTokenSecret");

		if (accessToken == null || accessTokenSecret == null) {
			throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND, "E*TRADE tokens incomplete. Please reconnect.");
		}

		Map<String, String> tokens = new HashMap<>();
		tokens.put("accessToken", accessToken);
		tokens.put("accessTokenSecret", accessTokenSecret);
		return tokens;
	}

	/**
	 * Store tokens in Vault.
	 */
	private void storeTokensInVault(String userId, String accessToken, String accessTokenSecret) {
		try {
			String secretKey = "users." + userId + ".providers.etrade";

			Map<String, Object> secretData = new HashMap<>();
			secretData.put("accessToken", accessToken);
			secretData.put("accessTokenSecret", accessTokenSecret);
			secretData.put("provider", PROVIDER_ID);
			secretData.put("storedAt", Instant.now().toString());
			// Note: E*TRADE tokens don't have a fixed expiration - they expire after 2hr
			// inactivity

			secretManager.createSecret(secretKey, secretData);
			log.debug("Stored E*TRADE OAuth tokens in Vault for user: {}", userId);

		}
		catch (Exception e) {
			log.error("Failed to store E*TRADE tokens for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to store E*TRADE tokens: " + e.getMessage());
		}
	}

	/**
	 * Fetch portfolio data from E*TRADE and store.
	 */
	private void fetchAndStorePortfolioData(String userId, String accessToken, String accessTokenSecret) {
		log.info("Fetching E*TRADE portfolio data for user: {}", userId);

		try {
			// Get accounts
			List<Map<String, Object>> accounts = etradeClient.getAccounts(accessToken, accessTokenSecret);

			if (accounts == null || accounts.isEmpty()) {
				log.warn("No accounts found for E*TRADE user: {}", userId);
				return;
			}

			// Transform to holdings entity
			ProviderHoldingsEntity holdingsEntity = transformToProviderHoldingsEntity(accounts, userId, accessToken,
					accessTokenSecret);

			// Store holdings
			providerHoldingsRepository.save(userId, PROVIDER_ID, holdingsEntity);

			// Update provider summary
			updateProviderSummary(userId, holdingsEntity);

			log.info("Successfully stored E*TRADE portfolio data for user: {} with {} holdings", userId,
					holdingsEntity.getHoldings() != null ? holdingsEntity.getHoldings().size() : 0);

		}
		catch (Exception e) {
			log.error("Failed to fetch and store E*TRADE portfolio data for user: {}", userId, e);
		}
	}

	/**
	 * Transform E*TRADE accounts to ProviderHoldingsEntity.
	 */
	private ProviderHoldingsEntity transformToProviderHoldingsEntity(List<Map<String, Object>> accounts, String userId,
			String accessToken, String accessTokenSecret) {

		ProviderHoldingsEntity entity = new ProviderHoldingsEntity(PROVIDER_ID, userId);
		entity.setSyncStatus("success");
		entity.setLastUpdatedAt(Instant.now());

		List<ProviderHoldingsEntity.Holding> holdings = new ArrayList<>();
		BigDecimal totalValue = BigDecimal.ZERO;
		BigDecimal cashBalance = BigDecimal.ZERO;
		BigDecimal totalProfitLoss = BigDecimal.ZERO;

		for (Map<String, Object> account : accounts) {
			try {
				// Get accountIdKey for portfolio fetch
				String accountIdKey = (String) account.get("accountIdKey");
				if (accountIdKey == null) {
					continue;
				}

				// Fetch portfolio for this account
				Map<String, Object> portfolioData = etradeClient.getPortfolio(accessToken, accessTokenSecret,
						accountIdKey);

				// Extract positions from portfolio response
				// E*TRADE returns: { "PortfolioResponse": { "AccountPortfolio": [...] } }
				List<Map<String, Object>> positions = extractPositionsFromPortfolio(portfolioData);

				for (Map<String, Object> position : positions) {
					try {
						ProviderHoldingsEntity.Holding holding = transformPosition(position);
						if (holding != null) {
							holdings.add(holding);

							if (holding.getCurrentValue() != null) {
								totalValue = totalValue.add(holding.getCurrentValue());
							}
							if (holding.getProfitLoss() != null) {
								totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
							}
						}
					}
					catch (Exception e) {
						log.error("Error transforming E*TRADE position", e);
					}
				}

				// Get account balance
				try {
					Map<String, Object> balanceData = etradeClient.getAccountBalance(accessToken, accessTokenSecret,
							accountIdKey);
					BigDecimal accountCash = extractCashBalance(balanceData);
					if (accountCash != null) {
						cashBalance = cashBalance.add(accountCash);
					}
				}
				catch (Exception e) {
					log.warn("Could not fetch balance for account {}: {}", accountIdKey, e.getMessage());
				}

			}
			catch (Exception e) {
				log.error("Error processing E*TRADE account", e);
			}
		}

		// Add cash as holding if present
		if (cashBalance.compareTo(BigDecimal.ZERO) > 0) {
			ProviderHoldingsEntity.Holding cashHolding = new ProviderHoldingsEntity.Holding();
			cashHolding.setAsset("CASH");
			cashHolding.setName("Cash & Sweep");
			cashHolding.setAssetType("cash");
			cashHolding.setQuantity(BigDecimal.ONE);
			cashHolding.setCurrentPrice(cashBalance);
			cashHolding.setCurrentValue(cashBalance);
			cashHolding.setCostBasis(cashBalance);
			cashHolding.setProfitLoss(BigDecimal.ZERO);
			cashHolding.setProfitLossPercent(BigDecimal.ZERO);
			holdings.add(cashHolding);
			totalValue = totalValue.add(cashBalance);
		}

		entity.setHoldings(holdings);
		entity.setTotalValue(totalValue);
		entity.setCashBalance(cashBalance);
		entity.setTotalProfitLoss(totalProfitLoss);

		// Calculate total P&L percent
		BigDecimal totalCostBasis = holdings.stream()
			.map(ProviderHoldingsEntity.Holding::getCostBasis)
			.filter(cb -> cb != null)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal totalProfitLossPercent = totalProfitLoss.divide(totalCostBasis, 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal("100"));
			entity.setTotalProfitLossPercent(totalProfitLossPercent);
		}

		return entity;
	}

	/**
	 * Extract positions list from E*TRADE portfolio response.
	 */
	private List<Map<String, Object>> extractPositionsFromPortfolio(Map<String, Object> portfolioData) {
		List<Map<String, Object>> positions = new ArrayList<>();

		if (portfolioData == null) {
			return positions;
		}

		// E*TRADE structure: { "PortfolioResponse": { "AccountPortfolio": [ {
		// "Position": [...] } ] } }
		try {
			Map<String, Object> portfolioResponse = (Map<String, Object>) portfolioData.get("PortfolioResponse");
			if (portfolioResponse == null) {
				return positions;
			}

			Object accountPortfolio = portfolioResponse.get("AccountPortfolio");
			List<Map<String, Object>> accountPortfolios;

			if (accountPortfolio instanceof List) {
				accountPortfolios = (List<Map<String, Object>>) accountPortfolio;
			}
			else if (accountPortfolio instanceof Map) {
				accountPortfolios = List.of((Map<String, Object>) accountPortfolio);
			}
			else {
				return positions;
			}

			for (Map<String, Object> portfolio : accountPortfolios) {
				Object positionData = portfolio.get("Position");
				if (positionData instanceof List) {
					positions.addAll((List<Map<String, Object>>) positionData);
				}
				else if (positionData instanceof Map) {
					positions.add((Map<String, Object>) positionData);
				}
			}
		}
		catch (Exception e) {
			log.warn("Error extracting positions from portfolio: {}", e.getMessage());
		}

		return positions;
	}

	/**
	 * Transform a single E*TRADE position to Holding.
	 */
	private ProviderHoldingsEntity.Holding transformPosition(Map<String, Object> position) {
		// E*TRADE position structure has Product (symbol info) and various fields
		Map<String, Object> product = (Map<String, Object>) position.get("Product");
		if (product == null) {
			return null;
		}

		String symbol = (String) product.get("symbol");
		if (symbol == null) {
			return null;
		}

		BigDecimal quantity = extractBigDecimal(position, "quantity");
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}

		BigDecimal marketValue = extractBigDecimal(position, "marketValue");
		BigDecimal currentPrice = extractBigDecimal(position, "lastTrade");
		BigDecimal costBasis = extractBigDecimal(position, "totalCost");
		BigDecimal pricePaid = extractBigDecimal(position, "pricePaid"); // Average buy price
		BigDecimal totalGain = extractBigDecimal(position, "totalGain");
		BigDecimal totalGainPct = extractBigDecimal(position, "totalGainPct");

		ProviderHoldingsEntity.Holding holding = new ProviderHoldingsEntity.Holding();
		holding.setAsset(symbol);
		holding.setName((String) product.get("securityType"));
		holding.setQuantity(quantity);
		holding.setCurrentPrice(currentPrice);
		holding.setCurrentValue(marketValue);
		holding.setCostBasis(costBasis);
		holding.setAverageBuyPrice(pricePaid);
		holding.setProfitLoss(totalGain);
		holding.setProfitLossPercent(totalGainPct);
		holding.setOriginalSymbol(symbol);
		holding.setIsStaked(false);

		// Set asset type
		String securityType = (String) product.get("securityType");
		if (securityType != null) {
			holding.setAssetType(securityType.toLowerCase());
		}
		else {
			holding.setAssetType("equity");
		}

		return holding;
	}

	/**
	 * Extract cash balance from E*TRADE balance response.
	 */
	private BigDecimal extractCashBalance(Map<String, Object> balanceData) {
		if (balanceData == null) {
			return null;
		}

		// E*TRADE structure: { "BalanceResponse": { "Computed": { "cashAvailableForInvestment": ... } } }
		try {
			Map<String, Object> balanceResponse = (Map<String, Object>) balanceData.get("BalanceResponse");
			if (balanceResponse == null) {
				return null;
			}

			Map<String, Object> computed = (Map<String, Object>) balanceResponse.get("Computed");
			if (computed == null) {
				return null;
			}

			return extractBigDecimal(computed, "cashAvailableForInvestment");
		}
		catch (Exception e) {
			log.warn("Error extracting cash balance: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Update provider summary document.
	 */
	private void updateProviderSummary(String userId, ProviderHoldingsEntity holdings) {
		try {
			Optional<PortfolioProviderEntity> providerOpt = portfolioProviderRepository
				.findByUserIdAndProviderId(userId, PROVIDER_ID);

			if (providerOpt.isPresent()) {
				PortfolioProviderEntity provider = providerOpt.get();

				provider.setTotalValue(holdings.getTotalValue());
				provider.setDayChange(holdings.getDayChange());
				provider.setDayChangePercent(holdings.getDayChangePercent());
				provider.setCashBalance(holdings.getCashBalance());
				provider.setHoldingsCount(holdings.getHoldings() != null ? holdings.getHoldings().size() : 0);
				provider.setLastSyncedAt(Instant.now());
				provider.setSyncStatus("success");

				portfolioProviderRepository.save(provider, userId);
				log.debug("Updated E*TRADE provider summary for user: {}", userId);
			}

			// Refresh portfolio summary
			if (portfolioSummaryManager != null) {
				try {
					portfolioSummaryManager.refreshPortfolioSummary(userId);
				}
				catch (Exception e) {
					log.warn("Failed to refresh portfolio summary for user {}: {}", userId, e.getMessage());
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to update provider summary for user {}: {}", userId, e.getMessage());
		}
	}

	/**
	 * Extract BigDecimal from map.
	 */
	private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof Number) {
			return new BigDecimal(value.toString());
		}
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

	private String generateSecureState(String userId) {
		return userId + "-" + UUID.randomUUID().toString();
	}

	// ProviderIntegrationHandler interface implementation

	@Override
	public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
		log.info("Testing E*TRADE connection for user: {}", userId);
		// E*TRADE uses OAuth, cannot test with API keys
		log.info("E*TRADE requires OAuth flow - cannot test with API keys");
		return true;
	}

	@Override
	public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
		log.info("Creating E*TRADE integration for user: {}", userId);

		try {
			// Create provider entity
			PortfolioProviderEntity entity = new PortfolioProviderEntity(PROVIDER_ID, "oauth", userId);
			entity.setProviderName(PROVIDER_NAME);
			entity.setProviderType(PROVIDER_TYPE);
			entity.setProviderCategory(PROVIDER_CATEGORY);
			entity.setStatus("disconnected"); // Not enabled until OAuth is complete

			portfolioProviderRepository.save(entity, userId);
			log.info("Created pending E*TRADE provider for user: {}", userId);

			// Generate OAuth URL
			String state = generateSecureState(userId);
			String authUrl = startOAuthFlow(userId, state);
			log.info("Generated E*TRADE OAuth URL for user: {}", userId);

			// Build result
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("oauthUrl", authUrl);
			metadata.put("state", state);
			metadata.put("oauthRequired", true);
			metadata.put("authMethod", "oauth1a");

			ProviderIntegrationResult result = new ProviderIntegrationResult();
			result.setSuccess(true);
			result.setMessage("E*TRADE OAuth URL generated successfully");
			result.setMetadata(metadata);
			return result;

		}
		catch (Exception e) {
			log.error("Error creating E*TRADE integration for user: {}", userId, e);
			throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
					"Failed to create E*TRADE integration: " + e.getMessage());
		}
	}

	@Override
	public String getProviderId() {
		return PROVIDER_ID;
	}

}
