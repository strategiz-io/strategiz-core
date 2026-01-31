package io.strategiz.service.provider.service;

import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.kraken.business.KrakenProviderBusiness;
import io.strategiz.business.provider.schwab.SchwabProviderBusiness;
import io.strategiz.business.provider.etrade.EtradeProviderBusiness;
// import io.strategiz.business.provider.webull.business.WebullProviderBusiness; // TODO: Add when webull module is ready
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.PortfolioInsightsCacheRepository;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling provider-specific synchronization. Syncs data from individual
 * providers (e.g., Coinbase, Kraken) without affecting others.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class SyncProviderService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-provider";
	}

	private final CoinbaseProviderBusiness coinbaseProviderBusiness;

	private final KrakenProviderBusiness krakenProviderBusiness;

	private final SchwabProviderBusiness schwabProviderBusiness;

	private final EtradeProviderBusiness etradeProviderBusiness;

	// private final WebullProviderBusiness webullProviderBusiness; // TODO: Add when
	// webull module is ready
	private final PortfolioSummaryManager portfolioSummaryManager;

	private final PortfolioInsightsCacheRepository insightsCacheRepository;

	@Autowired
	public SyncProviderService(CoinbaseProviderBusiness coinbaseProviderBusiness,
			KrakenProviderBusiness krakenProviderBusiness, SchwabProviderBusiness schwabProviderBusiness,
			EtradeProviderBusiness etradeProviderBusiness,
			// WebullProviderBusiness webullProviderBusiness, // TODO: Add when webull
			// module is ready
			PortfolioSummaryManager portfolioSummaryManager, PortfolioInsightsCacheRepository insightsCacheRepository) {
		this.coinbaseProviderBusiness = coinbaseProviderBusiness;
		this.krakenProviderBusiness = krakenProviderBusiness;
		this.schwabProviderBusiness = schwabProviderBusiness;
		this.etradeProviderBusiness = etradeProviderBusiness;
		// this.webullProviderBusiness = webullProviderBusiness; // TODO: Add when webull
		// module is ready
		this.portfolioSummaryManager = portfolioSummaryManager;
		this.insightsCacheRepository = insightsCacheRepository;
	}

	/**
	 * Sync a specific provider's data. Fetches latest data from the provider and updates
	 * Firestore.
	 * @param userId The user ID
	 * @param providerId The provider to sync (e.g., "coinbase", "kraken")
	 * @return Map containing sync result with synced data info
	 */
	public Map<String, Object> syncProvider(String userId, String providerId) {
		log.info("Syncing provider {} for user: {}", providerId, userId);

		try {
			ProviderHoldingsEntity syncedData = null;

			// Route to appropriate provider business logic
			switch (providerId.toLowerCase()) {
				case "coinbase":
					syncedData = coinbaseProviderBusiness.syncProviderData(userId);
					break;

				case "kraken":
					syncedData = krakenProviderBusiness.syncProviderData(userId);
					break;

				case "schwab":
					syncedData = schwabProviderBusiness.syncProviderData(userId);
					break;

				case "etrade":
					syncedData = etradeProviderBusiness.syncProviderData(userId);
					break;

				case "webull":
					// TODO: Implement Webull sync when module is ready
					throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED, getModuleName(),
							providerId, "Webull sync not yet implemented");

				case "alpaca":
					// TODO: Implement Alpaca sync when ready
					throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED, getModuleName(),
							providerId, "Alpaca sync not yet implemented");

				default:
					throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_FOUND, getModuleName(),
							providerId);
			}

			// Build response
			Map<String, Object> result = new HashMap<>();
			if (syncedData != null) {
				result.put("providerId", providerId);
				result.put("holdingsCount", syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);
				result.put("totalValue", syncedData.getTotalValue());
				result.put("lastSynced", Instant.now().toEpochMilli());
				result.put("syncStatus", "success");
			}
			else {
				result.put("providerId", providerId);
				result.put("syncStatus", "no_data");
				result.put("message", "No data returned from provider");
			}

			log.info("Successfully synced provider {} for user {}: {} holdings", providerId, userId,
					syncedData != null && syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);

			// Refresh portfolio summary after provider sync
			portfolioSummaryManager.refreshPortfolioSummary(userId);

			// Invalidate AI insights cache (force regeneration on next request)
			try {
				insightsCacheRepository.invalidateCache(userId);
				log.debug("Invalidated AI insights cache for user {} after {} sync", userId, providerId);
			}
			catch (Exception e) {
				log.warn("Failed to invalidate insights cache for user {}: {}", userId, e.getMessage());
				// Don't fail the sync operation if cache invalidation fails
			}

			return result;

		}
		catch (StrategizException e) {
			log.error("Failed to sync provider {} for user {}: {}", providerId, userId, e.getMessage());
			throw e;

		}
		catch (Exception e) {
			log.error("Unexpected error syncing provider {} for user {}", providerId, userId, e);
			throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED, getModuleName(),
					providerId, e.getMessage());
		}
	}

}
