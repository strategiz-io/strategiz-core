package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.exception.DataProviderErrorDetails;
import io.strategiz.data.provider.exception.ProviderIntegrationException;
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
 * Stores portfolio summary at: users/{userId}/portfolio/summary
 *
 * The summary document lives alongside provider documents in the portfolio collection.
 * Example structure:
 *   users/{userId}/portfolio/summary     ← This document
 *   users/{userId}/portfolio/coinbase    ← Provider document
 *   users/{userId}/portfolio/alpaca      ← Provider document
 */
@Repository
public class PortfolioSummaryBaseRepository extends BaseRepository<PortfolioSummaryEntity> {

    private static final String SUMMARY_DOC_ID = "summary";

    public PortfolioSummaryBaseRepository(Firestore firestore) {
        super(firestore, PortfolioSummaryEntity.class);
    }

    /**
     * Get user-scoped portfolio collection
     * Returns users/{userId}/portfolio
     */
    protected CollectionReference getPortfolioCollection(String userId) {
        return firestore.collection("users").document(userId).collection("portfolio");
    }
    
    /**
     * Override save to use user-scoped collection
     */
    @Override
    public PortfolioSummaryEntity save(PortfolioSummaryEntity entity, String userId) {
        try {
            validateInputs(entity, userId);

            // Always use "summary" as document ID for portfolio summary
            entity.setId(SUMMARY_DOC_ID);

            if (!entity._hasAudit()) {
                entity._initAudit(userId);
            } else {
                entity._updateAudit(userId);
            }

            // Save to portfolio collection with "summary" as document ID
            getPortfolioCollection(userId).document(SUMMARY_DOC_ID).set(entity).get();

            return entity;
        } catch (Exception e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioSummaryEntity");
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

            // Save to portfolio collection
            getPortfolioCollection(userId).document(documentId).set(entity).get();

            return entity;
        } catch (Exception e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_SAVE_FAILED, e, "PortfolioSummaryEntity");
        }
    }

    /**
     * Get current portfolio summary
     */
    public Optional<PortfolioSummaryEntity> findCurrent(String userId) {
        return findById(SUMMARY_DOC_ID, userId);
    }

    /**
     * Override findById to use user-scoped collection
     */
    public Optional<PortfolioSummaryEntity> findById(String id, String userId) {
        try {
            DocumentSnapshot doc = getPortfolioCollection(userId).document(id).get().get();
            if (doc.exists()) {
                PortfolioSummaryEntity entity = doc.toObject(PortfolioSummaryEntity.class);
                if (entity != null) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioSummaryEntity", id);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioSummaryEntity", id);
        }
    }

    /**
     * Find portfolio summaries within date range
     */
    public List<PortfolioSummaryEntity> findByDateRange(String userId, Instant from, Instant to) {
        try {
            Query query = getPortfolioCollection(userId)
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioSummaryEntity", userId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.REPOSITORY_FIND_FAILED, e, "PortfolioSummaryEntity", userId);
        }
    }

    /**
     * Check if current portfolio summary exists
     */
    public boolean currentExists(String userId) {
        try {
            DocumentSnapshot doc = getPortfolioCollection(userId).document(SUMMARY_DOC_ID).get().get();
            return doc.exists();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderIntegrationException(DataProviderErrorDetails.ENTITY_EXISTENCE_CHECK_FAILED, e, "PortfolioSummaryEntity", userId);
        } catch (ExecutionException e) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.ENTITY_EXISTENCE_CHECK_FAILED, e, "PortfolioSummaryEntity", userId);
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
            throw new ProviderIntegrationException(DataProviderErrorDetails.INVALID_ARGUMENT,
                "PortfolioSummaryEntity", "Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new ProviderIntegrationException(DataProviderErrorDetails.INVALID_ARGUMENT,
                "PortfolioSummaryEntity", "User ID cannot be null or empty");
        }
    }
}