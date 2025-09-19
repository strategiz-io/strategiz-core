package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CreateProviderDataRepository using BaseRepository
 */
@Repository
public class CreateProviderDataRepositoryImpl implements CreateProviderDataRepository {
    
    private final ProviderDataBaseRepository baseRepository;
    
    @Autowired
    public CreateProviderDataRepositoryImpl(ProviderDataBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public ProviderDataEntity createProviderData(String userId, String providerId, ProviderDataEntity data) {
        // Set the provider ID in the entity
        data.setProviderId(providerId);
        
        // Use providerId as document ID for easy lookup
        return baseRepository.saveWithProviderId(data, userId, providerId);
    }
    
    @Override
    public ProviderDataEntity createOrReplaceProviderData(String userId, String providerId, ProviderDataEntity data) {
        // Set the provider ID in the entity
        data.setProviderId(providerId);
        
        // This will overwrite if exists or create if not
        return baseRepository.saveWithProviderId(data, userId, providerId);
    }
}