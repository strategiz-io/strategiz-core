package io.strategiz.data.provider.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.provider.entity.PortfolioHistoryEntity;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for PortfolioHistory entities using Firestore.
 * Extends BaseRepository for standard CRUD operations with audit support.
 *
 * Stores portfolio history as subcollection under users/{userId}/portfolio_history.
 * Each snapshot is stored with document ID as date in YYYY-MM-DD format.
 */
@Repository
public class PortfolioHistoryBaseRepository extends BaseRepository<PortfolioHistoryEntity> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PortfolioHistoryBaseRepository(Firestore firestore) {
        super(firestore, PortfolioHistoryEntity.class);
    }

    /**
     * Override to use user-scoped collection for portfolio history.
     * Returns users/{userId}/portfolio_history
     */
    @Override
    protected CollectionReference getUserScopedCollection(String userId) {
        return firestore.collection("users").document(userId).collection("portfolio_history");
    }

    /**
     * Save portfolio history using date-based document ID.
     * Overrides to set document ID from snapshotDate before saving.
     */
    @Override
    public PortfolioHistoryEntity save(PortfolioHistoryEntity entity, String userId) {
        // Use date as document ID (YYYY-MM-DD format)
        if (entity.getSnapshotDate() != null && (entity.getId() == null || entity.getId().isEmpty())) {
            String documentId = entity.getSnapshotDate().format(DATE_FORMATTER);
            entity.setId(documentId);
        }

        if (entity.getId() == null || entity.getId().isEmpty()) {
            throw new IllegalArgumentException("PortfolioHistoryEntity must have snapshotDate or id set");
        }

        // Delegate to base implementation for audit handling
        return super.save(entity, userId);
    }

    /**
     * Find a history snapshot by user ID and date.
     */
    public Optional<PortfolioHistoryEntity> findByUserIdAndDate(String userId, LocalDate date) {
        try {
            String documentId = date.format(DATE_FORMATTER);
            return findByIdInUserScope(documentId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find PortfolioHistoryEntity by date: " + e.getMessage(), e);
        }
    }

    /**
     * Find by ID in user-scoped collection.
     */
    public Optional<PortfolioHistoryEntity> findByIdInUserScope(String id, String userId) {
        try {
            var document = getUserScopedCollection(userId).document(id).get().get();
            if (!document.exists()) {
                return Optional.empty();
            }
            PortfolioHistoryEntity entity = document.toObject(PortfolioHistoryEntity.class);
            if (entity != null) {
                entity.setId(document.getId());
            }
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find PortfolioHistoryEntity by id: " + e.getMessage(), e);
        }
    }

    /**
     * Find all history snapshots for a user.
     */
    public List<PortfolioHistoryEntity> findByUserId(String userId) {
        try {
            Query query = getUserScopedCollection(userId)
                    .whereEqualTo("isActive", true)
                    .orderBy("snapshotDate", Query.Direction.DESCENDING);

            return query.get().get().getDocuments()
                    .stream()
                    .map(doc -> {
                        PortfolioHistoryEntity entity = doc.toObject(PortfolioHistoryEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to find PortfolioHistoryEntity list: " + e.getMessage(), e);
        }
    }

    /**
     * Find history snapshots for a user within a date range.
     */
    public List<PortfolioHistoryEntity> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        try {
            Query query = getUserScopedCollection(userId)
                    .whereEqualTo("isActive", true)
                    .whereGreaterThanOrEqualTo("snapshotDate", startDate)
                    .whereLessThanOrEqualTo("snapshotDate", endDate)
                    .orderBy("snapshotDate", Query.Direction.DESCENDING);

            return query.get().get().getDocuments()
                    .stream()
                    .map(doc -> {
                        PortfolioHistoryEntity entity = doc.toObject(PortfolioHistoryEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to find PortfolioHistoryEntity by date range: " + e.getMessage(), e);
        }
    }

    /**
     * Find the most recent history snapshot for a user.
     */
    public Optional<PortfolioHistoryEntity> findLatestByUserId(String userId) {
        try {
            Query query = getUserScopedCollection(userId)
                    .whereEqualTo("isActive", true)
                    .orderBy("snapshotDate", Query.Direction.DESCENDING)
                    .limit(1);

            List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

            if (documents.isEmpty()) {
                return Optional.empty();
            }

            PortfolioHistoryEntity entity = documents.get(0).toObject(PortfolioHistoryEntity.class);
            entity.setId(documents.get(0).getId());
            return Optional.of(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find latest PortfolioHistoryEntity: " + e.getMessage(), e);
        }
    }

    /**
     * Find the last N history snapshots for a user.
     */
    public List<PortfolioHistoryEntity> findRecentByUserId(String userId, int limit) {
        try {
            Query query = getUserScopedCollection(userId)
                    .whereEqualTo("isActive", true)
                    .orderBy("snapshotDate", Query.Direction.DESCENDING)
                    .limit(limit);

            return query.get().get().getDocuments()
                    .stream()
                    .map(doc -> {
                        PortfolioHistoryEntity entity = doc.toObject(PortfolioHistoryEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to find recent PortfolioHistoryEntity list: " + e.getMessage(), e);
        }
    }

    /**
     * Delete by ID in user-scoped collection (soft delete via base class).
     */
    public boolean deleteByIdInUserScope(String id, String userId) {
        Optional<PortfolioHistoryEntity> entity = findByIdInUserScope(id, userId);
        if (entity.isPresent()) {
            entity.get()._softDelete(userId);
            save(entity.get(), userId);
            return true;
        }
        return false;
    }

    /**
     * Delete all history snapshots for a user (soft delete).
     */
    public int deleteAllByUserId(String userId) {
        List<PortfolioHistoryEntity> entities = findByUserId(userId);
        int deleted = 0;
        for (PortfolioHistoryEntity entity : entities) {
            entity._softDelete(userId);
            save(entity, userId);
            deleted++;
        }
        return deleted;
    }
}
