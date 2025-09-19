package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation of ReadProviderDataRepository using BaseRepository
 */
@Repository
public class ReadProviderDataRepositoryImpl implements ReadProviderDataRepository {
    
    private final ProviderDataBaseRepository baseRepository;
    
    @Autowired
    public ReadProviderDataRepositoryImpl(ProviderDataBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public ProviderDataEntity getProviderData(String userId, String providerId) {
        return baseRepository.findByProviderId(userId, providerId).orElse(null);
    }
    
    @Override
    public List<ProviderDataEntity> getAllProviderData(String userId) {
        return baseRepository.findAllByUserId(userId);
    }
    
    @Override
    public List<ProviderDataEntity> getProviderDataByType(String userId, String accountType) {
        return baseRepository.findByAccountType(userId, accountType);
    }
    
    @Override
    public boolean providerDataExists(String userId, String providerId) {
        return baseRepository.existsByProviderId(userId, providerId);
    }
}