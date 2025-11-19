package io.strategiz.service.provider.service;

import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.kraken.business.KrakenProviderBusiness;
import io.strategiz.business.provider.webull.business.WebullProviderBusiness;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling provider-specific synchronization.
 * Syncs data from individual providers (e.g., Coinbase, Kraken) without affecting others.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class SyncProviderService {

    private static final Logger log = LoggerFactory.getLogger(SyncProviderService.class);
    private static final String MODULE_NAME = "service-provider";

    private final CoinbaseProviderBusiness coinbaseProviderBusiness;
    private final KrakenProviderBusiness krakenProviderBusiness;
    private final WebullProviderBusiness webullProviderBusiness;

    @Autowired
    public SyncProviderService(CoinbaseProviderBusiness coinbaseProviderBusiness,
                               KrakenProviderBusiness krakenProviderBusiness,
                               WebullProviderBusiness webullProviderBusiness) {
        this.coinbaseProviderBusiness = coinbaseProviderBusiness;
        this.krakenProviderBusiness = krakenProviderBusiness;
        this.webullProviderBusiness = webullProviderBusiness;
    }

    /**
     * Sync a specific provider's data.
     * Fetches latest data from the provider and updates Firestore.
     *
     * @param userId The user ID
     * @param providerId The provider to sync (e.g., "coinbase", "kraken")
     * @return Map containing sync result with synced data info
     */
    public Map<String, Object> syncProvider(String userId, String providerId) {
        log.info("Syncing provider {} for user: {}", providerId, userId);

        try {
            ProviderDataEntity syncedData = null;

            // Route to appropriate provider business logic
            switch (providerId.toLowerCase()) {
                case "coinbase":
                    syncedData = coinbaseProviderBusiness.syncProviderData(userId);
                    break;

                case "kraken":
                    syncedData = krakenProviderBusiness.syncProviderData(userId);
                    break;

                case "webull":
                    // TODO: Implement Webull sync when ready
                    throw new StrategizException(
                        ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED,
                        MODULE_NAME,
                        providerId,
                        "Webull sync not yet implemented"
                    );

                case "alpaca":
                    // TODO: Implement Alpaca sync when ready
                    throw new StrategizException(
                        ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED,
                        MODULE_NAME,
                        providerId,
                        "Alpaca sync not yet implemented"
                    );

                default:
                    throw new StrategizException(
                        ServiceProviderErrorDetails.PROVIDER_NOT_FOUND,
                        MODULE_NAME,
                        providerId
                    );
            }

            // Build response
            Map<String, Object> result = new HashMap<>();
            if (syncedData != null) {
                result.put("providerId", providerId);
                result.put("holdingsCount", syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);
                result.put("totalValue", syncedData.getTotalValue());
                result.put("lastSynced", Instant.now().toEpochMilli());
                result.put("syncStatus", "success");
            } else {
                result.put("providerId", providerId);
                result.put("syncStatus", "no_data");
                result.put("message", "No data returned from provider");
            }

            log.info("Successfully synced provider {} for user {}: {} holdings",
                providerId, userId,
                syncedData != null && syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);

            return result;

        } catch (StrategizException e) {
            log.error("Failed to sync provider {} for user {}: {}", providerId, userId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error syncing provider {} for user {}", providerId, userId, e);
            throw new StrategizException(
                ServiceProviderErrorDetails.PROVIDER_DATA_SYNC_FAILED,
                MODULE_NAME,
                providerId,
                e.getMessage()
            );
        }
    }
}
