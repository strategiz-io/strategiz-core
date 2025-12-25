package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for ProviderIntegration entities using Firestore
 * Stores provider integrations as subcollection under users/{userId}/provider_integrations
 */
@Repository
public class ProviderIntegrationBaseRepository extends BaseRepository<ProviderIntegrationEntity> {
    
    private static final Logger log = LoggerFactory.getLogger(ProviderIntegrationBaseRepository.class);
    
    public ProviderIntegrationBaseRepository(Firestore firestore) {
        super(firestore, ProviderIntegrationEntity.class);
    }
    
    /**
     * Get user-scoped collection for provider integrations
     * Returns users/{userId}/provider_integrations
     */
    protected CollectionReference getUserScopedCollection(String userId) {
        return firestore.collection("users").document(userId).collection("provider_integrations");
    }
    
    /**
     * Override save to use user-scoped collection
     */
    @Override
    public ProviderIntegrationEntity save(ProviderIntegrationEntity entity, String userId) {
        try {
            validateInputs(entity, userId);
            
            // Always use user-scoped collection for provider integrations
            boolean isCreate = (entity.getId() == null || entity.getId().isEmpty());
            
            if (isCreate) {
                if (entity.getId() == null || entity.getId().isEmpty()) {
                    entity.setId(getUserScopedCollection(userId).document().getId());
                }
                if (!entity._hasAudit()) {
                    entity._initAudit(userId);
                }
            } else {
                if (!entity._hasAudit()) {
                    throw new ProviderIntegrationException(DataProviderErrorDetails.AUDIT_FIELDS_MISSING, "ProviderIntegrationEntity");
                }
                entity._updateAudit(userId);
            }
            
            // Log the status value before saving
            log.debug("Saving ProviderIntegrationEntity with status field value: {}", entity.getStatus());
            
            // Save to user-scoped collection
            getUserScopedCollection(userId).document(entity.getId()).set(entity).get();

            return entity;
        } catch (ProviderIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "ProviderIntegrationEntity");
        }
    }

    /**
     * Override findById to use user-scoped collection
     */
    public Optional<ProviderIntegrationEntity> findById(String id, String userId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(id).get().get();
            if (doc.exists()) {
                ProviderIntegrationEntity entity = doc.toObject(ProviderIntegrationEntity.class);
                if (entity != null) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderIntegrationEntity", id);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderIntegrationEntity", id);
        }
    }

    /**
     * Find all provider integrations for a user
     */
    public List<ProviderIntegrationEntity> findAllByUserId(String userId) {
        try {
            log.debug("ProviderIntegrationBaseRepository: Finding all providers for userId: {}", userId);

            // Query for both uppercase (legacy) and lowercase (new standard)
            // Also filter by isActive=true to exclude soft-deleted integrations
            Query query = getUserScopedCollection(userId)
                .whereIn("status", Arrays.asList("connected", "CONNECTED"))
                .whereEqualTo("isActive", true);
            
            log.debug("ProviderIntegrationBaseRepository: Query path: provider_integrations/users/{}/integrations, filter: status={}", 
                userId, ProviderStatus.CONNECTED.getValue());
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            log.info("ProviderIntegrationBaseRepository: Found {} documents for userId: {}", docs.size(), userId);
            
            List<ProviderIntegrationEntity> results = docs.stream()
                .map(doc -> {
                    ProviderIntegrationEntity entity = doc.toObject(ProviderIntegrationEntity.class);
                    entity.setId(doc.getId());
                    log.debug("ProviderIntegrationBaseRepository: Mapped entity - id: {}, providerId: {}, status: {}", 
                        entity.getId(), entity.getProviderId(), entity.getStatus());
                    return entity;
                })
                .collect(Collectors.toList());
                
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ProviderIntegrationBaseRepository: Failed to find entities for userId {}: {}", userId, e.getMessage());
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderIntegrationEntity", userId);
        } catch (ExecutionException e) {
            log.error("ProviderIntegrationBaseRepository: Failed to find entities for userId {}: {}", userId, e.getMessage());
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "ProviderIntegrationEntity", userId);
        }
    }

    /**
     * Find provider integration by userId and providerId
     */
    public Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId) {
        return findAllByUserId(userId).stream()
            .filter(entity -> providerId.equals(entity.getProviderId()))
            .findFirst();
    }
    
    private void validateInputs(ProviderIntegrationEntity entity, String userId) {
        if (entity == null) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.INVALID_ARGUMENT,
                "ProviderIntegrationEntity", "Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.INVALID_ARGUMENT,
                "ProviderIntegrationEntity", "User ID cannot be null or empty");
        }
    }
}