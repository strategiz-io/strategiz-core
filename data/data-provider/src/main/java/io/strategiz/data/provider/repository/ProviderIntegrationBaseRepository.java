package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for ProviderIntegration entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class ProviderIntegrationBaseRepository extends BaseRepository<ProviderIntegrationEntity> {
    
    public ProviderIntegrationBaseRepository(Firestore firestore) {
        super(firestore, ProviderIntegrationEntity.class);
    }
    
    /**
     * Find provider integrations by userId field
     */
    public java.util.List<ProviderIntegrationEntity> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }
    
    /**
     * Find provider integrations by userId and providerId
     */
    public java.util.Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId) {
        return findByField("userId", userId).stream()
            .filter(entity -> providerId.equals(entity.getProviderId()))
            .findFirst();
    }
}