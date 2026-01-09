package io.strategiz.service.marketplace.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.client.stripe.StripeService;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.entity.StrategyPerformance;
import io.strategiz.data.strategy.entity.StrategyPricing;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.data.strategy.repository.StrategySubscriptionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import io.strategiz.service.marketplace.model.response.StrategyDetailResponse;
import io.strategiz.service.marketplace.model.response.StrategySharePreviewResponse;
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

    @Autowired
    private StrategyAccessService strategyAccessService;

    @Autowired
    private ReadStrategyRepository readStrategyRepository;

    @Autowired
    private StrategySubscriptionRepository subscriptionRepository;

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
     * @param featured Optional featured filter
     * @return List of strategies with creator information
     */
    public List<Map<String, Object>> listPublicStrategies(String category, String sortBy, int limit, Boolean featured) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<Map<String, Object>> strategies = new ArrayList<>();

            // Build query for published strategies
            var query = firestore.collection("strategies")
                .whereNotEqualTo("publishedAt", null);

            // Add category filter if specified
            if (category != null && !category.isEmpty()) {
                query = query.whereEqualTo("category", category);
            }

            // Add featured filter if specified
            if (featured != null && featured) {
                query = query.whereEqualTo("isFeatured", true);
            }

            // Execute query with limit
            QuerySnapshot querySnapshot = query.limit(limit).get().get();

            // Convert each strategy document to marketplace format
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Strategy strategy = doc.toObject(Strategy.class);
                if (strategy != null) {
                    strategy.setId(doc.getId());
                    Map<String, Object> marketplaceStrategy = convertToMarketplaceStrategy(strategy, firestore);
                    strategies.add(marketplaceStrategy);
                }
            }

            return strategies;
        } catch (Exception e) {
            log.error("Error listing strategies", e);
            throw new StrategizException(MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED, "service-marketplace", e);
        }
    }
    
    /**
     * Get a specific strategy by ID with access control.
     *
     * TODO: Refactor to use ReadStrategyRepository instead of direct Firestore access
     * TODO: Use StrategyAccessService.enforceCanView() for proper access control
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

            // TODO: Add proper access control here
            // Should check: publishStatus, publicStatus, and user subscription status
            // For now, returning data without access control (security issue!)

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
     * Get comprehensive strategy details with access control.
     * This method replaces the legacy getStrategy() method with proper access control enforcement.
     *
     * @param id Strategy ID
     * @param userId User ID (can be null for anonymous/public access)
     * @param includeParams Set of optional parameters to include (trades, equityCurve, comments)
     * @return Strategy detail response
     */
    public StrategyDetailResponse getStrategyDetail(String id, String userId, java.util.Set<String> includeParams) {
        try {
            // 1. Fetch strategy from repository
            Strategy strategy = readStrategyRepository.findById(id)
                    .orElseThrow(() -> new StrategizException(
                            MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                            getModuleName(),
                            id));

            // 2. Enforce access control
            strategyAccessService.enforceCanView(id, userId);

            // 3. Build response DTO
            StrategyDetailResponse response = new StrategyDetailResponse();

            // 3a. Basic fields (always included)
            response.setId(strategy.getId());
            response.setName(strategy.getName());
            response.setDescription(strategy.getDescription());
            response.setLanguage(strategy.getLanguage());
            response.setType(strategy.getType());
            response.setCategory(strategy.getCategory());
            response.setTags(strategy.getTags());
            response.setParameters(strategy.getParameters());
            response.setPerformance(strategy.getPerformance());
            response.setIsPublished(strategy.getIsPublished());
            response.setIsPublic(strategy.getIsPublic());
            response.setIsListed(strategy.getIsListed());
            response.setPricing(strategy.getPricing());
            response.setSubscriberCount(strategy.getSubscriberCount());
            response.setCommentCount(strategy.getCommentCount());
            response.setAverageRating(strategy.getAverageRating());
            response.setReviewCount(strategy.getReviewCount());
            response.setDeploymentCount(strategy.getDeploymentCount());
            response.setIsBestSeller(strategy.getIsBestSeller());
            response.setIsTrending(strategy.getIsTrending());
            response.setIsNew(strategy.getIsNew());
            response.setIsFeatured(strategy.getIsFeatured());
            response.setCreatorId(strategy.getCreatorId());
            response.setOwnerId(strategy.getOwnerId());
            response.setCreatedAt(strategy.getCreatedDate() != null ?
                    new java.util.Date(strategy.getCreatedDate().getSeconds() * 1000) : null);
            response.setUpdatedAt(strategy.getModifiedDate() != null ?
                    new java.util.Date(strategy.getModifiedDate().getSeconds() * 1000) : null);

            // 3b. Conditional fields based on access control
            boolean isOwner = strategyAccessService.canViewCode(id, userId);
            if (isOwner) {
                response.setCode(strategy.getCode());
                response.setVisualRules(strategy.getVisualRules());
            }

            // 3c. Set access flags for frontend
            boolean isSubscriber = userId != null &&
                    subscriptionRepository.hasActiveSubscription(userId, strategy.getOwnerId());

            StrategyDetailResponse.AccessFlags accessFlags = new StrategyDetailResponse.AccessFlags(
                    isOwner,
                    isSubscriber,
                    strategyAccessService.canViewCode(id, userId),
                    strategyAccessService.canDeploy(id, userId),
                    strategyAccessService.canEdit(id, userId)
            );
            response.setAccess(accessFlags);

            // 3d. Fetch creator and owner info
            response.setCreator(fetchCreatorInfo(strategy.getCreatorId()));
            if (!strategy.getCreatorId().equals(strategy.getOwnerId())) {
                response.setOwner(fetchCreatorInfo(strategy.getOwnerId()));
            }

            // 3e. Conditionally include large datasets based on query params
            if (includeParams != null) {
                if (includeParams.contains("trades")) {
                    response.setTradeHistory(extractTradeHistory(strategy.getBacktestResults()));
                }
                if (includeParams.contains("equityCurve")) {
                    response.setEquityCurve(extractEquityCurve(strategy.getBacktestResults()));
                }
                // Comments inclusion can be added later when comment service is implemented
            }

            return response;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting strategy detail for id: " + id, e);
            throw new StrategizException(
                    MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED,
                    getModuleName(),
                    e,
                    id);
        }
    }

    /**
     * Get strategy share preview for Open Graph metadata.
     *
     * @param id Strategy ID
     * @param userId User ID (can be null for anonymous)
     * @return Share preview response
     */
    public StrategySharePreviewResponse getStrategySharePreview(String id, String userId) {
        try {
            // 1. Fetch strategy
            Strategy strategy = readStrategyRepository.findById(id)
                    .orElseThrow(() -> new StrategizException(
                            MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                            getModuleName(),
                            "Strategy not found or not accessible"));

            // 2. Check if user can view (return 404 if not accessible to prevent enumeration)
            if (!strategyAccessService.canViewStrategy(id, userId)) {
                throw new StrategizException(
                        MarketplaceErrorDetails.STRATEGY_NOT_FOUND,
                        getModuleName(),
                        "Strategy not found or not accessible");
            }

            // 3. Build preview response
            StrategySharePreviewResponse response = new StrategySharePreviewResponse();
            response.setStrategyId(strategy.getId());
            response.setName(strategy.getName());

            // Truncate description to 200 chars for preview
            String description = strategy.getDescription();
            if (description != null && description.length() > 200) {
                response.setDescription(description.substring(0, 197) + "...");
            } else {
                response.setDescription(description);
            }

            // Build performance summary text
            StrategyPerformance perf = strategy.getPerformance();
            if (perf != null && perf.hasData()) {
                String summary = String.format("%+.1f%% return, %.0f%% win rate",
                        perf.getTotalReturn() != null ? perf.getTotalReturn() : 0.0,
                        perf.getWinRate() != null ? perf.getWinRate() : 0.0);
                response.setPerformanceSummary(summary);
            }

            // TODO: Generate chart thumbnail image URL
            // For now, use placeholder or logo
            response.setThumbnailUrl(null);

            // Set page URL (frontend URL)
            response.setPageUrl("https://strategiz.io/strategy/" + strategy.getId());

            // Fetch creator info
            StrategyDetailResponse.CreatorInfo creatorInfo = fetchCreatorInfo(strategy.getCreatorId());
            StrategySharePreviewResponse.CreatorInfo shareCreatorInfo =
                    new StrategySharePreviewResponse.CreatorInfo(
                            creatorInfo.getName(),
                            creatorInfo.getPhotoURL());
            response.setCreator(shareCreatorInfo);

            return response;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting share preview for strategy: " + id, e);
            throw new StrategizException(
                    MarketplaceErrorDetails.STRATEGY_RETRIEVAL_FAILED,
                    getModuleName(),
                    e,
                    id);
        }
    }

    /**
     * Helper: Fetch creator/owner information from Firestore.
     * TODO: Add caching (15-min TTL) to reduce Firestore reads
     *
     * @param userId User ID
     * @return Creator info
     */
    private StrategyDetailResponse.CreatorInfo fetchCreatorInfo(String userId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();

            if (userDoc.exists()) {
                UserEntity user = userDoc.toObject(UserEntity.class);
                if (user != null && user.getProfile() != null) {
                    UserProfileEntity profile = user.getProfile();
                    return new StrategyDetailResponse.CreatorInfo(
                            userId,
                            profile.getName(),
                            profile.getEmail(),
                            profile.getPhotoURL());
                }
            }

            // Return default if user not found
            return new StrategyDetailResponse.CreatorInfo(userId, "Unknown User", null, null);
        } catch (Exception e) {
            log.warn("Failed to fetch creator info for userId: " + userId, e);
            return new StrategyDetailResponse.CreatorInfo(userId, "Unknown User", null, null);
        }
    }

    /**
     * Helper: Extract trade history from backtest results map.
     *
     * @param backtestResults Backtest results map
     * @return List of trade history items
     */
    private List<StrategyDetailResponse.TradeHistoryItem> extractTradeHistory(
            Map<String, Object> backtestResults) {
        if (backtestResults == null || !backtestResults.containsKey("trades")) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> trades = (List<Map<String, Object>>) backtestResults.get("trades");
            if (trades == null || trades.isEmpty()) {
                return null;
            }

            List<StrategyDetailResponse.TradeHistoryItem> tradeHistory = new ArrayList<>();
            for (Map<String, Object> trade : trades) {
                StrategyDetailResponse.TradeHistoryItem item = new StrategyDetailResponse.TradeHistoryItem();

                // Parse trade data (adjust field names based on actual backtest result structure)
                if (trade.get("entryTime") != null) {
                    item.setEntryTime(new java.util.Date((Long) trade.get("entryTime")));
                }
                if (trade.get("exitTime") != null) {
                    item.setExitTime(new java.util.Date((Long) trade.get("exitTime")));
                }
                item.setDirection((String) trade.get("direction"));
                item.setEntryPrice(getDoubleValue(trade.get("entryPrice")));
                item.setExitPrice(getDoubleValue(trade.get("exitPrice")));
                item.setQuantity(getDoubleValue(trade.get("quantity")));
                item.setPnl(getDoubleValue(trade.get("pnl")));
                item.setPnlPercent(getDoubleValue(trade.get("pnlPercent")));
                item.setSignal((String) trade.get("signal"));

                tradeHistory.add(item);
            }

            return tradeHistory;
        } catch (Exception e) {
            log.warn("Failed to extract trade history from backtest results", e);
            return null;
        }
    }

    /**
     * Helper: Extract equity curve from backtest results map.
     *
     * @param backtestResults Backtest results map
     * @return List of equity curve points
     */
    private List<StrategyDetailResponse.EquityCurvePoint> extractEquityCurve(
            Map<String, Object> backtestResults) {
        if (backtestResults == null || !backtestResults.containsKey("equityCurve")) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> curve = (List<Map<String, Object>>) backtestResults.get("equityCurve");
            if (curve == null || curve.isEmpty()) {
                return null;
            }

            List<StrategyDetailResponse.EquityCurvePoint> equityCurve = new ArrayList<>();
            for (Map<String, Object> point : curve) {
                if (point.get("timestamp") != null) {
                    StrategyDetailResponse.EquityCurvePoint item = new StrategyDetailResponse.EquityCurvePoint(
                            new java.util.Date((Long) point.get("timestamp")),
                            getDoubleValue(point.get("portfolioValue")),
                            getDoubleValue(point.get("returnPercent"))
                    );
                    equityCurve.add(item);
                }
            }

            return equityCurve;
        } catch (Exception e) {
            log.warn("Failed to extract equity curve from backtest results", e);
            return null;
        }
    }

    /**
     * Helper: Safely convert Object to Double.
     */
    private Double getDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
     * TODO: Use StrategyAccessService.enforceCanEdit() for proper access control
     * TODO: Check ownerId instead of createdBy (ownerId can change via ownership transfer)
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
            // TODO: Should check ownerId, not createdBy (createdBy never changes, ownerId can)
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
     * TODO: Use StrategyAccessService.enforceCanDelete() for proper access control
     * TODO: Check ownerId instead of createdBy (ownerId can change via ownership transfer)
     * TODO: Prevent deletion if strategy has active subscribers
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
            // TODO: Should check ownerId, not createdBy (createdBy never changes, ownerId can)
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

            if (!strategyDoc.exists()) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Convert to Strategy entity
            Strategy strategy = strategyDoc.toObject(Strategy.class);
            if (strategy == null) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_FOUND, "service-marketplace", id);
            }

            // Validate strategy is published
            if (!Boolean.TRUE.equals(strategy.getIsPublished())) {
                throw new StrategizException(MarketplaceErrorDetails.STRATEGY_NOT_AVAILABLE, "service-marketplace", id);
            }

            // Validate pricing
            StrategyPricing pricing = strategy.getPricing();
            if (pricing == null || pricing.isFree()) {
                throw new StrategizException(MarketplaceErrorDetails.INVALID_PRICING, "service-marketplace",
                        "Strategy is free, use purchaseStrategy instead");
            }

            // Fetch user email
            String userEmail = null;
            DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
            if (userDoc.exists()) {
                UserEntity user = userDoc.toObject(UserEntity.class);
                if (user != null && user.getProfile() != null) {
                    userEmail = user.getProfile().getEmail();
                }
            }

            // Create Stripe checkout
            long priceInCents = pricing.getOneTimePrice().multiply(BigDecimal.valueOf(100)).longValue();
            StripeService.CheckoutResult result = stripeService.createStrategyCheckoutSession(
                    userId, userEmail, id, strategy.getName(), priceInCents,
                    pricing.getCurrency(), null
            );

            return Map.of("sessionId", result.sessionId(), "checkoutUrl", result.url());
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating checkout session", e);
            throw new StrategizException(MarketplaceErrorDetails.CHECKOUT_SESSION_CREATION_FAILED, "service-marketplace", e, id);
        }
    }

    /**
     * Convert Strategy entity to marketplace response format with creator information
     *
     * @param strategy Strategy entity
     * @param firestore Firestore instance
     * @return Marketplace strategy map
     */
    private Map<String, Object> convertToMarketplaceStrategy(Strategy strategy, Firestore firestore) throws ExecutionException, InterruptedException {
        Map<String, Object> result = new HashMap<>();

        // Basic fields
        result.put("id", strategy.getId());
        result.put("name", strategy.getName());
        result.put("description", strategy.getDescription());
        result.put("creatorId", strategy.getOwnerId());

        // Fetch creator information from User entity
        try {
            DocumentSnapshot userDoc = firestore.collection("users").document(strategy.getOwnerId()).get().get();
            if (userDoc.exists()) {
                UserEntity user = userDoc.toObject(UserEntity.class);
                if (user != null && user.getProfile() != null) {
                    UserProfileEntity profile = user.getProfile();
                    result.put("creatorName", profile.getName());
                    result.put("creatorEmail", profile.getEmail());
                    result.put("creatorPhotoURL", profile.getPhotoURL());
                }
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
            result.put("pricingModel", "FREE");
            result.put("price", null);
            result.put("monthlyPrice", null);
            result.put("currency", "USD");
        }

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
            perfMap.put("lastUpdated", performance.getLastTestedAt());
            result.put("performance", perfMap);
        }

        // Status
        result.put("isPublished", Boolean.TRUE.equals(strategy.getIsPublished()));
        // TODO: publishedAt field removed - track publish timestamp separately if needed
        result.put("createdAt", strategy.getCreatedDate() != null ? strategy.getCreatedDate().toString() : null);
        result.put("updatedAt", strategy.getModifiedDate() != null ? strategy.getModifiedDate().toString() : null);

        // Badges
        result.put("isBestSeller", strategy.getIsBestSeller() != null ? strategy.getIsBestSeller() : false);
        result.put("isTrending", strategy.getIsTrending() != null ? strategy.getIsTrending() : false);
        result.put("isNew", strategy.getIsNew() != null ? strategy.getIsNew() : false);
        result.put("isFeatured", strategy.getIsFeatured() != null ? strategy.getIsFeatured() : false);

        // Category
        result.put("category", strategy.getCategory());

        return result;
    }
}
