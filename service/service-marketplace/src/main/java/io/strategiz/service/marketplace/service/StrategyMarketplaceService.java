package io.strategiz.service.marketplace.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for the Strategy Marketplace
 * Handles business logic related to creating, listing, purchasing, and applying strategies
 */
@Service
public class StrategyMarketplaceService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    /**
     * List all public strategies in the marketplace
     * 
     * @param category Optional category filter
     * @param sortBy Optional sort field
     * @param limit Maximum number of results to return
     * @return List of strategies
     */
    public List<Map<String, Object>> listPublicStrategies(String category, String sortBy, int limit) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<Map<String, Object>> strategies = new ArrayList<>();
            
            // Query for public strategies
            QuerySnapshot querySnapshot = firestore.collection("strategies")
                .whereEqualTo("isPublic", true)
                .limit(limit)
                .get()
                .get();
            
            querySnapshot.getDocuments().forEach(doc -> {
                Map<String, Object> strategy = doc.getData();
                strategy.put("id", doc.getId());
                strategies.add(strategy);
            });
            
            return strategies;
        } catch (Exception e) {
            log.error("Error listing strategies", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", e);
        }
    }
    
    /**
     * Get a specific strategy by ID
     * 
     * @param id Strategy ID
     * @return Strategy data
     */
    public Map<String, Object> getStrategy(String id) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentSnapshot doc = firestore.collection("strategies").document(id).get().get();
            
            if (!doc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            Map<String, Object> strategy = doc.getData();
            strategy.put("id", doc.getId());

            return strategy;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", e, id);
        }
    }
    
    /**
     * Create a new strategy
     * 
     * @param userId User ID of the creator
     * @param strategyData Strategy data
     * @return Created strategy with ID
     */
    public Map<String, Object> createStrategy(String userId, Map<String, Object> strategyData) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Add metadata to strategy
            strategyData.put("createdBy", userId);
            strategyData.put("createdAt", FieldValue.serverTimestamp());
            strategyData.put("updatedAt", FieldValue.serverTimestamp());
            strategyData.put("isPublic", strategyData.getOrDefault("isPublic", false));
            
            // Create strategy document
            DocumentReference docRef = firestore.collection("strategies").document();
            docRef.set(strategyData).get();
            
            // Return created strategy with ID
            Map<String, Object> response = new HashMap<>(strategyData);
            response.put("id", docRef.getId());
            
            return response;
        } catch (Exception e) {
            log.error("Error creating strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_CREATE_FAILED, "service-marketplace", e);
        }
    }
    
    /**
     * Update an existing strategy
     * 
     * @param id Strategy ID
     * @param userId User ID of the updater
     * @param strategyData Updated strategy data
     * @return Updated strategy
     */
    public Map<String, Object> updateStrategy(String id, String userId, Map<String, Object> strategyData) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentReference docRef = firestore.collection("strategies").document(id);
            DocumentSnapshot doc = docRef.get().get();
            
            // Check if strategy exists
            if (!doc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Check if user is the owner
            String createdBy = (String) doc.getData().get("createdBy");
            if (!userId.equals(createdBy)) {
                throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_UPDATE, "service-marketplace", id);
            }

            // Update metadata
            strategyData.put("updatedAt", FieldValue.serverTimestamp());

            // Update strategy
            docRef.set(strategyData, SetOptions.merge()).get();

            // Return updated strategy
            Map<String, Object> response = new HashMap<>(strategyData);
            response.put("id", id);

            return response;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_UPDATE_FAILED, "service-marketplace", e, id);
        }
    }
    
    /**
     * Delete a strategy
     * 
     * @param id Strategy ID
     * @param userId User ID of the deleter
     * @return Success message
     */
    public Map<String, String> deleteStrategy(String id, String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentReference docRef = firestore.collection("strategies").document(id);
            DocumentSnapshot doc = docRef.get().get();
            
            // Check if strategy exists
            if (!doc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Check if user is the owner
            String createdBy = (String) doc.getData().get("createdBy");
            if (!userId.equals(createdBy)) {
                throw new StrategizException(MarketplaceErrorDetails.UNAUTHORIZED_DELETE, "service-marketplace", id);
            }

            // Delete strategy
            docRef.delete().get();

            return Map.of("message", "Strategy deleted successfully");
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_DELETE_FAILED, "service-marketplace", e, id);
        }
    }
    
    /**
     * Purchase a strategy
     * 
     * @param id Strategy ID
     * @param userId User ID of the purchaser
     * @return Purchase details
     */
    public Map<String, Object> purchaseStrategy(String id, String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentReference strategyRef = firestore.collection("strategies").document(id);
            DocumentSnapshot strategyDoc = strategyRef.get().get();
            
            // Check if strategy exists
            if (!strategyDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Check if strategy is public
            Boolean isPublic = (Boolean) strategyDoc.getData().get("isPublic");
            if (isPublic == null || !isPublic) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_AVAILABLE, "service-marketplace", id);
            }

            // Create purchase record
            String purchaseId = UUID.randomUUID().toString();
            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("userId", userId);
            purchaseData.put("strategyId", id);
            purchaseData.put("purchaseDate", FieldValue.serverTimestamp());
            purchaseData.put("status", "completed");

            // Create transaction record
            DocumentReference purchaseRef = firestore.collection("purchases").document(purchaseId);
            purchaseRef.set(purchaseData).get();

            // Add strategy to user's purchased strategies
            DocumentReference userRef = firestore.collection("users").document(userId);
            userRef.update("purchasedStrategies", FieldValue.arrayUnion(id)).get();

            return Map.of(
                "message", "Strategy purchased successfully",
                "purchaseId", purchaseId
            );
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error purchasing strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.PURCHASE_FAILED, "service-marketplace", e, id);
        }
    }
    
    /**
     * Apply a strategy to a user's portfolio
     * 
     * @param id Strategy ID
     * @param userId User ID
     * @param applicationData Application data
     * @return Application details
     */
    public Map<String, Object> applyStrategy(String id, String userId, Map<String, Object> applicationData) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Check if strategy exists
            DocumentReference strategyRef = firestore.collection("strategies").document(id);
            DocumentSnapshot strategyDoc = strategyRef.get().get();
            if (!strategyDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Check if user has purchased the strategy or is the creator
            DocumentReference userRef = firestore.collection("users").document(userId);
            DocumentSnapshot userDoc = userRef.get().get();

            if (!userDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.USER_NOT_FOUND, "service-marketplace", userId);
            }

            String createdBy = (String) strategyDoc.getData().get("createdBy");
            List<String> purchasedStrategies = (List<String>) userDoc.getData().get("purchasedStrategies");

            if (!userId.equals(createdBy) && (purchasedStrategies == null || !purchasedStrategies.contains(id))) {
                throw new StrategizException(MarketplaceErrorDetails.PURCHASE_REQUIRED, "service-marketplace", id);
            }

            // Create application record
            String applicationId = UUID.randomUUID().toString();
            Map<String, Object> application = new HashMap<>(applicationData);
            application.put("userId", userId);
            application.put("strategyId", id);
            application.put("createdAt", FieldValue.serverTimestamp());
            application.put("status", "pending");

            DocumentReference applicationRef = firestore.collection("strategyApplications").document(applicationId);
            applicationRef.set(application).get();

            // Return application details
            Map<String, Object> response = new HashMap<>(application);
            response.put("id", applicationId);

            return response;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error applying strategy", e);
            throw new StrategizException(MarketplaceErrorDetails.APPLY_FAILED, "service-marketplace", e, id);
        }
    }
    
    /**
     * Get user's purchased strategies
     * 
     * @param userId User ID
     * @return List of purchased strategies
     */
    public List<Map<String, Object>> getUserPurchases(String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Get user document
            DocumentReference userRef = firestore.collection("users").document(userId);
            DocumentSnapshot userDoc = userRef.get().get();
            
            if (!userDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.USER_NOT_FOUND, "service-marketplace", userId);
            }

            // Get purchased strategies
            List<String> purchasedStrategyIds = (List<String>) userDoc.getData().get("purchasedStrategies");
            if (purchasedStrategyIds == null || purchasedStrategyIds.isEmpty()) {
                return List.of();
            }

            // Fetch strategy details
            List<Map<String, Object>> purchasedStrategies = new ArrayList<>();
            for (String strategyId : purchasedStrategyIds) {
                DocumentSnapshot strategyDoc = firestore.collection("strategies").document(strategyId).get().get();
                if (strategyDoc.exists()) {
                    Map<String, Object> strategy = strategyDoc.getData();
                    strategy.put("id", strategyDoc.getId());
                    purchasedStrategies.add(strategy);
                }
            }

            return purchasedStrategies;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user purchases", e);
            throw new StrategizException(MarketplaceErrorDetails.PURCHASES_RETRIEVAL_FAILED, "service-marketplace", e, userId);
        }
    }
    
    /**
     * Get strategies created by the user
     * 
     * @param userId User ID
     * @return List of created strategies
     */
    public List<Map<String, Object>> getUserStrategies(String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<Map<String, Object>> strategies = new ArrayList<>();
            
            // Query for strategies created by the user
            QuerySnapshot querySnapshot = firestore.collection("strategies")
                .whereEqualTo("createdBy", userId)
                .get()
                .get();
            
            querySnapshot.getDocuments().forEach(doc -> {
                Map<String, Object> strategy = doc.getData();
                strategy.put("id", doc.getId());
                strategies.add(strategy);
            });
            
            return strategies;
        } catch (Exception e) {
            log.error("Error getting user strategies", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", e, userId);
        }
    }
}
