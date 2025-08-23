package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateProviderIntegrationRepository using BaseRepository
 */
@Repository
public class CreateProviderIntegrationRepositoryImpl implements CreateProviderIntegrationRepository {
    
    private final ProviderIntegrationBaseRepository baseRepository;
    
    @Autowired
    public CreateProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public ProviderIntegrationEntity create(ProviderIntegrationEntity integration) {
        // DO NOT set ID here - let BaseRepository handle it as a create operation
        // The BaseRepository will generate the ID when it detects a null ID
        
        // Set default status if not provided
        if (integration.getStatus() == null || integration.getStatus().isEmpty()) {
            integration.setStatus("pending");
        }
        
        // Set default enabled state
        if (!integration.isEnabled()) {
            integration.setEnabled(true);
        }
        
        // Use BaseRepository's save method (requires userId)
        // Since we're not setting an ID, BaseRepository will treat this as a create operation
        return baseRepository.save(integration, integration.getUserId());
    }
    
    @Override
    public ProviderIntegrationEntity createForUser(ProviderIntegrationEntity integration, String userId) {
        integration.setUserId(userId);
        return create(integration);
    }
}