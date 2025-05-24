package io.strategiz.strategy.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.strategy.model.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of the StrategyRepository interface
 */
@Repository
public class FirestoreStrategyRepository implements StrategyRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestoreStrategyRepository.class);
    private static final String COLLECTION_PATH = "users";
    private static final String SUBCOLLECTION_PATH = "strategies";

    @Autowired
    private Firestore firestore;

    @Override
    public List<Strategy> findAllByUserId(String userId) {
        try {
            List<Strategy> strategies = new ArrayList<>();
            
            // Query strategies subcollection under the user document
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(SUBCOLLECTION_PATH)
                .get()
                .get();
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Strategy strategy = document.toObject(Strategy.class);
                if (strategy != null) {
                    strategy.setId(document.getId());
                    strategies.add(strategy);
                }
            }
            
            return strategies;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding strategies for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Strategy> findByIdAndUserId(String strategyId, String userId) {
        try {
            DocumentSnapshot document = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(SUBCOLLECTION_PATH)
                .document(strategyId)
                .get()
                .get();
            
            if (!document.exists()) {
                return Optional.empty();
            }
            
            Strategy strategy = document.toObject(Strategy.class);
            if (strategy == null) {
                return Optional.empty();
            }
            
            strategy.setId(document.getId());
            return Optional.of(strategy);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding strategy by id: {} for user: {}", strategyId, userId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Strategy save(Strategy strategy) {
        try {
            DocumentReference docRef;
            
            if (strategy.getId() != null && !strategy.getId().isEmpty()) {
                // Update existing strategy
                docRef = firestore.collection(COLLECTION_PATH)
                    .document(strategy.getUserId())
                    .collection(SUBCOLLECTION_PATH)
                    .document(strategy.getId());
            } else {
                // Create new strategy
                docRef = firestore.collection(COLLECTION_PATH)
                    .document(strategy.getUserId())
                    .collection(SUBCOLLECTION_PATH)
                    .document();
                strategy.setId(docRef.getId());
            }
            
            // Save to Firestore
            ApiFuture<WriteResult> result = docRef.set(strategy);
            result.get(); // Wait for write to complete
            
            return strategy;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving strategy: {}", strategy.getId(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save strategy", e);
        }
    }

    @Override
    public boolean deleteByIdAndUserId(String strategyId, String userId) {
        try {
            ApiFuture<WriteResult> result = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(SUBCOLLECTION_PATH)
                .document(strategyId)
                .delete();
            
            result.get(); // Wait for delete to complete
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting strategy: {} for user: {}", strategyId, userId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean updateStatus(String strategyId, String userId, String status, Object deploymentInfo) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(SUBCOLLECTION_PATH)
                .document(strategyId);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("updatedAt", new Date().toString());
            
            // Add deployment info if provided
            if (deploymentInfo != null) {
                updates.put("deployment", deploymentInfo);
            }
            
            ApiFuture<WriteResult> result = docRef.update(updates);
            result.get(); // Wait for update to complete
            
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error updating strategy status: {} for user: {}", strategyId, userId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
