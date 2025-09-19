package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for PortfolioSummary entities using Firestore
 * Stores portfolio summary as subcollection under users/{userId}/portfolio_summary
 * Current summary is always stored with document ID "current"
 */
@Repository
public class PortfolioSummaryBaseRepository extends BaseRepository<PortfolioSummaryEntity> {
    
    private static final String CURRENT_DOC_ID = "current";
    
    public PortfolioSummaryBaseRepository(Firestore firestore) {
        super(firestore, PortfolioSummaryEntity.class);
    }
    
    /**
     * Get user-scoped collection for portfolio summary
     * Returns users/{userId}/portfolio_summary
     */
    protected CollectionReference getUserScopedCollection(String userId) {
        return firestore.collection("users").document(userId).collection("portfolio_summary");
    }
    
    /**
     * Override save to use user-scoped collection
     */
    @Override
    public PortfolioSummaryEntity save(PortfolioSummaryEntity entity, String userId) {
        try {
            validateInputs(entity, userId);
            
            // Always use "current" as document ID for portfolio summary
            entity.setId(CURRENT_DOC_ID);
            
            if (!entity._hasAudit()) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }
            
            // Save to user-scoped collection with "current" as document ID
            getUserScopedCollection(userId).document(CURRENT_DOC_ID).set(entity).get();
            
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save PortfolioSummaryEntity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save portfolio summary with specific document ID (for historical data)
     */
    public PortfolioSummaryEntity saveWithDocumentId(PortfolioSummaryEntity entity, String userId, String documentId) {
        try {
            validateInputs(entity, userId);
            
            // Set the document ID
            entity.setId(documentId);
            
            if (!entity._hasAudit()) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }
            
            // Save to user-scoped collection
            getUserScopedCollection(userId).document(documentId).set(entity).get();
            
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save PortfolioSummaryEntity: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current portfolio summary
     */
    public Optional<PortfolioSummaryEntity> findCurrent(String userId) {
        return findById(CURRENT_DOC_ID, userId);
    }
    
    /**
     * Override findById to use user-scoped collection
     */
    public Optional<PortfolioSummaryEntity> findById(String id, String userId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(id).get().get();
            if (doc.exists()) {
                PortfolioSummaryEntity entity = doc.toObject(PortfolioSummaryEntity.class);
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
     * Find portfolio summaries within date range
     */
    public List<PortfolioSummaryEntity> findByDateRange(String userId, Instant from, Instant to) {
        try {
            Query query = getUserScopedCollection(userId)
                .whereGreaterThanOrEqualTo("last_synced_at", from)
                .whereLessThanOrEqualTo("last_synced_at", to)
                .orderBy("last_synced_at", Query.Direction.DESCENDING);
            
            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            
            return docs.stream()
                .map(doc -> {
                    PortfolioSummaryEntity entity = doc.toObject(PortfolioSummaryEntity.class);
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
     * Check if current portfolio summary exists
     */
    public boolean currentExists(String userId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(CURRENT_DOC_ID).get().get();
            return doc.exists();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to check entity existence: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get last sync time from current portfolio summary
     */
    public Instant getLastSyncTime(String userId) {
        Optional<PortfolioSummaryEntity> current = findCurrent(userId);
        return current.map(PortfolioSummaryEntity::getLastSyncedAt).orElse(null);
    }
    
    private void validateInputs(PortfolioSummaryEntity entity, String userId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}