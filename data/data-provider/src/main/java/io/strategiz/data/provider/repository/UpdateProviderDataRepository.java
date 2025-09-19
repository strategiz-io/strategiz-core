package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Repository interface for updating provider data in Firestore
 */
public interface UpdateProviderDataRepository {

    /**
     * Update provider data
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param data Updated provider data
     * @return Updated provider data entity
     */
    ProviderDataEntity updateProviderData(String userId, String providerId, ProviderDataEntity data);

    /**
     * Update provider balances only
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param balances Updated balance data
     * @return Updated provider data entity
     */
    ProviderDataEntity updateProviderBalances(String userId, String providerId, Map<String, Object> balances);

    /**
     * Update provider sync status
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param syncStatus Sync status (success, error, syncing)
     * @param errorMessage Error message if status is error
     * @return Updated provider data entity
     */
    ProviderDataEntity updateSyncStatus(String userId, String providerId, String syncStatus, String errorMessage);

    /**
     * Update provider total value
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param totalValue New total value
     * @return Updated provider data entity
     */
    ProviderDataEntity updateTotalValue(String userId, String providerId, BigDecimal totalValue);

    /**
     * Update last sync timestamp
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param timestamp Last sync timestamp
     * @return Updated provider data entity
     */
    ProviderDataEntity updateLastSyncTime(String userId, String providerId, Instant timestamp);
}