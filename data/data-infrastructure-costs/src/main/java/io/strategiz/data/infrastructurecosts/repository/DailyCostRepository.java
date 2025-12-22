package io.strategiz.data.infrastructurecosts.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.infrastructurecosts.entity.DailyCostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for DailyCostEntity stored at infrastructure/costs/daily/{date}
 */
@Repository
public class DailyCostRepository {

    private static final Logger logger = LoggerFactory.getLogger(DailyCostRepository.class);
    private static final String COLLECTION_PATH = "infrastructure";
    private static final String SUBCOLLECTION_COSTS = "costs";
    private static final String SUBCOLLECTION_DAILY = "daily";

    private final Firestore firestore;

    public DailyCostRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getDailyCollection() {
        return firestore.collection(COLLECTION_PATH)
                .document(SUBCOLLECTION_COSTS)
                .collection(SUBCOLLECTION_DAILY);
    }

    /**
     * Save or update a daily cost record
     */
    public DailyCostEntity save(DailyCostEntity entity) {
        try {
            DocumentReference docRef = getDailyCollection().document(entity.getDate());
            docRef.set(entity).get();
            logger.debug("Saved daily cost for date: {}", entity.getDate());
            return entity;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error saving daily cost: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save daily cost", e);
        }
    }

    /**
     * Find daily cost by date
     */
    public Optional<DailyCostEntity> findByDate(String date) {
        try {
            DocumentSnapshot doc = getDailyCollection().document(date).get().get();
            if (doc.exists()) {
                return Optional.ofNullable(doc.toObject(DailyCostEntity.class));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding daily cost by date: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Find daily costs for a date range
     */
    public List<DailyCostEntity> findByDateRange(String startDate, String endDate) {
        try {
            Query query = getDailyCollection()
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .orderBy("date", Query.Direction.DESCENDING);

            QuerySnapshot snapshot = query.get().get();
            List<DailyCostEntity> results = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                DailyCostEntity entity = doc.toObject(DailyCostEntity.class);
                if (entity != null) {
                    results.add(entity);
                }
            }
            return results;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding daily costs by range: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Find the most recent N daily cost records
     */
    public List<DailyCostEntity> findRecent(int limit) {
        try {
            Query query = getDailyCollection()
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(limit);

            QuerySnapshot snapshot = query.get().get();
            List<DailyCostEntity> results = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                DailyCostEntity entity = doc.toObject(DailyCostEntity.class);
                if (entity != null) {
                    results.add(entity);
                }
            }
            return results;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error finding recent daily costs: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Delete a daily cost record
     */
    public void delete(String date) {
        try {
            getDailyCollection().document(date).delete().get();
            logger.debug("Deleted daily cost for date: {}", date);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error deleting daily cost: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
