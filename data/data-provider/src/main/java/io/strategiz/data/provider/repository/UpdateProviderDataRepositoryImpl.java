package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Implementation of UpdateProviderDataRepository using BaseRepository
 */
@Repository
public class UpdateProviderDataRepositoryImpl implements UpdateProviderDataRepository {
    
    private final ProviderDataBaseRepository baseRepository;
    private final ReadProviderDataRepository readRepository;
    
    @Autowired
    public UpdateProviderDataRepositoryImpl(
            ProviderDataBaseRepository baseRepository,
            ReadProviderDataRepository readRepository) {
        this.baseRepository = baseRepository;
        this.readRepository = readRepository;
    }
    
    @Override
    public ProviderDataEntity updateProviderData(String userId, String providerId, ProviderDataEntity data) {
        // Ensure the provider ID is set
        data.setProviderId(providerId);
        
        // Set the ID to providerId for consistency
        data.setId(providerId);
        
        // Update the existing data
        return baseRepository.saveWithProviderId(data, userId, providerId);
    }
    
    @Override
    public ProviderDataEntity updateProviderBalances(String userId, String providerId, Map<String, Object> balances) {
        // Get existing data
        ProviderDataEntity existing = readRepository.getProviderData(userId, providerId);
        if (existing == null) {
            throw new RuntimeException("Provider data not found for update: " + providerId);
        }
        
        // Update balances and sync time
        existing.setBalances(balances);
        existing.setLastUpdatedAt(Instant.now());
        
        return baseRepository.saveWithProviderId(existing, userId, providerId);
    }
    
    @Override
    public ProviderDataEntity updateSyncStatus(String userId, String providerId, String syncStatus, String errorMessage) {
        // Get existing data
        ProviderDataEntity existing = readRepository.getProviderData(userId, providerId);
        if (existing == null) {
            throw new RuntimeException("Provider data not found for update: " + providerId);
        }
        
        // Update sync status
        existing.setSyncStatus(syncStatus);
        existing.setErrorMessage(errorMessage);
        existing.setLastUpdatedAt(Instant.now());
        
        return baseRepository.saveWithProviderId(existing, userId, providerId);
    }
    
    @Override
    public ProviderDataEntity updateTotalValue(String userId, String providerId, BigDecimal totalValue) {
        // Get existing data
        ProviderDataEntity existing = readRepository.getProviderData(userId, providerId);
        if (existing == null) {
            throw new RuntimeException("Provider data not found for update: " + providerId);
        }
        
        // Update total value
        existing.setTotalValue(totalValue);
        existing.setLastUpdatedAt(Instant.now());
        
        return baseRepository.saveWithProviderId(existing, userId, providerId);
    }
    
    @Override
    public ProviderDataEntity updateLastSyncTime(String userId, String providerId, Instant timestamp) {
        // Get existing data
        ProviderDataEntity existing = readRepository.getProviderData(userId, providerId);
        if (existing == null) {
            throw new RuntimeException("Provider data not found for update: " + providerId);
        }
        
        // Update last sync time
        existing.setLastUpdatedAt(timestamp);
        
        return baseRepository.saveWithProviderId(existing, userId, providerId);
    }
}