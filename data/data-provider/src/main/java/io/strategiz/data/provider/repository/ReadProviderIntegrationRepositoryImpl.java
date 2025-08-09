package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadProviderIntegrationRepository using BaseRepository
 */
@Repository
public class ReadProviderIntegrationRepositoryImpl implements ReadProviderIntegrationRepository {
    
    private final ProviderIntegrationBaseRepository baseRepository;
    
    @Autowired
    public ReadProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public Optional<ProviderIntegrationEntity> findById(String id) {
        return baseRepository.findById(id);
    }
    
    @Override
    public List<ProviderIntegrationEntity> findByUserId(String userId) {
        return baseRepository.findAllByUserId(userId);
    }
    
    @Override
    public Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId) {
        return baseRepository.findByUserIdAndProviderId(userId, providerId);
    }
    
    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndEnabledTrue(String userId) {
        return baseRepository.findAllByUserId(userId).stream()
            .filter(entity -> entity.isConnected()) // Use isConnected() method from entity
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndStatus(String userId, String status) {
        return baseRepository.findAllByUserId(userId).stream()
            .filter(entity -> status.equals(entity.getStatus()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndProviderType(String userId, String providerType) {
        return baseRepository.findAllByUserId(userId).stream()
            .filter(entity -> providerType.equals(entity.getProviderType()))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByUserIdAndProviderId(String userId, String providerId) {
        return findByUserIdAndProviderId(userId, providerId).isPresent();
    }
}