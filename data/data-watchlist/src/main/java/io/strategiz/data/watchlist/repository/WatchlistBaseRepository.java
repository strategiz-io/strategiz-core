package io.strategiz.data.watchlist.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.watchlist.entity.WatchlistItemEntity;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for WatchlistItem entities using Firestore
 * Stores watchlist items as subcollection under users/{userId}/watchlist
 */
@Repository
public class WatchlistBaseRepository extends BaseRepository<WatchlistItemEntity> {

    private static final Logger log = LoggerFactory.getLogger(WatchlistBaseRepository.class);

    public WatchlistBaseRepository(Firestore firestore) {
        super(firestore, WatchlistItemEntity.class);
    }

    /**
     * Get user-scoped collection for watchlist items
     * Returns users/{userId}/watchlist
     */
    @Override
    protected CollectionReference getUserScopedCollection(String userId) {
        return firestore.collection("users").document(userId).collection("watchlist");
    }

    /**
     * Override save to use user-scoped collection
     */
    @Override
    public WatchlistItemEntity save(WatchlistItemEntity entity, String userId) {
        try {
            validateInputs(entity, userId);

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
                    throw new DataRepositoryException(DataRepositoryErrorDetails.AUDIT_FIELDS_MISSING, "WatchlistItemEntity");
                }
                entity._updateAudit(userId);
            }

            // Save to user-scoped collection
            getUserScopedCollection(userId).document(entity.getId()).set(entity).get();

            log.debug("Saved watchlist item {} for user {}", entity.getSymbol(), userId);
            return entity;
        } catch (DataRepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "WatchlistItemEntity");
        }
    }

    /**
     * Find by ID in user's collection
     */
    public Optional<WatchlistItemEntity> findById(String id, String userId) {
        try {
            DocumentSnapshot doc = getUserScopedCollection(userId).document(id).get().get();
            if (doc.exists()) {
                WatchlistItemEntity entity = doc.toObject(WatchlistItemEntity.class);
                if (entity != null) {
                    entity.setId(doc.getId());
                    return Optional.of(entity);
                }
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", id);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "WatchlistItemEntity", id);
        }
    }

    /**
     * Find all watchlist items for a user
     */
    public List<WatchlistItemEntity> findAllByUserId(String userId) {
        try {
            log.debug("Finding all watchlist items for userId: {}", userId);

            List<QueryDocumentSnapshot> docs = getUserScopedCollection(userId).get().get().getDocuments();

            log.info("Found {} watchlist items for userId: {}", docs.size(), userId);

            return docs.stream()
                .map(doc -> {
                    WatchlistItemEntity entity = doc.toObject(WatchlistItemEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to find watchlist items for userId {}: {}", userId, e.getMessage());
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", userId);
        } catch (ExecutionException e) {
            log.error("Failed to find watchlist items for userId {}: {}", userId, e.getMessage());
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "WatchlistItemEntity", userId);
        }
    }

    /**
     * Check if symbol exists in user's watchlist
     */
    public boolean existsBySymbol(String symbol, String userId) {
        try {
            Query query = getUserScopedCollection(userId)
                .whereEqualTo("symbol", symbol.toUpperCase());

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            return !docs.isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", symbol);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "WatchlistItemEntity", symbol);
        }
    }

    /**
     * Find watchlist item by symbol for a user
     */
    public Optional<WatchlistItemEntity> findBySymbol(String symbol, String userId) {
        try {
            Query query = getUserScopedCollection(userId)
                .whereEqualTo("symbol", symbol.toUpperCase());

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (!docs.isEmpty()) {
                WatchlistItemEntity entity = docs.get(0).toObject(WatchlistItemEntity.class);
                entity.setId(docs.get(0).getId());
                return Optional.of(entity);
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", symbol);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "WatchlistItemEntity", symbol);
        }
    }

    /**
     * Delete watchlist item
     */
    public boolean delete(String id, String userId) {
        try {
            getUserScopedCollection(userId).document(id).delete().get();
            log.debug("Deleted watchlist item {} for user {}", id, userId);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", id);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_DELETE_FAILED, e, "WatchlistItemEntity", id);
        }
    }

    /**
     * Find items by type for a user
     */
    public List<WatchlistItemEntity> findByType(String type, String userId) {
        try {
            Query query = getUserScopedCollection(userId)
                .whereEqualTo("type", type.toUpperCase());

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    WatchlistItemEntity entity = doc.toObject(WatchlistItemEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "WatchlistItemEntity", type);
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "WatchlistItemEntity", type);
        }
    }

    private void validateInputs(WatchlistItemEntity entity, String userId) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }
}
