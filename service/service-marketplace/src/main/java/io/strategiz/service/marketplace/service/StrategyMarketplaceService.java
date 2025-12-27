package io.strategiz.service.marketplace.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.client.stripe.StripeService;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPerformance;
import io.strategiz.data.strategy.entity.StrategyPricing;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Autowired
    private StripeService stripeService;

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
     * @param featured Filter for featured strategies only
     * @return List of strategies with creator information
     */
    public List<Map<String, Object>> listPublicStrategies(String category, String sortBy, int limit, Boolean featured) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<Map<String, Object>> strategies = new ArrayList<>();

            // Build query for published strategies (where publishedAt is not null)
            Query query = firestore.collection("strategies")
                .whereNotEqualTo("publishedAt", null);

            // Add category filter if specified
            if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                query = query.whereEqualTo("category", category);
            }

            // Add featured filter if specified
            if (featured != null && featured) {
                query = query.whereEqualTo("isFeatured", true);
            }

            // Add sorting
            if (sortBy != null) {
                switch (sortBy) {
                    case "popular":
                        query = query.orderBy("deploymentCount", Query.Direction.DESCENDING);
                        break;
                    case "newest":
                        query = query.orderBy("publishedAt", Query.Direction.DESCENDING);
                        break;
                    case "rating":
                        query = query.orderBy("averageRating", Query.Direction.DESCENDING);
                        break;
                    default:
                        query = query.orderBy("publishedAt", Query.Direction.DESCENDING);
                }
            }

            // Apply limit
            query = query.limit(limit);

            // Execute query
            QuerySnapshot querySnapshot = query.get().get();

            // Convert each document to marketplace strategy format
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Strategy strategy = doc.toObject(Strategy.class);
                if (strategy != null) {
                    Map<String, Object> strategyMap = convertToMarketplaceStrategy(strategy, firestore);
                    strategies.add(strategyMap);
                }
            }

            return strategies;
        } catch (Exception e) {
            log.error("Error listing strategies", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", e);
        }
    }

    /**
     * Convert Strategy entity to marketplace response format with creator information
     */
    private Map<String, Object> convertToMarketplaceStrategy(Strategy strategy, Firestore firestore) throws ExecutionException, InterruptedException {
        Map<String, Object> result = new HashMap<>();

        // Basic fields
        result.put("id", strategy.getId());
        result.put("name", strategy.getName());
        result.put("description", strategy.getDescription());
        result.put("creatorId", strategy.getUserId());

        // Fetch creator information from User entity
        try {
            DocumentSnapshot userDoc = firestore.collection("users").document(strategy.getUserId()).get().get();
            if (userDoc.exists()) {
                UserEntity user = userDoc.toObject(UserEntity.class);
                if (user != null && user.getProfile() != null) {
                    UserProfileEntity profile = user.getProfile();
                    result.put("creatorName", profile.getName());
                    result.put("creatorEmail", profile.getEmail());
                    result.put("creatorPhotoURL", profile.getPhotoURL());
                } else {
                    // Fallback if user profile not found
                    result.put("creatorName", "Unknown");
                    result.put("creatorEmail", "");
                    result.put("creatorPhotoURL", null);
                }
            } else {
                // Fallback if user not found
                result.put("creatorName", "Unknown");
                result.put("creatorEmail", "");
                result.put("creatorPhotoURL", null);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch creator info for strategy " + strategy.getId(), e);
            result.put("creatorName", "Unknown");
            result.put("creatorEmail", "");
            result.put("creatorPhotoURL", null);
        }

        // Pricing information
        StrategyPricing pricing = strategy.getPricing();
        if (pricing != null) {
            result.put("pricingModel", pricing.getPricingType().toString());
            result.put("price", pricing.getOneTimePrice() != null ? pricing.getOneTimePrice().doubleValue() : null);
            result.put("monthlyPrice", pricing.getMonthlyPrice() != null ? pricing.getMonthlyPrice().doubleValue() : null);
            result.put("currency", pricing.getCurrency());
        } else {
            // Default to FREE if no pricing set
            result.put("pricingModel", "FREE");
            result.put("price", null);
            result.put("monthlyPrice", null);
            result.put("currency", "USD");
        }

        // Metadata
        result.put("tags", strategy.getTags() != null ? strategy.getTags() : new ArrayList<>());
        result.put("category", strategy.getCategory());

        // Stats
        result.put("deploymentCount", strategy.getDeploymentCount() != null ? strategy.getDeploymentCount() : 0);
        result.put("rating", strategy.getAverageRating());
        result.put("reviewCount", strategy.getReviewCount() != null ? strategy.getReviewCount() : 0);

        // Performance metrics
        StrategyPerformance performance = strategy.getPerformance();
        if (performance != null && performance.hasData()) {
            Map<String, Object> perfMap = new HashMap<>();
            perfMap.put("winRate", performance.getWinRate());
            perfMap.put("totalReturn", performance.getTotalReturn());
            perfMap.put("profitFactor", performance.getProfitFactor());
            perfMap.put("sharpeRatio", performance.getSharpeRatio());
            perfMap.put("maxDrawdown", performance.getMaxDrawdown());
            perfMap.put("lastUpdated", performance.getLastTestedAt()); // Maps to frontend's lastUpdated
            result.put("performance", perfMap);
        } else {
            result.put("performance", null);
        }

        // Status
        result.put("isPublished", strategy.isPublished());
        result.put("publishedAt", strategy.getPublishedAt() != null ? strategy.getPublishedAt().toString() : null);
        result.put("createdAt", strategy.getCreatedDate() != null ? strategy.getCreatedDate().toString() : null);
        result.put("updatedAt", strategy.getModifiedDate() != null ? strategy.getModifiedDate().toString() : null);

        // Badges
        result.put("isBestSeller", strategy.getIsBestSeller() != null ? strategy.getIsBestSeller() : false);
        result.put("isTrending", strategy.getIsTrending() != null ? strategy.getIsTrending() : false);
        result.put("isNew", strategy.getIsNew() != null ? strategy.getIsNew() : false);
        result.put("isFeatured", strategy.getIsFeatured() != null ? strategy.getIsFeatured() : false);

        return result;
    }
    
    /**
     * Get a specific strategy by ID
     *
     * @param id Strategy ID
     * @return Strategy data with creator information
     */
    public Map<String, Object> getStrategy(String id) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentSnapshot doc = firestore.collection("strategies").document(id).get().get();

            if (!doc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            Strategy strategy = doc.toObject(Strategy.class);
            if (strategy == null) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", id);
            }

            // Convert to marketplace format with creator info
            return convertToMarketplaceStrategy(strategy, firestore);
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
     * Create Stripe checkout session for strategy purchase
     *
     * @param id Strategy ID
     * @param userId User ID
     * @return Checkout session details
     */
    public Map<String, String> createStrategyCheckout(String id, String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentReference strategyRef = firestore.collection("strategies").document(id);
            DocumentSnapshot strategyDoc = strategyRef.get().get();

            // Check if strategy exists
            if (!strategyDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            Strategy strategy = strategyDoc.toObject(Strategy.class);
            if (strategy == null) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", id);
            }

            // Check if strategy is published
            if (!strategy.isPublished()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_AVAILABLE, "service-marketplace", id);
            }

            // Check if strategy has pricing
            StrategyPricing pricing = strategy.getPricing();
            if (pricing == null || pricing.isFree()) {
                throw new StrategizException(MarketplaceErrorDetails.INVALID_PRICING, "service-marketplace",
                        "Strategy is free, use purchaseStrategy instead");
            }

            // Only ONE_TIME pricing supported for checkout
            if (pricing.getPricingType() != io.strategiz.data.strategy.entity.PricingType.ONE_TIME) {
                throw new StrategizException(MarketplaceErrorDetails.INVALID_PRICING, "service-marketplace",
                        "Only ONE_TIME pricing supported for checkout. Use subscription endpoint for SUBSCRIPTION pricing.");
            }

            // Get price in cents for Stripe
            BigDecimal price = pricing.getOneTimePrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new StrategizException(MarketplaceErrorDetails.INVALID_PRICING, "service-marketplace",
                        "Invalid price for strategy");
            }

            long priceInCents = price.multiply(BigDecimal.valueOf(100)).longValue();
            String currency = pricing.getCurrency() != null ? pricing.getCurrency() : "USD";

            // Fetch user email from user document
            String userEmail = null;
            try {
                DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
                if (userDoc.exists()) {
                    UserEntity user = userDoc.toObject(UserEntity.class);
                    if (user != null && user.getProfile() != null) {
                        userEmail = user.getProfile().getEmail();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user email for checkout", e);
            }

            if (userEmail == null || userEmail.isEmpty()) {
                throw new StrategizException(MarketplaceErrorDetails.USER_NOT_FOUND, "service-marketplace",
                        "User email not found for checkout");
            }

            // Create checkout session
            StripeService.CheckoutResult result = stripeService.createStrategyCheckoutSession(
                    userId,
                    userEmail,
                    id,
                    strategy.getName(),
                    priceInCents,
                    currency,
                    null // Let Stripe service create customer if needed
            );

            return Map.of(
                    "sessionId", result.sessionId(),
                    "checkoutUrl", result.url()
            );
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating checkout session", e);
            throw new StrategizException(MarketplaceErrorDetails.CHECKOUT_SESSION_CREATION_FAILED, "service-marketplace", e, id);
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
     * @return List of purchased strategies with creator information
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
                    Strategy strategy = strategyDoc.toObject(Strategy.class);
                    if (strategy != null) {
                        Map<String, Object> strategyMap = convertToMarketplaceStrategy(strategy, firestore);
                        purchasedStrategies.add(strategyMap);
                    }
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
