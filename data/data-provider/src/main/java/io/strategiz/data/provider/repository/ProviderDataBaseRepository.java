package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.ProviderDataEntity;
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
 * Base repository for ProviderData entities using Firestore
 * Stores provider data as subcollection under users/{userId}/provider_data
 */
@Repository
public class ProviderDataBaseRepository extends BaseRepository<ProviderDataEntity> {
    
    public ProviderDataBaseRepository(Firestore firestore) {
        super(firestore, ProviderDataEntity.class);
    }
    
    /**
     * Get user-scoped collection for provider data
     * Returns users/{userId}/provider_data
     */
    public CollectionReference getUserScopedCollection(String userId) {
        return firestore.collection("users").document(userId).collection("provider_data");
    }
    
    /**
     * Override save to use user-scoped collection
     */
    @Override
    public ProviderDataEntity save(ProviderDataEntity entity, String userId) {
        try {
            validateInputs(entity, userId);
            
            // Always use user-scoped collection for provider data
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
            throw new RuntimeException("Failed to save ProviderDataEntity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save provider data with specific document ID (providerId)
     */
    public ProviderDataEntity saveWithProviderId(ProviderDataEntity entity, String userId, String providerId) {
        try {
            validateInputs(entity, userId);
            
            // Set the document ID to the providerId for easy lookup
            entity.setId(providerId);
            
            if (!entity._hasAudit()) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }
            
            // Save to user-scoped collection with providerId as document ID
            getUserScopedCollection(userId).document(providerId).set(entity).get();
            
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save ProviderDataEntity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Override findById to use user-scoped collection
     */
    public Optional<ProviderDataEntity> findById(String id, String userId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(id).get().get();
            if (doc.exists()) {
                ProviderDataEntity entity = doc.toObject(ProviderDataEntity.class);
                if (entity != null) {
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
     * Find provider data by providerId (document ID)
     */
    public Optional<ProviderDataEntity> findByProviderId(String userId, String providerId) {
        return findById(providerId, userId);
    }
    
    /**
     * Find all provider data for a user
     */
    public List<ProviderDataEntity> findAllByUserId(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = getUserScopedCollection(userId)
                .get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    ProviderDataEntity entity = doc.toObject(ProviderDataEntity.class);
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
     * Find provider data by account type
     */
    public List<ProviderDataEntity> findByAccountType(String userId, String accountType) {
        try {
            List<QueryDocumentSnapshot> docs = getUserScopedCollection(userId)
                .whereEqualTo("account_type", accountType)
                .get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    ProviderDataEntity entity = doc.toObject(ProviderDataEntity.class);
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
     * Check if provider data exists
     */
    public boolean existsByProviderId(String userId, String providerId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(providerId).get().get();
            return doc.exists();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to check entity existence: " + e.getMessage(), e);
        }
    }
    
    private void validateInputs(ProviderDataEntity entity, String userId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}