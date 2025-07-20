package io.strategiz.client.firebase.repository.auth;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.auth.entity.ProviderIntegrationEntity;
import io.strategiz.data.auth.repository.ProviderIntegrationRepository;
import io.strategiz.data.base.repository.SubcollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of ProviderIntegrationRepository
 * Stores provider integrations in users/{userId}/provider_integrations subcollection
 */
@Repository
public class ProviderIntegrationRepositoryImpl extends SubcollectionRepository<ProviderIntegrationEntity> 
        implements ProviderIntegrationRepository {

    private static final Logger log = LoggerFactory.getLogger(ProviderIntegrationRepositoryImpl.class);
    private static final String COLLECTION_NAME = "provider_integrations";

    @Autowired
    public ProviderIntegrationRepositoryImpl(Firestore firestore) {
        super(firestore, ProviderIntegrationEntity.class);
    }

    @Override
    protected String getParentCollectionName() {
        return "users";
    }

    @Override
    protected String getSubcollectionName() {
        return COLLECTION_NAME;
    }

    @Override
    public List<ProviderIntegrationEntity> findByUserId(String userId) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(ProviderIntegrationEntity::isEnabled)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding provider integrations for user: {}", userId, e);
            throw new RuntimeException("Failed to find provider integrations", e);
        }
    }

    @Override
    public Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> providerId.equals(entity.getProviderId()))
                    .findFirst();
        } catch (Exception e) {
            log.error("Error finding provider integration for user: {}, provider: {}", userId, providerId, e);
            throw new RuntimeException("Failed to find provider integration", e);
        }
    }

    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndIsEnabledTrue(String userId) {
        return findByUserId(userId); // Already filters by isEnabled
    }

    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndStatus(String userId, String status) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> status.equals(entity.getStatus()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding provider integrations by status for user: {}, status: {}", userId, status, e);
            throw new RuntimeException("Failed to find provider integrations by status", e);
        }
    }

    @Override
    public List<ProviderIntegrationEntity> findByUserIdAndProviderType(String userId, String providerType) {
        try {
            return findAllInSubcollection(userId).stream()
                    .filter(entity -> providerType.equals(entity.getProviderType()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding provider integrations by type for user: {}, type: {}", userId, providerType, e);
            throw new RuntimeException("Failed to find provider integrations by type", e);
        }
    }

    @Override
    public boolean existsByUserIdAndProviderId(String userId, String providerId) {
        return findByUserIdAndProviderId(userId, providerId).isPresent();
    }

    @Override
    public ProviderIntegrationEntity saveForUser(String userId, ProviderIntegrationEntity entity) {
        // Use the standardized subcollection save method from base class
        // The userId parameter acts as both the parent document ID and the audit user
        return saveInSubcollection(userId, entity, userId);
    }

    @Override
    public boolean deleteByUserIdAndProviderId(String userId, String providerId) {
        Optional<ProviderIntegrationEntity> entity = findByUserIdAndProviderId(userId, providerId);
        if (entity.isPresent()) {
            return deleteInSubcollection(userId, entity.get().getId(), userId);
        }
        return false;
    }

    @Override
    public boolean updateStatusByUserIdAndProviderId(String userId, String providerId, String status) {
        Optional<ProviderIntegrationEntity> entity = findByUserIdAndProviderId(userId, providerId);
        if (entity.isPresent()) {
            ProviderIntegrationEntity integration = entity.get();
            integration.setStatus(status);
            saveInSubcollection(userId, integration, userId);
            return true;
        }
        return false;
    }
}