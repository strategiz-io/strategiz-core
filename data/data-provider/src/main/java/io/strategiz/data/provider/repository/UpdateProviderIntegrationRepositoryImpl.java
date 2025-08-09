package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of UpdateProviderIntegrationRepository using BaseRepository
 */
@Repository
public class UpdateProviderIntegrationRepositoryImpl implements UpdateProviderIntegrationRepository {
    
    private final ProviderIntegrationBaseRepository baseRepository;
    private final ReadProviderIntegrationRepository readRepository;
    
    @Autowired
    public UpdateProviderIntegrationRepositoryImpl(
            ProviderIntegrationBaseRepository baseRepository,
            ReadProviderIntegrationRepository readRepository) {
        this.baseRepository = baseRepository;
        this.readRepository = readRepository;
    }
    
    @Override
    public ProviderIntegrationEntity update(ProviderIntegrationEntity integration) {
        // Use the userId from the entity for audit
        return baseRepository.save(integration, integration.getUserId());
    }
    
    @Override
    public ProviderIntegrationEntity updateWithUserId(ProviderIntegrationEntity integration, String userId) {
        return baseRepository.save(integration, userId);
    }
    
    @Override
    public boolean updateStatus(String userId, String providerId, String status) {
        Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
        if (entity.isPresent()) {
            ProviderIntegrationEntity integration = entity.get();
            integration.setStatus(status);
            baseRepository.save(integration, userId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean updateEnabled(String userId, String providerId, boolean enabled) {
        Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
        if (entity.isPresent()) {
            ProviderIntegrationEntity integration = entity.get();
            integration.setEnabled(enabled);
            baseRepository.save(integration, userId);
            return true;
        }
        return false;
    }
}