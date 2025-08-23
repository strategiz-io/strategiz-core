package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

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
                    throw new RuntimeException("Cannot update entity without audit fields");
                }
                entity._updateAudit(userId);
            }
            
            // Save to user-scoped collection
            getUserScopedCollection(userId).document(entity.getId()).set(entity).get();
            
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save ProviderIntegrationEntity: " + e.getMessage(), e);
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
                if (entity != null && Boolean.TRUE.equals(entity.getIsActive())) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find entity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find all provider integrations for a user
     */
    public List<ProviderIntegrationEntity> findAllByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = getUserScopedCollection(userId)
                .whereEqualTo("auditFields.isActive", true)
                .get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    ProviderIntegrationEntity entity = doc.toObject(ProviderIntegrationEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find entities: " + e.getMessage(), e);
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
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}