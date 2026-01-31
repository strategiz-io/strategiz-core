package io.strategiz.service.dashboard.service;

import io.strategiz.service.base.BaseService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.business.portfolio.PortfolioSummaryManager;

// Provider Business Modules
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.alpaca.AlpacaProviderBusiness;
import io.strategiz.business.provider.schwab.SchwabProviderBusiness;
import io.strategiz.business.provider.kraken.business.KrakenProviderBusiness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for refreshing portfolio data from connected exchange/brokerage providers.
 *
 * This service delegates to provider-specific business modules which handle: - Retrieving
 * credentials from Vault - Calling provider APIs to fetch latest data - Storing updated
 * holdings in Firestore
 *
 * Triggered by: - User sign-in (initial data load) - Manual refresh button click -
 * Auto-refresh interval (every 10 minutes from frontend)
 */
@Service
public class PortfolioRefreshService extends BaseService {

	private static final Logger log = LoggerFactory.getLogger(PortfolioRefreshService.class);

	private static final String MODULE_NAME = "service-dashboard";

	// Provider Business Modules - each handles its own API calls and data transformation
	@Autowired(required = false)
	private CoinbaseProviderBusiness coinbaseProviderBusiness;

	@Autowired(required = false)
	private AlpacaProviderBusiness alpacaProviderBusiness;

	@Autowired(required = false)
	private SchwabProviderBusiness schwabProviderBusiness;

	@Autowired(required = false)
	private KrakenProviderBusiness krakenProviderBusiness;

	// Repository to get list of connected providers
	@Autowired(required = false)
	private PortfolioProviderRepository portfolioProviderRepository;

	// Portfolio summary manager to update totals after refresh
	@Autowired(required = false)
	private PortfolioSummaryManager portfolioSummaryManager;

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Refresh portfolio data for user on sign-in. This is the initial data load after
	 * authentication.
	 * @param userId User ID
	 * @param demoMode Whether user is in demo mode
	 * @return Refresh result with status and refreshed providers
	 */
	public Map<String, Object> refreshOnSignIn(String userId, Boolean demoMode) {
		log.info("Refreshing portfolio on sign-in for user: {}, demoMode: {}", userId, demoMode);

		Map<String, Object> result = new HashMap<>();
		result.put("userId", userId);
		result.put("demoMode", demoMode);
		result.put("startTime", Instant.now().toString());

		// Skip refresh if in demo mode
		if (Boolean.TRUE.equals(demoMode)) {
			result.put("status", "skipped");
			result.put("reason", "Demo mode active");
			return result;
		}

		return executeRefresh(userId, result);
	}

	/**
	 * Manually refresh portfolio data for user. Called when user clicks refresh button or
	 * auto-refresh interval triggers.
	 * @param userId User ID
	 * @return Refresh result with status and refreshed providers
	 */
	public Map<String, Object> refreshPortfolio(String userId) {
		log.info("Refreshing portfolio for user: {}", userId);

		Map<String, Object> result = new HashMap<>();
		result.put("userId", userId);
		result.put("startTime", Instant.now().toString());

		return executeRefresh(userId, result);
	}

	/**
	 * Execute the portfolio refresh for all connected providers.
	 */
	private Map<String, Object> executeRefresh(String userId, Map<String, Object> result) {
		try {
			// Get all connected providers for this user
			List<PortfolioProviderEntity> connectedProviders = getConnectedProviders(userId);

			if (connectedProviders.isEmpty()) {
				result.put("status", "skipped");
				result.put("reason", "No connected providers");
				return result;
			}

			log.info("Found {} connected providers for user: {}", connectedProviders.size(), userId);

			// Refresh each provider using its business module
			List<String> refreshedProviders = new ArrayList<>();
			List<String> failedProviders = new ArrayList<>();

			for (PortfolioProviderEntity provider : connectedProviders) {
				String providerId = provider.getProviderId();
				try {
					refreshProvider(userId, providerId);
					refreshedProviders.add(providerId);
					log.info("Successfully refreshed {} for user: {}", providerId, userId);
				}
				catch (Exception e) {
					log.error("Failed to refresh {} for user {}: {}", providerId, userId, e.getMessage());
					failedProviders.add(providerId);
				}
			}

			// Update portfolio summary after all providers are refreshed
			if (!refreshedProviders.isEmpty() && portfolioSummaryManager != null) {
				try {
					portfolioSummaryManager.refreshPortfolioSummary(userId);
					log.info("Updated portfolio summary for user: {}", userId);
				}
				catch (Exception e) {
					log.warn("Failed to update portfolio summary for user {}: {}", userId, e.getMessage());
				}
			}

			result.put("status", "completed");
			result.put("refreshedProviders", refreshedProviders);
			result.put("failedProviders", failedProviders);
			result.put("totalProviders", connectedProviders.size());
			result.put("endTime", Instant.now().toString());

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error during portfolio refresh for user: {}", userId, e);
			throw new StrategizException(ServiceDashboardErrorDetails.SYNC_INITIALIZATION_FAILED, MODULE_NAME, userId,
					e.getMessage());
		}

		return result;
	}

	/**
	 * Refresh a specific provider using its business module. Each business module
	 * handles: - Retrieving credentials from Vault - Calling the provider's API -
	 * Transforming and storing the data in Firestore
	 */
	private void refreshProvider(String userId, String providerId) {
		log.debug("Refreshing provider {} for user: {}", providerId, userId);

		switch (providerId.toLowerCase()) {
			case "coinbase":
				if (coinbaseProviderBusiness != null) {
					coinbaseProviderBusiness.syncProviderData(userId);
				}
				else {
					log.warn("CoinbaseProviderBusiness not available");
				}
				break;

			case "alpaca":
				if (alpacaProviderBusiness != null) {
					alpacaProviderBusiness.syncProviderData(userId);
				}
				else {
					log.warn("AlpacaProviderBusiness not available");
				}
				break;

			case "schwab":
				if (schwabProviderBusiness != null) {
					schwabProviderBusiness.syncProviderData(userId);
				}
				else {
					log.warn("SchwabProviderBusiness not available");
				}
				break;

			case "kraken":
				if (krakenProviderBusiness != null) {
					krakenProviderBusiness.syncProviderData(userId);
				}
				else {
					log.warn("KrakenProviderBusiness not available");
				}
				break;

			default:
				log.warn("Unknown provider: {} - no business module available", providerId);
		}
	}

	/**
	 * Get all connected providers for a user.
	 */
	private List<PortfolioProviderEntity> getConnectedProviders(String userId) {
		try {
			if (portfolioProviderRepository == null) {
				log.warn("PortfolioProviderRepository not available");
				return new ArrayList<>();
			}

			// Get all connected providers (findAllByUserId already filters out
			// disconnected)
			return portfolioProviderRepository.findAllByUserId(userId);

		}
		catch (Exception e) {
			log.error("Error getting connected providers for user: {}", userId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get refresh status for user.
	 * @param userId User ID
	 * @return Current refresh status with last refresh times per provider
	 */
	public Map<String, Object> getRefreshStatus(String userId) {
		Map<String, Object> status = new HashMap<>();

		try {
			List<PortfolioProviderEntity> providers = getConnectedProviders(userId);

			Map<String, Object> providerStatuses = new HashMap<>();
			for (PortfolioProviderEntity provider : providers) {
				Map<String, Object> providerStatus = new HashMap<>();
				providerStatus.put("status", provider.getStatus());
				providerStatus.put("syncStatus", provider.getSyncStatus());
				providerStatus.put("lastRefreshedAt", provider.getLastSyncedAt());
				providerStatuses.put(provider.getProviderId(), providerStatus);
			}

			status.put("providers", providerStatuses);
			status.put("connectedCount", providers.size());
			status.put("status", "success");

		}
		catch (Exception e) {
			log.error("Error getting refresh status for user: {}", userId, e);
			throw new StrategizException(ServiceDashboardErrorDetails.DASHBOARD_ERROR, MODULE_NAME,
					"get-refresh-status", e.getMessage());
		}

		return status;
	}

}
