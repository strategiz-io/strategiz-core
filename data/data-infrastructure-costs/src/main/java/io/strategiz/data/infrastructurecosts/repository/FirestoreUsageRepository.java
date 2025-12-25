package io.strategiz.data.infrastructurecosts.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.infrastructurecosts.entity.FirestoreUsageEntity;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for FirestoreUsageEntity stored at infrastructure/usage/firestore/{date}
 */
@Repository
public class FirestoreUsageRepository {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreUsageRepository.class);
    private static final String COLLECTION_PATH = "infrastructure";
    private static final String SUBCOLLECTION_USAGE = "usage";
    private static final String SUBCOLLECTION_FIRESTORE = "firestore";

    private final Firestore firestore;

    public FirestoreUsageRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getFirestoreUsageCollection() {
        return firestore.collection(COLLECTION_PATH)
                .document(SUBCOLLECTION_USAGE)
                .collection(SUBCOLLECTION_FIRESTORE);
    }

    /**
     * Save or update a Firestore usage record
     */
    public FirestoreUsageEntity save(FirestoreUsageEntity entity) {
        try {
            DocumentReference docRef = getFirestoreUsageCollection().document(entity.getDate());
            docRef.set(entity).get();
            logger.debug("Saved Firestore usage for date: {}", entity.getDate());
            return entity;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error saving Firestore usage: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_SAVE_FAILED, e, "FirestoreUsageEntity");
        }
    }

    /**
     * Find Firestore usage by date
     */
    public Optional<FirestoreUsageEntity> findByDate(String date) {
        try {
            DocumentSnapshot doc = getFirestoreUsageCollection().document(date).get().get();
            if (doc.exists()) {
                return Optional.ofNullable(doc.toObject(FirestoreUsageEntity.class));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding Firestore usage by date: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Find Firestore usage for a date range
     */
    public List<FirestoreUsageEntity> findByDateRange(String startDate, String endDate) {
        try {
            Query query = getFirestoreUsageCollection()
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .orderBy("date", Query.Direction.DESCENDING);

            QuerySnapshot snapshot = query.get().get();
            List<FirestoreUsageEntity> results = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                FirestoreUsageEntity entity = doc.toObject(FirestoreUsageEntity.class);
                if (entity != null) {
                    results.add(entity);
                }
            }
            return results;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding Firestore usage by range: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Find the most recent N Firestore usage records
     */
    public List<FirestoreUsageEntity> findRecent(int limit) {
        try {
            Query query = getFirestoreUsageCollection()
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(limit);

            QuerySnapshot snapshot = query.get().get();
            List<FirestoreUsageEntity> results = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                FirestoreUsageEntity entity = doc.toObject(FirestoreUsageEntity.class);
                if (entity != null) {
                    results.add(entity);
                }
            }
            return results;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding recent Firestore usage: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Get or create today's usage record
     */
    public FirestoreUsageEntity getOrCreateForDate(String date) {
        return findByDate(date).orElseGet(() -> {
            FirestoreUsageEntity entity = new FirestoreUsageEntity(date);
            entity.setReadsByCollection(new HashMap<>());
            entity.setWritesByCollection(new HashMap<>());
            entity.setDeletesByCollection(new HashMap<>());
            return save(entity);
        });
    }

    /**
     * Increment read count for a collection
     */
    public void incrementReads(String date, String collection, long count) {
        FirestoreUsageEntity entity = getOrCreateForDate(date);
        entity.incrementReads(collection, count);
        save(entity);
    }

    /**
     * Increment write count for a collection
     */
    public void incrementWrites(String date, String collection, long count) {
        FirestoreUsageEntity entity = getOrCreateForDate(date);
        entity.incrementWrites(collection, count);
        save(entity);
    }
}
